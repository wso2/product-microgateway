/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.security.mtls;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.api.Certificate;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

/**
 * Common functions for certificates.
 */
public class MtlsUtils {
    private static final Logger log = LogManager.getLogger(MtlsUtils.class);
    public static boolean isPublicCertificate(String cert) {
        return cert.contains(APIConstants.BEGIN_CERTIFICATE_STRING) &&
                cert.contains(APIConstants.END_CERTIFICATE_STRING);
    }

    public static String getCertContent(String cert) {
        return getCertContent(cert, false);
    }
    public static String getCertContent(String cert, boolean isEncoded) {
        if (isEncoded) {
            try {
                cert = URLDecoder.decode(cert, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.debug("Provided client certificate is unable to decode.");
                throw new SecurityException(e);
            }
        }
        if (isPublicCertificate(cert)) {
            String content = cert.replaceAll(APIConstants.BEGIN_CERTIFICATE_STRING, "")
                    .replaceAll(APIConstants.END_CERTIFICATE_STRING, "");
            return content.trim();
        }
        log.debug("Provided client certificate is not a public certificate.");
        return "";
    }

    public static KeyStore createTrustStore(List<Certificate> clientCertificates) throws KeyStoreException {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try {
            trustStore.load(null, null);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            log.debug("Creating the client certificate truststore was unsuccessful.");
            throw new SecurityException(e);
        }
        for (Certificate certificate : clientCertificates) {
            X509Certificate x509cert;
            String alias = certificate.getAlias();
            String cert = certificate.getContent().toStringUtf8();
            String certContent = getCertContent(cert);
            try {
                x509cert = getX509Cert(certContent);
            } catch (CertificateException e) {
                throw new SecurityException(e);
            }
            trustStore.setCertificateEntry(alias, x509cert);
        }
        return trustStore;
    }

    public static X509Certificate getX509Cert(String certContent)
            throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] bytes = Base64.decodeBase64(certContent);
        X509Certificate x509Certificate;
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            x509Certificate =
                    (X509Certificate) cf.generateCertificate(byteArrayInputStream);
        } catch (IOException e) {
            log.error("Unable to generate x509 certificate format.");
            throw new CertificateException(e);
        }
        return x509Certificate;
    }

    public static String getMatchedCertificateAliasFromTrustStore(X509Certificate certificate, KeyStore trustStore)
            throws CertificateException {
        String alias = null;
        try {
            if (!Objects.isNull(trustStore)) {
                alias = trustStore.getCertificateAlias(certificate);
            } else {
                log.debug("The API truststore has not been initialized.");
            }
        } catch (KeyStoreException e) {
            log.debug("Error occurred while checking the API truststore.");
            throw new CertificateException(e);
        }
        return alias;
    }
}
