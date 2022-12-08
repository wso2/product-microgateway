/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the metadata related to the resources/operations of an API.
 */
public class ResourceConfig {

    private String path;
    private HttpMethods method;
    private Map<String, List<String>> securitySchemas = new HashMap(); // security_schema_name -> scopes
    private String tier = "Unlimited";
    private boolean disableSecurity = false;
    private Map<String, EndpointCluster> endpoints; // "PRODUCTION" OR "SANDBOX" -> endpoint cluster
    private String rateLimitPolicy;

    /**
     * ENUM to hold http operations.
     */
    public enum HttpMethods {
        GET("get"), POST("post"), PUT("put"), DELETE("delete"), HEAD("head"),
        PATCH("patch"), OPTIONS("options");

        private String value;

        private HttpMethods(String value) {
            this.value = value;
        }
    }

    /**
     * Get the matching path Template for the request.
     *
     * @return path Template
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the matching HTTP Method.
     *
     * @return HTTP method
     */
    public HttpMethods getMethod() {
        return method;
    }

    public void setMethod(HttpMethods method) {
        this.method = method;
    }

    /**
     * Get the List of security Schemas. Keys would define the available set of security schemas and
     * its value would denote the scopes assigned.
     *
     * @return security schemas as a map of <security_schema_name, list of scopes.
     */
    public Map<String, List<String>> getSecuritySchemas() {
        return securitySchemas;
    }

    public void setSecuritySchemas(Map<String, List<String>> securitySchemas) {
        this.securitySchemas = securitySchemas;
    }

    /**
     * Get the resource level throttling tier assigned for the corresponding Resource.
     *
     * @return resource level throttling tier
     */
    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    /**
     * Returns true if the security is disabled for the corresponding resource using
     * x-wso2-disable-security openAPI extension.
     *
     * @return true if security is disabled.
     */
    public boolean isDisableSecurity() {
        return disableSecurity;
    }

    public void setDisableSecurity(boolean disableSecurity) {
        this.disableSecurity = disableSecurity;
    }

    /**
     * Get the resource level endpoint cluster map for the corresponding Resource
     * where the map-key is either "PRODUCTION" or "SANDBOX".
     *
     * @return resource level endpoint cluster map
     */
    public Map<String, EndpointCluster> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointCluster> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Returns rate-limit policy relevant to the operation
     * @return rate-limit policy as a String
     */
    public String getRateLimitPolicy() {
        return rateLimitPolicy;
    }

    /**
     * Sets rate-limit policy relevant to the operation
     * @param rateLimitPolicy rate-limit policy name
     */
    public void setRateLimitPolicy(String rateLimitPolicy) {
        this.rateLimitPolicy = rateLimitPolicy;
    }
}

