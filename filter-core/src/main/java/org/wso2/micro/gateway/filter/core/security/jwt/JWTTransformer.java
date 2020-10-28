/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.filter.core.security.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import org.wso2.micro.gateway.filter.core.dto.TokenIssuerDto;
import org.wso2.micro.gateway.filter.core.exception.MGWException;

import java.util.List;

/**
 * This Class will be used to transform JWT claims to local claims
 */
public interface JWTTransformer {

    /**
     * This method used to retrieve ConsumerKey From JWT
     * @param jwtClaimsSet retrieved JwtClaimSet
     * @return consumerKey of JWT
     */
    public String getTransformedConsumerKey(JWTClaimsSet jwtClaimsSet) throws MGWException;

    /**
     * This method used to retrieve Scopes From JWT
     * @param jwtClaimsSet retrieved JwtClaimSet
     * @return scopes of JWT
     */
    public List<String> getTransformedScopes(JWTClaimsSet jwtClaimsSet) throws MGWException;


    /**
     * This method used to transform JWT claimset from given JWT into required format
     *
     * @param jwtClaimsSet jwtClaimSet from given JWT
     * @return transformed JWT Claims.
     */
    public JWTClaimsSet transform(JWTClaimsSet jwtClaimsSet) throws MGWException;

    /**
     * This method returns issuer name which used the implementation to transform JWT.
     *
     * @return issuer url.
     */
    public String getIssuer();

    public void loadConfiguration(TokenIssuerDto tokenIssuerConfiguration);
}
