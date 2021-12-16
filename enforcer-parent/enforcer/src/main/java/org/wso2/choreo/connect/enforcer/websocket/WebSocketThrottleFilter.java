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

import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.json.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottleConfigDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleAgent;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleConstants;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleDataHolder;
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
 * WebSocketThrottleFilter handles the throttling of web socket connections
 */
public class WebSocketThrottleFilter implements Filter {
    private static final Logger log = LogManager.getLogger(WebSocketThrottleFilter.class);

    private final boolean isGlobalThrottlingEnabled;
    private final ThrottleDataHolder dataHolder;

    public WebSocketThrottleFilter() {
        this.dataHolder = ThrottleDataHolder.getInstance();
        this.isGlobalThrottlingEnabled = ConfigHolder.getInstance().getConfig().getThrottleConfig()
                .isGlobalPublishingEnabled();
    }

    @Override public boolean handleRequest(RequestContext requestContext) {
        TracingSpan wsSpan = null;
        Scope wsSpanScope = null;
        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                wsSpan = Utils.startSpan(TracingConstants.WS_THROTTLE_SPAN, tracer);
                wsSpanScope = wsSpan.getSpan().makeCurrent();
                Utils.setTag(wsSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));

            }
            if (doThrottle(requestContext)) {
                // breaking filter chain since request is throttled
                return false;
            }
            // publish throttle event and continue the filter chain
            ThrottleAgent.publishNonThrottledEvent(getThrottleEventMap(requestContext));
            return true;
        } finally {
            if (Utils.tracingEnabled()) {
                wsSpanScope.close();
                Utils.finishSpan(wsSpan);
            }
        }

    }

    private boolean doThrottle(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        if (authContext != null) {
            APIConfig api = requestContext.getMatchedAPI();
            String apiContext = api.getBasePath();
            String apiVersion = api.getVersion();
            int appId = authContext.getApplicationId();
            String apiTier = getApiTier(api);
            String resourceTier = getApiTier(api);
            String apiThrottleKey = getApiThrottleKey(apiContext, apiVersion);
            String subTier = authContext.getTier();
            String appTier = authContext.getApplicationTier();
            String appTenant = authContext.getSubscriberTenantDomain();
            String clientIp = requestContext.getWebSocketFrameContext().getRemoteIp();
            String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
            String authorizedUser = FilterUtils.buildUsernameWithTenant(authContext.getUsername(), appTenant);
            if (apiTenantDomain == null) {
                apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
            }

            // Check for blocking conditions
            if (dataHolder.isBlockingConditionsPresent()) {
                String appBlockingKey = authContext.getSubscriber() + ":" + authContext.getApplicationName();
                String subBlockingKey = apiContext + ":" + apiVersion + ":" + authContext.getSubscriber()
                        + "-" + authContext.getApplicationName() + ":" + authContext.getKeyType();

                if (dataHolder.isRequestBlocked(apiContext, appBlockingKey, authorizedUser, clientIp,
                        subBlockingKey, apiTenantDomain)) {
                    FilterUtils.setThrottleErrorToContext(requestContext,
                            ThrottleConstants.BLOCKED_ERROR_CODE,
                            ThrottleConstants.BLOCKING_MESSAGE,
                            ThrottleConstants.BLOCKING_DESCRIPTION);
                    requestContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                            ThrottleConstants.THROTTLE_OUT_REASON_REQUEST_BLOCKED);
                    log.debug("Request blocked as it violates blocking conditions, for API: {}," +
                            " application: {}, user: {}", apiContext, appBlockingKey, authorizedUser);
                    return true;
                }
            }

            // Checking API level throttling.
            Decision apiDecision = checkApiThrottled(apiThrottleKey, apiTier, requestContext);
            if (apiDecision.isThrottled()) {
                log.debug("Setting api throttle out response");
                int errorCode;
                String reason;
                errorCode = ThrottleConstants.API_THROTTLE_OUT_ERROR_CODE;
                reason = ThrottleConstants.THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED;
                FilterUtils.setThrottleErrorToContext(requestContext, errorCode, ThrottleConstants.THROTTLE_OUT_MESSAGE,
                        ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                requestContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON, reason);
                ThrottleUtils.setRetryAfterWebsocket(requestContext, apiDecision.getResetAt());
                return true;
            }

            // Checking subscription level throttling.
            String subThrottleKey = getSubscriptionThrottleKey(appId, apiContext, apiVersion);
            Decision subDecision = checkSubscriptionLevelThrottled(subThrottleKey, subTier);
            if (subDecision.isThrottled()) {
                if (authContext.isStopOnQuotaReach()) {
                    log.debug("Setting subscription throttle out response");
                    FilterUtils.setThrottleErrorToContext(requestContext,
                            ThrottleConstants.SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE,
                            ThrottleConstants.THROTTLE_OUT_MESSAGE,
                            ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                    requestContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                            ThrottleConstants.THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED);
                    ThrottleUtils.setRetryAfterWebsocket(requestContext, subDecision.getResetAt());
                    return true;
                }
                log.debug("Proceeding since stopOnQuotaReach is false");
            }

            // Checking Application level throttling
            String appThrottleKey = appId + ":" + authorizedUser;
            Decision appDecision = checkAppLevelThrottled(appThrottleKey, appTier);
            if (appDecision.isThrottled()) {
                log.debug("Setting application throttle out response");
                FilterUtils.setThrottleErrorToContext(requestContext,
                        ThrottleConstants.APPLICATION_THROTTLE_OUT_ERROR_CODE,
                        ThrottleConstants.THROTTLE_OUT_MESSAGE,
                        ThrottleConstants.THROTTLE_OUT_DESCRIPTION);
                requestContext.getProperties().put(ThrottleConstants.THROTTLE_OUT_REASON,
                        ThrottleConstants.THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED);
                ThrottleUtils.setRetryAfterWebsocket(requestContext, appDecision.getResetAt());
                return true;
            }
        }
        return false;
    }

    private String getApiTier(APIConfig apiConfig) {
        if (!apiConfig.getTier().isBlank()) {
            return apiConfig.getTier();
        }
        return ThrottleConstants.UNLIMITED_TIER;
    }

    private String getApiThrottleKey(String apiContext, String apiVersion) {
        String apiThrottleKey = apiContext;
        if (!apiVersion.isBlank()) {
            apiThrottleKey += ':' + apiVersion;
        }
        return apiThrottleKey;
    }

    private String getSubscriptionThrottleKey(int appId, String apiContext, String apiVersion) {
        String subThrottleKey = appId + ':' + apiContext;
        if (!apiVersion.isBlank()) {
            subThrottleKey += ':' + apiVersion;
        }
        return subThrottleKey;
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
        String authorizedUser = FilterUtils.buildUsernameWithTenant(authContext.getUsername(), appTenant);

        if (tenantDomain == null) {
            tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }

        throttleEvent.put(ThrottleEventConstants.MESSAGE_ID, requestContext.getRequestID());
        throttleEvent.put(ThrottleEventConstants.APP_KEY, authContext.getApplicationId() + ":" + authorizedUser);
        throttleEvent.put(ThrottleEventConstants.APP_TIER, authContext.getApplicationTier());
        throttleEvent.put(ThrottleEventConstants.API_KEY, apiContext);
        throttleEvent.put(ThrottleEventConstants.API_TIER, apiTier);
        throttleEvent.put(ThrottleEventConstants.RESOURCE_TIER, apiTier);
        throttleEvent.put(ThrottleEventConstants.RESOURCE_KEY, apiContext);
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
        return throttleEvent;
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

        String callerToken = requestContext.getAuthenticationContext().getCallerToken();
        if (config.isJwtClaimConditionsEnabled() && callerToken != null) {
            Map<String, String> claims = ThrottleUtils.getJWTClaims(callerToken);
            for (String key : claims.keySet()) {
                jsonObMap.put(key, claims.get(key));
            }
        }

        int frameLength = requestContext.getWebSocketFrameContext().getFrameLength();
        jsonObMap.put(MetadataConstants.MESSAGE_SIZE, frameLength);

        return jsonObMap;
    }

    private Decision checkApiThrottled(String throttleKey, String tier, RequestContext context) {
        log.debug("Checking if request is throttled at API level for tier: {}, key: {}", tier, throttleKey);
        Decision decision = new Decision();

        if (ThrottleConstants.UNLIMITED_TIER.equals(tier)) {
            return decision;
        }

        if (isGlobalThrottlingEnabled) {
            decision = dataHolder.isAdvancedThrottled(throttleKey, context);
            log.debug("API Level throttle decision: {}", decision.isThrottled());
            return decision;
        }
        return decision;
    }
}
