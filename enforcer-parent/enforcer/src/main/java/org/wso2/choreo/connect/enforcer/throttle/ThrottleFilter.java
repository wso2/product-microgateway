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
package org.wso2.choreo.connect.enforcer.throttle;

import io.opentelemetry.context.Scope;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.json.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottleConfigDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.ThrottleEventConstants;
import org.wso2.choreo.connect.enforcer.throttle.dto.Decision;
import org.wso2.choreo.connect.enforcer.throttle.utils.ThrottleUtils;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

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
    public boolean handleRequest(RequestContext requestContext) {

        // If global throttle event publishing is disabled, throttle filter should be skipped.
        if (!ConfigHolder.getInstance().getConfig().getThrottleConfig().isGlobalPublishingEnabled()) {
            return true;
        }

        if (APIConstants.WEBSOCKET.equals(requestContext.getHeaders().get(APIConstants.UPGRADE_HEADER))) {
            log.debug("Throttle filter discarded the request as it is a websocket upgrade request");
            return true;
        }

        log.debug("Throttle filter received the request");
        if (doThrottle(requestContext)) {
            // breaking filter chain since request is throttled
            return false;
        }
        TracingSpan publishThrottleEventSpan = null;
        Scope publishThrottleEventSpanScope = null;
        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                publishThrottleEventSpan = Utils.startSpan(TracingConstants.PUBLISH_THROTTLE_EVENT_SPAN, tracer);
                publishThrottleEventSpanScope = publishThrottleEventSpan.getSpan().makeCurrent();
                Utils.setTag(publishThrottleEventSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));
            }
            // publish throttle event and continue the filter chain
            ThrottleAgent.publishNonThrottledEvent(getThrottleEventMap(requestContext));
        } finally {
            if (Utils.tracingEnabled()) {
                publishThrottleEventSpanScope.close();
                Utils.finishSpan(publishThrottleEventSpan);
            }
        }

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
        TracingSpan doThrottleSpan = null;
        Scope doThrottleSpanScope = null;
        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                doThrottleSpan = Utils.startSpan(TracingConstants.DO_THROTTLE_SPAN, tracer);
                doThrottleSpanScope = doThrottleSpan.getSpan().makeCurrent();
                Utils.setTag(doThrottleSpan, APIConstants.LOG_TRACE_ID, ThreadContext.get(APIConstants.LOG_TRACE_ID));
            }
            AuthenticationContext authContext = reqContext.getAuthenticationContext();

            // TODO: (Praminda) Handle unauthenticated + subscription validation false scenarios
            if (authContext != null) {
                log.debug("Found AuthenticationContext for the request");
                APIConfig api = reqContext.getMatchedAPI();
                String apiContext = api.getBasePath();
                String apiVersion = api.getVersion();
                int appId = authContext.getApplicationId();
                String apiTier = getApiTier(api);
                String apiThrottleKey = getApiThrottleKey(apiContext, apiVersion);
                String resourceTier = getResourceTier(reqContext.getMatchedResourcePath());
                String resourceThrottleKey = getResourceThrottleKey(reqContext, apiContext, apiVersion);
                String subTier = authContext.getTier();
                String appTier = authContext.getApplicationTier();
                String appTenant = authContext.getSubscriberTenantDomain();
                String clientIp = reqContext.getClientIp();
                String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
                // API Tenant Domain is required to be taken in order to support internal Key scenario.
                // Using apiTenant is valid as the choreo connect does not work in multi-tenant mode.
                String authorizedUser = FilterUtils.buildUsernameWithTenant(authContext.getUsername(),
                        apiTenantDomain);
                boolean isApiLevelTriggered = false;

                if (!StringUtils.isEmpty(api.getTier())) {
                    resourceThrottleKey = apiThrottleKey;
                    resourceTier = apiTier;
                    isApiLevelTriggered = true;
                }
                if (apiTenantDomain == null) {
                    apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
                }

                if (dataHolder.isBlockingConditionsPresent()) {
                    String appBlockingKey = authContext.getSubscriber() + ":" + authContext.getApplicationName();
                    String subBlockingKey = apiContext + ":" + apiVersion + ":" + authContext.getSubscriber()
                            + "-" + authContext.getApplicationName() + ":" + authContext.getKeyType();

                    if (dataHolder.isRequestBlocked(apiContext, appBlockingKey, authorizedUser,
                            reqContext.getClientIp(), subBlockingKey, apiTenantDomain)) {
                        FilterUtils.setThrottleErrorToContext(reqContext,
                                ThrottleConstants.BLOCKED_ERROR_CODE,
                                ThrottleConstants.BLOCKING_MESSAGE,
                                ThrottleConstants.BLOCKING_DESCRIPTION);
                        reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                                ThrottleConstants.THROTTLE_OUT_REASON_REQUEST_BLOCKED);
                        log.debug("Request blocked as it violates blocking conditions, for API: {}," +
                                " application: {}, user: {}", apiContext, appBlockingKey, authorizedUser);
                        return true;
                    }
                }

                // Checking API and Resource level throttling. If API tier is defined,
                // we ignore the resource level tier definition.
                Decision apiDecision = checkResourceThrottled(resourceThrottleKey, resourceTier, reqContext);
                if (apiDecision.isThrottled()) {
                    int errorCode;
                    String reason;
                    if (isApiLevelTriggered) {
                        errorCode = ThrottleConstants.API_THROTTLE_OUT_ERROR_CODE;
                        reason = ThrottleConstants.THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED;
                    } else {
                        errorCode = ThrottleConstants.RESOURCE_THROTTLE_OUT_ERROR_CODE;
                        reason = ThrottleConstants.THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED;
                    }
                    FilterUtils.setThrottleErrorToContext(reqContext, errorCode, ThrottleConstants.THROTTLE_OUT_MESSAGE,
                            ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                    reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON, reason);
                    ThrottleUtils.setRetryAfterHeader(reqContext, apiDecision.getResetAt());
                    return true;
                }

                // Checking subscription level throttling
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

                // Checking Application level throttling
                String appThrottleKey = appId + ":" + authorizedUser;
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

                // Checking Custom policy throttling
                Decision customDecision = dataHolder.isThrottledByCustomPolicy(authorizedUser, resourceThrottleKey,
                        apiContext, apiVersion, appTenant, apiTenantDomain, appId, clientIp);
                log.debug("Custom policy throttle decision is {}", customDecision.isThrottled());
                if (customDecision.isThrottled()) {
                    log.debug("Setting custom policy throttle out response");
                    FilterUtils.setThrottleErrorToContext(reqContext,
                            ThrottleConstants.CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE,
                            ThrottleConstants.THROTTLE_OUT_MESSAGE,
                            ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                    reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                            ThrottleConstants.THROTTLE_OUT_REASON_CUSTOM_LIMIT_EXCEED);
                    ThrottleUtils.setRetryAfterHeader(reqContext, customDecision.getResetAt());
                    return true;
                }
            }
            return false;
        } finally {
            if (Utils.tracingEnabled()) {
                doThrottleSpanScope.close();
                Utils.finishSpan(doThrottleSpan);
            }

        }
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
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        Map<String, String> throttleEvent = new HashMap<>();
        APIConfig api = requestContext.getMatchedAPI();

        String basePath = api.getBasePath();
        String apiVersion = api.getVersion();
        String apiContext = basePath + ':' + apiVersion;
        String apiName = api.getName();
        String apiTier = getApiTier(api);
        String tenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
        String appTenant = authContext.getSubscriberTenantDomain();
        // API Tenant Domain is required to be taken in order to support internal Key scenario.
        // Using apiTenant is valid as the choreo connect does not work in multi-tenant mode.
        String authorizedUser = FilterUtils.buildUsernameWithTenant(authContext.getUsername(), tenantDomain);
        String resourceTier;
        String resourceKey;

        if (tenantDomain == null) {
            tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }

        // apiConfig instance will have the tier assigned only if openapi definition contains the
        // extension
        if (!StringUtils.isEmpty(api.getTier())) {
            resourceTier = apiTier;
            resourceKey = apiContext;
        } else {
            resourceTier = getResourceTier(requestContext.getMatchedResourcePath());
            resourceKey = getResourceThrottleKey(requestContext, basePath, apiVersion);
        }

        throttleEvent.put(ThrottleEventConstants.MESSAGE_ID, requestContext.getRequestID());
        throttleEvent.put(ThrottleEventConstants.APP_KEY, authContext.getApplicationId() + ":" + authorizedUser);
        throttleEvent.put(ThrottleEventConstants.APP_TIER, authContext.getApplicationTier());
        throttleEvent.put(ThrottleEventConstants.API_KEY, apiContext);
        throttleEvent.put(ThrottleEventConstants.API_TIER, apiTier);
        throttleEvent.put(ThrottleEventConstants.SUBSCRIPTION_KEY, authContext.getApplicationId() + ":" +
                apiContext);
        throttleEvent.put(ThrottleEventConstants.SUBSCRIPTION_TIER, authContext.getTier());
        throttleEvent.put(ThrottleEventConstants.RESOURCE_KEY, resourceKey);
        throttleEvent.put(ThrottleEventConstants.RESOURCE_TIER, resourceTier);
        // TODO: (Praminda) should publish with tenant domain?
        throttleEvent.put(ThrottleEventConstants.USER_ID, authorizedUser);
        throttleEvent.put(ThrottleEventConstants.API_CONTEXT, basePath);
        throttleEvent.put(ThrottleEventConstants.API_VERSION, apiVersion);
        throttleEvent.put(ThrottleEventConstants.APP_TENANT, authContext.getSubscriberTenantDomain());
        throttleEvent.put(ThrottleEventConstants.API_TENANT, tenantDomain);
        throttleEvent.put(ThrottleEventConstants.APP_ID, String.valueOf(authContext.getApplicationId()));
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

    private String getSubscriptionThrottleKey(int appId, String apiContext, String apiVersion) {
        String subThrottleKey = appId + ":" + apiContext;
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
        if (!StringUtils.isEmpty(apiConfig.getTier())) {
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
                // To avoid publishing user token to the traffic manager.
                if (requestContext.getProtectedHeaders().contains(name)) {
                    continue;
                }
                // Sending path header is stopped as it could contain query parameters which are used
                // to secure APIs.
                if (name.equals(APIConstants.PATH_HEADER)) {
                    continue;
                }
                jsonObMap.put(name, headers.get(name));
            }
        }

        if (config.isQueryConditionsEnabled()) {
            Map<String, String> params = requestContext.getQueryParameters();
            for (String name : params.keySet()) {
                // To avoid publishing apiKey to the traffic manager.
                if (requestContext.getQueryParamsToRemove().contains(name)) {
                    continue;
                }
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
