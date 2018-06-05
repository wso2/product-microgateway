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
package org.wso2.apimgt.gateway.codegen.config.bean;

public class TokenConfig {

    private String publisherEndpoint;
    private String registrationEndpoint;
    private String tokenEndpoint;
    private String username;
    private String clientId;
    private String clientSecret;
    private String trustoreLocation;
    private String trustorePassword;

    public String getTrustoreLocation() {
        return trustoreLocation;
    }

    public void setTrustoreLocation(String trustoreLocation) {
        this.trustoreLocation = trustoreLocation;
    }

    public String getTrustorePassword() {
        return trustorePassword;
    }

    public void setTrustorePassword(String trustorePassword) {
        this.trustorePassword = trustorePassword;
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
}
