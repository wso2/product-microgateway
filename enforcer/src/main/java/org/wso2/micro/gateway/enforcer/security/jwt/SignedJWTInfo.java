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

package org.wso2.micro.gateway.enforcer.security.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.Serializable;

/**
 * JWT internal Representation.
 */
public class SignedJWTInfo implements Serializable {

    private String token;
    private SignedJWT signedJWT;
    private JWTClaimsSet jwtClaimsSet;

    public SignedJWTInfo(String token, SignedJWT signedJWT, JWTClaimsSet jwtClaimsSet) {

        this.token = token;
        this.signedJWT = signedJWT;
        this.jwtClaimsSet = jwtClaimsSet;
    }

    public SignedJWTInfo() {

    }

    public SignedJWT getSignedJWT() {

        return signedJWT;
    }

    public void setSignedJWT(SignedJWT signedJWT) {

        this.signedJWT = signedJWT;
    }

    public JWTClaimsSet getJwtClaimsSet() {

        return jwtClaimsSet;
    }

    public void setJwtClaimsSet(JWTClaimsSet jwtClaimsSet) {

        this.jwtClaimsSet = jwtClaimsSet;
    }

    public String getToken() {

        return token;
    }

    public void setToken(String token) {

        this.token = token;
    }
}
