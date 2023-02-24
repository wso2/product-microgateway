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
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
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
import org.wso2.choreo.connect.enforcer.websocket.MetadataConstants;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

        log.debug("Throttle filter received the request");
        Decision decision = doThrottle(requestContext);

        if (APIConstants.WEBSOCKET.equals(requestContext.getHeaders().get(APIConstants.UPGRADE_HEADER))) {
            if (decision.isThrottled() && decision.isDueToBlockedCondition()) {
                log.debug("Throttle filter does not allow the upgrade request, if any relevant blocking"
                        + "conditions are present.");
                return false;
            }
            requestContext.getMetadataMap().put(MetadataConstants.IS_THROTTLED,
                    String.valueOf(decision.isThrottled()));
            if (decision.isThrottled()) {
                requestContext.getMetadataMap().put(MetadataConstants.INITIAL_APIM_ERROR_CODE,
                        requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_CODE).toString());
                requestContext.getMetadataMap().put(MetadataConstants.THROTTLE_CONDITION_EXPIRE_TIMESTAMP,
                        String.valueOf(decision.getResetAt() / 1000));
            }
            log.debug("Throttle filter discarded the request as it is a websocket upgrade request");
            return true;
        }

        if (decision.isThrottled()) {
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
            handleEventPublish(requestContext);
        } finally {
            if (Utils.tracingEnabled()) {
                publishThrottleEventSpanScope.close();
                Utils.finishSpan(publishThrottleEventSpan);
            }
        }
        return true;
    }

    private void handleEventPublish(RequestContext requestContext) {
        for (Map<String, String> event : getThrottleEventMap(requestContext)) {
            ThrottleAgent.publishNonThrottledEvent(event);
        }
    }

    /**
     * Evaluate the throttle policies to find out if the request is throttled at any supported throttling level.
     *
     * @param reqContext request context with all request related details,
     *                   including the authentication details
     * @return {@code Decision} with true for isThrottled property if the request is throttled, otherwise
     * false for isThrottled property with the reset timestamp.
     */
    private Decision doThrottle(RequestContext reqContext) {
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
                String subTier = authContext.getTier();
                String appTier = authContext.getApplicationTier();
                String appTenant = authContext.getSubscriberTenantDomain();
                String clientIp = reqContext.getClientIp();
                String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
                // API Tenant Domain is required to be taken in order to support internal Key scenario.
                // Using apiTenant is valid as the choreo connect does not work in multi-tenant mode.
                String authorizedUser = FilterUtils.buildUsernameWithTenant(authContext.getUsername(),
                        apiTenantDomain);
                String customPropertyString = String.valueOf(reqContext.getProperties()
                        .get(ThrottleConstants.CUSTOM_THROTTLE_PROPERTIES));
                boolean isApiLevelTriggered = false;

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
                        Decision decision = new Decision();
                        decision.setThrottled(true);
                        decision.setDueToBlockedCondition(true);
                        return decision;
                    }
                }

                ArrayList<Decision> apiDecisions = new ArrayList<>();
                // Checking API and Resource level throttling.
                // If API tier is defined,
                // we ignore the resource level tier definition.
                if (!StringUtils.isEmpty(api.getTier())) {
                    isApiLevelTriggered = true;
                    apiDecisions.add(checkResourceThrottled(apiThrottleKey, apiTier, reqContext));
                } else {
                    for (ResourceConfig resourceConfig : reqContext.getMatchedResourcePaths()) {
                        String resourceTier = getResourceTier(resourceConfig);
                        String resourceThrottleKey = getResourceThrottleKey(resourceConfig, apiContext, apiVersion);
                        apiDecisions.add(checkResourceThrottled(resourceThrottleKey, resourceTier, reqContext));
                    }
                }

                long apiThrottleResetAT = 0;
                Decision throttledAPIDecision = null;
                for (Decision apiDecision : apiDecisions) {
                    if (apiDecision.isThrottled() && apiDecision.getResetAt() > apiThrottleResetAT) {
                        apiThrottleResetAT = apiDecision.getResetAt();
                        throttledAPIDecision = apiDecision;
                    }
                }
                if (throttledAPIDecision != null) {
                    int errorCode;
                    String reason;
                    if (isApiLevelTriggered) {
                        errorCode = ThrottleConstants.API_THROTTLE_OUT_ERROR_CODE;
                        reason = ThrottleConstants.THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED;
                    } else {
                        errorCode = ThrottleConstants.RESOURCE_THROTTLE_OUT_ERROR_CODE;
                        reason = ThrottleConstants.THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED;
                    }
                    FilterUtils.setThrottleErrorToContext(reqContext, errorCode,
                            ThrottleConstants.THROTTLE_OUT_MESSAGE, ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                    reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON, reason);
                    ThrottleUtils.setRetryAfterHeader(reqContext, apiThrottleResetAT);
                    return throttledAPIDecision;
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
                        return subDecision;
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
                    return appDecision;
                }

                // Checking Custom policy throttling
                ArrayList<Decision> customDecisions = new ArrayList<>();
                for (ResourceConfig resourceConfig : reqContext.getMatchedResourcePaths()) {
                    String resourceThrottleKey = getResourceThrottleKey(resourceConfig, apiContext, apiVersion);
                    Decision customDecision = dataHolder.isThrottledByCustomPolicy(authorizedUser,
                            resourceThrottleKey, apiContext, apiVersion, appTenant, apiTenantDomain, appId,
                            clientIp, customPropertyString);
                    log.debug("Custom policy throttle decision is {}", customDecision.isThrottled());
                    customDecisions.add(customDecision);
                }
                Decision throttledCustomDecision = null;
                long customThrottleResetAT = 0;
                for (Decision customDecision : customDecisions) {
                    if (customDecision.isThrottled() && customThrottleResetAT < customDecision.getResetAt()) {
                        log.debug("Setting custom policy throttle out response");
                        throttledCustomDecision = customDecision;
                        customThrottleResetAT = customDecision.getResetAt();
                    }
                }
                if (throttledCustomDecision != null) {
                    FilterUtils.setThrottleErrorToContext(reqContext,
                            ThrottleConstants.CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE,
                            ThrottleConstants.THROTTLE_OUT_MESSAGE,
                            ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                    reqContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                            ThrottleConstants.THROTTLE_OUT_REASON_CUSTOM_LIMIT_EXCEED);
                    ThrottleUtils.setRetryAfterHeader(reqContext, customThrottleResetAT);
                    return throttledCustomDecision;
                }
            }
            return new Decision();
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
     * <p>
     * Note: since there could be multiple matching resources for a request (ex: graphQL API request),
     * there could be multiple throttle events
     *
     * @param requestContext request context
     * @return List of Map of throttle event data
     */
    private ArrayList<Map<String, String>> getThrottleEventMap(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        ArrayList<Map<String, String>> throttleEvents = new ArrayList<>();
        Map<String, String> throttleEvent = new HashMap<>();
        APIConfig api = requestContext.getMatchedAPI();

        String basePath = api.getBasePath();
        String apiVersion = api.getVersion();
        String apiContext = basePath + ':' + apiVersion;
        String apiName = api.getName();
        String apiTier = getApiTier(api);
        String tenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
        // API Tenant Domain is required to be taken in order to support internal Key scenario.
        // Using apiTenant is valid as the choreo connect does not work in multi-tenant mode.
        String authorizedUser = FilterUtils.buildUsernameWithTenant(authContext.getUsername(), tenantDomain);


        if (tenantDomain == null) {
            tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }

        throttleEvent.put(ThrottleEventConstants.MESSAGE_ID, requestContext.getRequestID());
        throttleEvent.put(ThrottleEventConstants.APP_KEY, authContext.getApplicationId() + ":" + authorizedUser);
        throttleEvent.put(ThrottleEventConstants.APP_TIER, authContext.getApplicationTier());
        throttleEvent.put(ThrottleEventConstants.API_KEY, apiContext);
        throttleEvent.put(ThrottleEventConstants.API_TIER, apiTier);
        throttleEvent.put(ThrottleEventConstants.SUBSCRIPTION_KEY, authContext.getApplicationId() + ":" +
                apiContext);
        throttleEvent.put(ThrottleEventConstants.SUBSCRIPTION_TIER, authContext.getTier());
        // TODO: (Praminda) should publish with tenant domain?
        throttleEvent.put(ThrottleEventConstants.USER_ID, authorizedUser);
        throttleEvent.put(ThrottleEventConstants.API_CONTEXT, basePath);
        throttleEvent.put(ThrottleEventConstants.API_VERSION, apiVersion);
        throttleEvent.put(ThrottleEventConstants.APP_TENANT, authContext.getSubscriberTenantDomain());
        throttleEvent.put(ThrottleEventConstants.API_TENANT, tenantDomain);
        throttleEvent.put(ThrottleEventConstants.APP_ID, String.valueOf(authContext.getApplicationId()));
        throttleEvent.put(ThrottleEventConstants.API_NAME, apiName);
        throttleEvent.put(ThrottleEventConstants.PROPERTIES, getProperties(requestContext).toString());

        // apiConfig instance will have the tier assigned only if openapi definition contains the
        // extension
        if (!StringUtils.isEmpty(api.getTier())) {
            throttleEvent.put(ThrottleEventConstants.RESOURCE_KEY, apiContext);
            throttleEvent.put(ThrottleEventConstants.RESOURCE_TIER, apiTier);
            throttleEvents.add(throttleEvent);
        } else {
            for (ResourceConfig resourceConfig : requestContext.getMatchedResourcePaths()) {
                Map<String, String> throttleEventClone = new HashMap<>(throttleEvent);
                String resourceTier = getResourceTier(resourceConfig);
                String resourceKey = getResourceThrottleKey(resourceConfig, basePath, apiVersion);
                throttleEventClone.put(ThrottleEventConstants.RESOURCE_KEY, resourceKey);
                throttleEventClone.put(ThrottleEventConstants.RESOURCE_TIER, resourceTier);
                throttleEvents.add(throttleEventClone);
            }
        }
        return throttleEvents;
    }

    private String getResourceThrottleKey(ResourceConfig resourceConfig, String apiContext, String apiVersion) {
        String resourceThrottleKey = apiContext;
        if (!apiVersion.isBlank()) {
            resourceThrottleKey += "/" + apiVersion;
        }
        resourceThrottleKey += resourceConfig.getPath() + ':' + resourceConfig.getMethod();
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
        String customPropertyString = String.valueOf(requestContext.getProperties()
                .get(ThrottleConstants.CUSTOM_THROTTLE_PROPERTIES));

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
                log.error("Error while parsing host IP {}", remoteIP,
                        ErrorDetails.errorLog(LoggingConstants.Severity.MAJOR, 6901), e);
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
            net.minidev.json.JSONObject claims = ThrottleUtils.getJWTClaims(callerToken);
            for (String key : claims.keySet()) {
                jsonObMap.put(key, claims.get(key));
            }
        }

        // If custom throttle properties exist, add custom properties to properties map.
        if (!customPropertyString.equals("null")) {
            String[] customPropertyList = customPropertyString.split(" ");
            for (String customProperty: customPropertyList) {
                String[] propertyPair = customProperty.split("=");
                if (propertyPair.length == 2) {
                    jsonObMap.put(propertyPair[0], propertyPair[1]);
                } else {
                    log.debug("Invalid custom property string : {}", customProperty);
                }
            }
        }

        return jsonObMap;
    }
}
