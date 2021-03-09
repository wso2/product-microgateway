/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;

/**
 * WebSocketHandler handles requests coming through websocket metadata service.
 */
public class WebSocketHandler implements RequestHandler<RateLimitRequest, AuthenticationContext> {
    private static final Logger logger = LogManager.getLogger(WebSocketHandler.class);
    @Override
    public AuthenticationContext process(RateLimitRequest request) {
        WebSocketAPI matchedAPI = APIFactory.getInstance().getMatchedAPI(request);
        if (matchedAPI == null) {
            return null;
        } else if (logger.isDebugEnabled()) {
            APIConfig api = matchedAPI.getAPIConfig();
            logger.debug("API {}/{} found in the cache", api.getBasePath(), api.getVersion());
        }
        RequestContext webSocketMetadata = buildRequestContext(matchedAPI, request);

        return matchedAPI.processMetadata(webSocketMetadata);
    }

    private RequestContext buildRequestContext(WebSocketAPI api, RateLimitRequest rateLimitRequest) {
        try {
            logger.info("contextString:" + rateLimitRequest.getMetadataContext().getFilterMetadataMap().toString());
        } catch (Exception e) {
            logger.error(e);
        }
        String streamId = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.WEBSOCKET_STREAM_ID)
                .getStringValue();
        String apiName = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.GW_API_NAME_PARAM)
                .getStringValue();
        String apiVersion = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.GW_VERSION_PARAM)
                .getStringValue();
        String apiBasepath = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.GW_BASE_PATH_PARAM)
                .getStringValue();
        String username = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.USERNAME)
                .getStringValue();
        String appTier = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.APP_TIER)
                .getStringValue();
        String tier = rateLimitRequest.getMetadataContext().getFilterMetadataMap().get(APIConstants.EXT_AUTHZ_METADATA)
                .getFieldsMap().get(AuthenticationConstants.TIER).getStringValue();
        String apiTier = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.API_TIER)
                .getStringValue();
        boolean isContentAwareTierPresent = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap()
                .get(AuthenticationConstants.CONTENT_AWARE_TIER_PRESENT).getBoolValue();
        String apiKey = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.API_KEY)
                .getStringValue();
        String keyType = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.KEY_TYPE)
                .getStringValue();
        String callerToken = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.CALLER_TOKEN)
                .getStringValue();
        String applicationId = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.APP_ID)
                .getStringValue();
        String applicationName = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.APP_NAME)
                .getStringValue();
        String consumerKey = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.CONSUMER_KEY)
                .getStringValue();
        String subscriber = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.SUBSCRIBER)
                .getStringValue();
        int spikeArrestLimit = (int) rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.SPIKE_ARREST_LIMIT)
                .getNumberValue();
        String subscriberTenantDomain = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap()
                .get(AuthenticationConstants.SUBSCRIBER_TENANT_DOMAIN).getStringValue();
        String spikeArrestUnit = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.SPIKE_ARREST_UNIT)
                .getStringValue();
        boolean stopOnQuota = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.STOP_ON_QUOTA)
                .getBoolValue();
        String productName = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.PRODUCT_NAME)
                .getStringValue();
        String productProvider = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.PRODUCT_PROVIDER)
                .getStringValue();
        String apiPublisher = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(AuthenticationConstants.API_PUBLISHER)
                .getStringValue();

        int frameLength = (int) rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.MGW_WEB_SOCKET).getFieldsMap().get(APIConstants.FRAME_LENGTH).getNumberValue();
        String upstreamHost = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.MGW_WEB_SOCKET).getFieldsMap().get(APIConstants.UPSTREAM_HOST).getStringValue();

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

        WebSocketMetadataContext webSocketMetadataContext = new WebSocketMetadataContext.Builder(streamId)
                .setAuthenticationContext(authenticationContext).setFrameLength(frameLength)
                .setUpstreamHost(upstreamHost).build();

        return new RequestContext.Builder(apiBasepath).matchedAPI(api).authenticationContext(authenticationContext)
                .webSocketMetadataContext(webSocketMetadataContext).build();
    }
}
