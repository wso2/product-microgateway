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
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.RetryConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.AuthHeaderDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        Map<String, SecuritySchemaConfig> securitySchemeDefinitions = requestContext.getMatchedAPI()
                .getSecuritySchemeDefinitions();
        // API key headers are considered to be protected headers, such that the header
        // would not be sent
        // to backend and traffic manager.
        // This would prevent leaking credentials, even if user is invoking unsecured
        // resource with some
        // credentials.
        for (Map.Entry<String, SecuritySchemaConfig> entry : securitySchemeDefinitions.entrySet()) {
            SecuritySchemaConfig schema = entry.getValue();
            if (APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME.equalsIgnoreCase(schema.getType())) {
                if (APIConstants.SWAGGER_API_KEY_IN_HEADER.equals(schema.getIn())) {
                    requestContext.getProtectedHeaders().add(schema.getName());
                    requestContext.getRemoveHeaders().add(schema.getName());
                    continue;
                }
                if (APIConstants.SWAGGER_API_KEY_IN_QUERY.equals(schema.getIn())) {
                    requestContext.getQueryParamsToRemove().add(schema.getName());
                }
            }
        }

        // Internal-Key credential is considered to be protected headers, such that the
        // header would not be sent
        // to backend and traffic manager.
        String internalKeyHeader = ConfigHolder.getInstance().getConfig().getAuthHeader()
                .getTestConsoleHeaderName().toLowerCase();
        requestContext.getRemoveHeaders().add(internalKeyHeader);
        // Avoid internal key being published to the Traffic Manager
        requestContext.getProtectedHeaders().add(internalKeyHeader);

        // Remove Authorization Header
        AuthHeaderDto authHeader = ConfigHolder.getInstance().getConfig().getAuthHeader();
        String authHeaderName = FilterUtils.getAuthHeaderName(requestContext);
        if (!authHeader.isEnableOutboundAuthHeader()) {
            requestContext.getRemoveHeaders().add(authHeaderName);
        }
        // Authorization Header should not be included in the throttle publishing event.
        requestContext.getProtectedHeaders().add(authHeaderName);
    }
}
