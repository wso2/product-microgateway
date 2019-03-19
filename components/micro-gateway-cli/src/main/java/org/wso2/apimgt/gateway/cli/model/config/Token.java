/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.model.config;

import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.File;

public class Token {

    private String baseURL;
    private String restVersion;
    private String publisherEndpoint;
    private String adminEndpoint;
    private String registrationEndpoint;
    private String tokenEndpoint;
    private String username;
    private String clientId;
    private String clientSecret;
    private String trustStoreLocation;
    private String trustStorePassword;

    public String getTrustStoreLocation() {
        return trustStoreLocation;
    }

    public String getTrustStoreAbsoluteLocation() {
        String absoluteTrustoreLocation = trustStoreLocation;
        File file = new File(absoluteTrustoreLocation);
        if (!file.isAbsolute()) {
            absoluteTrustoreLocation = GatewayCmdUtils.getCLIHome() + File.separator + absoluteTrustoreLocation;
            file = new File(absoluteTrustoreLocation);
            if (!file.exists()) {
                System.err.println("Error while loading trust store location: " + absoluteTrustoreLocation);
                Runtime.getRuntime().exit(1);
            }
        }
        return absoluteTrustoreLocation;
    }

    public void setTrustStoreLocation(String trustStoreLocation) {
        this.trustStoreLocation = trustStoreLocation;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustorePassword) {
        this.trustStorePassword = trustorePassword;
    }

    public String getPublisherEndpoint() {
        return publisherEndpoint;
    }

    public void setPublisherEndpoint(String publisherEndpoint) {
        this.publisherEndpoint = publisherEndpoint;
    }

    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAdminEndpoint() {
        return adminEndpoint;
    }

    public void setAdminEndpoint(String adminEndpoint) {
        this.adminEndpoint = adminEndpoint;
    }

    public String getRestVersion() {
        return restVersion;
    }

    public void setRestVersion(String restVersion) {
        this.restVersion = restVersion;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }
}
