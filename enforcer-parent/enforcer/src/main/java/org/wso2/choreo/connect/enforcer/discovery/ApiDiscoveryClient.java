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

package org.wso2.choreo.connect.enforcer.discovery;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.api.Api;
import org.wso2.choreo.connect.discovery.service.api.ApiDiscoveryServiceGrpc;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.discovery.common.XDSCommonUtils;
import org.wso2.choreo.connect.enforcer.discovery.scheduler.XdsSchedulerManager;
import org.wso2.choreo.connect.enforcer.util.GRPCUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with API discovery service at the adapter.
 */
public class ApiDiscoveryClient implements Runnable {
    private static final Logger logger = LogManager.getLogger(ApiDiscoveryClient.class);
    private static ApiDiscoveryClient instance;
    private final APIFactory apiFactory;
    private final String host;
    private final int port;
    private ManagedChannel channel;
    private ApiDiscoveryServiceGrpc.ApiDiscoveryServiceStub stub;
    private StreamObserver<DiscoveryRequest> reqObserver;
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
     * Node struct for the discovery client
     */
    private final Node node;

    private ApiDiscoveryClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.apiFactory = APIFactory.getInstance();
        this.node = XDSCommonUtils.generateXDSNode(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerLabel());
        this.latestACKed = DiscoveryResponse.getDefaultInstance();
        initConnection();
    }

    private void initConnection() {
        if (GRPCUtils.isReInitRequired(channel)) {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdownNow();
                do {
                    try {
                        channel.awaitTermination(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        logger.error("API discovery channel shutdown wait was interrupted", e);
                    }
                } while (!channel.isShutdown());
            }
            this.channel = GRPCUtils.createSecuredChannel(logger, host, port);
            this.stub = ApiDiscoveryServiceGrpc.newStub(channel);
        } else if (channel.getState(true) == ConnectivityState.READY) {
            XdsSchedulerManager.getInstance().stopAPIDiscoveryScheduling();
        }
    }

    public static ApiDiscoveryClient getInstance() {
        if (instance == null) {
            String adsHost = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
            int adsPort = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getAdapterXdsPort());
            instance = new ApiDiscoveryClient(adsHost, adsPort);
        }
        return instance;
    }

    public void run() {
        initConnection();
        watchApis();
    }

    public void watchApis() {
        int maxSize = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getXdsMaxMsgSize());
        reqObserver = stub.withMaxInboundMessageSize(maxSize).streamApis(new StreamObserver<>() {
            @Override
            public void onNext(DiscoveryResponse response) {
                logger.info("API event received with version : " + response.getVersionInfo());
                logger.debug("Received API discovery response " + response);
                XdsSchedulerManager.getInstance().stopAPIDiscoveryScheduling();
                latestReceived = response;
                try {
                    List<Api> apis = handleResponse(response);
                    apiFactory.addApis(apis);
                    logger.info("Number of API artifacts received : " + apis.size());
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
                XdsSchedulerManager.getInstance().startAPIDiscoveryScheduling();
                nack(throwable);
            }

            @Override
            public void onCompleted() {
                logger.info("Completed receiving APIs");
            }
        });

        try {
            DiscoveryRequest req = DiscoveryRequest.newBuilder()
                    .setNode(node)
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
                .setNode(node)
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
                .setNode(node)
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
