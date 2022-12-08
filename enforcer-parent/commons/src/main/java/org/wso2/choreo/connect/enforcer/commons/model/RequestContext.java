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
package org.wso2.choreo.connect.enforcer.commons.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds the set of meta data related to current request flowing through the gateway. This context should be shared
 * through out the complete request flow through the gateway enforcer.
 */
public class RequestContext {
    private static final Logger logger = LogManager.getLogger(RequestContext.class);

    //constants
    public static final String CLUSTER_HEADER = "x-wso2-cluster-header";

    private APIConfig matchedAPI;
    private String requestPath;
    private String requestMethod;
    private ResourceConfig matchedResourcePath;
    private Map<String, String> headers;
    private Map<String, Object> properties = new HashMap<>();
    private AuthenticationContext authenticationContext;
    private String requestID;
    private String clientIp;
    // Denotes the cluster header name for each environment. Both properties can be null if
    // the openAPI has production endpoints alone.
    private String prodClusterHeader;
    private String sandClusterHeader;
    //Denotes the specific headers which needs to be passed to response object
    private Map<String, String> addHeaders;
    private Map<String, String> metadataMap = new HashMap<>();
    private String requestPathTemplate;
    private ArrayList<String> removeHeaders;
    // Consist of web socket frame related data like frame length, remote IP
    private WebSocketFrameContext webSocketFrameContext;
    private Map<String, String> queryParameters;
    private Map<String, String> pathParameters;
    private ArrayList<String> queryParamsToRemove;
    // This is used to keep protected headers like authorization header. The protected headers will not be
    // sent to the Traffic Manager when header based rate limiting is enabled.
    private ArrayList<String> protectedHeaders;

    // Request Timestamp is required for analytics
    private long requestTimeStamp;
    // Rate-limit policy value relevant to the request
    // This value will be only available if operation level rate-limit policies are enabled
    private String rateLimitPolicy;

    /**
     * The dynamic metadata sent from enforcer are stored in this metadata map.
     * @return dynamic metadata map
     */
    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    /**
     * If the dynamic metadata sent from enforcer needs to be updated/ new keys needs to be added,
     * the key, value pairs should be updated in this map.
     *
     * @param key metadata key
     * @param value metadata value
     */
    public void addMetadataToMap(String key, String value) {
        metadataMap.put(key, value);
    }

    /**
     * Authentication Context for the request. This is populated after auth Filter.
     *
     * Note:
     * Depending on the authenticator being used, some properties may remain un-initialized.
     * Example: Internal-Key authenticator would not populate application details.
     *
     * @return {@code AuthenticationContext}
     */
    public AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    /**
     * Set Authentication Context for the request.
     *
     * Note:
     * The content inside this object is used for throttling and analytics. In addition,
     * this object is populated within Authentication Filter. Hence adding a filter prior to
     * Authentication Filter and modifying it may not work.
     */
    public void setAuthenticationContext(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    /**
     * Returns corresponding requestPathTemplate.
     *
     * @return path Template
     */
    public String getRequestPathTemplate() {
        return requestPathTemplate;
    }

    /**
     * Request timestamp of the request.
     *
     * @return request timestamp.
     */
    public long getRequestTimeStamp() {
        return requestTimeStamp;
    }

    private RequestContext() {}

    /**
     * Get the path parameters and the assigned values as a map.
     *
     * Ex: /pet/{petID} -> /pet/1
     *  then, under 'petId' value would be marked as '1' (string format)
     *
     * Note:
     *  Modifying this map does not result in change of path parameter values. This is
     *  only for read purposes.
     *
     * @return Map contains path parameter name and assigned path param value in the request.
     */
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    /**
     * Request ID for the request.
     *
     * This can be used to correlate the router access log entries and the enforcer logs.
     *
     * @return request id.
     */
    public String getRequestID() {
        return requestID;
    }

    /**
     * Returns the client IP address.
     * If the x-forwarded-for header is attached, the value of that header would be assigned as
     * client IP.
     *
     * @return Client IP address
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * Get Matched API Details for the request.
     *
     * @return Matched API
     */
    public APIConfig getMatchedAPI() {
        return matchedAPI;
    }

    /**
     * Original Request Path where path parameters are provided with parameter values.
     * ex: /pet/1?status=available
     * @return
     */
    public String getRequestPath() {
        return requestPath;
    }

    /**
     * Request Method.
     *
     * @return request method.
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * Get the complete resource configuration for matched resource path.
     *
     * Note:
     *  ResourceConfig can be null if the request has the method OPTIONS but there is no such method listed
     *  in the OpenAPI definition.
     *
     * @return {@code ResourceConfig} object
     */
    public ResourceConfig getMatchedResourcePath() {
        return matchedResourcePath;
    }

    /**
     * Get the set of request headers.
     *
     * Note: Modifying the headers map here does not result in changing the headers.
     * It is for reading the headers.
     * If you need to add/modify headers, use addHeaders() method and for removing use
     * RemoveHeaders() method.
     *
     * @return request headers map
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * If there are specific properties needs to be maintained across the filters, the property
     * map can be used.
     *
     * @return property map.
     */
    public Map<String, Object> getProperties() {
        return properties;
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
     * If a certain header needs to be added/modified within the request from enforcer additionally,
     * those header-value pairs  should be added from here.
     *
     * @param key   header needs to be added/modified
     * @param value headerValue
     */
    public void addOrModifyHeaders(String key, String value) {
        if (addHeaders == null) {
            addHeaders = new TreeMap<>();
        }
        addHeaders.put(key, value);
    }

    /**
     * If there is a set of headers needs to be removed from the request, those headers should
     * be added to the arrayList here.
     *
     * @return header names which are supposed to be removed.
     */
    public ArrayList<String> getRemoveHeaders() {
        return removeHeaders;
    }

    /**
     * Returns the set of newly added/modified header values.
     *
     * @return the added/modified set of headers.
     */
    public Map<String, String> getAddHeaders() {
        return addHeaders;
    }

    /**
     * Retrieve a map of query parameters in the request.
     *
     * @return query parameters as a map of {@code <param_name, param_value>}
     */
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    /**
     * This is used for websocket specific implementation.
     *
     * When the websocket communication happens, there is a specific filter which sends some metadata
     * related to the websocket frames for throttling and analytics purposes. This publishing happens
     * asynchronously.
     *
     * Note:
     *  This can't be used for modifying/reading websocket frame data.
     * @return {@code WebSocketFrameContext} object
     */
    public WebSocketFrameContext getWebSocketFrameContext() {
        return webSocketFrameContext;
    }

    /**
     * If there is a set of query parameters needs to be removed from the outbound request, those parameters should
     * be added to the arrayList here.
     *
     * @return query parameters which are supposed to be removed.
     */
    public ArrayList<String> getQueryParamsToRemove() {
        return queryParamsToRemove;
    }

    /**
     * If there is a set of headers needs to be removed from the throttle publishing event, those headers should
     * be added to the arrayList here.
     *
     * Ex. Authorization Header
     *
     * @return header names which are not supposed to be published to the traffic manager.
     */
    public ArrayList<String> getProtectedHeaders() {
        return protectedHeaders;
    }

    /**
     * Gives rate-limit policy relevant to the request
     *
     * @return rate-limit policy
     */
    public String getRateLimitPolicy() {
        return rateLimitPolicy;
    }

    /**
     * Implements builder pattern to build an {@link RequestContext} object.
     */
    public static class Builder {
        private APIConfig matchedAPI;
        private String requestPath;
        private String requestMethod;
        private String requestPathTemplate;
        private ResourceConfig matchedResourceConfig;
        private Map<String, String> headers;
        private String prodClusterHeader;
        private String sandClusterHeader;
        private long requestTimeStamp;
        private Map<String, Object> properties = new HashMap<>();
        private AuthenticationContext authenticationContext = new AuthenticationContext();
        private String requestID;
        private String clientIp;
        private ArrayList<String> removeHeaders;
        private WebSocketFrameContext webSocketFrameContext;
        private String rateLimitPolicy;

        public Builder(String requestPath) {
            this.requestPath = requestPath;
        }

        public Builder matchedAPI(APIConfig api) {
            this.matchedAPI = api;
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

        public Builder requestTimeStamp(long requestTimeStampInMillies) {
            this.requestTimeStamp = requestTimeStampInMillies;
            return this;
        }

        public Builder authenticationContext(AuthenticationContext authenticationContext) {
            this.authenticationContext = authenticationContext;
            return this;
        }

        public Builder requestID(String requestID) {
            this.requestID = requestID;
            return this;
        }

        public Builder address(String address) {
            this.clientIp = address;
            return this;
        }

        public Builder webSocketFrameContext(WebSocketFrameContext webSocketFrameContext) {
            this.webSocketFrameContext = webSocketFrameContext;
            return this;
        }

        public Builder rateLimitPolicy(String rateLimitPolicy) {
            this.rateLimitPolicy = rateLimitPolicy;
            return this;
        }

        public RequestContext build() {
            RequestContext requestContext = new RequestContext();
            requestContext.matchedResourcePath = this.matchedResourceConfig;
            requestContext.matchedAPI = this.matchedAPI;
            requestContext.requestMethod = this.requestMethod;
            requestContext.requestPath = this.requestPath;
            requestContext.headers = this.headers;
            requestContext.prodClusterHeader = this.prodClusterHeader;
            requestContext.sandClusterHeader = this.sandClusterHeader;
            requestContext.properties = this.properties;
            requestContext.requestPathTemplate = this.requestPathTemplate;
            requestContext.requestTimeStamp = this.requestTimeStamp;
            requestContext.authenticationContext = this.authenticationContext;
            requestContext.requestID = this.requestID;
            requestContext.clientIp = this.clientIp;
            requestContext.addHeaders = new HashMap<>();
            requestContext.removeHeaders = new ArrayList<>();
            requestContext.queryParamsToRemove = new ArrayList<>();
            requestContext.protectedHeaders = new ArrayList<>();
            requestContext.rateLimitPolicy = this.rateLimitPolicy;
            String[] queryParts = this.requestPath.split("\\?");
            String queryPrams = queryParts.length > 1 ? queryParts[1] : "";

            requestContext.queryParameters = new HashMap<>();
            List<NameValuePair> queryParams = URLEncodedUtils.parse(queryPrams, StandardCharsets.UTF_8);
            for (NameValuePair param : queryParams) {
                requestContext.queryParameters.put(param.getName(), param.getValue());
            }

            requestContext.pathParameters = populatePathParameters(
                    matchedAPI.getBasePath(), requestPathTemplate, this.requestPath);

            if (this.webSocketFrameContext != null) {
                requestContext.webSocketFrameContext = this.webSocketFrameContext;
            }

            // sanitize wso2 added headers
            // not allow client's to set cluster header manually
            requestContext.removeHeaders.add(CLUSTER_HEADER);

            return requestContext;
        }

        public Builder pathTemplate(String requestPathTemplate) {
            this.requestPathTemplate = requestPathTemplate;
            return this;
        }

        /**
         * Create and populate path parameters map.
         *
         * @param basePath         basePath of the API
         * @param resourceTemplate resourceTemplate (as listed in OpenAPI)
         * @param rawPath          raw request Path
         * @return map which contains path parameters
         */
        private Map<String, String> populatePathParameters(String basePath, String resourceTemplate,
                                                           String rawPath) {
            if (resourceTemplate == null || rawPath == null) {
                logger.debug("Skip populating the path parameters. template: {}, rawPath: {}", resourceTemplate,
                        rawPath);
                return null;
            }
            // Format the basePath and resourcePath to maintain consistency
            String formattedBasePath = basePath.startsWith("/") ? basePath : "/" + basePath;
            formattedBasePath = formattedBasePath.endsWith("/") ?
                    formattedBasePath.substring(0, formattedBasePath.length() - 1) : formattedBasePath;
            String formattedResourcePathTemplate = resourceTemplate.startsWith("/") ?
                    resourceTemplate : "/" + resourceTemplate;

            String formattedRawPath = rawPath.split("\\?")[0];
            final ParameterResolver parameterResolver = new ParameterResolver
                    (formattedBasePath + formattedResourcePathTemplate);
            return parameterResolver.parametersByName(formattedRawPath);
        }
    }
}
