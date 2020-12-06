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

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.config.enforcer.Config;
import org.wso2.gateway.discovery.service.config.ConfigDiscoveryServiceGrpc;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.exception.DiscoveryException;

import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with configuration discovery service at the adapter.
 */
public class ConfigDiscoveryClient {
    private final ManagedChannel channel;
    private final ConfigDiscoveryServiceGrpc.ConfigDiscoveryServiceBlockingStub blockingStub;

    public ConfigDiscoveryClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.blockingStub = ConfigDiscoveryServiceGrpc.newBlockingStub(channel);
    }

    public Config requestInitConfig() throws DiscoveryException {
        // TODO: praminda implement nodeid (label) behavior
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(Node.newBuilder().setId("enforcer").build())
                .setTypeUrl(Constants.CONFIG_TYPE_URL).build();
        DiscoveryResponse res = DiscoveryResponse.getDefaultInstance();
        try {
            res = blockingStub.withDeadlineAfter(60, TimeUnit.SECONDS).fetchConfigs(req);
            shutdown();

            // Theres only one config root resource here. Therefore taking 0, no need to iterate
            return res.getResources(0).unpack(Config.class);
        } catch (Exception e) {
            // catching generic error here to wrap any grpc communication errors in the runtime
            throw new DiscoveryException("Couldn't fetch init configs", e);
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
