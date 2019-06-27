/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.rest;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS configuration for the APIDetailedDTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class APICorsConfigurationDTO {

    private List<String> accessControlAllowOrigins = new ArrayList<String>();
    private Boolean accessControlAllowCredentials = false;
    private Boolean corsConfigurationEnabled = false;
    private List<String> accessControlAllowHeaders = new ArrayList<String>();
    private List<String> accessControlAllowMethods = new ArrayList<String>();

    @JsonAlias({"accessControlAllowOrigins", "access_control_allow_origins", "access-control-allow-origins"})
    public List<String> getAccessControlAllowOrigins() {
        return accessControlAllowOrigins;
    }

    public void setAccessControlAllowOrigins(List<String> accessControlAllowOrigins) {
        this.accessControlAllowOrigins = accessControlAllowOrigins;
    }

    @JsonAlias({"accessControlAllowCredentials", "access_control_allow_credentials",
            "access-control-allow-credentials"})
    public Boolean getAccessControlAllowCredentials() {
        return accessControlAllowCredentials;
    }

    public void setAccessControlAllowCredentials(Boolean accessControlAllowCredentials) {
        this.accessControlAllowCredentials = accessControlAllowCredentials;
    }

    @JsonAlias("corsConfigurationEnabled")
    public Boolean getCorsConfigurationEnabled() {
        return corsConfigurationEnabled;
    }

    public void setCorsConfigurationEnabled(Boolean corsConfigurationEnabled) {
        this.corsConfigurationEnabled = corsConfigurationEnabled;
    }

    @JsonAlias({"accessControlAllowHeaders", "access_control_allow_headers", "access-control-allow-headers"})
    public List<String> getAccessControlAllowHeaders() {
        return accessControlAllowHeaders;
    }

    public void setAccessControlAllowHeaders(List<String> accessControlAllowHeaders) {
        this.accessControlAllowHeaders = accessControlAllowHeaders;
    }

    @JsonAlias({"accessControlAllowMethods", "access_control_allow_methods", "access-control-allow-methods"})
    public List<String> getAccessControlAllowMethods() {
        return accessControlAllowMethods;
    }

    public void setAccessControlAllowMethods(List<String> accessControlAllowMethods) {
        this.accessControlAllowMethods = accessControlAllowMethods;
    }
}
