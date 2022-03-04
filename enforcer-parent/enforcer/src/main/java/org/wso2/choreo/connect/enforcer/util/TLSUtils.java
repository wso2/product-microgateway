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

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

/**
 * Utility Functions related to TLS Certificates.
 */
public class TLSUtils {
    private static final Logger log = LogManager.getLogger(TLSUtils.class);
    private static final String X509 = "X.509";
    private static final String crtExtension = ".crt";
    private static final String pemExtension = ".pem";
    private static final String endCertificateDelimiter = "-----END CERTIFICATE-----";

    /**
     * Read the certificate file and return the certificate.
     *
     * @param filePath Filepath of the corresponding certificate
     * @return Certificate
     */
    public static Certificate getCertificateFromFile(String filePath)
            throws CertificateException, IOException, EnforcerException {
        return getCertsFromFile(filePath, true).get(0);
    }

    /**
     * Read the pem encoded certificate content and generate certificate.
     *
     * @param certificateContent Pem Encoded certificate Content
     * @return Certificate
     */
    public static Certificate getCertificateFromString(String certificateContent)
            throws CertificateException, IOException {
        // A single certificate file is expected
        try (InputStream inputStream = new ByteArrayInputStream(certificateContent.getBytes())) {
            CertificateFactory fact = CertificateFactory.getInstance(X509);
            return fact.generateCertificate(inputStream);
        }
    }

    /**
     * Add the certificates to the truststore.
     *
     * @param filePath   Filepath of the corresponding certificate or directory containing the certificates
     * @param trustStore Keystore with trusted certificates
     */
    public static void addCertsToTruststore(KeyStore trustStore, String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            log.error("The provided certificates directory/file path does not exist. : " + filePath);
            return;
        }
        if (Files.isDirectory(Paths.get(filePath))) {
            log.debug("Provided Path is a directory: " + filePath);
            Files.walk(Paths.get(filePath)).filter(path -> {
                Path fileName = path.getFileName();
                return fileName != null && (fileName.toString().endsWith(crtExtension) ||
                        fileName.toString().endsWith(pemExtension));
            }).forEach(path -> {
                updateTruststoreWithMultipleCertPem(trustStore, path.toAbsolutePath().toString());
            });
        } else {
            log.debug("Provided Path is a regular File Path : " + filePath);
            updateTruststoreWithMultipleCertPem(trustStore, filePath);
        }
    }

    private static List<Certificate> getCertsFromFile(String filepath, boolean restrictToOne)
            throws CertificateException, IOException, EnforcerException {
        String content = new String(Files.readAllBytes(Paths.get(filepath)));

        if (!content.contains(endCertificateDelimiter)) {
            throw new EnforcerException("Content Provided within the certificate file:" + filepath + "is invalid.");
        }

        int endIndex = content.lastIndexOf(endCertificateDelimiter) + endCertificateDelimiter.length();
        // If there are any additional characters afterwards,
        if (endIndex < content.length()) {
            content = content.substring(0, endIndex);
        }

        List<Certificate> certList = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance(X509);
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        int count = 1;
        while (bufferedInputStream.available() > 0) {
            if (count > 1 && restrictToOne) {
                    log.warn("Provided PEM file " + filepath +
                            "contains more than one certificate. Hence proceeding with" +
                            "the first certificate in the File for the JWT configuraion related certificate.");
                    return certList;
            }
            Certificate cert = cf.generateCertificate(bufferedInputStream);
            certList.add(cert);
            count++;
        }
        return certList;
    }

    private static void updateTruststoreWithMultipleCertPem (KeyStore trustStore, String filePath) {
        try {
            List<Certificate> certificateList = getCertsFromFile(filePath, false);
            certificateList.forEach(certificate -> {
                try {
                    trustStore.setCertificateEntry(RandomStringUtils.random(10, true, false),
                            certificate);
                } catch (KeyStoreException e) {
                    log.error("Error while adding the trusted certificates to the trustStore.", e);
                }
            });
            log.debug("Certificate Added to the truststore : " + filePath);
        } catch (CertificateException | IOException | EnforcerException e) {
            log.error("Error while adding certificates to the truststore.", e);
        }
    }

    public static Certificate getCertificate(String filePath) throws CertificateException, IOException {
        Certificate certificate = null;
        CertificateFactory fact = CertificateFactory.getInstance(X509);
        FileInputStream is = new FileInputStream(filePath);
        X509Certificate cert = (X509Certificate) fact.generateCertificate(is);
        certificate = (Certificate) cert;
        return certificate;
    }

    /**
     * Generate the gRPC Server SSL Context where the mutual SSL is also enabled.
     * @return {@code SsLContext} generated SSL Context
     * @throws SSLException
     */
    public static SslContext buildGRPCServerSSLContext() throws SSLException {
        File certFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPublicKeyPath()).toFile();
        File keyFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPrivateKeyPath()).toFile();

        return GrpcSslContexts.forServer(certFile, keyFile)
                .trustManager(ConfigHolder.getInstance().getTrustManagerFactory())
                .clientAuth(ClientAuth.REQUIRE)
                .build();
    }

    public static javax.security.cert.Certificate convertCertificate(Certificate cert) {
        javax.security.cert.Certificate certificate = null;
        try {
            InputStream inputStream = new ByteArrayInputStream(cert.getEncoded());
            javax.security.cert.X509Certificate x509Certificate = javax.security.cert.X509Certificate.
                    getInstance(inputStream);
            certificate = (javax.security.cert.Certificate) x509Certificate;
            return certificate;

        } catch (javax.security.cert.CertificateException | java.security.cert.CertificateEncodingException e) {
            log.debug("Error in loading certificate");
        }
        return certificate;
    }
}
