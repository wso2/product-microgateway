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
import org.wso2.choreo.connect.discovery.keymgt.KeyManagerConfig;
import org.wso2.choreo.connect.discovery.service.keymgt.KMDiscoveryServiceGrpc;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.discovery.common.XDSCommonUtils;
import org.wso2.choreo.connect.enforcer.discovery.scheduler.XdsSchedulerManager;
import org.wso2.choreo.connect.enforcer.grpc.HealthService;
import org.wso2.choreo.connect.enforcer.keymgt.KeyManagerHolder;
import org.wso2.choreo.connect.enforcer.util.GRPCUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with API discovery service at the adapter.
 */
public class KeyManagerDiscoveryClient implements Runnable, DiscoveryClient {
    private static KeyManagerDiscoveryClient instance;
    private ManagedChannel channel;
    private KMDiscoveryServiceGrpc.KMDiscoveryServiceStub stub;
    private static final Logger logger = LogManager.getLogger(KeyManagerDiscoveryClient.class);
    private StreamObserver<DiscoveryRequest> reqObserver;
    private static final Logger log = LogManager.getLogger(KeyManagerDiscoveryClient.class);
    private final KeyManagerHolder kmHolder;
    private final String host;
    private final int port;
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
    private boolean initialFetchCompleted = false;

    private KeyManagerDiscoveryClient(String host, int port) {
        this.host = host;
        this.port = port;
        initConnection();
        this.node = XDSCommonUtils.generateXDSNode(AdapterConstants.COMMON_ENFORCER_LABEL);
        this.latestACKed = DiscoveryResponse.getDefaultInstance();
        this.kmHolder = KeyManagerHolder.getInstance();
    }

    private void initConnection() {
        if (GRPCUtils.isReInitRequired(channel)) {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdownNow();
                do {
                    try {
                        channel.awaitTermination(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        log.error("Key manager discovery channel shutdown wait was interrupted", e);
                    }
                } while (!channel.isShutdown());
            }
            this.channel = GRPCUtils.createSecuredChannel(log, host, port);
            this.stub = KMDiscoveryServiceGrpc.newStub(channel);
        } else if (channel.getState(true) == ConnectivityState.READY) {
            XdsSchedulerManager.getInstance().stopKeyManagerDiscoveryScheduling();
        }
    }

    public static KeyManagerDiscoveryClient getInstance() {
        if (instance == null) {
            String adsHost = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
            int adsPort = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getAdapterXdsPort());
            instance = new KeyManagerDiscoveryClient(adsHost, adsPort);
        }
        HealthService.registerDiscoveryClient("KeyManagerDiscoveryClient", instance);
        return instance;
    }

    public void run() {
        initConnection();
        watchKeyManagers();
    }

    public void watchKeyManagers() {
        // TODO: implement a deadline with retries
        int maxSize = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getXdsMaxMsgSize());
        reqObserver = stub.withMaxInboundMessageSize(maxSize).streamKeyManagers(new StreamObserver<>() {
            @Override
            public void onNext(DiscoveryResponse response) {
                logger.info("Key manager event received with version : " + response.getVersionInfo() +
                        " and size(bytes) : " + response.getSerializedSize());
                if ((double) response.getSerializedSize() / maxSize > 0.80) {
                    logger.error("Current response size exceeds 80% of the maximum message size for the type : " +
                            response.getTypeUrl());
                }
                logger.debug("Received KeyManagers discovery response " + response);
                XdsSchedulerManager.getInstance().stopKeyManagerDiscoveryScheduling();
                latestReceived = response;
                try {
                    List<KeyManagerConfig> keyManagerConfig = handleResponse(response);
                    kmHolder.populateKMIssuerConfiguration(keyManagerConfig);
                    logger.info("Number of key managers received : " + keyManagerConfig.size());
                    // TODO: fix recursive ack on ack failure
                    long sendStartTime = System.currentTimeMillis();
                    ack();
                    long sendDuration = System.currentTimeMillis() - sendStartTime;
                    logger.info("Key manager discovery response acked after " + sendDuration + " ms");
                    initialFetchCompleted = true;
                } catch (Exception e) {
                    // catching generic error here to wrap any grpc communication errors in the runtime
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error occurred during Key Manager discovery", throwable);
                XdsSchedulerManager.getInstance().startKeyManagerDiscoveryScheduling();
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
                    .setTypeUrl(Constants.KEY_MANAGER_TYPE_URL).build();
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
                .setTypeUrl(Constants.KEY_MANAGER_TYPE_URL).build();
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
                .setTypeUrl(Constants.KEY_MANAGER_TYPE_URL)
                .setErrorDetail(Status.newBuilder().setMessage(e.getMessage()))
                .build();
        reqObserver.onNext(req);
    }

    private List<KeyManagerConfig> handleResponse(DiscoveryResponse response) throws InvalidProtocolBufferException {
        List<KeyManagerConfig> keyManagers = new ArrayList<>();
        for (Any res : response.getResourcesList()) {
            keyManagers.add(res.unpack(KeyManagerConfig.class));
        }
        return keyManagers;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    public boolean isInitialFetchCompleted() {
        return initialFetchCompleted;
    }
}
