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
package org.wso2.micro.gateway.enforcer.server;

import com.google.protobuf.Struct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.api.APIFactory;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.WebSocketAPI;
import org.wso2.micro.gateway.enforcer.api.WebSocketMetadataContext;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.AuthenticationConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;
import org.wso2.micro.gateway.enforcer.websocket.WebSocketFrameRequest;

/**
 * WebSocketHandler handles requests coming through websocket metadata service.
 */
public class WebSocketHandler implements RequestHandler<WebSocketFrameRequest, AuthenticationContext> {
    private static final Logger logger = LogManager.getLogger(WebSocketHandler.class);
    @Override
    public AuthenticationContext process(WebSocketFrameRequest webSocketFrameRequest) {
//        WebSocketAPI matchedAPI = APIFactory.getInstance().getMatchedAPI(webSocketFrameRequest);
//        if (matchedAPI == null) {
//            return null;
//        } else if (logger.isDebugEnabled()) {
//            APIConfig api = matchedAPI.getAPIConfig();
//            logger.debug("API {}/{} found in the cache", api.getBasePath(), api.getVersion());
//        }
//        RequestContext webSocketMetadata = buildRequestContext(matchedAPI, webSocketFrameRequest);
//
//        return matchedAPI.processMetadata(webSocketMetadata);
        return null;
    }

    private RequestContext buildRequestContext(WebSocketAPI api, WebSocketFrameRequest webSocketFrameRequest) {
        try {
            logger.info("contextString:" + webSocketFrameRequest.getFilterMetadata().toString());
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
//        // google.protobuf.struct that holds the dynamic metadata from ext_auth filter
//        Struct externalAuthMetadata = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
//                .get(APIConstants.EXT_AUTHZ_METADATA);
//        // google.protobuf.struct that holds dynamic metadata from mgw_websocket filter
//        Struct mgwWebSocketMetadata = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
//                .get(APIConstants.MGW_WEB_SOCKET);
//        String streamId = externalAuthMetadata.getFieldsMap().get(APIConstants.WEBSOCKET_STREAM_ID).getStringValue();
//        String apiName = externalAuthMetadata.getFieldsMap().get(APIConstants.GW_API_NAME_PARAM).getStringValue();
//        String apiVersion = externalAuthMetadata.getFieldsMap().get(APIConstants.GW_VERSION_PARAM).getStringValue();
//        String apiBasepath = externalAuthMetadata.getFieldsMap().get(APIConstants.GW_BASE_PATH_PARAM).getStringValue();
//        String username = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.USERNAME).getStringValue();
//        String appTier = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.APP_TIER).getStringValue();
//        String tier = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.TIER).getStringValue();
//        String apiTier = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.API_TIER).getStringValue();
//        boolean isContentAwareTierPresent = externalAuthMetadata.getFieldsMap()
//                .get(AuthenticationConstants.CONTENT_AWARE_TIER_PRESENT).getBoolValue();
//        String apiKey = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.API_KEY).getStringValue();
//        String keyType = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.KEY_TYPE).getStringValue();
//        String callerToken = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.CALLER_TOKEN)
//                .getStringValue();
//        String applicationId = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.APP_ID).getStringValue();
//        String applicationName = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.APP_NAME)
//                .getStringValue();
//        String consumerKey = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.CONSUMER_KEY)
//                .getStringValue();
//        String subscriber = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.SUBSCRIBER)
//                .getStringValue();
//        int spikeArrestLimit = (int) externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.SPIKE_ARREST_LIMIT)
//                .getNumberValue();
//        String subscriberTenantDomain = externalAuthMetadata.getFieldsMap()
//                .get(AuthenticationConstants.SUBSCRIBER_TENANT_DOMAIN).getStringValue();
//        String spikeArrestUnit = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.SPIKE_ARREST_UNIT)
//                .getStringValue();
//        boolean stopOnQuota = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.STOP_ON_QUOTA)
//                .getBoolValue();
//        String productName = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.PRODUCT_NAME)
//                .getStringValue();
//        String productProvider = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.PRODUCT_PROVIDER)
//                .getStringValue();
//        String apiPublisher = externalAuthMetadata.getFieldsMap().get(AuthenticationConstants.API_PUBLISHER)
//                .getStringValue();
//
//        int frameLength = (int) mgwWebSocketMetadata.getFieldsMap().get(APIConstants.FRAME_LENGTH).getNumberValue();
//        String upstreamHost = mgwWebSocketMetadata.getFieldsMap().get(APIConstants.UPSTREAM_HOST).getStringValue();
//        String remoteIp = mgwWebSocketMetadata.getFieldsMap().get(APIConstants.REMOTE_IP).getStringValue();
//
//        AuthenticationContext authenticationContext = new AuthenticationContext();
//        authenticationContext.setApiName(apiName);
//        authenticationContext.setApiVersion(apiVersion);
//        authenticationContext.setUsername(username);
//        authenticationContext.setApplicationTier(appTier);
//        authenticationContext.setTier(tier);
//        authenticationContext.setApiTier(apiTier);
//        authenticationContext.setIsContentAware(isContentAwareTierPresent);
//        authenticationContext.setApiKey(apiKey);
//        authenticationContext.setKeyType(keyType);
//        authenticationContext.setCallerToken(callerToken);
//        authenticationContext.setApplicationId(applicationId);
//        authenticationContext.setApplicationName(applicationName);
//        authenticationContext.setConsumerKey(consumerKey);
//        authenticationContext.setSubscriber(subscriber);
//        authenticationContext.setSpikeArrestLimit(spikeArrestLimit);
//        authenticationContext.setSubscriberTenantDomain(subscriberTenantDomain);
//        authenticationContext.setSpikeArrestUnit(spikeArrestUnit);
//        authenticationContext.setStopOnQuotaReach(stopOnQuota);
//        authenticationContext.setProductName(productName);
//        authenticationContext.setProductProvider(productProvider);
//        authenticationContext.setApiPublisher(apiPublisher);
//
//        WebSocketMetadataContext webSocketMetadataContext = new WebSocketMetadataContext.Builder(streamId)
//                .setAuthenticationContext(authenticationContext).setFrameLength(frameLength).setRemoteIp(remoteIp)
//                .setUpstreamHost(upstreamHost).build();
//
//        return new RequestContext.Builder(apiBasepath).matchedAPI(api).authenticationContext(authenticationContext)
//                .webSocketMetadataContext(webSocketMetadataContext).build();
    }
}
