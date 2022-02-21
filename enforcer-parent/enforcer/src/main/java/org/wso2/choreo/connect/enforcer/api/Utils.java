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
import org.wso2.choreo.connect.enforcer.commons.model.RetryConfig;

import java.util.ArrayList;
import java.util.List;

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
}
