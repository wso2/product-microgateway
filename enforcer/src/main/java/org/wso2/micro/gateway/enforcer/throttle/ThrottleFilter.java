/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.enforcer.throttle;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.config.dto.ThrottleConfigDto;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;
import org.wso2.micro.gateway.enforcer.throttle.databridge.agent.util.ThrottleEventConstants;
import org.wso2.micro.gateway.enforcer.throttle.dto.Decision;
import org.wso2.micro.gateway.enforcer.util.FilterUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the filter handling the authentication for the requests flowing through the gateway.
 */
public class ThrottleFilter implements Filter {
    private static final Logger log = LogManager.getLogger(ThrottleFilter.class);

    private final boolean isGlobalThrottlingEnabled;
    private final ThrottleDataHolder dataHolder;

    public ThrottleFilter() {
        this.dataHolder = ThrottleDataHolder.getInstance();
        this.isGlobalThrottlingEnabled = ConfigHolder.getInstance().getConfig().getThrottleConfig()
                .isGlobalPublishingEnabled();
    }

    @Override
    public void init(APIConfig apiConfig) {
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        log.debug("Throttle filter received the request");

        if (doThrottle(requestContext)) {
            // breaking filter chain since request is throttled
            return false;
        }

        // publish throttle event and continue the filter chain
        ThrottleAgent.publishNonThrottledEvent(getThrottleEventMap(requestContext));
        return true;
    }

    /**
     * Evaluate the throttle policies to find out if the request is throttled at any supported throttling level.
     *
     * @param reqContext request context with all request related details,
     *                   including the authentication details
     * @return {@code true} if the request is throttled, otherwise {@code false}
     */
    private boolean doThrottle(RequestContext reqContext) {
        AuthenticationContext authContext = reqContext.getAuthenticationContext();

        // TODO: (Praminda) Handle unauthenticated + subscription validation false scenarios
        if (reqContext.getAuthenticationContext() != null) {
            log.debug("Found AuthenticationContext for the request");
            APIConfig api = reqContext.getMathedAPI().getAPIConfig();
            String apiContext = api.getBasePath();
            String apiVersion = api.getVersion();
            String appId = authContext.getApplicationId();
            String apiTier = getApiTier(api);
            String apiThrottleKey = getApiThrottleKey(apiContext, apiVersion);
            String resourceTier = getResourceTier(reqContext.getMatchedResourcePath());
            String resourceThrottleKey = getResourceThrottleKey(reqContext, apiContext, apiVersion);
            String subTier = authContext.getTier();
            String appTier = authContext.getApplicationTier();
            boolean isApiLevelTriggered = false;

            if (!StringUtils.isEmpty(apiTier) && !ThrottleConstants.UNLIMITED_TIER.equalsIgnoreCase(apiTier)) {
                resourceThrottleKey = apiThrottleKey;
                resourceTier = apiTier;
                isApiLevelTriggered = true;
            }

            // Checking API and Resource level throttling. If API tier is defined,
            // we ignore the resource level tier definition.
            Decision apiDecision = checkResourceThrottled(resourceThrottleKey, resourceTier, reqContext);
            if (apiDecision.isThrottled()) {
                int errorCode;
                if (isApiLevelTriggered) {
                    errorCode = ThrottleConstants.API_THROTTLE_OUT_ERROR_CODE;
                } else {
                    errorCode = ThrottleConstants.RESOURCE_THROTTLE_OUT_ERROR_CODE;
                }
                FilterUtils.setThrottleErrorToContext(reqContext, errorCode, ThrottleConstants.THROTTLE_OUT_MESSAGE,
                        ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                        ThrottleConstants.THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED);
                ThrottleUtils.setRetryAfterHeader(reqContext, apiDecision.getResetAt());
                return true;
            }

            String subThrottleKey = getSubscriptionThrottleKey(appId, apiContext, apiVersion);
            Decision subDecision = checkSubscriptionLevelThrottled(subThrottleKey, subTier);
            if (subDecision.isThrottled()) {
                if (authContext.isStopOnQuotaReach()) {
                    log.debug("Setting subscription throttle out response");
                    FilterUtils.setThrottleErrorToContext(reqContext,
                            ThrottleConstants.SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE,
                            ThrottleConstants.THROTTLE_OUT_MESSAGE,
                            ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                    reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                            ThrottleConstants.THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED);
                    ThrottleUtils.setRetryAfterHeader(reqContext, subDecision.getResetAt());
                    return true;
                }
                log.debug("Proceeding since stopOnQuotaReach is false");
            }

            String appThrottleKey = appId + ':' + authContext.getUsername();
            Decision appDecision = checkAppLevelThrottled(appThrottleKey, appTier);
            if (appDecision.isThrottled()) {
                log.debug("Setting application throttle out response");
                FilterUtils.setThrottleErrorToContext(reqContext,
                        ThrottleConstants.APPLICATION_THROTTLE_OUT_ERROR_CODE,
                        ThrottleConstants.THROTTLE_OUT_MESSAGE,
                        ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                        ThrottleConstants.THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED);
                ThrottleUtils.setRetryAfterHeader(reqContext, appDecision.getResetAt());
                return true;
            }
        }
        return false;
    }

    private Decision checkSubscriptionLevelThrottled(String throttleKey, String tier) {
        Decision decision = dataHolder.isThrottled(throttleKey);
        log.debug("Subscription Level throttle decision is {} for key:tier {}:{}", decision.isThrottled(),
                throttleKey, tier);
        return decision;
    }

    private Decision checkAppLevelThrottled(String throttleKey, String tier) {
        Decision decision = dataHolder.isThrottled(throttleKey);
        log.debug("Application Level throttle decision is {} for key:tier {}:{}", decision.isThrottled(),
                throttleKey, tier);
        return decision;
    }

    private Decision checkResourceThrottled(String throttleKey, String tier, RequestContext context) {
        log.debug("Checking if request is throttled at API/Resource level for tier: {}, key: {}", tier, throttleKey);
        Decision decision = new Decision();

        if (ThrottleConstants.UNLIMITED_TIER.equals(tier)) {
            return decision;
        }

        if (isGlobalThrottlingEnabled) {
            decision = dataHolder.isAdvancedThrottled(throttleKey, context);
            log.debug("API/Resource Level throttle decision: {}", decision.isThrottled());
            return decision;
        }
        return decision;
    }

    /**
     * This will generate the throttling event map to be publish to the traffic manager.
     *
     * @param requestContext request context
     * @return Map of throttle event data
     */
    private Map<String, String> getThrottleEventMap(RequestContext requestContext) {
        AuthenticationContext authenticationContext = requestContext.getAuthenticationContext();
        Map<String, String> throttleEvent = new HashMap<>();
        APIConfig api = requestContext.getMathedAPI().getAPIConfig();

        String basePath = api.getBasePath();
        String apiVersion = api.getVersion();
        String apiContext = basePath + ':' + apiVersion;
        String apiName = api.getName();
        String apiTier = getApiTier(api);
        String tenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
        if (tenantDomain == null) {
            tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }
        String resourceTier;
        String resourceKey;

        if (!ThrottleConstants.UNLIMITED_TIER.equals(apiTier) && apiTier != null && !apiTier.isBlank()) {
            resourceTier = apiTier;
            resourceKey = apiContext;
        } else {
            resourceTier = getResourceTier(requestContext.getMatchedResourcePath());
            resourceKey = getResourceThrottleKey(requestContext, basePath, apiVersion);
        }

        throttleEvent.put(ThrottleEventConstants.MESSAGE_ID, requestContext.getRequestID());
        throttleEvent.put(ThrottleEventConstants.APP_KEY, authenticationContext.getApplicationId() + ':' +
                authenticationContext.getUsername());
        throttleEvent.put(ThrottleEventConstants.APP_TIER, authenticationContext.getApplicationTier());
        throttleEvent.put(ThrottleEventConstants.API_KEY, apiContext);
        throttleEvent.put(ThrottleEventConstants.API_TIER, apiTier);
        throttleEvent.put(ThrottleEventConstants.SUBSCRIPTION_KEY, authenticationContext.getApplicationId() + ':' +
                apiContext);
        throttleEvent.put(ThrottleEventConstants.SUBSCRIPTION_TIER, authenticationContext.getTier());
        throttleEvent.put(ThrottleEventConstants.RESOURCE_KEY, resourceKey);
        throttleEvent.put(ThrottleEventConstants.RESOURCE_TIER, resourceTier);
        throttleEvent.put(ThrottleEventConstants.USER_ID, authenticationContext.getUsername());
        throttleEvent.put(ThrottleEventConstants.API_CONTEXT, basePath);
        throttleEvent.put(ThrottleEventConstants.API_VERSION, apiVersion);
        throttleEvent.put(ThrottleEventConstants.APP_TENANT, authenticationContext.getSubscriberTenantDomain());
        throttleEvent.put(ThrottleEventConstants.API_TENANT, tenantDomain);
        throttleEvent.put(ThrottleEventConstants.APP_ID, authenticationContext.getApplicationId());
        throttleEvent.put(ThrottleEventConstants.API_NAME, apiName);
        throttleEvent.put(ThrottleEventConstants.PROPERTIES, getProperties(requestContext).toString());
        return throttleEvent;
    }

    private String getResourceThrottleKey(RequestContext requestContext, String apiContext, String apiVersion) {
        String resourceThrottleKey = apiContext;
        if (!apiVersion.isBlank()) {
            resourceThrottleKey += "/" + apiVersion;
        }
        resourceThrottleKey += requestContext.getMatchedResourcePath().getPath() + ':' +
                requestContext.getRequestMethod();
        return resourceThrottleKey;
    }

    private String getApiThrottleKey(String apiContext, String apiVersion) {
        String apiThrottleKey = apiContext;
        if (!apiVersion.isBlank()) {
            apiThrottleKey += ':' + apiVersion;
        }
        return apiThrottleKey;
    }

    private String getSubscriptionThrottleKey(String appId, String apiContext, String apiVersion) {
        String subThrottleKey = appId + ':' + apiContext;
        if (!apiVersion.isBlank()) {
            subThrottleKey += ':' + apiVersion;
        }
        return subThrottleKey;
    }

    private String getResourceTier(ResourceConfig resourceConfig) {
        if (!resourceConfig.getTier().isBlank()) {
            return resourceConfig.getTier();
        }
        return ThrottleConstants.UNLIMITED_TIER;
    }

    private String getApiTier(APIConfig apiConfig) {
        if (!apiConfig.getTier().isBlank()) {
            return apiConfig.getTier();
        }
        return ThrottleConstants.UNLIMITED_TIER;
    }

    private JSONObject getProperties(RequestContext requestContext) {
        String remoteIP = requestContext.getClientIp();
        JSONObject jsonObMap = new JSONObject();
        ThrottleConfigDto config = ConfigHolder.getInstance().getConfig().getThrottleConfig();

        if (remoteIP != null && remoteIP.length() > 0) {
            try {
                InetAddress address = InetAddress.getByName(remoteIP);
                if (address instanceof Inet4Address) {
                    jsonObMap.put(ThrottleConstants.IP, FilterUtils.ipToLong(remoteIP));
                    jsonObMap.put(ThrottleConstants.IPV6, 0);
                } else if (address instanceof Inet6Address) {
                    jsonObMap.put(ThrottleConstants.IPV6, FilterUtils.ipToBigInteger(remoteIP));
                    jsonObMap.put(ThrottleConstants.IP, 0);
                }
            } catch (UnknownHostException e) {
                //send empty value as ip
                log.error("Error while parsing host IP {}", remoteIP, e);
                jsonObMap.put(ThrottleConstants.IPV6, 0);
                jsonObMap.put(ThrottleConstants.IP, 0);
            }
        }

        if (config.isHeaderConditionsEnabled()) {
            Map<String, String> headers = requestContext.getHeaders();
            for (String name : headers.keySet()) {
                jsonObMap.put(name, headers.get(name));
            }
        }

        if (config.isQueryConditionsEnabled()) {
            Map<String, String> params = requestContext.getQueryParameters();
            for (String name : params.keySet()) {
                jsonObMap.put(name, params.get(name));
            }
        }

        String callerToken = requestContext.getAuthenticationContext().getCallerToken();
        if (config.isJwtClaimConditionsEnabled() && callerToken != null) {
            Map<String, String> claims = ThrottleUtils.getJWTClaims(callerToken);
            for (String key : claims.keySet()) {
                jsonObMap.put(key, claims.get(key));
            }
        }

        return jsonObMap;
    }
}
