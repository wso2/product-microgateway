/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.filter.core.api;

import org.wso2.micro.gateway.filter.core.api.config.ResourceConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the set of meta data related to current request flowing through the gateway. This context should be shared
 * through out the complete request flow through the gateway filter chain.
 */
public class RequestContext {

    private API mathedAPI;
    private String requestPath;
    private String requestMethod;
    private ResourceConfig matchedResourcePath;
    private Map<String, String> headers;
    private Map<String, Object> properties = new HashMap();

    private RequestContext() {

    }

    /**
     * Implements builder pattern to build an {@link RequestContext} object.
     */
    public static class Builder {
        private API mathedAPI;
        private String requestPath;
        private String requestMethod;
        private ResourceConfig matchedResourceConfig;
        private Map<String, String> headers;
        private Map<String, Object> properties = new HashMap();

        public Builder(String requestPath) {
            this.requestPath = requestPath;
        }

        public Builder matchedAPI(API api) {
            this.mathedAPI = api;
            return this;
        }

        public Builder requestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public Builder matchedResourceConfig(ResourceConfig matchedResourcePath) {
            this.matchedResourceConfig = matchedResourcePath;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public RequestContext build() {
            RequestContext requestContext = new RequestContext();
            requestContext.matchedResourcePath = this.matchedResourceConfig;
            requestContext.mathedAPI = this.mathedAPI;
            requestContext.requestMethod = this.requestMethod;
            requestContext.requestPath = this.requestPath;
            requestContext.headers = this.headers;
            requestContext.properties = this.properties;
            return requestContext;
        }
    }

    public API getMathedAPI() {
        return mathedAPI;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public ResourceConfig getMatchedResourcePath() {
        return matchedResourcePath;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
