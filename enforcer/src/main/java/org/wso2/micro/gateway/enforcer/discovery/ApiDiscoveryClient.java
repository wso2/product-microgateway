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
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.api.Api;
import org.wso2.gateway.discovery.service.api.ApiDiscoveryServiceGrpc;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.exception.DiscoveryException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with API discovery service at the adapter.
 */
public class ApiDiscoveryClient {
    private final ManagedChannel channel;
    private final ApiDiscoveryServiceGrpc.ApiDiscoveryServiceStub stub;
    private final ApiDiscoveryServiceGrpc.ApiDiscoveryServiceBlockingStub blockingStub;
    private static final Logger logger = LogManager.getLogger(ApiDiscoveryClient.class);

    public ApiDiscoveryClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.stub = ApiDiscoveryServiceGrpc.newStub(channel);
        this.blockingStub = ApiDiscoveryServiceGrpc.newBlockingStub(channel);
    }

    public List<Api> requestInitApis() throws DiscoveryException {
        // TODO: praminda implement nodeid (label) behavior
        List<Api> apis = new ArrayList<>();
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(Node.newBuilder().setId("enforcer").build())
                .setTypeUrl(Constants.API_TYPE_URL).build();
        try {
            DiscoveryResponse response = blockingStub.withDeadlineAfter(60, TimeUnit.SECONDS).fetchApis(req);
            shutdown();

            List<Any> resources = response.getResourcesList();
            for (Any res: resources) {
                apis.add(res.unpack(Api.class));
            }
        } catch (Exception e) {
            // catching generic error here to wrap any grpc communication errors in the runtime
            throw new DiscoveryException("Couldn't fetch init APIs", e);
        }
        return apis;
    }

    public void watchApis() {
        StreamObserver<DiscoveryRequest> reqObserver = stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .streamApis(new StreamObserver<DiscoveryResponse>() {
                    @Override
                    public void onNext(DiscoveryResponse discoveryResponse) {
                        logger.info("received response " + discoveryResponse);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Error occurred during API discovery", throwable);
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Completed receiving APIs");
                    }
                });

        try {
            DiscoveryRequest req = DiscoveryRequest.newBuilder().build();
            reqObserver.onNext(req);
        } catch (Exception e) {
            logger.error("Unexpected error occurred in API discovery service", e);
            reqObserver.onError(e);
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
