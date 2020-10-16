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
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import io.envoyproxy.envoy.service.auth.v2.CheckResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.api.RequestContext;

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
import java.util.Base64;
import java.util.Map;
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

    public JWTValidator() {
        publicKey = readPublicKey();
        jwsVerifier = new RSASSAVerifier(publicKey);
    }

    //validate JWT token
    public boolean validateToken(RequestContext requestContext) {
        boolean valid = false;
        CheckResponse response;
        Map<String, String> headers = requestContext.getHeaders();
        String token = headers.get(JWTConstants.AUTHORIZATION);
        valid = handleJWT(token);

        if (!valid) {
            requestContext.getProperties().put("code", "401");
            requestContext.getProperties().put("error_code", "900901");
            requestContext.getProperties().put("error_description", "Invalid credentials");
        }

        return valid;
    }

    //handle JWT token
    private boolean handleJWT(String accessToken) {
        String[] tokenContent = accessToken.split("\\.");
        boolean isVerified = validateSignature(accessToken, tokenContent[2]);
        return isVerified;
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
}
