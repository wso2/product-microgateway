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

package org.wso2.micro.gateway.filter.core.auth.jwt.validator;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.auth.jwt.DefaultJWTTransformer;
import org.wso2.micro.gateway.filter.core.auth.jwt.JWKSConfigurationDTO;
import org.wso2.micro.gateway.filter.core.auth.jwt.JWTTransformer;
import org.wso2.micro.gateway.filter.core.auth.jwt.JWTUtil;
import org.wso2.micro.gateway.filter.core.auth.jwt.JWTValidationInfo;
import org.wso2.micro.gateway.filter.core.auth.jwt.SignedJWTInfo;
import org.wso2.micro.gateway.filter.core.auth.jwt.TokenIssuerDto;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;
import org.wso2.micro.gateway.filter.core.exception.MGWException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible to validate jwt. This should validate the JWT signature, expiry time.
 * validating the sub, aud, iss can be made optional.
 */
public class JWTValidator {
    private static final Logger logger = LogManager.getLogger(JWTValidator.class);

    private RSAPublicKey publicKey;
    private JWSVerifier jwsVerifier;
    private String enableCache = System.getenv("ENVOY_GW_CACHE_ENABLE");
    private LoadingCache<String, String> gatewayApiKeyCache = CacheBuilder.newBuilder()
            .maximumSize(100)                                     // maximum 100 tokens can be cached
            .expireAfterAccess(30, TimeUnit.MINUTES)      // cache will expire after 30 minutes of access
            .build(new CacheLoader<String, String>() {            // build the cacheloader
                @Override public String load(String s) throws Exception {
                    return JWTConstants.UNAVAILABLE;
                }

            });
    private LoadingCache<String, String> invalidGatewayApiKeyCache = CacheBuilder.newBuilder()
            .maximumSize(100)                                     // maximum 100 tokens can be cached
            .expireAfterAccess(30, TimeUnit.MINUTES)      // cache will expire after 30 minutes of access
            .build(new CacheLoader<String, String>() {            // build the cacheloader
                @Override public String load(String s) throws Exception {
                    return JWTConstants.UNAVAILABLE;
                }

            });
    TokenIssuerDto tokenIssuer;
    JWTTransformer jwtTransformer;
    private JWKSet jwkSet;

    public JWTValidator() {
        loadTokenIssuerConfiguration();
    }

    public void loadTokenIssuerConfiguration() {
        String issuer = "https://localhost:9443/oauth2/token"; //TODO: get the issuer
        TokenIssuerDto tokenIssuerDto = new TokenIssuerDto(issuer);
        tokenIssuerDto.setConsumerKeyClaim("azp");
        tokenIssuerDto.setPublicKey(readPublicKey());
        JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
        jwksConfigurationDTO.setEnabled(false);
        tokenIssuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);
        this.jwtTransformer = new DefaultJWTTransformer();
        this.jwtTransformer.loadConfiguration(tokenIssuerDto);
        this.tokenIssuer = tokenIssuerDto;
//      JWTTransformer jwtTransformer = ServiceReferenceHolder.getInstance().getJWTTransformer(tokenIssuer.getIssuer());
//        if (jwtTransformer != null) {
//            this.jwtTransformer = jwtTransformer;
//        } else {
//            this.jwtTransformer = new DefaultJWTTransformer();
//        }
//        this.jwtTransformer.loadConfiguration(tokenIssuer);
    }

    public JWTValidationInfo validateJWTToken(SignedJWTInfo signedJWTInfo) throws MGWException {
        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
        String issuer = signedJWTInfo.getJwtClaimsSet().getIssuer();
        if (StringUtils.isNotEmpty(issuer)) {
            JWTValidationInfo validationInfo = validateToken(signedJWTInfo);
            return validationInfo;
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
                    RSAPublicKey rsaPublicKey = tokenIssuer.getPublicKey();
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

    // validate the signature
    private boolean validateSignature(String jwtToken, String signature) {
        JWSHeader header;
        JWTClaimsSet payload = null;
        SignedJWT parsedJWTToken;
        boolean isVerified = false;
        try {
            if (enableCache != null && enableCache.equals("true")) {
                if (gatewayApiKeyCache.get(signature) != JWTConstants.UNAVAILABLE) {
                    logger.debug("Api Key retrieved from the Api Key cache.");
                    isVerified = true;
                } else if (invalidGatewayApiKeyCache.get(signature) != JWTConstants.UNAVAILABLE) {
                    logger.debug("Api Key retrieved from the invalid Api Key cache.");
                    isVerified = false;
                } else {
                    logger.debug("Token is not available in the cache.");
                    try {
                        parsedJWTToken = (SignedJWT) JWTParser.parse(jwtToken);
                        isVerified = verifyTokenSignature(parsedJWTToken);
                        if (isVerified) {
                            gatewayApiKeyCache.put(signature, JWTConstants.VALID);
                        } else {
                            invalidGatewayApiKeyCache.put(signature, JWTConstants.INVALID);
                        }
                    } catch (ParseException e) {
                        logger.error("Invalid JWT token. Failed to decode the token.", e);
                    }
                }
            } else {
                try {
                    parsedJWTToken = (SignedJWT) JWTParser.parse(jwtToken);
                    isVerified = verifyTokenSignature(parsedJWTToken);
                } catch (ParseException e) {
                    logger.error("Invalid JWT token. Failed to decode the token.", e);
                }
            }
        } catch (Exception e) {
            //TODO: Remove catching the exception class.
            logger.error(e);
        }
        return isVerified;
    }

    private boolean verifyTokenSignature(SignedJWT parsedJWTToken) {
        boolean state = false;
        if (publicKey == null) {
            publicKey = readPublicKey();
        }
        if (publicKey != null) {
            JWSAlgorithm algorithm = parsedJWTToken.getHeader().getAlgorithm();
            if (algorithm != null && (JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.RS512.equals(algorithm)
                    || JWSAlgorithm.RS384.equals(algorithm))) {
                try {
                    state = parsedJWTToken.verify(jwsVerifier);
                } catch (JOSEException e) {
                    logger.error(e);
                }
            }
        }
        return state;
    }

    private RSAPublicKey readPublicKey() {
        try {
            String strKeyPEM = "";
            String fileName = "wso2carbon.pem";
            InputStream inputStream = JWTValidator.class.getClassLoader().getResourceAsStream(fileName);
            InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(streamReader);
            String line;
            while ((line = br.readLine()) != null) {
                strKeyPEM += line + "\n";
            }
            br.close();
            strKeyPEM = strKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
            strKeyPEM = strKeyPEM.replaceAll(System.lineSeparator(), "");
            strKeyPEM = strKeyPEM.replace("-----END PUBLIC KEY-----", "");
            byte[] encoded = Base64.getDecoder().decode(strKeyPEM);
            KeyFactory kf = KeyFactory.getInstance(JWTConstants.RSA);
            publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error(e);
        }
        return publicKey;
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
