/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.jwt.generator;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class to implement standard claims and custom claims.
 */
public class MGWJWTGeneratorImpl extends AbstractMGWJWTGenerator {
    private static final Logger logger = LogManager.getLogger(MGWJWTGeneratorImpl.class);

    public MGWJWTGeneratorImpl(String dialectURI, String signatureAlgorithm, String trustStorePath,
                               String trustStorePassword, String certificateAlias, String privateKeyAlias,
                               int jwtExpiryTime, String[] restrictedClaims, boolean jwtCacheEnabled,
                               int jwtCacheExpiry, String tokenIssuer, String[] tokenAudience) {
        super(dialectURI, signatureAlgorithm, trustStorePath, trustStorePassword, certificateAlias, privateKeyAlias,
                jwtExpiryTime, restrictedClaims, jwtCacheEnabled, jwtCacheExpiry, tokenIssuer, tokenAudience);
    }

    @Override
    public Map<String, Object> populateStandardClaims(Map<String, Object> jwtInfo) {
        long currentTime = System.currentTimeMillis();
        long expireIn = currentTime + getTTL();
        String dialect = this.getDialectURI();
        Map<String, Object> claims = new HashMap<>();
        HashMap<String, Object> customClaims = (HashMap<String, Object>) jwtInfo
                .get(MGWJWTGeneratorConstants.CUSTOM_CLAIMS);
        claims.put(MGWJWTGeneratorConstants.ISSUER_CLAIM, getTokenIssuer());
        claims.put(MGWJWTGeneratorConstants.JTI_CLAIM, UUID.randomUUID().toString());
        claims.put(MGWJWTGeneratorConstants.IAT_CLAIM, (int) (currentTime / 1000));
        claims.put(MGWJWTGeneratorConstants.EXP_CLAIM, (int) (expireIn / 1000));
        if (StringUtils.isNotEmpty((CharSequence) jwtInfo.get(MGWJWTGeneratorConstants.SUB_CLAIM))) {
            claims.put(MGWJWTGeneratorConstants.SUB_CLAIM, jwtInfo.get(MGWJWTGeneratorConstants.SUB_CLAIM));
            claims.put(dialect + "/enduser", jwtInfo.get(MGWJWTGeneratorConstants.SUB_CLAIM));
        }
        if (StringUtils.isNotEmpty((CharSequence) customClaims.get(MGWJWTGeneratorConstants.SCOPES_CLAIM))) {
            claims.put(MGWJWTGeneratorConstants.SCOPES_CLAIM,
                    (customClaims.get(MGWJWTGeneratorConstants.SCOPES_CLAIM)));
        }
        if (customClaims.get(MGWJWTGeneratorConstants.APPLICATION_CLAIM) != null) {
            if (StringUtils.isNotEmpty(((HashMap) customClaims.get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                    .get(MGWJWTGeneratorConstants.APPLICATION_ID_CLAIM).toString())) {
                claims.put(dialect + "/applicationid", ((HashMap) customClaims
                        .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                        .get(MGWJWTGeneratorConstants.APPLICATION_ID_CLAIM).toString());
            }
            if (StringUtils.isNotEmpty(((HashMap) customClaims.get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                    .get(MGWJWTGeneratorConstants.APPLICATION_UUID_CLAIM).toString())) {
                claims.put(dialect + "/applicationUUId", ((HashMap) customClaims
                        .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                        .get(MGWJWTGeneratorConstants.APPLICATION_UUID_CLAIM).toString());
            }
            if (StringUtils.isNotEmpty((CharSequence) ((HashMap) customClaims
                    .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                    .get(MGWJWTGeneratorConstants.APPLICATION_OWNER_CLAIM))) {
                claims.put(dialect + "/subscriber", ((HashMap) customClaims
                        .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                        .get(MGWJWTGeneratorConstants.APPLICATION_OWNER_CLAIM));
            }
            if (StringUtils.isNotEmpty((CharSequence) ((HashMap) customClaims
                    .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                    .get(MGWJWTGeneratorConstants.APPLICATION_NAME_CLAIM))) {
                claims.put(dialect + "/applicationname", ((HashMap) customClaims
                        .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                        .get(MGWJWTGeneratorConstants.APPLICATION_NAME_CLAIM));
            }
            if (StringUtils.isNotEmpty((CharSequence) ((HashMap) customClaims
                    .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                    .get(MGWJWTGeneratorConstants.APPLICATION_TIER_CLAIM))) {
                claims.put(dialect + "/applicationtier", ((HashMap) customClaims
                        .get(MGWJWTGeneratorConstants.APPLICATION_CLAIM))
                        .get(MGWJWTGeneratorConstants.APPLICATION_TIER_CLAIM));
            }
        }
        if (StringUtils.isNotEmpty((CharSequence) getApiDetails().get(MGWJWTGeneratorConstants.API_NAME_CLAIM))) {
            claims.put(dialect + "/apiname", getApiDetails().get(MGWJWTGeneratorConstants.API_NAME_CLAIM));
        }
        if (StringUtils.isNotEmpty((CharSequence) getApiDetails()
                .get(MGWJWTGeneratorConstants.SUBSCRIBER_TENANT_DOMAIN_CLAIM))) {
            claims.put(dialect + "/enduserTenantDomain", getApiDetails()
                    .get(MGWJWTGeneratorConstants.SUBSCRIBER_TENANT_DOMAIN_CLAIM));
        }
        if (StringUtils.isNotEmpty((CharSequence) getApiDetails().get(MGWJWTGeneratorConstants.API_CONTEXT_CLAIM))) {
            claims.put(dialect + "/apicontext", getApiDetails().get(MGWJWTGeneratorConstants.API_CONTEXT_CLAIM));
        }
        if (StringUtils.isNotEmpty((CharSequence) getApiDetails().get(MGWJWTGeneratorConstants.API_VERSION_CLAIM))) {
            claims.put(dialect + "/version", getApiDetails().get(MGWJWTGeneratorConstants.API_VERSION_CLAIM));
        }
        if (StringUtils.isNotEmpty((CharSequence) getApiDetails().get(MGWJWTGeneratorConstants.API_TIER_CLAIM))) {
            claims.put(dialect + "/tier", getApiDetails().get(MGWJWTGeneratorConstants.API_TIER_CLAIM));
        }
        if (StringUtils.isNotEmpty((CharSequence) customClaims.get(MGWJWTGeneratorConstants.KEY_TYPE_CLAIM))) {
            claims.put(dialect + "/keytype", customClaims.get(MGWJWTGeneratorConstants.KEY_TYPE_CLAIM));
        } else {
            claims.put(dialect + "/keytype", MGWJWTGeneratorConstants.KEY_TYPE_PRODUCTION);
        }
        claims.put(dialect + "/usertype", MGWJWTGeneratorConstants.AUTH_APPLICATION_USER_LEVEL_TOKEN);
        return claims;
    }

    @Override
    public Map<String, Object> populateCustomClaims(Map<String, Object> jwtInfo,
                                                    ArrayList<String> restrictedClaims) {
        Map<String, Object> claims = new HashMap<>();
        for (String key : jwtInfo.keySet()) {
            if (key.equals(MGWJWTGeneratorConstants.CUSTOM_CLAIMS)) {
                Map<String, Object> customClaims = (Map<String, Object>) jwtInfo.get(key);
                for (String subKey : customClaims.keySet()) {
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
