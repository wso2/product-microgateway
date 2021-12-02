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

import java.util.Map;

/**
 * SecurityInfo holds the backend security configurations for an endpoint.
 */
public class SecurityInfo {
    private char[] password;
    private String username;
    private boolean enabled;
    private String securityType;
    private Map<String, String> customParameters;

    /**
     * Get the password for endpoint (Basic Auth)
     *
     * @return password (as a char array) under endpoint security
     */
    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    /**
     * Get the username for endpoint (Basic Auth)
     *
     * @return username under endpoint security
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * true if Endpoint Security is enabled.
     *
     * @return whether endpoint security is enabled or not
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * The endpoint security type for Endpoint Security object.
     *
     * Note:
     *  Only supported security type is "Basic" at the moment.
     *
     * @return security Type
     */
    public String getSecurityType() {
        return securityType;
    }

    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }

    /**
     * The Custom Parameters listed under endpoint security object are returned as a string.
     * TODO: (VirajSalaka) map ?
     *
     * Note:
     *  Only supported security type is "Basic" at the moment.
     *
     * @return security Type
     */
    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(Map<String, String> customParameters) {
        this.customParameters = customParameters;
    }
}
