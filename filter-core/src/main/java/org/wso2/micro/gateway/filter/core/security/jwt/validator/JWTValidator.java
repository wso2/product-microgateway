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

package org.wso2.micro.gateway.filter.core.security.jwt.validator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.common.ReferenceHolder;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;
import org.wso2.micro.gateway.filter.core.dto.TokenIssuerDto;
import org.wso2.micro.gateway.filter.core.exception.MGWException;
import org.wso2.micro.gateway.filter.core.security.jwt.DefaultJWTTransformer;
import org.wso2.micro.gateway.filter.core.security.jwt.JWTTransformer;
import org.wso2.micro.gateway.filter.core.security.jwt.JWTUtil;
import org.wso2.micro.gateway.filter.core.security.jwt.JWTValidationInfo;
import org.wso2.micro.gateway.filter.core.security.jwt.SignedJWTInfo;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class responsible to validate jwt. This should validate the JWT signature, expiry time.
 * validating the sub, aud, iss can be made optional.
 */
public class JWTValidator {
    private static final Logger logger = LogManager.getLogger(JWTValidator.class);
    private Map<String, TokenIssuerDto> tokenIssuers;
    private TokenIssuerDto tokenIssuer;
    private JWTTransformer jwtTransformer;
    private JWKSet jwkSet;

    public JWTValidator() {
        loadTokenIssuerConfiguration();
    }

    public void loadTokenIssuerConfiguration() {
        tokenIssuers = ReferenceHolder.getInstance().getMGWConfiguration().getJWTIssuers();
        this.jwtTransformer = new DefaultJWTTransformer();
    }

    public JWTValidationInfo validateJWTToken(SignedJWTInfo signedJWTInfo) throws MGWException {
        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
        String issuer = signedJWTInfo.getJwtClaimsSet().getIssuer();
        if (StringUtils.isNotEmpty(issuer) && tokenIssuers.containsKey(issuer)) {
            this.tokenIssuer = this.tokenIssuers.get(issuer);
            this.jwtTransformer.loadConfiguration(tokenIssuer);
            return validateToken(signedJWTInfo);
        }
        jwtValidationInfo.setValid(false);
        jwtValidationInfo.setValidationCode(APIConstants.KeyValidationStatus.API_AUTH_GENERAL_ERROR);
        return jwtValidationInfo;
    }

    private JWTValidationInfo validateToken(SignedJWTInfo signedJWTInfo) throws MGWException {

        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
        boolean state;
        try {
            state = validateSignature(signedJWTInfo.getSignedJWT());
            if (state) {
                JWTClaimsSet jwtClaimsSet = signedJWTInfo.getJwtClaimsSet();
                state = validateTokenExpiry(jwtClaimsSet);
                if (state) {
                    jwtValidationInfo.setConsumerKey(getConsumerKey(jwtClaimsSet));
                    jwtValidationInfo.setScopes(getScopes(jwtClaimsSet));
                    JWTClaimsSet transformedJWTClaimSet = transformJWTClaims(jwtClaimsSet);
                    createJWTValidationInfoFromJWT(jwtValidationInfo, transformedJWTClaimSet);
                    jwtValidationInfo.setRawPayload(signedJWTInfo.getToken());
                    jwtValidationInfo.setKeyManager(this.tokenIssuer.getName());
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
        } catch (ParseException e) {
            throw new MGWException("Error while parsing JWT", e);
        }
    }

    protected boolean validateSignature(SignedJWT signedJWT) throws MGWException {

        String certificateAlias = APIConstants.GATEWAY_PUBLIC_CERTIFICATE_ALIAS;
        try {
            String keyID = signedJWT.getHeader().getKeyID();
            if (StringUtils.isNotEmpty(keyID)) {
                if (tokenIssuer.getJwksConfigurationDTO().isEnabled() && StringUtils
                        .isNotEmpty(tokenIssuer.getJwksConfigurationDTO().getUrl())) {
                    // Check JWKSet Available in Cache
                    if (jwkSet == null) {
                        jwkSet = retrieveJWKSet();
                    }
                    if (jwkSet.getKeyByKeyId(keyID) == null) {
                        jwkSet = retrieveJWKSet();
                    }
                    if (jwkSet.getKeyByKeyId(keyID) instanceof RSAKey) {
                        RSAKey keyByKeyId = (RSAKey) jwkSet.getKeyByKeyId(keyID);
                        RSAPublicKey rsaPublicKey = keyByKeyId.toRSAPublicKey();
                        if (rsaPublicKey != null) {
                            return JWTUtil.verifyTokenSignature(signedJWT, rsaPublicKey);
                        }
                    } else {
                        throw new MGWException("Key Algorithm not supported");
                    }
                } else if (tokenIssuer.getCertificate() != null) {
                    logger.debug("Retrieve certificate from Token issuer and validating");
                    RSAPublicKey rsaPublicKey = (RSAPublicKey) tokenIssuer.getCertificate().getPublicKey();;
                    return JWTUtil.verifyTokenSignature(signedJWT, rsaPublicKey);
                } else {
                    return JWTUtil.verifyTokenSignature(signedJWT, keyID);
                }
            }
            return JWTUtil.verifyTokenSignature(signedJWT, certificateAlias);
        } catch (ParseException | JOSEException | IOException e) {
            logger.error("Error while parsing JWT", e);
        }

        return true;
    }

    protected boolean validateTokenExpiry(JWTClaimsSet jwtClaimsSet) {

        long timestampSkew = 5; //TODO : Read from config.
        Date now = new Date();
        Date exp = jwtClaimsSet.getExpirationTime();
        return exp == null || DateUtils.isAfter(exp, now, timestampSkew);
    }

    private JWKSet retrieveJWKSet() throws IOException, ParseException {
        String jwksInfo = JWTUtil.retrieveJWKSConfiguration(tokenIssuer.getJwksConfigurationDTO().getUrl());
        jwkSet = JWKSet.parse(jwksInfo);
        return jwkSet;
    }

    protected String getConsumerKey(JWTClaimsSet jwtClaimsSet) throws MGWException {

        return jwtTransformer.getTransformedConsumerKey(jwtClaimsSet);
    }

    protected List<String> getScopes(JWTClaimsSet jwtClaimsSet) throws MGWException {

        return jwtTransformer.getTransformedScopes(jwtClaimsSet);
    }

    protected JWTClaimsSet transformJWTClaims(JWTClaimsSet jwtClaimsSet) throws MGWException {

        return jwtTransformer.transform(jwtClaimsSet);
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
