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

package org.wso2.micro.gateway.enforcer.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
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

/**
 * Utility Functions related to TLS Certificates.
 */
public class TLSUtils {
    private static final Logger log = LogManager.getLogger(TLSUtils.class);
    private static final String X509 = "X.509";
    private static final String crtExtension = ".crt";
    private static final String pemExtension = ".pem";

    /**
     * Read the certificate file and return the certificate.
     *
     * @param filePath Filepath of the corresponding certificate
     * @return Certificate
     */
    public static Certificate getCertificateFromFile(String filePath)
            throws CertificateException, IOException {
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
     * Add the certificates to the the truststore.
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
                updateTruststoreWithMutlipleCertPem(trustStore, path.toAbsolutePath().toString());
            });
        } else {
            log.debug("Provided Path is a regular File Path : " + filePath);
            updateTruststoreWithMutlipleCertPem(trustStore, filePath);
        }
    }

    private static List<Certificate> getCertsFromFile(String filepath, boolean restrictToOne)
            throws CertificateException, IOException {
        try (FileInputStream fileInputStream = new FileInputStream(filepath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            List<Certificate> certList = new ArrayList<>();
            CertificateFactory cf = CertificateFactory.getInstance(X509);
            int count = 0;
            while (bufferedInputStream.available() > 0) {
                if (count > 1 && restrictToOne) {
                    log.warn("Provided PEM file contains more than one certificate. Hence proceeding with" +
                            "the first certificate in the File");
                    return certList;
                }
                Certificate cert = cf.generateCertificate(bufferedInputStream);
                certList.add(cert);
                count++;
            }
            return certList;
        }
    }

    private static void updateTruststoreWithMutlipleCertPem (KeyStore trustStore, String filePath) {
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
        } catch (CertificateException | IOException e) {
            log.error("Error while adding certificates to the truststore.", e);
        }
    }

    public static Certificate getCertificate() throws CertificateException, IOException {
        Certificate certificate = null;
        CertificateFactory fact = CertificateFactory.getInstance(X509);
        FileInputStream is = new FileInputStream(ConfigHolder.getInstance().getConfig().
                getPublicCertificatePath());
        X509Certificate cert = (X509Certificate) fact.generateCertificate(is);
        certificate = (Certificate) cert;
        return certificate;
    }
}
