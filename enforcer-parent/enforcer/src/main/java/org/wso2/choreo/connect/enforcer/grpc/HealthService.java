/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.service.health.HealthCheckRequest;
import org.wso2.choreo.connect.discovery.service.health.HealthCheckResponse;
import org.wso2.choreo.connect.discovery.service.health.HealthGrpc;
import org.wso2.choreo.connect.enforcer.discovery.DiscoveryClient;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the gRPC server written to serve the health state of enforcer.
 */
public class HealthService extends HealthGrpc.HealthImplBase {

    private static final Logger logger = LogManager.getLogger(HealthService.class);
    private static final Map<String, DiscoveryClient> discoveryClients = new HashMap<>();
    private static boolean isDiscoveryClientsInitialFetchCompleted = false;

    /**
     * Register a discovery client to the health service.
     *
     * @param name            the name of the discovery client
     * @param discoveryClient the discovery client to be registered
     */
    public static void registerDiscoveryClient(String name, DiscoveryClient discoveryClient) {
        logger.info("Registering discovery client: {}", discoveryClient.getClass().getName());
        discoveryClients.put(name, discoveryClient);
    }

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        if (!checkDiscoveryClientsHealth(responseObserver)) {
            return;
        }

        logger.debug("Responding health state of Enforcer as HEALTHY");
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(HealthCheckResponse.ServingStatus.SERVING).build();
        // respond for all without checking requested service name
        // service name format: package_names.ServiceName
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Check the health of the discovery clients.
     *
     * @param responseObserver the response observer
     * @return true if all the discovery clients are healthy, false otherwise
     */
    private static boolean checkDiscoveryClientsHealth(StreamObserver<HealthCheckResponse> responseObserver) {
        if (!isDiscoveryClientsInitialFetchCompleted) {
            boolean initialized = true;
            for (String clientName : discoveryClients.keySet()) {
                DiscoveryClient discoveryClient = discoveryClients.get(clientName);
                if (!discoveryClient.isInitialFetchCompleted()) {
                    logger.info("Discovery client: {} has not completed initial fetch", clientName);
                    initialized = false;
                    break;
                }
            }
            isDiscoveryClientsInitialFetchCompleted = initialized;
        }

        if (!isDiscoveryClientsInitialFetchCompleted) {
            logger.info("Responding health state of Enforcer as NOT_SERVING");
            HealthCheckResponse response = HealthCheckResponse.newBuilder()
                    .setStatus(HealthCheckResponse.ServingStatus.NOT_SERVING).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return false;
        }
        return true;
    }
}
