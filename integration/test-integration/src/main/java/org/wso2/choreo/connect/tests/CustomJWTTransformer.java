/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wso2.choreo.connect.tests;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.lang3.StringUtils;
import org.wso2.carbon.apimgt.common.gateway.jwttransformer.JWTTransformer;
import org.wso2.carbon.apimgt.common.gateway.exception.JWTGeneratorException;
import org.wso2.carbon.apimgt.common.gateway.dto.TokenIssuerDto;
import org.wso2.carbon.apimgt.common.gateway.dto.ClaimMappingDto;
import org.wso2.choreo.connect.enforcer.security.jwt.JwtTransformerAnnotation;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JwtTransformerAnnotation(
        enabled = true,
        name = "CustomJWTTransformer",
        issuer = "https://localhost:9443/oauth2/token"
)
public class CustomJWTTransformer implements JWTTransformer {

    protected TokenIssuerDto tokenIssuer = null;

    @Override
    public String getTransformedConsumerKey(JWTClaimsSet jwtClaimsSet) throws JWTGeneratorException {

        try {
            if (tokenIssuer.getConsumerKeyClaim() == null) {
                if (jwtClaimsSet.getClaim("consumerKey") != null) {
                    return jwtClaimsSet.getStringClaim("consumerKey");
                } else if (jwtClaimsSet.getClaim("azp") != null) {
                    return jwtClaimsSet.getStringClaim("azp");
                }
            } else {
                if (jwtClaimsSet.getClaim(tokenIssuer.getConsumerKeyClaim()) != null) {
                    return jwtClaimsSet.getStringClaim(tokenIssuer.getConsumerKeyClaim());
                }
            }
        } catch (ParseException e) {
            throw new JWTGeneratorException("Error while parsing JWT claims", e);
        }

        return null;
    }

    @Override
    public List<String> getTransformedScopes(JWTClaimsSet jwtClaimsSet) throws JWTGeneratorException {

        try {
            String scopeClaim = "scope";
            if (StringUtils.isNotEmpty(tokenIssuer.getScopesClaim())) {
                scopeClaim = tokenIssuer.getScopesClaim();
            }
            if (jwtClaimsSet.getClaim(scopeClaim) instanceof String) {
                return Arrays.asList(jwtClaimsSet.getStringClaim(scopeClaim)
                        .split(" "));
            } else if (jwtClaimsSet.getClaim(scopeClaim) instanceof List) {
                return jwtClaimsSet.getStringListClaim(scopeClaim);
            }
        } catch (ParseException e) {
            throw new JWTGeneratorException("Error while parsing JWT claims", e);
        }
        return Arrays.asList("default");
    }

    @Override
    public JWTClaimsSet transform(JWTClaimsSet jwtClaimsSet) {

        JWTClaimsSet.Builder transformedJWT = new JWTClaimsSet.Builder();
        if (tokenIssuer != null) {
            Map<String, ClaimMappingDto> claimConfigurations = tokenIssuer.getClaimConfigurations();
            for (Map.Entry<String, Object> claimEntry : jwtClaimsSet.getClaims().entrySet()) {
                ClaimMappingDto claimMappingDto = claimConfigurations.get(claimEntry.getKey());
                String claimKey = claimEntry.getKey();
                if (claimMappingDto != null) {
                    claimKey = claimMappingDto.getLocalClaim();
                    claimKey = "CustomClaim: " + claimKey;
                }
                transformedJWT.claim(claimKey, claimEntry.getValue());
            }
            return transformedJWT.build();
        }
        return jwtClaimsSet;
    }

    @Override
    public String getIssuer() {

        return "";
    }

    @Override
    public void loadConfiguration(TokenIssuerDto tokenIssuerConfiguration) {

        this.tokenIssuer = tokenIssuerConfiguration;
    }

    /**
     * Returns whether the token type is Application or not by checking if 'aut' claim is APPLICATION or not. If 'aut'
     * claim is not present, returns null.
     *
     * @param jwtClaimsSet JWT Claim set
     * @return Boolean whether Application token type or not
     */
    @Override
    public Boolean getTransformedIsAppTokenType(JWTClaimsSet jwtClaimsSet) throws JWTGeneratorException {

        try {
            if (jwtClaimsSet.getClaim("aut") != null) {
                String aut = jwtClaimsSet.getStringClaim("aut");
                return StringUtils.equalsIgnoreCase(aut, "application");
            }
        } catch (ParseException e) {
            throw new JWTGeneratorException("Error while parsing JWT claims", e);
        }
        return false;
    }
}

