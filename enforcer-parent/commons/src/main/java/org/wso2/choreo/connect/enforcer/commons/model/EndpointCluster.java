//  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
//  WSO2 Inc. licenses this file to you under the Apache License,
//  Version 2.0 (the "License"); you may not use this file except
//  in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing,
//  software distributed under the License is distributed on an
//  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//  KIND, either express or implied.  See the License for the
//  specific language governing permissions and limitations
//  under the License.

package org.wso2.choreo.connect.enforcer.commons.model;

import java.util.List;

/**
 * EndpointCluster contains the URLs and the config for a set of Endpoints such as prod, sandbox
 */
public class EndpointCluster {
    private List<String> urls;
    private RetryConfig retryConfig;
    private Integer routeTimeoutInMillis;
    private String basePath;

    /**
     * @return URLs of the cluster
     */
    public List<String> getUrls() {
        return urls;
    }

    /**
     * @param urls URLs of the cluster
     */
    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    /**
     * @return Retry configuration of the cluster
     */
    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    /**
     * @param retryConfig Retry configuration of the cluster
     */
    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    /**
     * @return Route timeout of the cluster
     */
    public Integer getRouteTimeoutInMillis() {
        return routeTimeoutInMillis;
    }

    /**
     * @param routeTimeoutInMillis Route timeout of the cluster
     */
    public void setRouteTimeoutInMillis(Integer routeTimeoutInMillis) {
        this.routeTimeoutInMillis = routeTimeoutInMillis;
    }

    /**
     * @return basepath of the backend endpoint
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * @param basePath of the backend endpoint
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
