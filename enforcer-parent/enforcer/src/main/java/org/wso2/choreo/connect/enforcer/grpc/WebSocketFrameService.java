/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameServiceGrpc;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketResponseObserver;


import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC service for processing web socket frame related metadata for websocket throttling and analytics. This class
 * contains a ConcurrentHashMap of the StreamObservers that corresponds to open grpc bidirectional streams with
 * envoy mgw_wasm_websocket filter.
 */
public class WebSocketFrameService extends WebSocketFrameServiceGrpc.WebSocketFrameServiceImplBase {
    private static final Logger logger = LogManager.getLogger(WebSocketFrameService.class);
    private static ConcurrentHashMap<String, WebSocketResponseObserver> responseObservers = new ConcurrentHashMap<>();
    @Override
    public StreamObserver<WebSocketFrameRequest> publishFrameData(StreamObserver<WebSocketFrameResponse>
            responseObserver) {
        logger.debug("publishMetadata invoked from websocket metadata service");
        return new WebSocketResponseObserver(responseObserver);
    }

    public static void addObserver(String streamId, WebSocketResponseObserver observer) {
        responseObservers.put(streamId, observer);
    }

    public static void removeObserver(String streamId) {
        // As per the class java doc, the map of observers is maintained to keep track of open grpc streams.
        // The map is updated when the first message from the router arrives at the enforcer. If a stream
        // was created, yet no WebSocketFrameRequests arrives, and the stream gets deleted, the class variable
        // streamId will not be set (streamId will be null) and an observer will not be added to the map.
        // Ex: when a backend does not exist.
        if (streamId != null) {
            responseObservers.remove(streamId);
        }
    }


}
