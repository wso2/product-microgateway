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
import org.wso2.choreo.connect.discovery.keymgt.RevokedToken;
import org.wso2.choreo.connect.discovery.service.keymgt.RevokedTokenDiscoveryServiceGrpc;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.discovery.scheduler.XdsSchedulerManager;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.RevokedJWTDataHolder;
import org.wso2.choreo.connect.enforcer.util.GRPCUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 * Client to communicate with API discovery service at the adapter.
 */
public class RevokedTokenDiscoveryClient implements Runnable {

    private static RevokedTokenDiscoveryClient instance;
    private ManagedChannel channel;
    private RevokedTokenDiscoveryServiceGrpc.RevokedTokenDiscoveryServiceStub stub;
    private static final Logger logger = LogManager.getLogger(RevokedTokenDiscoveryClient.class);
    private final RevokedJWTDataHolder revokedJWTDataHolder;
    private StreamObserver<DiscoveryRequest> reqObserver;
    private static final Logger log = LogManager.getLogger(RevokedTokenDiscoveryClient.class);
    private String host;
    private int port;
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

    private RevokedTokenDiscoveryClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.revokedJWTDataHolder = RevokedJWTDataHolder.getInstance();
        initConnection();
        // Since revoked tokens should be received by every enforcer, adapter creates a
        // common snapshot for revoked tokens. Hence each enforces requests data using the
        // common enforcer label to avoid redundent snapshots
        this.nodeId = AdapterConstants.COMMON_ENFORCER_LABEL;
        this.latestACKed = DiscoveryResponse.getDefaultInstance();
    }

    private void initConnection() {
        if (GRPCUtils.isReInitRequired(channel)) {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdownNow();
                do {
                    try {
                        channel.awaitTermination(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        log.error("Revoked  tokens discovery channel shutdown wait was interrupted", e);
                    }
                } while (!channel.isShutdown());
            }
            this.channel = GRPCUtils.createSecuredChannel(log, host, port);
            this.stub = RevokedTokenDiscoveryServiceGrpc.newStub(channel);
        } else if (channel.getState(true) == ConnectivityState.READY) {
            XdsSchedulerManager.getInstance().stopRevokedTokenDiscoveryScheduling();
        }
    }

    public static RevokedTokenDiscoveryClient getInstance() {
        if (instance == null) {
            String adsHost = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
            int adsPort = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getAdapterXdsPort());
            instance = new RevokedTokenDiscoveryClient(adsHost, adsPort);
        }
        return instance;
    }

    public void run() {
        initConnection();
        watchRevokedTokens();
    }

    public void watchRevokedTokens() {
        // TODO: (Praminda) implement a deadline with retries
        int maxSize = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getXdsMaxMsgSize());
        reqObserver = stub.withMaxInboundMessageSize(maxSize).streamTokens(new StreamObserver<DiscoveryResponse>() {
                    @Override
                    public void onNext(DiscoveryResponse response) {
                        logger.info("Revoked  token event received with version : " + response.getVersionInfo());
                        XdsSchedulerManager.getInstance().stopRevokedTokenDiscoveryScheduling();
                        latestReceived = response;
                        try {
                            List<RevokedToken> tokens = handleResponse(response);
                            handleRevokedTokens(tokens);
                            // TODO: (Praminda) fix recursive ack on ack failure
                            ack();
                        } catch (Exception e) {
                            logger.info(e);
                            // catching generic error here to wrap any grpc communication errors in the runtime
                            onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Error occurred during revoked token discovery", throwable);
                        XdsSchedulerManager.getInstance().startRevokedTokenDiscoveryScheduling();
                        nack(throwable);
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Completed receiving revoke tokens");
                    }
                });

        try {
            DiscoveryRequest req = DiscoveryRequest.newBuilder()
                    .setNode(Node.newBuilder().setId(nodeId).build())
                    .setVersionInfo(latestACKed.getVersionInfo())
                    .setTypeUrl(Constants.REVOKED_TOKEN_TYPE_URL).build();
            reqObserver.onNext(req);
           logger.debug("Sent Discovery request for type url: " + Constants.REVOKED_TOKEN_TYPE_URL);

        } catch (Exception e) {
            logger.error("Unexpected error occurred in revoked token discovery service", e);
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
                .setTypeUrl(Constants.REVOKED_TOKEN_TYPE_URL).build();
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
                .setTypeUrl(Constants.REVOKED_TOKEN_TYPE_URL)
                .setErrorDetail(Status.newBuilder().setMessage(e.getMessage()))
                .build();
        reqObserver.onNext(req);
    }

    private void handleRevokedTokens(List<RevokedToken> tokens) {
        for (RevokedToken revokedToken : tokens) {
            revokedJWTDataHolder.addRevokedJWTToMap(revokedToken.getJti(), Long.valueOf(revokedToken.getExpirytime()));
        }
    }

    private List<RevokedToken> handleResponse(DiscoveryResponse response) throws InvalidProtocolBufferException {
        List<RevokedToken> apis = new ArrayList<>();
        for (Any res : response.getResourcesList()) {
            apis.add(res.unpack(RevokedToken.class));
        }
        return apis;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
