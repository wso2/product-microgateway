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
package org.wso2.apimgt.gateway.cli.oauth;

public interface OAuthService {

    /**
     * Generate access token using password grant
     *
     * @param tokenEndpoint token endpoint
     * @param username      userid
     * @param password      password
     * @param clientId      client consumer key
     * @param clientSecret  client consumer secret
     * @param inSecure
     * @return access token
     */
    String generateAccessToken(String tokenEndpoint, String username, char[] password, String clientId,
                               String clientSecret, boolean inSecure);

    /**
     * Generate OAuth application via DCR
     *  @param dcrEndpoint DCR endpoint
     * @param username    user
     * @param password    password password of the user
     * @param inSecure
     */
    String[] generateClientIdAndSecret(String dcrEndpoint, String username, char[] password, boolean inSecure);
}
