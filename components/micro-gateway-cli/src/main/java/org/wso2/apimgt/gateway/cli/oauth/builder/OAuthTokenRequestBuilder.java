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
package org.wso2.apimgt.gateway.cli.oauth.builder;

import org.wso2.apimgt.gateway.cli.model.oauth.OAuthTokenRequest;

public class OAuthTokenRequestBuilder {

    private OAuthTokenRequest oAuthTokenRequest;
    private String request = "";

    public OAuthTokenRequestBuilder() {
        oAuthTokenRequest = new OAuthTokenRequest();
    }

    public OAuthTokenRequest build() {
        return oAuthTokenRequest;
    }

    public String requestBody() {
        return request;
    }

    public OAuthTokenRequestBuilder setClientSecret(char[] clientSecret) {
        this.request += "&client_secret=" + new String(clientSecret);
        oAuthTokenRequest.setClientSecret(clientSecret);
        return this;
    }

    public OAuthTokenRequestBuilder setClientKey(String clientKey) {
        this.request += "client_id=" + clientKey;
        this.oAuthTokenRequest.setClientKey(clientKey);
        return this;
    }

    public OAuthTokenRequestBuilder setScopes(String[] scopes) {
        this.request += "&scope=" + String.join(" ", scopes);
        this.oAuthTokenRequest.setScopes(scopes);
        return this;
    }

    public OAuthTokenRequestBuilder setPassword(char[] password) {
        this.request += "&password=" + new String(password);
        this.oAuthTokenRequest.setPassword(password);
        return this;
    }

    public OAuthTokenRequestBuilder setUsername(String username) {
        this.request += "&username=" + username;
        oAuthTokenRequest.setUsername(username);
        return this;
    }

    public OAuthTokenRequestBuilder setGrantType(String grantType) {
        this.request += "grant_type=" + grantType;
        oAuthTokenRequest.setGrantType(grantType);
        return this;
    }

    public OAuthTokenRequestBuilder setValidityPeriod(String validityPeriod) {
        this.request += "&validity_period=" + validityPeriod;
        oAuthTokenRequest.setValidityPeriod(validityPeriod);
        return this;

    }
}
