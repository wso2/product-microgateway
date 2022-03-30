/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.security.jwt.validator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.exception.JWTGeneratorException;
import org.wso2.carbon.apimgt.common.gateway.jwttransformer.JWTTransformer;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ExtendedTokenIssuerDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.security.jwt.SignedJWTInfo;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Class responsible to validate jwt. This should validate the JWT signature, expiry time.
 * validating the sub, aud, iss can be made optional.
 */
public class JWTValidator {
    private static final Logger logger = LogManager.getLogger(JWTValidator.class);
    private JWKSet jwkSet;

    public JWTValidator() {
    }


    public JWTValidationInfo validateJWTToken(SignedJWTInfo signedJWTInfo) throws EnforcerException {
        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
        String issuer = signedJWTInfo.getJwtClaimsSet().getIssuer();
        Map<String, ExtendedTokenIssuerDto> tokenIssuers = ConfigHolder.getInstance().getConfig().getIssuersMap();

        if (StringUtils.isNotEmpty(issuer) && tokenIssuers.containsKey(issuer)) {
            ExtendedTokenIssuerDto tokenIssuer = tokenIssuers.get(issuer);
            JWTTransformer jwtTransformer = ConfigHolder.getInstance().getConfig().getJwtTransformer(issuer);
            jwtTransformer.loadConfiguration(tokenIssuer);
            return validateToken(signedJWTInfo, tokenIssuer, jwtTransformer);
        }
        jwtValidationInfo.setValid(false);
        jwtValidationInfo.setValidationCode(APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
        logger.error("No matching issuer found for the token with issuer : " + issuer);
        return jwtValidationInfo;
    }

    private JWTValidationInfo validateToken(SignedJWTInfo signedJWTInfo, ExtendedTokenIssuerDto tokenIssuer,
                                            JWTTransformer jwtTransformer) throws EnforcerException {
        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
        boolean state;
        try {
            state = validateSignature(signedJWTInfo.getSignedJWT(), tokenIssuer);
            if (state) {
                JWTClaimsSet jwtClaimsSet = signedJWTInfo.getJwtClaimsSet();
                state = validateTokenExpiry(jwtClaimsSet);
                if (state) {
                    jwtValidationInfo.setConsumerKey(jwtTransformer.getTransformedConsumerKey(jwtClaimsSet));
                    jwtValidationInfo.setScopes(jwtTransformer.getTransformedScopes(jwtClaimsSet));
                    JWTClaimsSet transformedJWTClaimSet = jwtTransformer.transform(jwtClaimsSet);
                    createJWTValidationInfoFromJWT(jwtValidationInfo, transformedJWTClaimSet);
                    jwtValidationInfo.setRawPayload(signedJWTInfo.getToken());
                    jwtValidationInfo.setKeyManager(tokenIssuer.getName());
                    return jwtValidationInfo;
                } else {
                    jwtValidationInfo.setValid(false);
                    jwtValidationInfo.setValidationCode(APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
                    return jwtValidationInfo;
                }
            } else {
                jwtValidationInfo.setValid(false);
                jwtValidationInfo.setValidationCode(APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
                return jwtValidationInfo;
            }
        } catch (ParseException | JWTGeneratorException e) {
            throw new EnforcerException("Error while parsing JWT", e);
        }
    }

    protected boolean validateSignature(SignedJWT signedJWT, ExtendedTokenIssuerDto tokenIssuer)
            throws EnforcerException {
        try {
            String certificateAlias = tokenIssuer.getCertificateAlias();
            String keyID = signedJWT.getHeader().getKeyID();
            if (StringUtils.isNotEmpty(keyID)) {
                if (tokenIssuer.getJwksConfigurationDTO().isEnabled() && StringUtils
                        .isNotEmpty(tokenIssuer.getJwksConfigurationDTO().getUrl())) {
                    // Check JWKSet Available in Cache
                    if (jwkSet == null) {
                        jwkSet = retrieveJWKSet(tokenIssuer);
                    }
                    if (jwkSet.getKeyByKeyId(keyID) == null) {
                        jwkSet = retrieveJWKSet(tokenIssuer);
                    }
                    if (jwkSet.getKeyByKeyId(keyID) instanceof RSAKey) {
                        RSAKey keyByKeyId = (RSAKey) jwkSet.getKeyByKeyId(keyID);
                        RSAPublicKey rsaPublicKey = keyByKeyId.toRSAPublicKey();
                        if (rsaPublicKey != null) {
                            return JWTUtils.verifyTokenSignature(signedJWT, rsaPublicKey);
                        }
                    } else {
                        throw new EnforcerException("Key Algorithm not supported");
                    }
                } else if (tokenIssuer.getCertificate() != null) {
                    logger.debug("Retrieve certificate from Token issuer and validating");
                    RSAPublicKey rsaPublicKey = (RSAPublicKey) tokenIssuer.getCertificate().getPublicKey();
                    return JWTUtils.verifyTokenSignature(signedJWT, rsaPublicKey);
                } else {
                    //TODO: (VirajSalaka) Come up with a fix
                    return JWTUtils.verifyTokenSignature(signedJWT, keyID);
                }
            }
            return JWTUtils.verifyTokenSignature(signedJWT, certificateAlias);
        } catch (ParseException | JOSEException | IOException e) {
            throw new EnforcerException("JWT Signature verification failed", e);
        }
    }

    protected boolean validateTokenExpiry(JWTClaimsSet jwtClaimsSet) {

        long timestampSkew = 5; //TODO : Read from config.
        Date now = new Date();
        Date exp = jwtClaimsSet.getExpirationTime();
        return exp == null || DateUtils.isAfter(exp, now, timestampSkew);
    }

    private JWKSet retrieveJWKSet(ExtendedTokenIssuerDto tokenIssuer) throws IOException, ParseException {
        String jwksInfo = JWTUtils.retrieveJWKSConfiguration(tokenIssuer.getJwksConfigurationDTO().getUrl());
        jwkSet = JWKSet.parse(jwksInfo);
        return jwkSet;
    }

    private void createJWTValidationInfoFromJWT(JWTValidationInfo jwtValidationInfo, JWTClaimsSet jwtClaimsSet)
            throws ParseException {
        jwtValidationInfo.setIssuer(jwtClaimsSet.getIssuer());
        jwtValidationInfo.setValid(true);
        jwtValidationInfo.setClaims(jwtClaimsSet.getClaims());
        jwtValidationInfo.setExpiryTime(jwtClaimsSet.getExpirationTime().getTime());
        jwtValidationInfo.setIssuedTime(jwtClaimsSet.getIssueTime().getTime());
        jwtValidationInfo.setUser(jwtClaimsSet.getSubject());
        jwtValidationInfo.setJti(jwtClaimsSet.getJWTID());
        if (jwtClaimsSet.getClaim(APIConstants.JwtTokenConstants.SCOPE) != null) {
            jwtValidationInfo.setScopes(Arrays.asList(jwtClaimsSet.getStringClaim(APIConstants.JwtTokenConstants.SCOPE)
                    .split(APIConstants.JwtTokenConstants.SCOPE_DELIMITER)));
        }
    }


}
