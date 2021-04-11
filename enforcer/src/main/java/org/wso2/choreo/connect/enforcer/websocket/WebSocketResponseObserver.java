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
package org.wso2.choreo.connect.enforcer.websocket;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.grpc.WebSocketFrameService;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketMetadataContext;
import org.wso2.choreo.connect.enforcer.server.WebSocketHandler;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse;

/**
 * Wrapper class for StreamObserver<RateLimitRequest> with extra fields added to identify relevant information about
 * the related API, application, subscriber etc
 */
public class WebSocketResponseObserver implements StreamObserver<WebSocketFrameRequest> {

    private static final Logger logger = LogManager.getLogger(WebSocketResponseObserver.class);
    private WebSocketMetadataContext webSocketMetadataContext;
    private final StreamObserver<WebSocketFrameResponse> responseStreamObserver;
    private final WebSocketHandler webSocketHandler = new WebSocketHandler();
    private String streamId;

    public WebSocketResponseObserver(StreamObserver<WebSocketFrameResponse> responseStreamObserver) {
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(WebSocketFrameRequest webSocketFrameRequest) {
        logger.info(webSocketFrameRequest.toString());
        //authenticationContext = webSocketHandler.process(webSocketFrameRequest);
        //streamId = getStreamId(webSocketFrameRequest);
        //WebSocketFrameService.addObserver(streamId, this);
        WebSocketFrameResponse response = WebSocketFrameResponse.newBuilder().setMessage("Hello from server").build();
        responseStreamObserver.onNext(response);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.debug("websocket metadata service onError: " + throwable.toString());
        WebSocketFrameService.removeObserver(streamId);
    }

    @Override
    public void onCompleted() {
        WebSocketFrameService.removeObserver(streamId);
    }

//    private String getStreamId(WebSocketFrameRequest webSocketFrameRequest) {
//        return webSocketFrameRequest.getMetadata().get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.WEBSOCKET_STREAM_ID)
//                .getStringValue();
//    }

}
