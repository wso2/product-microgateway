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

package org.wso2.micro.gateway.enforcer.security.jwt;

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
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.constants.JwtConstants;
import org.wso2.micro.gateway.enforcer.exception.MGWException;
import org.wso2.micro.gateway.enforcer.security.jwt.validator.JWTConstants;
import org.wso2.micro.gateway.enforcer.util.FilterUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility functions used for jwt authentication.
 */
public class JWTUtil {

    private static final Logger log = LogManager.getLogger(JWTUtil.class);
    private static volatile long ttl = -1L;

    /**
     * This method used to retrieve JWKS keys from endpoint
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
     * @throws MGWException in case of signature verification failure
     */
    public static boolean verifyTokenSignature(SignedJWT jwt, String alias) throws MGWException {

        Certificate publicCert = null;
        //Read the client-truststore.jks into a KeyStore
        try {
            publicCert = ConfigHolder.getInstance().getTrustStoreForJWT().getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new MGWException("Error while retrieving the certificate for JWT verification.", e);
        }

        if (publicCert != null) {
            JWSAlgorithm algorithm = jwt.getHeader().getAlgorithm();
            if ((JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.RS512.equals(algorithm) || JWSAlgorithm.RS384
                    .equals(algorithm))) {
                return verifyTokenSignature(jwt, (RSAPublicKey) publicCert.getPublicKey());
            } else {
                log.error("Public key is not RSA");
                throw new MGWException("Public key is not RSA");
            }
        } else {
            log.debug("Couldn't find a public certificate to verify the signature");
            throw new MGWException("Couldn't find a public certificate to verify the signature");
        }
    }

    public static PrivateKey getPrivateKey() {
        PrivateKey privateKey = null;
        try {
            String strKeyPEM = "";
            BufferedReader br = new BufferedReader(new FileReader(ConfigHolder.getInstance().getConfig().
                    getPrivateKeyPath()));
            String line;
            while ((line = br.readLine()) != null) {
                strKeyPEM += line + "\n";
            }
            br.close();
            strKeyPEM = strKeyPEM.replace(Constants.BEGINING_OF_PRIVATE_KEY, "");
            strKeyPEM = strKeyPEM.replaceAll(System.lineSeparator(), "");
            strKeyPEM = strKeyPEM.replace(Constants.END_OF_PRIVATE_KEY, "");

            byte[] encoded = Base64.getDecoder().decode(strKeyPEM);
            KeyFactory kf = KeyFactory.getInstance(Constants.RSA);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            privateKey = (PrivateKey) rsaPrivateKey;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.debug("Error obtaining private key", e);
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
            // Load custom jwt generator class
            // Get the names of jar files available in the location.
            List<String> jarFilesList = getJarFilesList();

            for (int fileIndex = 0; fileIndex < jarFilesList.size(); fileIndex++) {
                try {
                    String pathToJar = JwtConstants.DROPINS_FOLDER + jarFilesList.get(fileIndex);
                    JarFile jarFile = new JarFile(pathToJar);
                    Enumeration<JarEntry> e = jarFile.entries();

                    URL[] urls = {new URL("jar:file:" + pathToJar + "!/")};
                    URLClassLoader cl = URLClassLoader.newInstance(urls);

                    while (e.hasMoreElements()) {
                        JarEntry je = e.nextElement();
                        if (je.isDirectory() || !je.getName().endsWith(JwtConstants.CLASS)) {
                            continue;
                        }
                        // -6 because of .class
                        String className = je.getName().substring(0, je.getName().length() - 6);
                        className = className.replace('/', '.');
                        if (classNameInConfig.equals(className)) {
                            Class classInJar = cl.loadClass(className);
                            try {
                                jwtGenerator = (AbstractAPIMgtGatewayJWTGenerator) classInJar.newInstance();
                                return jwtGenerator;
                            } catch (InstantiationException | IllegalAccessException exception) {
                                log.debug("Error in generating an object from the class", exception);
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    log.debug("Error in loading class", e);
                }
            }
        }
        return jwtGenerator;
    }

    public static List<String> getJarFilesList() {
        List<String> jarFilesList = new ArrayList<String>();
        File[] files = new File(JwtConstants.DROPINS_FOLDER).listFiles();
        //If this pathname does not denote a directory, then listFiles() returns null.
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.endsWith(JwtConstants.JAR)) {
                    jarFilesList.add(file.getName());
                }
            }
        }
        return jarFilesList;
    }
}

