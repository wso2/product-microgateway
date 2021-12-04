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
package org.wso2.choreo.connect.enforcer.server;

import io.opentelemetry.context.Scope;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.api.WebSocketAPI;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.WebSocketFrameContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.websocket.MetadataConstants;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketThrottleResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * WebSocketHandler handles requests coming through websocket frame service.
 */
public class WebSocketHandler implements RequestHandler<WebSocketFrameRequest, WebSocketThrottleResponse> {
    private static final Logger logger = LogManager.getLogger(WebSocketHandler.class);

    /**
     *
     * @param webSocketFrameRequest - WebSocketFrameRequest received from WebSocketFrameRequest
     * @return WebSocketResponseObject - Response sent via WebSocketFrameService
     */
    @Override
    public WebSocketThrottleResponse process(WebSocketFrameRequest webSocketFrameRequest) {
        TracingSpan webSocketHandlerSpan = null;
        Scope webSocketHandlerSpanScope = null;
        if (Utils.tracingEnabled()) {
            TracingTracer tracer =  Utils.getGlobalTracer();
            webSocketHandlerSpan = Utils.startSpan(TracingConstants.WS_HANDLER_SPAN, tracer);
            webSocketHandlerSpanScope = webSocketHandlerSpan.getSpan().makeCurrent();
            Utils.setTag(webSocketHandlerSpan, APIConstants.LOG_TRACE_ID, ThreadContext.get(APIConstants.LOG_TRACE_ID));
        }
        try {
            WebSocketAPI matchedAPI = APIFactory.getInstance().getMatchedAPI(webSocketFrameRequest);
            if (matchedAPI == null) {
                WebSocketThrottleResponse webSocketThrottleResponse = new WebSocketThrottleResponse();
                webSocketThrottleResponse.setUnknownState();
                String basePath = webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap()
                        .get(APIConstants.GW_BASE_PATH_PARAM);
                String version = webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap()
                        .get(APIConstants.GW_VERSION_PARAM);
                logger.info("API {}/{} not found in the cache", basePath, version);
                return webSocketThrottleResponse;
            } else if (logger.isDebugEnabled()) {
                APIConfig api = matchedAPI.getAPIConfig();
                logger.info("API {}/{} found in the cache", api.getBasePath(), api.getVersion());
            }
            RequestContext requestContext = buildRequestContext(matchedAPI, webSocketFrameRequest);
            return matchedAPI.processFramedata(requestContext);
        } finally {
            if (Utils.tracingEnabled()) {
                webSocketHandlerSpanScope.close();
                Utils.finishSpan(webSocketHandlerSpan);
            }
        }
    }

    /**
     * @param api - Matched WebSocket API
     * @param webSocketFrameRequest - WebSocketFrameRequest received from WebSocketFrameRequest
     * @return RequestContext - Build RequestContext by populating relevant fields from api and webSocketFrameRequest
     */
    private RequestContext buildRequestContext(WebSocketAPI api, WebSocketFrameRequest webSocketFrameRequest) {
        Map<String, String> extAuthMetadata = webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap();
        // Extracting ext_authz metadata from WebSocketFrameRequest
        String apiName = extAuthMetadata.get(APIConstants.GW_API_NAME_PARAM);
        String apiVersion = extAuthMetadata.get(APIConstants.GW_VERSION_PARAM);
        String apiBasePath = extAuthMetadata.get(APIConstants.GW_BASE_PATH_PARAM);
        String username = extAuthMetadata.get(MetadataConstants.USERNAME);
        String appTier = extAuthMetadata.get(MetadataConstants.APP_TIER);
        String tier = extAuthMetadata.get(MetadataConstants.TIER);
        boolean isContentAwareTierPresent = Boolean.parseBoolean(extAuthMetadata
                .get(MetadataConstants.CONTENT_AWARE_TIER_PRESENT));
        String apiKey = extAuthMetadata.get(MetadataConstants.API_KEY);
        String keyType = extAuthMetadata.get(MetadataConstants.KEY_TYPE);
        String callerToken = extAuthMetadata.get(MetadataConstants.CALLER_TOKEN);
        int applicationId = -1;
        if (!StringUtils.isEmpty(extAuthMetadata.get(MetadataConstants.APP_ID))) {
            applicationId = Integer.parseInt(extAuthMetadata.get(MetadataConstants.APP_ID));
        }

        String applicationName = extAuthMetadata.get(MetadataConstants.APP_NAME);
        String consumerKey = extAuthMetadata.get(MetadataConstants.CONSUMER_KEY);
        String subscriber = extAuthMetadata.get(MetadataConstants.SUBSCRIBER);
        int spikeArrestLimit = Integer.parseInt(extAuthMetadata.get(MetadataConstants.SPIKE_ARREST_LIMIT));
        String subscriberTenantDomain = extAuthMetadata.get(MetadataConstants.SUBSCRIBER_TENANT_DOMAIN);
        String spikeArrestUnit = extAuthMetadata.get(MetadataConstants.SPIKE_ARREST_UNIT);
        boolean stopOnQuota = Boolean.parseBoolean(extAuthMetadata.get(MetadataConstants.STOP_ON_QUOTA));
        String productName = extAuthMetadata.get(MetadataConstants.PRODUCT_NAME);
        String productProvider = extAuthMetadata.get(MetadataConstants.PRODUCT_PROVIDER);
        String apiPublisher = extAuthMetadata.get(MetadataConstants.API_PUBLISHER);
        String requestId = extAuthMetadata.get(MetadataConstants.REQUEST_ID);

        // Extracting mgw_wasm_websocket filter metadata
        int frameLength = webSocketFrameRequest.getFrameLength();
        String remoteIp = webSocketFrameRequest.getRemoteIp();
        String streamId = extAuthMetadata.get(MetadataConstants.GRPC_STREAM_ID);

        WebSocketFrameContext webSocketFrameContext = new WebSocketFrameContext(streamId, frameLength, remoteIp);

        AuthenticationContext authenticationContext = new AuthenticationContext();
        authenticationContext.setApiName(apiName);
        authenticationContext.setApiVersion(apiVersion);
        authenticationContext.setUsername(username);
        authenticationContext.setApplicationTier(appTier);
        authenticationContext.setTier(tier);
        authenticationContext.setIsContentAware(isContentAwareTierPresent);
        authenticationContext.setApiKey(apiKey);
        authenticationContext.setKeyType(keyType);
        authenticationContext.setCallerToken(callerToken);
        authenticationContext.setApplicationId(applicationId);
        authenticationContext.setApplicationName(applicationName);
        authenticationContext.setConsumerKey(consumerKey);
        authenticationContext.setSubscriber(subscriber);
        authenticationContext.setSpikeArrestLimit(spikeArrestLimit);
        authenticationContext.setSubscriberTenantDomain(subscriberTenantDomain);
        authenticationContext.setSpikeArrestUnit(spikeArrestUnit);
        authenticationContext.setStopOnQuotaReach(stopOnQuota);
        authenticationContext.setProductName(productName);
        authenticationContext.setProductProvider(productProvider);
        authenticationContext.setApiPublisher(apiPublisher);

        return new RequestContext.Builder(apiBasePath).authenticationContext(authenticationContext).
                webSocketFrameContext(webSocketFrameContext).matchedAPI(api.getAPIConfig())
                .requestID(requestId).address(extractIpAddress(remoteIp)).build();
    }

    private String extractIpAddress(String remoteIpStringWithPort) {
        try {
            URI uri = new URI("ws://" + remoteIpStringWithPort);
            return uri.getHost();
        } catch (URISyntaxException e) {
            logger.error(e);
            return null;
        }
    }
}
