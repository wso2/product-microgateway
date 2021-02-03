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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.enforcer.discovery;

import com.google.protobuf.Any;
import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.service.subscription.SubscriptionDiscoveryServiceGrpc;
import org.wso2.gateway.discovery.subscription.Subscription;
import org.wso2.gateway.discovery.subscription.SubscriptionList;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.subscription.SubscriptionDataStoreImpl;
import org.wso2.micro.gateway.enforcer.util.GRPCUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Client to communicate with Subscription discovery service at the adapter.
 */
public class SubscriptionDiscoveryClient {
    private static final Logger logger = LogManager.getLogger(SubscriptionDiscoveryClient.class);
    private static SubscriptionDiscoveryClient instance;
    private final ManagedChannel channel;
    private final SubscriptionDiscoveryServiceGrpc.SubscriptionDiscoveryServiceStub stub;
    private StreamObserver<DiscoveryRequest> reqObserver;
    private SubscriptionDataStoreImpl subscriptionDataStore;

    /**
     * This is a reference to the latest received response from the ADS.
     * <p>
     * Usage: When ack/nack a DiscoveryResponse this value is used to identify the latest received DiscoveryResponse
     * which may not have been acked/nacked so far.
     * </p>
     */

    private DiscoveryResponse latestReceived;
    /**
     * This is a reference to the latest acked response from the ADS.
     * <p>
     * Usage: When nack a DiscoveryResponse this value is used to find the latest successfully processed
     * DiscoveryResponse. Information sent in the nack request will contain information about this response value.
     * </p>
     */
    private DiscoveryResponse latestACKed;

    /**
     * Label of this node.
     */
    private final String nodeId;

    private SubscriptionDiscoveryClient(String host, int port) {
        this.subscriptionDataStore = SubscriptionDataStoreImpl.getInstance();
        this.channel = GRPCUtils.createSecuredChannel(logger, host, port);
        this.stub = SubscriptionDiscoveryServiceGrpc.newStub(channel);
        this.nodeId = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
        this.latestACKed = DiscoveryResponse.getDefaultInstance();
    }

    public static SubscriptionDiscoveryClient getInstance() {
        if (instance == null) {
            String sdsHost = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
            int sdsPort = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getAdapterXdsPort());
            instance = new SubscriptionDiscoveryClient(sdsHost, sdsPort);
        }
        return instance;
    }

    public void watchSubscriptions() {
        // TODO: (Praminda) implement a deadline with retries
        reqObserver = stub.streamSubscriptions(new StreamObserver<DiscoveryResponse>() {
            @Override
            public void onNext(DiscoveryResponse response) {
                logger.debug("Received Subscription discovery response " + response);
                latestReceived = response;
                try {
                    List<Subscription> subscriptionList = new ArrayList<>();
                    for (Any res : response.getResourcesList()) {
                        subscriptionList.addAll(res.unpack(SubscriptionList.class).getListList());
                    }
                    subscriptionDataStore.addSubscriptions(subscriptionList);
                    ack();

                } catch (Exception e) {
                    // catching generic error here to wrap any grpc communication errors in the runtime
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error occurred during Subscription discovery", throwable);
                // TODO: (Praminda) if adapter is unavailable keep retrying
                nack(throwable);
            }

            @Override
            public void onCompleted() {
                logger.info("Completed receiving Subscription data");
            }
        });

        try {
            DiscoveryRequest req = DiscoveryRequest.newBuilder()
                    .setNode(Node.newBuilder().setId(nodeId).build())
                    .setVersionInfo(latestACKed.getVersionInfo())
                    .setTypeUrl(Constants.SUBSCRIPTION_LIST_TYPE_URL).build();
            reqObserver.onNext(req);
            logger.debug("Sent Discovery request for type url: " + Constants.SUBSCRIPTION_LIST_TYPE_URL);

        } catch (Exception e) {
            logger.error("Unexpected error occurred in API discovery service", e);
            reqObserver.onError(e);
        }
    }

    /**
     * Send acknowledgement of successfully processed DiscoveryResponse from the xDS server. This is part of the xDS
     * communication protocol.
     */
    private void ack() {
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(Node.newBuilder().setId(nodeId).build())
                .setVersionInfo(latestReceived.getVersionInfo())
                .setResponseNonce(latestReceived.getNonce())
                .setTypeUrl(Constants.SUBSCRIPTION_LIST_TYPE_URL).build();
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
                .setTypeUrl(Constants.SUBSCRIPTION_LIST_TYPE_URL)
                .setErrorDetail(Status.newBuilder().setMessage(e.getMessage()))
                .build();
        reqObserver.onNext(req);
    }
}
