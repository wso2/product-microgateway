/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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
package org.wso2.choreo.connect.enforcer.api;

import org.wso2.choreo.connect.discovery.api.EndpointClusterConfig;
import org.wso2.choreo.connect.discovery.api.Operation;
import org.wso2.choreo.connect.discovery.api.Scopes;
import org.wso2.choreo.connect.discovery.api.SecurityList;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.ExtendedOperation;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RetryConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.AuthHeaderDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility Methods used across different APIs.
 */
public class Utils {

    /**
     * Construct Enforcer Specific DTOs for endpoints from XDS specific DTOs.
     *
     * @param rpcEndpointCluster XDS specific EndpointCluster Instance
     * @return Enforcer Specific XDS cluster instance
     */
    public static EndpointCluster processEndpoints(org.wso2.choreo.connect.discovery.api.EndpointCluster
                                                           rpcEndpointCluster) {
        if (rpcEndpointCluster == null || rpcEndpointCluster.getUrlsCount() == 0) {
            return null;
        }
        List<String> urls = new ArrayList<>(1);
        rpcEndpointCluster.getUrlsList().forEach(endpoint -> {
            String url = endpoint.getURLType().toLowerCase() + "://" +
                    endpoint.getHost() + ":" + endpoint.getPort() + endpoint.getBasepath();
            urls.add(url);
        });
        EndpointCluster endpointCluster = new EndpointCluster();
        endpointCluster.setUrls(urls);

        if (rpcEndpointCluster.hasConfig()) {
            EndpointClusterConfig endpointClusterConfig = rpcEndpointCluster.getConfig();
            if (endpointClusterConfig.hasRetryConfig()) {
                org.wso2.choreo.connect.discovery.api.RetryConfig rpcRetryConfig
                        = endpointClusterConfig.getRetryConfig();
                RetryConfig retryConfig = new RetryConfig(rpcRetryConfig.getCount(),
                        rpcRetryConfig.getStatusCodesList().toArray(new Integer[0]));
                endpointCluster.setRetryConfig(retryConfig);
            }
            if (endpointClusterConfig.hasTimeoutConfig()) {
                org.wso2.choreo.connect.discovery.api.TimeoutConfig timeoutConfig
                        = endpointClusterConfig.getTimeoutConfig();
                endpointCluster.setRouteTimeoutInMillis(timeoutConfig.getRouteTimeoutInMillis());
            }
        }
        return endpointCluster;
    }

    static void populateRemoveAndProtectedHeaders(RequestContext requestContext) {
        // If the resource has disabled security, then the authorization headers are
        // passed as it is.
        // Expectation is that the backend should validate the authorization header if
        // it is not processed
        // at the gateway level.
        // It is required to check if the resource Path is not null, because when CORS
        // preflight request handling, or
        // generic OPTIONS method call happens, matchedResourcePath becomes null.
        if (requestContext.getMatchedResourcePath() != null &&
                requestContext.getMatchedResourcePath().isDisableSecurity()) {
            return;
        }

        // Choreo-API-Key is considered as a protected header, hence header value should be treated
        // same as other security headers.
        if (ConfigHolder.getInstance().getConfig().getApiKeyConfig().getApiKeyInternalHeaders() != null) {
            requestContext.getProtectedHeaders().addAll(ConfigHolder.getInstance().getConfig().getApiKeyConfig()
                    .getApiKeyInternalHeaders().stream().map(String::toLowerCase).collect(Collectors.toList()));
        }

        // Internal-Key credential is considered to be protected headers, such that the
        // header would not be sent
        // to backend and traffic manager.
        if (ConfigHolder.getInstance().getConfig().getAuthHeader().isDropConsoleTestHeaders()) {
            String internalKeyHeader = ConfigHolder.getInstance().getConfig().getAuthHeader()
                    .getTestConsoleHeaderName().toLowerCase();
            if (!keepHeaderForMCP(requestContext)) {
                requestContext.getRemoveHeaders().add(internalKeyHeader);
            }
            // Avoid internal key being published to the Traffic Manager
            requestContext.getProtectedHeaders().add(internalKeyHeader);
        }

        // If the temp test console headers are in active mode,
        // then those headers are also removed and considered as protected.
        String tempConsoleTestHeadersMode = ConfigHolder.getInstance().getConfig().getAuthHeader()
                .getTempTestConsoleTestHeadersMode();
        if (Constants.TEMP_CONSOLE_TEST_HEADERS_ACTIVE_MODE.equals(tempConsoleTestHeadersMode) &&
                ConfigHolder.getInstance().getConfig().getAuthHeader().isDropConsoleTestHeaders()) {
            List<String> tempConsoleTestHeaders = ConfigHolder.getInstance().getConfig().getAuthHeader()
                    .getTempTestConsoleHeaderNames();
            requestContext.getRemoveHeaders().addAll(tempConsoleTestHeaders);
            requestContext.getProtectedHeaders().addAll(tempConsoleTestHeaders);
        }

        // Remove Authorization Header
        AuthHeaderDto authHeader = ConfigHolder.getInstance().getConfig().getAuthHeader();
        String authHeaderName = FilterUtils.getAuthHeaderName(requestContext);
        // We need to keep the Authorization header in case of MCP APIs as we need to pass it to the underlying API.
        if (!authHeader.isEnableOutboundAuthHeader() && !keepHeaderForMCP(requestContext)) {
            requestContext.getRemoveHeaders().add(authHeaderName);
        }
        // Authorization Header should not be included in the throttle publishing event.
        requestContext.getProtectedHeaders().add(authHeaderName);
    }

    public static ResourceConfig buildResource(Operation operation, String resPath, Map<String,
            List<String>> apiLevelSecurityList) {
        ResourceConfig resource = new ResourceConfig();
        resource.setPath(resPath);
        resource.setMethod(ResourceConfig.HttpMethods.valueOf(operation.getMethod().toUpperCase()));
        resource.setTier(operation.getTier());
        resource.setDisableSecurity(operation.getDisableSecurity());
        Map<String, List<String>> securityMap = new HashMap<>();
        if (operation.getSecurityList().size() > 0) {
            for (SecurityList securityList : operation.getSecurityList()) {
                for (Map.Entry<String, Scopes> entry : securityList.getScopeListMap().entrySet()) {
                    securityMap.put(entry.getKey(), new ArrayList<>());
                    if (entry.getValue() != null && entry.getValue().getScopesList().size() > 0) {
                        List<String> scopeList = new ArrayList<>(entry.getValue().getScopesList());
                        securityMap.replace(entry.getKey(), scopeList);
                    }
                    // only supports security scheme OR combinations. Example -
                    // Security:
                    // - api_key: []
                    //   oauth: [] <-- AND operation is not supported hence ignoring oauth here.
                    break;
                }
            }
            resource.setSecuritySchemas(securityMap);
        } else {
            resource.setSecuritySchemas(apiLevelSecurityList);
        }
        return resource;
    }

    /**
     * Check whether the auth headers should be kept for an MCP API.
     *
     * @param requestContext request context
     * @return true if the auth headers should be kept, false otherwise
     */
    private static boolean keepHeaderForMCP(RequestContext requestContext) {
        String apiType = requestContext.getMatchedAPI().getApiType();
        List<ExtendedOperation> operations = requestContext.getMatchedAPI().getExtendedOperations();
        String subType = "";
        if (operations != null && !operations.isEmpty()) {
            subType = operations.get(0).getMode();
        }

        return APIConstants.ApiType.MCP.equals(apiType) && "PROXY_EXISTING_REST_API".equalsIgnoreCase(subType);
    }
}
