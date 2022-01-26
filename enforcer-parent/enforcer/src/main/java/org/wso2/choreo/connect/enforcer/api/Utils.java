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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.api.EndpointClusterConfig;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.MockedApiConfig;
import org.wso2.choreo.connect.enforcer.commons.model.MockedHeaderConfig;
import org.wso2.choreo.connect.enforcer.commons.model.MockedPayloadConfig;
import org.wso2.choreo.connect.enforcer.commons.model.MockedResponseConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RetryConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility Methods used across different APIs.
 */
public class Utils {
    private static final Logger log = LogManager.getLogger(Utils.class);

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

    /**
     * Handles mock API call and prepares response object considering provided values in the request.
     *
     * @param requestContext request context
     * @param responseObject response object for the mock API call
     */
    public static void processMockedApiCall(RequestContext requestContext, ResponseObject responseObject) {
        responseObject.setDirectResponse(true);
        String acceptType = "";
        ResourceConfig resourceConfig = requestContext.getMatchedResourcePath();
        MockedApiConfig mockedApiConfig = resourceConfig.getMockedApiConfig();
        Map<String, String> headersMap = requestContext.getHeaders();
        if (headersMap.containsKey(APIConstants.ACCEPT_HEADER.toLowerCase())) {
            acceptType = headersMap.get(APIConstants.ACCEPT_HEADER.toLowerCase());
        }

        if (mockedApiConfig.getIn().equalsIgnoreCase(APIConstants.MockApiConstants.HEADER)) {
            setMockApiResponse(responseObject, headersMap, mockedApiConfig, acceptType);
        } else {
            Map<String, String> queryParamMap = requestContext.getQueryParameters();
            setMockApiResponse(responseObject, queryParamMap, mockedApiConfig, acceptType);
        }
    }

    /**
     * Prepares mock API response considering the properties map and mockedApiConfig.
     *
     * @param responseObject  Response object (represents response for the API call)
     * @param propertiesMap   A map which includes values specified in headers or query parameters
     * @param mockedApiConfig Holds the JSON values specified in the mock API implementation
     * @param acceptType      Denotes accepted contend type as the response
     */
    private static void setMockApiResponse(ResponseObject responseObject, Map<String, String> propertiesMap,
                                           MockedApiConfig mockedApiConfig, String acceptType) {
        String requestValuePosition = mockedApiConfig.getIn();
        String name;

        // condition handles case-sensitiveness of query parameters.
        if (requestValuePosition.equalsIgnoreCase(APIConstants.MockApiConstants.HEADER)) {
            name = mockedApiConfig.getName().toLowerCase();
        } else {
            name = mockedApiConfig.getName();
        }

        if (propertiesMap == null || !propertiesMap.containsKey(name)) {
            log.error("Response determining value not available in the mock API request.");
            return;
        }
        String nameInRequest = propertiesMap.get(name);
        List<MockedResponseConfig> responseConfigList = mockedApiConfig.getResponses();
        // iterates over the mock API responses
        for (MockedResponseConfig responseConfig : responseConfigList) {
            if (!responseConfig.getValue().equalsIgnoreCase(nameInRequest)) {
                log.error("Mocked response config JSON does not contain the value provided in the request.");
                return;
            }
            responseObject.setStatusCode(responseConfig.getCode());
            Map<String, String> headerMap = new HashMap<>();
            // iterates over the headers list in the mock API JSON
            for (MockedHeaderConfig header : responseConfig.getHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }
            MockedPayloadConfig payload = responseConfig.getPayload();
            // checks and assigns the accepted content type
            if (acceptType.equalsIgnoreCase(APIConstants.APPLICATION_XML) &&
                    payload.getPayloadMap().get(APIConstants.APPLICATION_XML) != null) {
                headerMap.put(APIConstants.CONTENT_TYPE_HEADER, APIConstants.APPLICATION_XML);
                responseObject.setMockApiResponsePayload(payload.getPayloadMap().get(APIConstants.APPLICATION_XML));
            } else {
                headerMap.put(APIConstants.CONTENT_TYPE_HEADER, APIConstants.APPLICATION_JSON);
                responseObject.setMockApiResponsePayload(payload.getPayloadMap().get(APIConstants.APPLICATION_JSON));
            }
            responseObject.setHeaderMap(headerMap);
        }
    }
}
