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

package org.wso2.choreo.connect.enforcer.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayJWTGeneratorImpl;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.carbon.apimgt.common.gateway.jwttransformer.JWTTransformer;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTConstants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Utility functions used for jwt authentication.
 */
public class JWTUtil {

    private static final Logger log = LogManager.getLogger(JWTUtil.class);
    private static volatile long ttl = -1L;

    /**
     * This method used to retrieve JWKS keys from endpoint.
     *
     * @param jwksEndpoint
     * @return
     * @throws IOException
     */
    public static String retrieveJWKSConfiguration(String jwksEndpoint) throws IOException {

        URL url = new URL(jwksEndpoint);
        try (CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol())) {
            HttpGet httpGet = new HttpGet(jwksEndpoint);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream content = entity.getContent()) {
                        return IOUtils.toString(content);
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
     * @return whether the signature is verified or or not
     * @throws EnforcerException in case of signature verification failure
     */
    public static boolean verifyTokenSignature(SignedJWT jwt, String alias) throws EnforcerException {

        Certificate publicCert = null;
        //Read the client-truststore.jks into a KeyStore
        try {
            publicCert = ConfigHolder.getInstance().getTrustStoreForJWT().getCertificate(alias);
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
        PrivateKey privateKey = null;
        try {
            String strKeyPEM = "";
            Path keyPath = Paths.get(filePath);
            String key = Files.readString(keyPath, Charset.defaultCharset());
            strKeyPEM = key
                    .replace(Constants.BEGINING_OF_PRIVATE_KEY, "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace(Constants.END_OF_PRIVATE_KEY, "");

            byte[] encoded = Base64.getDecoder().decode(strKeyPEM);
            KeyFactory kf = KeyFactory.getInstance(Constants.RSA);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            privateKey = (PrivateKey) rsaPrivateKey;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.debug("Error obtaining private key", e);
            throw new EnforcerException("Error obtaining private key");
        }
        return privateKey;
    }

    public static long getTTL() {
        return ttl * 1000;
    }

    public static AbstractAPIMgtGatewayJWTGenerator getApiMgtGatewayJWTGenerator() {
        JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance().getConfig().getJwtConfigurationDto();
        String classNameInConfig = jwtConfigurationDto.getGatewayJWTGeneratorImpl();
        AbstractAPIMgtGatewayJWTGenerator jwtGenerator = null;

        // Load default jwt generator class
        if (classNameInConfig.equals(JWTConstants.DEFAULT_JWT_GENERATOR_CLASS_NAME)) {
            jwtGenerator = new APIMgtGatewayJWTGeneratorImpl();
            return jwtGenerator;
        } else {
            Class<AbstractAPIMgtGatewayJWTGenerator> clazz;
            try {
                clazz = (Class<AbstractAPIMgtGatewayJWTGenerator>) Class.forName(classNameInConfig);
                Constructor<AbstractAPIMgtGatewayJWTGenerator> constructor = clazz.getConstructor();
                jwtGenerator = constructor.newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                    | InstantiationException | InvocationTargetException | ClassCastException e) {
                log.error("Error while generating AbstractAPIMgtGatewayJWTGenerator from the class", e);
            }
        }
        return jwtGenerator;
    }

    public static Map<String, JWTTransformer> loadJWTTransformers() {
        ServiceLoader<JWTTransformer> loader = ServiceLoader.load(JWTTransformer.class);
        Iterator<JWTTransformer> classIterator = loader.iterator();
        Map<String, JWTTransformer> jwtTransformersMap = new HashMap<>();

        if (!classIterator.hasNext()) {
            log.debug("No JWTTransformers found.");
            return jwtTransformersMap;
        }

        while (classIterator.hasNext()) {
            JWTTransformer transformer = classIterator.next();
            Annotation[] annotations = transformer.getClass().getAnnotations();
            if (annotations.length == 0) {
                log.debug("JWTTransformer is discarded as no annotations found. : " +
                        transformer.getClass().getCanonicalName());
                continue;
            }
            for (Annotation annotation : annotations) {
                if (annotation instanceof JwtTransformerAnnotation) {
                    JwtTransformerAnnotation jwtTransformerAnnotation =
                            (JwtTransformerAnnotation) annotation;
                    if (jwtTransformerAnnotation.enabled()) {
                        log.debug("JWTTransformer for the issuer : " + jwtTransformerAnnotation.issuer() +
                                "is enabled.");
                        jwtTransformersMap.put(jwtTransformerAnnotation.issuer(), transformer);
                    } else {
                        log.debug("JWTTransformer for the issuer : " + jwtTransformerAnnotation.issuer() +
                                "is disabled.");
                    }
                }
            }
        }
        return jwtTransformersMap;
    }
}
