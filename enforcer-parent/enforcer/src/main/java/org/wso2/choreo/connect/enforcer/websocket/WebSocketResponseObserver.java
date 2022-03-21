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
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.Opcode;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.Framedata;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse;
import org.wso2.choreo.connect.enforcer.analytics.AnalyticsFilter;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.grpc.WebSocketFrameService;
import org.wso2.choreo.connect.enforcer.server.WebSocketHandler;

import java.nio.ByteBuffer;
import java.util.List;
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
    private Draft_6455 decoder;

    public WebSocketResponseObserver(StreamObserver<WebSocketFrameResponse> responseStreamObserver) {
        if (ConfigHolder.getInstance().getConfig().getAnalyticsConfig().isEnabled()) {
            AnalyticsFilter.getInstance();
        }
        this.responseStreamObserver = responseStreamObserver;
        this.decoder = new Draft_6455();
    }

    @Override
    public void onNext(WebSocketFrameRequest webSocketFrameRequest) {
        logger.debug("Websocket frame received for api:basepath : {}:{}", webSocketFrameRequest.getMetadata()
                .getExtAuthzMetadataMap().get(APIConstants.GW_API_NAME_PARAM), webSocketFrameRequest.getMetadata()
                .getExtAuthzMetadataMap().get(APIConstants.GW_BASE_PATH_PARAM));
        try {
            // In case a stream of websocket frames are intercepted by the filter, envoy will buffer them as mini
            // batches and aggregate the frames. In that case if we directly send the frames to traffic manager, the
            // frame count will be wrong. Instead we can decode the buffer into frames and then process them
            // individually for throttling.
            AnalyticsFilter.getInstance().handleWebsocketFrameRequest(webSocketFrameRequest);
            List<Framedata> frames = decoder.translateFrame(
                    ByteBuffer.wrap(webSocketFrameRequest.getPayload().toByteArray()));
            frames.forEach((framedata -> {
                // Only consider text, binary and continous frames
                if (framedata.getOpcode() == Opcode.TEXT || framedata.getOpcode() == Opcode.BINARY
                    || framedata.getOpcode() == Opcode.CONTINUOUS) {
                    WebSocketFrameRequest webSocketFrameRequestClone = webSocketFrameRequest.toBuilder()
                            .setFrameLength(framedata.getPayloadData().remaining()).build();
                    sendWebSocketFrameResponse(webSocketFrameRequestClone);
                } else {
                    logger.debug("Websocket frame type not related to throttling: {}", framedata.getOpcode());
                }
            }));
        } catch (InvalidDataException e) {
            // temp fix for https://github.com/wso2/product-microgateway/issues/2693
            logger.error("Error {} when decoding websocket frame. Could be a batched set of " +
                    "multiple compressed frames. Processing the frame in raw form.", e.getMessage());
            sendWebSocketFrameResponse(webSocketFrameRequest);
        }

        if (!this.throttleKeysInitiated) {
            initializeThrottleKeys(webSocketFrameRequest);
        }
    }

    private void sendWebSocketFrameResponse(WebSocketFrameRequest webSocketFrameRequest) {
        WebSocketThrottleResponse webSocketThrottleResponse = webSocketHandler
                .process(webSocketFrameRequest);
        if (WebSocketThrottleState.OK == webSocketThrottleResponse.getWebSocketThrottleState()) {
            WebSocketFrameResponse response = WebSocketFrameResponse.newBuilder().setThrottleState(
                    WebSocketFrameResponse.Code.OK).setApimErrorCode(0).build();
            responseStreamObserver.onNext(response);
        } else if (WebSocketThrottleState.OVER_LIMIT == webSocketThrottleResponse
                .getWebSocketThrottleState()) {
            logger.debug("throttle out period" + webSocketThrottleResponse.getThrottlePeriod());
            WebSocketFrameResponse response = WebSocketFrameResponse.newBuilder()
                    .setThrottleState(WebSocketFrameResponse.Code.OVER_LIMIT)
                    .setThrottlePeriod(webSocketThrottleResponse.getThrottlePeriod())
                    .setApimErrorCode(webSocketThrottleResponse.getApimErrorCode())
                    .build();
            responseStreamObserver.onNext(response);
        } else {
            logger.debug("throttle state of the connection is not available in enforcer");
            WebSocketFrameResponse webSocketFrameResponse = WebSocketFrameResponse.newBuilder()
                    .setThrottleState(WebSocketFrameResponse.Code.UNKNOWN).build();
            responseStreamObserver.onNext(webSocketFrameResponse);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("websocket frame service onError: " + throwable.toString());
        WebSocketFrameService.removeObserver(streamId);
    }

    @Override
    public void onCompleted() {
        WebSocketFrameService.removeObserver(streamId);
    }

    // Not used for throttling purposes currently. Only kept as a reference
    private void initializeThrottleKeys(WebSocketFrameRequest webSocketFrameRequest) {
        Map<String, String> extAuthMetadata = webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap();
        String basePath = extAuthMetadata.get(APIConstants.GW_BASE_PATH_PARAM);
        String version = extAuthMetadata.get(APIConstants.GW_VERSION_PARAM);
        String applicationId = extAuthMetadata.get(MetadataConstants.APP_ID);
        String streamId = extAuthMetadata.get(MetadataConstants.GRPC_STREAM_ID);
        String authorizedUser = extAuthMetadata.get(MetadataConstants.USERNAME);
        String apiContext = basePath + ':' + version;
        this.apiThrottleKey = apiContext;
        this.subscriptionThrottleKey = applicationId + ":" + apiContext;
        this.applicationThrottleKey = applicationId + ":" + authorizedUser;
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
