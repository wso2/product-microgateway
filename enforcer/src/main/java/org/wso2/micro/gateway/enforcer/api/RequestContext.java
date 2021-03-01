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
package org.wso2.micro.gateway.enforcer.api;

import org.apache.commons.lang3.StringUtils;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds the set of meta data related to current request flowing through the gateway. This context should be shared
 * through out the complete request flow through the gateway enforcer.
 */
public class RequestContext{

    private API mathedAPI;
    private String requestPath;
    private String requestMethod;
    private ResourceConfig matchedResourcePath;
    private Map<String, String> headers;
    private Map<String, Object> properties = new HashMap();
    // Denotes the cluster header name for each environment. Both properties can be null if
    // the openAPI has production endpoints alone.
    private String prodClusterHeader;
    private String sandClusterHeader;
    private boolean clusterHeaderEnabled = false;
    //Denotes the specific headers which needs to be passed to response object
    private Map<String, String> responseHeaders;
    private AuthenticationContext authenticationContext = new AuthenticationContext();
    private WebSocketMetadataContext webSocketMetadataContext;

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
        private String prodClusterHeader;
        private String sandClusterHeader;
        private Map<String, Object> properties = new HashMap();
        private AuthenticationContext authenticationContext = new AuthenticationContext();
        private WebSocketMetadataContext webSocketMetadataContext;

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

        public Builder prodClusterHeader(String cluster) {
            if (!StringUtils.isEmpty(cluster)) {
                this.prodClusterHeader = cluster;
            }
            return this;
        }

        public Builder sandClusterHeader(String cluster) {
            if (!StringUtils.isEmpty(cluster)) {
                this.sandClusterHeader = cluster;
            }
            return this;
        }

        public Builder authenticationContext(AuthenticationContext authenticationContext) {
            this.authenticationContext = authenticationContext;
            return this;
        }

        public Builder webSocketMetadataContext(WebSocketMetadataContext webSocketMetadataContext){
            this.webSocketMetadataContext = webSocketMetadataContext;
            return this;
        }

        public RequestContext build() {
            RequestContext requestContext = new RequestContext();
            requestContext.matchedResourcePath = this.matchedResourceConfig;
            requestContext.mathedAPI = this.mathedAPI;
            requestContext.requestMethod = this.requestMethod;
            requestContext.requestPath = this.requestPath;
            requestContext.headers = this.headers;
            requestContext.prodClusterHeader = this.prodClusterHeader;
            requestContext.sandClusterHeader = this.sandClusterHeader;
            requestContext.properties = this.properties;
            requestContext.authenticationContext = this.authenticationContext;

            // Adapter assigns header based routing only if both type of endpoints are present.
            if (!StringUtils.isEmpty(prodClusterHeader) && !StringUtils.isEmpty(sandClusterHeader)) {
                requestContext.clusterHeaderEnabled = true;
            }
            if(this.webSocketMetadataContext != null){
                requestContext.webSocketMetadataContext = this.webSocketMetadataContext;
            }
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

    public AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    /**
     * Returns the production cluster header value.
     * can be null if the openAPI has production endpoints alone.
     * In that case, no header should not be set.
     *
     * @return prod Cluster name header value
     */
    public String getProdClusterHeader() {
        return prodClusterHeader;
    }

    /**
     * Returns the sandbox cluster header value.
     * can be null if the openAPI has production endpoints alone.
     * In that case, no header should not be set.
     * If this property is null and the keytype is sand box, the request should be blocked
     *
     * @return sand Cluster name header value
     */
    public String getSandClusterHeader() {
        return sandClusterHeader;
    }

    /**
     * Returns true if both sandbox cluster header and prod cluster header is
     * available.
     *
     * @return true if cluster-header is enabled.
     */
    public boolean isClusterHeaderEnabled() {
        return clusterHeaderEnabled;
    }

    /**
     * If a certain header needs to be added to the response additionally from enforcer,
     * those header-value pairs  should be defined here.
     *
     * @param key   header
     * @param value headerValue
     */
    public void addResponseHeaders(String key, String value) {
        if (responseHeaders == null) {
            responseHeaders = new TreeMap<>();
        }
        responseHeaders.put(key, value);
    }

    /**
     * Returns the introduced response headers.
     *
     * @return response headers
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public WebSocketMetadataContext getWebSocketMetadataContext() {
        return webSocketMetadataContext;
    }
}
