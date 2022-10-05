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

package org.wso2.choreo.connect.enforcer.util;

import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.constants.JwtConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.security.jwt.SignedJWTInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Utility functions used for jwt authentication.
 */
public class JWTUtils {
    private static final Logger log = LogManager.getLogger(JWTUtils.class);

    /**
     * This method used to retrieve JWKS keys from endpoint.
     *
     * @param jwksEndpoint jwksEndpoint
     * @return JwksKeys
     * @throws IOException Exception while invoking the JWKS endpoint
     */
    public static String retrieveJWKSConfiguration(String jwksEndpoint) throws IOException {

        URL url = new URL(jwksEndpoint);
        try (CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol())) {
            HttpGet httpGet = new HttpGet(jwksEndpoint);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream content = entity.getContent()) {
                        return IOUtils.toString(content, Charset.defaultCharset());
                    }
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Verify the JWT token signature.
     *
     * @param jwt       SignedJwt Token
     * @param publicKey public certificate
     * @return whether the signature is verified or or not
     */
    public static boolean verifyTokenSignature(SignedJWT jwt, RSAPublicKey publicKey) {

        JWSAlgorithm algorithm = jwt.getHeader().getAlgorithm();
        if ((JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.RS512.equals(algorithm) || JWSAlgorithm.RS384
                .equals(algorithm))) {
            try {
                JWSVerifier jwsVerifier = new RSASSAVerifier(publicKey);
                return jwt.verify(jwsVerifier);
            } catch (JOSEException e) {
                log.error("Error while verifying JWT signature", e);
                return false;
            }
        } else {
            log.error("Public key is not a RSA");
            return false;
        }
    }

    /**
     * Verify the JWT token signature.
     *
     * @param jwt   SignedJwt Token
     * @param alias public certificate alias
     * @return whether the signature is verified or not
     * @throws EnforcerException in case of signature verification failure
     */
    public static boolean verifyTokenSignature(SignedJWT jwt, String alias) throws EnforcerException {

        Certificate publicCert;
        try {
            if (ConfigHolder.getInstance().getTrustStoreForJWT().containsAlias(alias)) {
                publicCert = ConfigHolder.getInstance().getTrustStoreForJWT().getCertificate(alias);
            } else {
                throw new EnforcerException("Could not find the certificate for the token service.");
            }
        } catch (KeyStoreException e) {
            throw new EnforcerException("Error while retrieving the certificate for JWT verification.", e);
        }

        if (publicCert != null) {
            JWSAlgorithm algorithm = jwt.getHeader().getAlgorithm();
            if ((JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.RS512.equals(algorithm) || JWSAlgorithm.RS384
                    .equals(algorithm))) {
                return verifyTokenSignature(jwt, (RSAPublicKey) publicCert.getPublicKey());
            } else {
                log.error("Public key is not RSA");
                throw new EnforcerException("Public key is not RSA");
            }
        } else {
            log.error("Couldn't find a public certificate to verify the signature");
            throw new EnforcerException("Couldn't find a public certificate to verify the signature");
        }
    }

    public static PrivateKey getPrivateKey(String filePath) throws EnforcerException {
        PrivateKey privateKey;
        try {
            String strKeyPEM;
            Path keyPath = Paths.get(filePath);
            String key = Files.readString(keyPath, Charset.defaultCharset());

            strKeyPEM = key
                    .replace(Constants.BEGINING_OF_PRIVATE_KEY, "")
                    .replaceAll("\n", "").replaceAll("\r", "") // certs could be created in a Unix/Windows platform
                    .replace(Constants.END_OF_PRIVATE_KEY, "");

            byte[] encoded = Base64.getDecoder().decode(strKeyPEM);
            KeyFactory kf = KeyFactory.getInstance(Constants.RSA);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            privateKey = rsaPrivateKey;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.debug("Error obtaining private key", e);
            throw new EnforcerException("Error obtaining private key");
        }
        return privateKey;
    }

    /**
     * Get the internal representation of the JWT.
     *
     * @param accessToken the raw access token
     * @return the internal representation of the JWT
     * @throws ParseException if an error occurs when decoding the JWT
     */
    public static SignedJWTInfo getSignedJwt(String accessToken) throws ParseException {
        String signature = accessToken.split("\\.")[2];
        SignedJWTInfo signedJWTInfo = null;
        //Check whether GatewaySignedJWTParseCache is correct
        LoadingCache gatewaySignedJWTParseCache = CacheProvider.getGatewaySignedJWTParseCache();
        if (gatewaySignedJWTParseCache != null) {
            Object cachedEntry = gatewaySignedJWTParseCache.getIfPresent(signature);
            if (cachedEntry != null) {
                signedJWTInfo = (SignedJWTInfo) cachedEntry;
            }
            if (signedJWTInfo == null  || !signedJWTInfo.getToken().equals(accessToken)) {
                SignedJWT signedJWT = SignedJWT.parse(accessToken);
                JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                signedJWTInfo = new SignedJWTInfo(accessToken, signedJWT, jwtClaimsSet);
                gatewaySignedJWTParseCache.put(signature, signedJWTInfo);
            }
        } else {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            signedJWTInfo = new SignedJWTInfo(accessToken, signedJWT, jwtClaimsSet);
        }
        return signedJWTInfo;
    }

    /**
     * Check if the JWT token is expired.
     *
     * @param token the JWT token
     * @return true if expired
     */
    public static boolean isExpired(String token) {
        String[] splitToken = token.split("\\.");
        org.json.JSONObject payload = new org.json.JSONObject(new String(Base64.getUrlDecoder().
                decode(splitToken[1])));
        long exp = payload.getLong(JwtConstants.EXP);
        long timestampSkew = FilterUtils.getTimeStampSkewInSeconds();
        return (exp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < timestampSkew);
    }

    /**
     * Populate an empty JWT info DTO for anonymous, with no App or API info.
     *
     * @param apiKeyValidationInfoDTO empty JWT info DTO to be populated with anonymous details
     * @param kmReference             name of the token service
     */
    public static void updateApplicationNameForSubscriptionDisabledKM(APIKeyValidationInfoDTO apiKeyValidationInfoDTO,
                                                                      String kmReference) {
        String applicationRef = APIConstants.ANONYMOUS_PREFIX + kmReference;
        apiKeyValidationInfoDTO.setApplicationName(applicationRef);
        apiKeyValidationInfoDTO.setApplicationId(-1);
        apiKeyValidationInfoDTO.setApplicationUUID(
                UUID.nameUUIDFromBytes(
                        applicationRef.getBytes(StandardCharsets.UTF_8)).toString());
        apiKeyValidationInfoDTO.setApplicationTier(APIConstants.UNLIMITED_TIER);
    }
}
