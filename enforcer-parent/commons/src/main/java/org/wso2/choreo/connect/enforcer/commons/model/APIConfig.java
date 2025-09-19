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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * APIConfig contains the details related to the MatchedAPI for the inbound request.
 */
public class APIConfig {
    private String name;
    private String version;
    private String vhost;
    private String basePath;
    private String apiType;
    private Map<String, EndpointCluster> endpoints; // "PRODUCTION" OR "SANDBOX" -> endpoint cluster
    private Map<String, SecuritySchemaConfig> securitySchemeDefinitions; // security scheme def name -> scheme def
    private String apiLifeCycleState;
    private String authorizationHeader;
    private String apiKeyHeader;
    private EndpointSecurity endpointSecurity;
    private String organizationId;
    private String uuid;
    private String apiProvider;
    private boolean enableBackendJWT;
    private BackendJWTConfiguration backendJWTConfiguration;
    private Map<String, List<String>> apiSecurity = new HashMap<>();
    private String tier;
    private boolean disableSecurity = false;
    private List<ResourceConfig> resources = new ArrayList<>();
    private String deploymentType;
    private String environmentId;
    private String choreoEnvironmentId;
    private String environmentName;
    private ChoreoComponentInfo choreoComponentInfo;
    private List<ExtendedOperation> extendedOperations = new ArrayList<>();

    /**
     * getApiType returns the API type. This could be one of the following.
     * HTTP, WS, WEBHOOK
     *
     * @return the apiType
     */
    public String getApiType() {
        return apiType;
    }

    /**
     * getProductionEndpoints returns the map of EndpointCluster objects based on
     * whether the key type is prod or sandbox.
     *
     * @return getProductionEndpoints returns the map of EndpointClusters
     */
    public Map<String, EndpointCluster> getEndpoints() {
        return endpoints;
    }

    /**
     * Corresponding API's organization UUID (TenantDomain) is returned.
     *
     * @return Organization UUID
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Corresponding API's API UUID is returned.
     * @return API UUID
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @return API security definitions
     */
    public Map<String, SecuritySchemaConfig> getSecuritySchemeDefinitions() {
        return securitySchemeDefinitions;
    }

    /**
     * Corresponding API's API Name is returned.
     * @return API name
     */
    public String getName() {
        return name;
    }

    /**
     * Corresponding API's API Version is returned.
     * @return API version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Corresponding API's Backend Endpoint Security Object is returned.
     * If the backend endpoint is protected with BasicAuth, those information could
     * be fetched from here.
     * @return {@code EndpointSecurity} object of the API
     */
    public EndpointSecurity getEndpointSecurity() {
        return endpointSecurity;
    }

    /**
     * Corresponding API's Host is returned.
     * @return API's host
     */
    public String getVhost() {
        return vhost;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public String getChoreoEnvironmentId() {
        return choreoEnvironmentId;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    /**
     * Corresponding API's Basepath is returned.
     * @return basePath of the API
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * If the Authentication Header for the API is modified using x-wso2-auth-header extension,
     * this would return the changed value. If it remains unchanged, it returns null.
     * @return Authentication header for the API if changed.
     */
    public String getAuthHeader() {
        return authorizationHeader;
    }

    /**
     * If the API Key Header for the API is modified using x-wso2-api-key-header extension,
     * this would return the changed value. If it remains unchanged, it returns null.
     * @return API Key header for the API if changed.
     */
    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    /**
     * Current API Lifecycle state is returned.
     * @return lifecycle state
     */
    public String getApiLifeCycleState() {
        return apiLifeCycleState;
    }

    /**
     * Security Schemas assigned for the corresponding API together with the scopes.
     * Items of the map currently does not support being applied as AND.
     * Authenticators are applied as OR. In other words, authentication succeeds if
     * at least one security scheme matches.
     *
     * @return array of security schemes and scopes assigned for the API.
     */
    public Map<String, List<String>> getApiSecurity() {
        return apiSecurity;
    }

    /**
     * API level Throttling tier assigned for the corresponding API.
     * @return API level throttling tier
     */
    public String getTier() {
        return tier;
    }

    /**
     * If the authentication is disabled for the API using x-wso2-disable-security extension.
     *
     * @return true if x-wso2-disable-security extension is assigned for the API and its value is true.
     */
    public boolean isDisableSecurity() {
        return disableSecurity;
    }

    /**
     * Returns the complete list of resources under the corresponding API.
     * Each operation in the openAPI definition is listed under here.
     * @return Resources of the API.
     */
    public List<ResourceConfig> getResources() {
        return resources;
    }

    /**
     * Returns the API Provider of the API. If there is no provider, it would return an empty string.
     * @return API Provider
     */
    public String getApiProvider() {
        return apiProvider;
    }

    /**
     * Returns whether or not the backend JWT should be disabled
     * @return True if backend JWT should not be included, False if it should
     */
    public boolean isEnableBackendJWT() {
        return enableBackendJWT;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public BackendJWTConfiguration getBackendJWTConfiguration() {
        return backendJWTConfiguration;
    }

    public void setBackendJWTConfiguration(BackendJWTConfiguration backendJWTConfiguration) {
        this.backendJWTConfiguration = backendJWTConfiguration;
    }

    public ChoreoComponentInfo getChoreoComponentInfo() {
        return choreoComponentInfo;
    }

    /**
     * Returns the API level extended operations such as MCP tools
     * @return List of extended operations
     */
    public List<ExtendedOperation> getExtendedOperations() {
        return extendedOperations;
    }

    public void setExtendedOperations(List<ExtendedOperation> extendedOperations) {
        this.extendedOperations = extendedOperations;
    }

    /**
     * Implements builder pattern to build an API Config object.
     */
    public static class Builder {

        private String name;
        private String version;
        private String vhost;
        private String basePath;
        private String apiType;
        private Map<String, EndpointCluster> endpoints;
        private String apiLifeCycleState;
        private String authorizationHeader;
        private String apiKeyHeader;
        private EndpointSecurity endpointSecurity;
        private String organizationId;
        private String uuid;
        private Map<String, SecuritySchemaConfig> securitySchemeDefinitions;
        private Map<String, List<String>> apiSecurity = new HashMap<>();
        private String tier;
        private boolean disableSecurity = false;
        private String apiProvider;
        private List<ResourceConfig> resources = new ArrayList<>();
        private String deploymentType = "PRODUCTION";
        private String environmentId;
        private String choreoEnvironmentId;
        private String environmentName;
        private boolean enableBackendJWT;
        private BackendJWTConfiguration backendJWTConfiguration;
        private ChoreoComponentInfo choreoComponentInfo;
        private List<ExtendedOperation> extendedOperations;

        public Builder(String name) {
            this.name = name;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder vhost(String vhost) {
            this.vhost = vhost;
            return this;
        }

        public Builder basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public Builder apiType(String apiType) {
            this.apiType = apiType;
            return this;
        }

        public Builder apiLifeCycleState(String apiLifeCycleState) {
            this.apiLifeCycleState = apiLifeCycleState;
            return this;
        }

        public Builder tier(String tier) {
            this.tier = tier;
            return this;
        }

        public Builder disableSecurity(boolean enabled) {
            this.disableSecurity = enabled;
            return this;
        }

        public Builder resources(List<ResourceConfig> resources) {
            this.resources = resources;
            return this;
        }

        public Builder endpoints(Map<String, EndpointCluster> endpoints) {
            this.endpoints = endpoints;
            return this;
        }

        public Builder apiSecurity(Map<String, List<String>> apiSecurity) {
            this.apiSecurity = apiSecurity;
            return this;
        }

        public Builder endpointSecurity(EndpointSecurity endpointSecurity) {
            this.endpointSecurity = endpointSecurity;
            return this;
        }

        public Builder authHeader(String authorizationHeader) {
            this.authorizationHeader = authorizationHeader;
            return this;
        }

        public Builder apiKeyHeader(String apiKeyHeader) {
            this.apiKeyHeader = apiKeyHeader;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder securitySchemeDefinitions(Map<String, SecuritySchemaConfig> securitySchemeDefinitions) {
            this.securitySchemeDefinitions = securitySchemeDefinitions;
            return this;
        }

        public Builder apiProvider(String apiProvider) {
            this.apiProvider = apiProvider;
            return this;
        }

        public Builder enableBackendJWT(boolean enableBackendJWT) {
            this.enableBackendJWT = enableBackendJWT;
            return this;
        }

        public Builder backendJWTConfiguration(BackendJWTConfiguration backendJWTConfiguration) {
            this.backendJWTConfiguration = backendJWTConfiguration;
            return this;
        }

        public Builder deploymentType(String deploymentType) {
            if (!StringUtils.isEmpty(deploymentType)) {
                this.deploymentType = deploymentType;
            }
            return this;
        }

        public Builder environmentId(String environmentId) {
            if (!StringUtils.isEmpty(environmentId)) {
                this.environmentId = environmentId;
            }
            return this;
        }

        public Builder choreoEnvironmentId(String choreoEnvironmentId) {
            if (!StringUtils.isEmpty(choreoEnvironmentId)) {
                this.choreoEnvironmentId = choreoEnvironmentId;
            }
            return this;
        }

        public Builder environmentName(String environmentName) {
            if (!StringUtils.isEmpty(environmentName)) {
                this.environmentName = environmentName;
            }
            return this;
        }

        public Builder choreoComponentInfo(ChoreoComponentInfo choreoComponentInfo) {
            this.choreoComponentInfo = choreoComponentInfo;
            return this;
        }

        public Builder extendedOperations(List<ExtendedOperation> extendedOperations) {
            this.extendedOperations = extendedOperations;
            return this;
        }

        public APIConfig build() {
            APIConfig apiConfig = new APIConfig();
            apiConfig.name = this.name;
            apiConfig.vhost = this.vhost;
            apiConfig.basePath = this.basePath;
            apiConfig.version = this.version;
            apiConfig.apiLifeCycleState = this.apiLifeCycleState;
            apiConfig.resources = this.resources;
            apiConfig.apiType = this.apiType;
            apiConfig.endpoints = this.endpoints;
            apiConfig.apiSecurity = this.apiSecurity;
            apiConfig.tier = this.tier;
            apiConfig.endpointSecurity = this.endpointSecurity;
            apiConfig.authorizationHeader = this.authorizationHeader;
            apiConfig.apiKeyHeader = this.apiKeyHeader;
            apiConfig.disableSecurity = this.disableSecurity;
            apiConfig.organizationId = this.organizationId;
            apiConfig.uuid = this.uuid;
            apiConfig.securitySchemeDefinitions = this.securitySchemeDefinitions;
            apiConfig.apiProvider = this.apiProvider;
            apiConfig.enableBackendJWT = this.enableBackendJWT;
            apiConfig.backendJWTConfiguration = this.backendJWTConfiguration;
            apiConfig.deploymentType = this.deploymentType;
            apiConfig.environmentId = this.environmentId;
            apiConfig.choreoEnvironmentId = this.choreoEnvironmentId;
            apiConfig.environmentName = this.environmentName;
            apiConfig.choreoComponentInfo = this.choreoComponentInfo;
            apiConfig.extendedOperations = this.extendedOperations;
            return apiConfig;
        }
    }

    private APIConfig() {
    }
}
