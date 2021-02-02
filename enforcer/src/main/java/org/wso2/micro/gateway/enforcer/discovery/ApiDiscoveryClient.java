/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.enforcer.discovery;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.api.Api;
import org.wso2.gateway.discovery.service.api.ApiDiscoveryServiceGrpc;
import org.wso2.micro.gateway.enforcer.api.APIFactory;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.exception.DiscoveryException;
import org.wso2.micro.gateway.enforcer.util.GRPCUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with API discovery service at the adapter.
 */
public class ApiDiscoveryClient {
    private static ApiDiscoveryClient instance;
    private final ManagedChannel channel;
    private final ApiDiscoveryServiceGrpc.ApiDiscoveryServiceStub stub;
    private final ApiDiscoveryServiceGrpc.ApiDiscoveryServiceBlockingStub blockingStub;
    private static final Logger logger = LogManager.getLogger(ApiDiscoveryClient.class);
    private final APIFactory apiFactory;
    private StreamObserver<DiscoveryRequest> reqObserver;
    private static final Logger log = LogManager.getLogger(ApiDiscoveryClient.class);
    /**
     * This is a reference to the latest received response from the ADS.
     * <p>
     *     Usage: When ack/nack a DiscoveryResponse this value is used to identify the
     *     latest received DiscoveryResponse which may not have been acked/nacked so far.
     * </p>
     */
    private DiscoveryResponse latestReceived;
    /**
     * This is a reference to the latest acked response from the ADS.
     * <p>
     *     Usage: When nack a DiscoveryResponse this value is used to find the latest
     *     successfully processed DiscoveryResponse. Information sent in the nack request
     *     will contain information about this response value.
     * </p>
     */
    private DiscoveryResponse latestACKed;
    /**
     * Label of this node.
     */
    private final String nodeId;

    private ApiDiscoveryClient(String host, int port) {
        this.channel = GRPCUtils.createSecuredChannel(log, host, port);
        this.apiFactory = APIFactory.getInstance();
        this.stub = ApiDiscoveryServiceGrpc.newStub(channel);
        this.blockingStub = ApiDiscoveryServiceGrpc.newBlockingStub(channel);
        this.nodeId = ConfigHolder.getInstance().getEnvVarConfig().getEnforcerLabel();
        this.latestACKed = DiscoveryResponse.getDefaultInstance();
    }

    public static ApiDiscoveryClient getInstance() {
        if (instance == null) {
            String adsHost = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
            int adsPort = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getAdapterXdsPort());
            instance = new ApiDiscoveryClient(adsHost, adsPort);
        }
        return instance;
    }

    public List<Api> requestInitApis() throws DiscoveryException {
        List<Api> apis;
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(Node.newBuilder().setId(nodeId).build())
                .setTypeUrl(Constants.API_TYPE_URL).build();
        try {
            DiscoveryResponse response = blockingStub.withDeadlineAfter(60, TimeUnit.SECONDS).fetchApis(req);
            shutdown();

            apis = handleResponse(response);
        } catch (Exception e) {
            // catching generic error here to wrap any grpc communication errors in the runtime
            throw new DiscoveryException("Couldn't fetch init APIs", e);
        }
        return apis;
    }

    public void watchApis() {
        // TODO: (Praminda) implement a deadline with retries
        reqObserver = stub.streamApis(new StreamObserver<DiscoveryResponse>() {
                    @Override
                    public void onNext(DiscoveryResponse response) {
                        logger.debug("Received API discovery response " + response);
                        latestReceived = response;
                        try {
                            List<Api> apis = handleResponse(response);
                            apiFactory.addApis(apis);
                            // TODO: (Praminda) fix recursive ack on ack failure
                            ack();
                        } catch (Exception e) {
                            // catching generic error here to wrap any grpc communication errors in the runtime
                            onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Error occurred during API discovery", throwable);
                        // TODO: (Praminda) if adapter is unavailable keep retrying
                        nack(throwable);
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Completed receiving APIs");
                    }
                });

        try {
            DiscoveryRequest req = DiscoveryRequest.newBuilder()
                    .setNode(Node.newBuilder().setId(nodeId).build())
                    .setVersionInfo(latestACKed.getVersionInfo())
                    .setTypeUrl(Constants.API_TYPE_URL).build();
            reqObserver.onNext(req);
        } catch (Exception e) {
            logger.error("Unexpected error occurred in API discovery service", e);
            reqObserver.onError(e);
        }
    }

    /**
     * Send acknowledgement of successfully processed DiscoveryResponse from the xDS server.
     * This is part of the xDS communication protocol.
     */
    private void ack() {
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(Node.newBuilder().setId(nodeId).build())
                .setVersionInfo(latestReceived.getVersionInfo())
                .setResponseNonce(latestReceived.getNonce())
                .setTypeUrl(Constants.API_TYPE_URL).build();
        reqObserver.onNext(req);
        latestACKed = latestReceived;
    }

    private void nack(Throwable e) {
        if (latestReceived == null) {
            return;
        }
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(Node.newBuilder().setId(nodeId).build())
                .setVersionInfo(latestACKed.getVersionInfo())
                .setResponseNonce(latestReceived.getNonce())
                .setTypeUrl(Constants.API_TYPE_URL)
                .setErrorDetail(Status.newBuilder().setMessage(e.getMessage()))
                .build();
        reqObserver.onNext(req);
    }

    private List<Api> handleResponse(DiscoveryResponse response) throws InvalidProtocolBufferException {
        List<Api> apis = new ArrayList<>();
        for (Any res : response.getResourcesList()) {
            apis.add(res.unpack(Api.class));
        }
        return apis;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
