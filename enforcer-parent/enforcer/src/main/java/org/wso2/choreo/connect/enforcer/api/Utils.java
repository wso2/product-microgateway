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
import org.wso2.choreo.connect.enforcer.commons.model.PrototypeConfig;
import org.wso2.choreo.connect.enforcer.commons.model.PrototypeHeader;
import org.wso2.choreo.connect.enforcer.commons.model.PrototypePayload;
import org.wso2.choreo.connect.enforcer.commons.model.PrototypeResponse;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RetryConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     * Handles prototyped API call and prepares response object considering provided values in the request.
     *
     * @param requestContext request context
     * @param responseObject response object for the prototyped API call
     * @return response object to provide requested prototype values.
     */
    public static ResponseObject processPrototypedApiCall(RequestContext requestContext,
                                                          ResponseObject responseObject) {
        responseObject.setDirectResponse(true);
        String acceptType = "";
        ResourceConfig resourceConfig = requestContext.getMatchedResourcePath();
        PrototypeConfig prototypeConfig = resourceConfig.getPrototypeConfig();
        Map<String, String> headersMap = requestContext.getHeaders();
        if (headersMap.containsKey(APIConstants.ACCEPT_HEADER.toLowerCase())) {
            acceptType = headersMap.get(APIConstants.ACCEPT_HEADER.toLowerCase());
        }

        if (prototypeConfig.getIn().equalsIgnoreCase(APIConstants.PrototypeApiConstants.HEADER)) {
            setPrototypedResponse(responseObject, headersMap, prototypeConfig, acceptType);
        } else {
            Map<String, String> queryParamMap = requestContext.getQueryParameters();
            Map<String, String> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            treeMap.putAll(queryParamMap);
            setPrototypedResponse(responseObject, treeMap, prototypeConfig, acceptType);
        }
        return responseObject;
    }

    /**
     * Prepares prototype response considering the properties map and prototypeConfig.
     *
     * @param responseObject Response object (represents response for the API call)
     * @param propertiesMap A map which includes values specified in headers or query parameters
     * @param prototypeConfig Holds the JSON values specified in the prototype implementation
     * @param acceptType Denotes accepted contend type as the response
     */
    private static void setPrototypedResponse(ResponseObject responseObject, Map<String, String> propertiesMap,
                                              PrototypeConfig prototypeConfig, String acceptType) {
        try {
            if (propertiesMap != null && propertiesMap.containsKey(prototypeConfig.getName().toLowerCase())) {
                String value = propertiesMap.get(prototypeConfig.getName());
                List<PrototypeResponse> responseConfigList = prototypeConfig.getResponses();
                // iterates over the prototyped responses
                for (PrototypeResponse responseConfig : responseConfigList) {
                    if (responseConfig.getValue().equalsIgnoreCase(value)) {
                        responseObject.setStatusCode(responseConfig.getCode());
                        Map<String, String> headerMap = new HashMap<>();
                        // iterates over the headers list in the prototyped JSON
                        for (PrototypeHeader header : responseConfig.getHeaders()) {
                            headerMap.put(header.getName(), header.getValue());
                        }
                        PrototypePayload payload = responseConfig.getPayload();
                        // checks and assigns the accepted content type
                        if (acceptType.equalsIgnoreCase(APIConstants.APPLICATION_XML)) {
                            headerMap.put(APIConstants.CONTENT_TYPE_HEADER, APIConstants.APPLICATION_XML);
                            responseObject.setPrototypeResponsePayload(payload.getApplicationXML());
                        } else {
                            headerMap.put(APIConstants.CONTENT_TYPE_HEADER, APIConstants.APPLICATION_JSON);
                            responseObject.setPrototypeResponsePayload(payload.getApplicationJSON());
                        }
                        responseObject.setHeaderMap(headerMap);
                    }
                }
            } else {
                throw new EnforcerException("Response determining value not provided in the request.");
            }
        } catch (Exception e) {
            log.error("Error occurred while creating prototyped response.", e);
        }
    }
}
