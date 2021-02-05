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
import org.wso2.micro.gateway.jwt.generator.MGWJWTGeneratorConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is for default Jwt transformer.
 */
public class TestJWTGenerator extends AbstractMGWJWTGenerator {
    public TestJWTGenerator(String dialectURI, String signatureAlgorithm, String keyStorePath, String keyStorePassword,
                            String certificateAlias, String privateKeyAlias, int jwtExpiryTime,
                            String[] restrictedClaims, boolean cacheEnabled, int cacheExpiry, String tokenIssuer,
                            String[] tokenAudience) {
        super(dialectURI, signatureAlgorithm, keyStorePath, keyStorePassword, certificateAlias, privateKeyAlias,
                jwtExpiryTime, restrictedClaims, cacheEnabled, cacheExpiry, tokenIssuer, tokenAudience);
    }

    /**
     * Method to populate standard claims and APIM related claims
     * @param jwtInfo - JWT payload
     * @return generated standard claims
     */
    @Override
    public Map<String, Object> populateStandardClaims(Map<String, Object> jwtInfo) {
        long currentTime = System.currentTimeMillis();
        long expireIn = currentTime + getTTL();
        String dialect = this.getDialectURI();
        Map<String, Object> claims = new HashMap<>();
        HashMap<String, Object> customClaims = (HashMap<String, Object>) jwtInfo
                .get(MGWJWTGeneratorConstants.CUSTOM_CLAIMS);
        claims.put(MGWJWTGeneratorConstants.ISSUER_CLAIM, getTokenIssuer());
        if (getTokenAudience().length == 1) {
            claims.put(MGWJWTGeneratorConstants.AUDIENCE_CLAIM, getTokenAudience()[0]);
        } else if (getTokenAudience().length != 0) {
            claims.put(MGWJWTGeneratorConstants.AUDIENCE_CLAIM, arrayToJSONArray(getTokenAudience()));
        }
        claims.put(MGWJWTGeneratorConstants.JTI_CLAIM, UUID.randomUUID().toString());
        claims.put(MGWJWTGeneratorConstants.IAT_CLAIM, (int) (currentTime / 1000));
        claims.put(MGWJWTGeneratorConstants.EXP_CLAIM, (int) (expireIn / 1000));
        if (StringUtils.isNotEmpty((CharSequence) jwtInfo.get(MGWJWTGeneratorConstants.SUB_CLAIM))) {
            claims.put(MGWJWTGeneratorConstants.SUB_CLAIM, jwtInfo.get(MGWJWTGeneratorConstants.SUB_CLAIM));
            claims.put(dialect + "/endUser", jwtInfo.get(MGWJWTGeneratorConstants.SUB_CLAIM));
        }
        if (StringUtils.isNotEmpty((CharSequence) customClaims.get(MGWJWTGeneratorConstants.SCOPES_CLAIM))) {
            claims.put(MGWJWTGeneratorConstants.SCOPES_CLAIM,
                    (customClaims.get(MGWJWTGeneratorConstants.SCOPES_CLAIM)));
        }
        return claims;
    }

    /**
     * Method to populate custom claims apart from the standard claims
     * @param jwtInfo - JWT payload
     * @param restrictedClaims - restricted claims that should not be included in the generated token payload
     * @return generated custom claims
     */
    @Override
    public Map<String, Object> populateCustomClaims(Map<String, Object> jwtInfo, ArrayList<String> restrictedClaims) {
        Map<String, Object> claims = new HashMap<>();
        for (String key: jwtInfo.keySet()) {
            if (key.equals(MGWJWTGeneratorConstants.CUSTOM_CLAIMS)) {
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
