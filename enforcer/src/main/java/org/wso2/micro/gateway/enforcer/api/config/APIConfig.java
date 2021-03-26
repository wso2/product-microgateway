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
package org.wso2.micro.gateway.enforcer.api.config;

import org.wso2.micro.gateway.enforcer.throttle.ThrottleConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the metadata related to the API. Common collection to hold data about any API type like REST, gRPC and etc.
 */
public class APIConfig {
    private String name;
    private String version;
    private String basePath;
    private String apiLifeCycleState;

    private List<String> securitySchemes = new ArrayList<>();
    private String tier = ThrottleConstants.UNLIMITED_TIER;
    private boolean disableSecurity = false;
    private List<ResourceConfig> resources = new ArrayList<>();

    /**
     * Implements builder pattern to build an API Config object.
     */
    public static class Builder {

        private String name;
        private String version;
        private String basePath;
        private String apiLifeCycleState;

        private List<String> securitySchemes = new ArrayList<>();
        private String tier = ThrottleConstants.UNLIMITED_TIER;
        private boolean disableSecurity = false;
        private List<ResourceConfig> resources = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder basePath(String basePath) {
            this.basePath = basePath;
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

        public Builder securitySchema(List<String> securityScheme) {
            this.securitySchemes = securitySchemes;
            return this;
        }

        public APIConfig build() {
            APIConfig apiConfig = new APIConfig();
            apiConfig.name = this.name;
            apiConfig.basePath = this.basePath;
            apiConfig.version = this.version;
            apiConfig.apiLifeCycleState = this.apiLifeCycleState;
            apiConfig.resources = this.resources;
            apiConfig.securitySchemes = this.securitySchemes;
            apiConfig.tier = this.tier;
            apiConfig.disableSecurity = this.disableSecurity;
            return apiConfig;
        }
    }

    private APIConfig() {
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getApiLifeCycleState() {
        return apiLifeCycleState;
    }

    public List<String> getSecuritySchemas() {
        return securitySchemes;
    }

    public String getTier() {
        return tier;
    }

    public boolean isDisableSecurity() {
        return disableSecurity;
    }

    public List<ResourceConfig> getResources() {
        return resources;
    }
}
