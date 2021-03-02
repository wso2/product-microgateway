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
package org.wso2.micro.gateway.enforcer.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.throttle.ThrottleAgent;
import org.wso2.micro.gateway.enforcer.throttle.databridge.agent.util.ThrottleEventConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;
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
    private APIConfig apiConfig;

    @Override
    public void init(APIConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        log.info("handle request called");
        ThrottleAgent.publishNonThrottledEvent(getThrottleEventMap(requestContext));
        return true;
    }

    //TODO (amaliMatharaarachchi) Add default values to keys.
    // Handle fault invocations.
    // Test all flows.
    // Add unit tests.
    /**
     * This will generate the throttling event map to be publish to the traffic manager.
     *
     * @param requestContext request context
     * @return Map of throttle event data
     */
    private Map<String, String> getThrottleEventMap(RequestContext requestContext) {
        AuthenticationContext authenticationContext = requestContext.getAuthenticationContext();
        Map<String, String> throttleEvent = new HashMap<>();

        String basePath = requestContext.getMathedAPI().getAPIConfig().getBasePath();
        String apiVersion = requestContext.getMathedAPI().getAPIConfig().getVersion();
        String apiContext = basePath + ":" + apiVersion;
        String apiName = requestContext.getMathedAPI().getAPIConfig().getName();
        String tenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
        if (tenantDomain == null) {
            tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }
        String resourceTier;
        String resourceKey;

        if (!APIConstants.UNLIMITED_TIER.equals(authenticationContext.getApiTier()) &&
                authenticationContext.getApiTier() != null &&
                !authenticationContext.getApiTier().isBlank()) {
            resourceTier = authenticationContext.getApiTier();
            resourceKey = apiContext;
        } else {
            resourceTier = getResourceTier(requestContext.getMatchedResourcePath());
            resourceKey = getResourceThrottleKey(requestContext, apiContext, apiVersion);
        }


        throttleEvent.put(ThrottleEventConstants.MESSAGE_ID, requestContext.getRequestID());
        throttleEvent.put(ThrottleEventConstants.APP_KEY, authenticationContext.getApplicationId() + ":" +
                authenticationContext.getUsername());
        throttleEvent.put(ThrottleEventConstants.APP_TIER, authenticationContext.getApplicationTier());
        throttleEvent.put(ThrottleEventConstants.API_KEY, apiContext);
        throttleEvent.put(ThrottleEventConstants.API_TIER, authenticationContext.getApiTier());
        throttleEvent.put(ThrottleEventConstants.SUBSCRIPTION_KEY, authenticationContext.getApplicationId() + ":" +
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
        String resourceLevelThrottleKey = apiContext;
        if (!apiVersion.isBlank()) {
            resourceLevelThrottleKey += "/" + apiVersion;
        }
        resourceLevelThrottleKey += requestContext.getMatchedResourcePath().getPath() + ":" +
                requestContext.getRequestMethod();
        return resourceLevelThrottleKey;
    }

    private String getResourceTier(ResourceConfig resourceConfig) {
        if (!resourceConfig.getTier().isBlank()) {
            return resourceConfig.getTier();
        }
        return APIConstants.UNLIMITED_TIER;
    }


    private JSONObject getProperties(RequestContext requestContext) {
        String remoteIP = requestContext.getAddress();
        JSONObject jsonObMap = new JSONObject();
        if (remoteIP != null && remoteIP.length() > 0) {
            try {
                InetAddress address = InetAddress.getByName(remoteIP);
                if (address instanceof Inet4Address) {
                    jsonObMap.put(APIConstants.IP, FilterUtils.ipToLong(remoteIP));
                    jsonObMap.put(APIConstants.IPV6, 0);
                } else if (address instanceof Inet6Address) {
                    jsonObMap.put(APIConstants.IPV6, FilterUtils.ipToBigInteger(remoteIP));
                    jsonObMap.put(APIConstants.IP, 0);
                }
            } catch (UnknownHostException e) {
                //send empty value as ip
                log.error("Error while parsing host IP " + remoteIP, e);
                jsonObMap.put(APIConstants.IPV6, 0);
                jsonObMap.put(APIConstants.IP, 0);
            }
        }
        // TODO(amaliMatharaarachchi) Add advance throttling data to additional properties.
        return jsonObMap;
    }
}
