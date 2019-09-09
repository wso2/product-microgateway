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
package org.wso2.apimgt.gateway.cli.model.oauth;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * OAuth token request definition.
 */
public class OAuthTokenRequest {

    private char[] clientSecret;
    private String clientKey;
    private String[] scopes;
    private char[] password;
    private String username;
    private String grantType;

    public char[] getClientSecret() {
        if (clientSecret == null) {
            return new char[0];
        }

        return clientSecret.clone();
    }

    public void setClientSecret(char[] clientSecret) {
        this.clientSecret = clientSecret.clone();
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public String[] getScopes() {
        return scopes;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public char[] getPassword() {
        if (password == null) {
            return new char[0];
        }
        return password.clone();
    }

    public void setPassword(char[] password) {
        this.password = password.clone();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
}
