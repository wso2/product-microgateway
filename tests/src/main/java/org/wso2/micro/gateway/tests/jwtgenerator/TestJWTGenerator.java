/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.tests.jwtgenerator;

import org.apache.commons.lang3.StringUtils;
import org.wso2.micro.gateway.jwt.generator.AbstractMGWJWTGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is for default Jwt transformer.
 */
public class TestJWTGenerator extends AbstractMGWJWTGenerator {
    public TestJWTGenerator(String dialectURI,
                            String signatureAlgorithm,
                            String keyStorePath,
                            String keyStorePassword,
                            String certificateAlias,
                            String privateKeyAlias,
                            int jwtExpiryTime,
                            String[] restrictedClaims,
                            boolean cacheEnabled,
                            int cacheExpiry,
                            String tokenIssuer,
                            String[] tokenAudience) {
        super(dialectURI,
                signatureAlgorithm,
                keyStorePath,
                keyStorePassword,
                certificateAlias,
                privateKeyAlias,
                jwtExpiryTime,
                restrictedClaims,
                cacheEnabled,
                cacheExpiry,
                tokenIssuer,
                tokenAudience);
    }

    @Override
    public Map<String, Object> populateStandardClaims(Map<String, Object> jwtInfo) {
        long currentTime = System.currentTimeMillis();
        long expireIn = currentTime + getTTL();
        String dialect = this.getDialectURI();
        Map<String, Object> claims = new HashMap<>();
        HashMap<String, Object> customClaims = (HashMap<String, Object>) jwtInfo.get("customClaims");
        claims.put("iss", getTokenIssuer());
        if (getTokenAudience().length == 1) {
            claims.put("aud", getTokenAudience()[0]);
        } else if (getTokenAudience().length != 0) {
            claims.put("aud", arrayToJSONArray(getTokenAudience()));
        }
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", (int) (currentTime / 1000));
        claims.put("exp", (int) (expireIn / 1000));
        if (StringUtils.isNotEmpty((CharSequence) jwtInfo.get("sub"))) {
            claims.put("sub", jwtInfo.get("sub"));
            claims.put(dialect + "/endUser", jwtInfo.get("sub"));
        }
        if (StringUtils.isNotEmpty((CharSequence) customClaims.get("scopes"))) {
            claims.put("scopes", (customClaims.get("scopes")));
        }
        return claims;
    }

    @Override
    public Map<String, Object> populateCustomClaims(Map<String, Object> jwtInfo, ArrayList<String> restrictedClaims) {
        Map<String, Object> claims = new HashMap<>();
        for (String key: jwtInfo.keySet()) {
            if (key.equals("customClaims")) {
                Map<String, Object> customClaims = (Map<String, Object>) jwtInfo.get(key);
                for (String subKey: customClaims.keySet()) {
                    if (!restrictedClaims.contains(subKey)) {
                        claims.put(subKey, customClaims.get(subKey));
                    }
                }
            } else {
                if (!restrictedClaims.contains(key)) {
                    claims.put(key, jwtInfo.get(key));
                }
            }
        }
        return claims;
    }
}
