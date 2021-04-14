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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.api.RequestContext;
import org.wso2.choreo.connect.enforcer.api.WebSocketAPI;
import org.wso2.choreo.connect.enforcer.api.config.APIConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.security.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.websocket.MetadataConstants;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketFrameContext;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketResponseObject;

import java.util.Map;

/**
 * WebSocketHandler handles requests coming through websocket frame service.
 */
public class WebSocketHandler implements RequestHandler<WebSocketFrameRequest, WebSocketResponseObject> {
    private static final Logger logger = LogManager.getLogger(WebSocketHandler.class);

    /**
     *
     * @param webSocketFrameRequest - WebSocketFrameRequest received from WebSocketFrameRequest
     * @return WebSocketResponseObject - Response sent via WebSocketFrameService
     */
    @Override
    public WebSocketResponseObject process(WebSocketFrameRequest webSocketFrameRequest) {
        WebSocketAPI matchedAPI = APIFactory.getInstance().getMatchedAPI(webSocketFrameRequest);
        if (matchedAPI == null) {
            return null;
        } else if (logger.isDebugEnabled()) {
            APIConfig api = matchedAPI.getAPIConfig();
            logger.info("API {}/{} found in the cache", api.getBasePath(), api.getVersion());
        }
        RequestContext requestContext = buildRequestContext(matchedAPI, webSocketFrameRequest);
        logger.info(requestContext.toString());
        return matchedAPI.processFramedata(requestContext);
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
        String apiTier = extAuthMetadata.get(MetadataConstants.API_TIER);
        boolean isContentAwareTierPresent = Boolean.parseBoolean(extAuthMetadata
                .get(MetadataConstants.CONTENT_AWARE_TIER_PRESENT));
        String apiKey = extAuthMetadata.get(MetadataConstants.API_KEY);
        String keyType = extAuthMetadata.get(MetadataConstants.KEY_TYPE);
        String callerToken = extAuthMetadata.get(MetadataConstants.CALLER_TOKEN);
        String applicationId = extAuthMetadata.get(MetadataConstants.APP_ID);
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
        authenticationContext.setApiTier(apiTier);
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
                webSocketFrameContext(webSocketFrameContext).matchedAPI(api).requestID(requestId)
                .address(remoteIp).build();
    }
}
