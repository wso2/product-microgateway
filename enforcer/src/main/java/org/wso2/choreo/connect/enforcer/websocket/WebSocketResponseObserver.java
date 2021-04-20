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
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.grpc.WebSocketFrameService;
import org.wso2.choreo.connect.enforcer.server.WebSocketHandler;

import java.util.Map;

/**
 * Wrapper class for StreamObserver<RateLimitRequest> with extra fields added to identify relevant information about
 * the related API, application, subscriber etc
 */
public class WebSocketResponseObserver implements StreamObserver<WebSocketFrameRequest> {

    private static final Logger logger = LogManager.getLogger(WebSocketResponseObserver.class);
    private String apiThrottleKey;
    private String subscriptionThrottleKey;
    private String applicationThrottleKey;
    private final StreamObserver<WebSocketFrameResponse> responseStreamObserver;
    private final WebSocketHandler webSocketHandler = new WebSocketHandler();
    private String streamId;
    private boolean throttleKeysInitiated;

    public WebSocketResponseObserver(StreamObserver<WebSocketFrameResponse> responseStreamObserver) {
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(WebSocketFrameRequest webSocketFrameRequest) {
        logger.info(webSocketFrameRequest.toString());
        logger.info(webSocketFrameRequest.getPayload().toStringUtf8());
        logger.info(webSocketFrameRequest.getPayload().toByteArray());
//        Draft_6455 decoder = new Draft_6455();
//        try {
//            List<Framedata> frames = decoder.translateFrame(
//                    ByteBuffer.wrap(webSocketFrameRequest.getPayload().toByteArray()));
//            logger.info(Arrays.toString(frames.toArray()));
//        } catch (InvalidDataException e) {
//           logger.error(e);
//        }
        if (!this.throttleKeysInitiated) {
            initializeThrottleKeys(webSocketFrameRequest);
        }

        WebSocketThrottleResponse webSocketThrottleResponse = webSocketHandler.process(webSocketFrameRequest);
        if (webSocketThrottleResponse.getWebSocketThrottleState() == WebSocketThrottleState.OK) {
            WebSocketFrameResponse response = WebSocketFrameResponse.newBuilder().setThrottleState(
                    WebSocketFrameResponse.Code.OK).build();
            responseStreamObserver.onNext(response);
        } else {
            logger.info("throttle period" + webSocketThrottleResponse.getThrottlePeriod());
            WebSocketFrameResponse response = WebSocketFrameResponse.newBuilder().setThrottleState(
                    WebSocketFrameResponse.Code.OVER_LIMIT).setThrottlePeriod(
                    webSocketThrottleResponse.getThrottlePeriod()).build();
            responseStreamObserver.onNext(response);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.info("websocket metadata service onError: " + throwable.toString());
        WebSocketFrameService.removeObserver(streamId);
    }

    @Override
    public void onCompleted() {
        WebSocketFrameService.removeObserver(streamId);
    }

    private void initializeThrottleKeys(WebSocketFrameRequest webSocketFrameRequest) {
        Map<String, String> extAuthMetadata = webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap();
        String basePath = extAuthMetadata.get(APIConstants.GW_BASE_PATH_PARAM);
        String version = extAuthMetadata.get(APIConstants.GW_VERSION_PARAM);
        String applicationId = extAuthMetadata.get(MetadataConstants.APP_ID);
        String streamId = extAuthMetadata.get(MetadataConstants.GRPC_STREAM_ID);
        String apiContext = basePath + ':' + version;
        this.apiThrottleKey = apiContext;
        this.subscriptionThrottleKey = applicationId + ":" + apiContext;
        this.applicationThrottleKey = applicationId;
        this.streamId = streamId;
        WebSocketFrameService.addObserver(streamId, this);
        this.throttleKeysInitiated = true;
    }

    public String getApiThrottleKey() {
        return apiThrottleKey;
    }

    public String getSubscriptionThrottleKey() {
        return subscriptionThrottleKey;
    }

    public String getApplicationThrottleKey() {
        return applicationThrottleKey;
    }
}
