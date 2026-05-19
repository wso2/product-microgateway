/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.core.mutualssl;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.core.Constants;
import org.wso2.micro.gateway.core.utils.ErrorUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This class is responsible for do certificate level functionalities.
 */
public class CertificateUtils {
    private static final Logger log = LogManager.getLogger(CertificateUtils.class);

    public static String getAliasFromHeaderCert(String base64EncodedCertificate) {
        try {
            base64EncodedCertificate = URLDecoder.decode(base64EncodedCertificate, StandardCharsets.UTF_8.name())
                    .replaceAll(Constants.BEGIN_CERTIFICATE_STRING, "")
                    .replaceAll(Constants.END_CERTIFICATE_STRING, "");
            byte[] bytes = Base64.decodeBase64(base64EncodedCertificate);
            InputStream inputStream = new ByteArrayInputStream(bytes);
            X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(inputStream);
            String alias = getAliasFromTrustStore(x509Certificate, LoadKeyStore.trustStore);
            return alias != null ? alias : "";
        } catch (KeyStoreException | CertificateException | UnsupportedEncodingException e) {
            String msg = "Error while decoding certificate present in the header and validating with the trust store.";
            log.error(msg, e);
            throw ErrorUtils.getBallerinaError(msg, e);
        }
    }

    /**
     * Gets the certificate alias for a certificate from the request header by
     * validating it against the trust store.
     *
     * @param certificate the X509 certificate from the request header
     * @param trustStore  the trust store to validate against
     * @return the certificate alias if found and valid
     * @throws CertificateException if certificate validation fails
     * @throws KeyStoreException    if trust store access fails
     */
    public static String getAliasFromTrustStore(X509Certificate certificate, KeyStore trustStore)
            throws CertificateException, KeyStoreException {
        certificate.checkValidity();
        String certificateAlias = trustStore.getCertificateAlias(certificate);
        return certificateAlias;
    }

    /**
     * Gets the certificate alias for a certificate from the request context.
     *
     * @param certB64 the Base64-encoded certificate from the request
     * @return the certificate alias if found in the trust store, empty string
     *         otherwise
     */
    public static String getAliasFromRequest(String certB64) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(certB64);
            X509Certificate cert = (X509Certificate) CertificateFactory
                    .getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
            String certificateAlias = LoadKeyStore.trustStore.getCertificateAlias(cert);
            if (certificateAlias != null) {
                return certificateAlias;
            }
            return "";
        } catch (CertificateException | KeyStoreException e) {
            String msg = "Error while decoding certificate present in the context and validating with the trust store.";
            log.error(msg, e);
            throw ErrorUtils.getBallerinaError(msg, e);
        }
    }
}
