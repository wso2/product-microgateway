/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.apimgt.gateway.cli.model.config;

/**
 * Builder class for Token class
 */
public class TokenBuilder {
    private Token token;

    public TokenBuilder() {
        token = new Token();
    }

    public TokenBuilder setTrustStoreLocation(String trustStoreLocation) {
        this.token.setTrustStoreLocation(trustStoreLocation);
        return this;
    }

    public TokenBuilder setTrustStorePassword(String trustorePassword) {
        this.token.setTrustStorePassword(trustorePassword);
        return this;
    }

    public TokenBuilder setPublisherEndpoint(String publisherEndpoint) {
        this.token.setPublisherEndpoint(publisherEndpoint);
        return this;
    }

    public TokenBuilder setRegistrationEndpoint(String registrationEndpoint) {
        this.token.setRegistrationEndpoint(registrationEndpoint);
        return this;
    }

    public TokenBuilder setTokenEndpoint(String tokenEndpoint) {
        this.token.setTokenEndpoint(tokenEndpoint);
        return this;
    }

    public TokenBuilder setUsername(String username) {
        this.token.setUsername(username);
        return this;
    }

    public TokenBuilder setClientId(String clientId) {
        this.token.setClientId(clientId);
        return this;
    }

    public TokenBuilder setClientSecret(String clientSecret) {
        this.token.setClientSecret(clientSecret);
        return this;
    }

    public TokenBuilder setAdminEndpoint(String adminEndpoint) {
        this.token.setAdminEndpoint(adminEndpoint);
        return this;
    }

    public TokenBuilder setRestVersion(String restVersion) {
        this.token.setRestVersion(restVersion);
        return this;
    }

    public TokenBuilder setBaseURL(String baseURL) {
        this.token.setBaseURL(baseURL);
        return this;
    }

    public Token build() {
        return token;
    }
}
