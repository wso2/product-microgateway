/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.service.throttle.ThrottleDataDiscoveryServiceGrpc;
import org.wso2.gateway.discovery.throttle.ThrottleData;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.AdapterConstants;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.util.GRPCUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with ThrottleData discovery service at the adapter.
 */
public class ThrottleDataDiscoveryClient {
    private static ThrottleDataDiscoveryClient instance;
    private final ManagedChannel channel;
    private final ThrottleDataDiscoveryServiceGrpc.ThrottleDataDiscoveryServiceStub stub;
    private static final Logger logger = LogManager.getLogger(ThrottleDataDiscoveryClient.class);
    private StreamObserver<DiscoveryRequest> reqObserver;
    /**
     * This is a reference to the latest received response from the TDDS.
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

    private ThrottleDataDiscoveryClient(String host, int port) {
        this.channel = GRPCUtils.createSecuredChannel(logger, host, port);
        this.stub = ThrottleDataDiscoveryServiceGrpc.newStub(channel);
        this.nodeId = AdapterConstants.COMMON_ENFORCER_LABEL;
        this.latestACKed = DiscoveryResponse.getDefaultInstance();
    }

    public static ThrottleDataDiscoveryClient getInstance() {
        if (instance == null) {
            String adsHost = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
            int adsPort = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getAdapterXdsPort());
            instance = new ThrottleDataDiscoveryClient(adsHost, adsPort);
        }
        return instance;
    }

    public void watchThrottleData() {
        int maxSize = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getXdsMaxMsgSize());
        reqObserver = stub.withMaxInboundMessageSize(maxSize)
                .streamThrottleData(new StreamObserver<DiscoveryResponse>() {
            @Override
            public void onNext(DiscoveryResponse response) {
                logger.debug("Received ThrottleData discovery response " + response);
                latestReceived = response;
                try {
                    List<String> keyTemplates = handleResponse(response);
                    ack();
                } catch (Exception e) {
                    // catching generic error here to wrap any grpc communication errors in the runtime
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error occurred during ThrottleData discovery", throwable);
                // TODO: (Praminda) if adapter is unavailable keep retrying
                nack(throwable);
            }

            @Override
            public void onCompleted() {
                logger.info("Completed receiving ThrottleData");
            }
        });

        try {
            DiscoveryRequest req = DiscoveryRequest.newBuilder()
                    .setNode(Node.newBuilder().setId(nodeId).build())
                    .setVersionInfo(latestACKed.getVersionInfo())
                    .setTypeUrl(Constants.THROTTLE_DATA_TYPE_URL).build();
            reqObserver.onNext(req);
        } catch (Exception e) {
            logger.error("Unexpected error occurred in ThrottleData discovery service", e);
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
                .setTypeUrl(Constants.THROTTLE_DATA_TYPE_URL).build();
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
                .setTypeUrl(Constants.THROTTLE_DATA_TYPE_URL)
                .setErrorDetail(Status.newBuilder().setMessage(e.getMessage()))
                .build();
        reqObserver.onNext(req);
    }

    private List<String> handleResponse(DiscoveryResponse response) throws InvalidProtocolBufferException {
        // Currently theres only one ThrottleData resource here. Therefore taking 0, no need to iterate
        ThrottleData res = response.getResources(0).unpack(ThrottleData.class);
        return res.getKeyTemplatesList();
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
