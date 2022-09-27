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
import org.wso2.choreo.connect.discovery.api.OperationPolicies;
import org.wso2.choreo.connect.discovery.api.Scopes;
import org.wso2.choreo.connect.discovery.api.SecurityList;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.Policy;
import org.wso2.choreo.connect.enforcer.commons.model.PolicyConfig;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RetryConfig;

import java.util.ArrayList;
import java.util.HashMap;
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

        // getting the first endpoint's basepath would be enough,
        // as all endpoints in the cluster should have the same basepath as of now
        endpointCluster.setBasePath(rpcEndpointCluster.getUrlsList().get(0).getBasepath());
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

    public static PolicyConfig genPolicyConfig(OperationPolicies operationPolicies) {
        PolicyConfig policyConfig = new PolicyConfig();
        if (operationPolicies.getRequestCount() > 0) {
            policyConfig.setRequest(genPolicyList(operationPolicies.getRequestList()));
        }
        if (operationPolicies.getResponseCount() > 0) {
            policyConfig.setResponse(genPolicyList(operationPolicies.getResponseList()));
        }
        if (operationPolicies.getFaultCount() > 0) {
            policyConfig.setFault(genPolicyList(operationPolicies.getFaultList()));
        }
        return policyConfig;
    }

    private static ArrayList<Policy> genPolicyList
            (List<org.wso2.choreo.connect.discovery.api.Policy> operationPoliciesList) {
        ArrayList<Policy> policyList = new ArrayList<>();
        for (org.wso2.choreo.connect.discovery.api.Policy policy : operationPoliciesList) {
            policyList.add(new Policy(policy.getAction(), policy.getParametersMap()));
        }
        return policyList;
    }

}
