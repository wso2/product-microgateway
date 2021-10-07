/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.config.dto;

/**
 * This contains authorization header properties.
 */
public class AuthHeaderDto {
    private boolean enableOutboundAuthHeader = false;
    private String authorizationHeader = "";
    private String testConsoleHeaderName = "";

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public boolean isEnableOutboundAuthHeader() {
        return enableOutboundAuthHeader;
    }

    public void setEnableOutboundAuthHeader(boolean enableOutboundAuthHeader) {
        this.enableOutboundAuthHeader = enableOutboundAuthHeader;
    }

    public void setTestConsoleHeaderName(String testConsoleHeaderName) {
        this.testConsoleHeaderName = testConsoleHeaderName;
    }

    public String getTestConsoleHeaderName() {
        return testConsoleHeaderName;
    }
}
