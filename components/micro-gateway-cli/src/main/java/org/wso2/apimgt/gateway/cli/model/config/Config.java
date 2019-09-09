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

import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;

/**
 * Configuration data holder.
 * Holds toolkit config and few other configurations.
 */
public class Config {
    private Client client;
    private Token token;
    private APICorsConfigurationDTO corsConfiguration;
    private MutualSSL mutualSSL;
    private BasicAuth basicAuth;

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public APICorsConfigurationDTO getCorsConfiguration() {
        return corsConfiguration;
    }

    public void setCorsConfiguration(APICorsConfigurationDTO corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }

    public MutualSSL getMutualSSL() {
        return mutualSSL;
    }

    public void setMutualSSL(MutualSSL mutualSSL) {
        this.mutualSSL = mutualSSL;
    }

    public BasicAuth getBasicAuth() {
        return basicAuth;
    }

    public void setBasicAuth(BasicAuth basicAuth) {
        this.basicAuth = basicAuth;
    }
}
