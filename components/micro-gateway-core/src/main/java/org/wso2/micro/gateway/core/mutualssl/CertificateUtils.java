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
import java.net.URLDecoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateFactory;

import javax.security.cert.CertificateEncodingException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

/**
 * This class is responsible for do certificate level functionalities.
 */
public class CertificateUtils {
    private static final Logger log = LogManager.getLogger(CertificateUtils.class);

    public static String getAliasFromHeaderCert(String base64EncodedCertificate) {
        try {
            base64EncodedCertificate = URLDecoder.decode(base64EncodedCertificate).
                    replaceAll(Constants.BEGIN_CERTIFICATE_STRING, "").replaceAll(Constants.END_CERTIFICATE_STRING, "");
            byte[] bytes = Base64.decodeBase64(base64EncodedCertificate);
            InputStream inputStream = new ByteArrayInputStream(bytes);
            X509Certificate x509Certificate = X509Certificate.getInstance(inputStream);
            if (getAliasFromTrustStore(x509Certificate, LoadKeyStore.trustStore) != null) {
                return getAliasFromTrustStore(x509Certificate, LoadKeyStore.trustStore);
            }
            return "";
        } catch (KeyStoreException | java.security.cert.CertificateException | CertificateException e) {
            String msg = "Error while decoding certificate present in the header and validating with the trust store.";
            log.error(msg, e);
            throw ErrorUtils.getBallerinaError(msg, e);
        }
    }

    /**
     *  Used to get the certificate alias for a certificate which is get from header send by payload.
     */
    public static String getAliasFromTrustStore(X509Certificate certificate, KeyStore truststore) throws
            java.security.cert.CertificateException, CertificateEncodingException, KeyStoreException {
        KeyStore trustStore = truststore;
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certificateEncoded = certificate.getEncoded();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(certificateEncoded);
        java.security.cert.X509Certificate x509Certificate =
                (java.security.cert.X509Certificate) cf.generateCertificate(byteArrayInputStream);
        x509Certificate.checkValidity();
        String certificateAlias = trustStore.getCertificateAlias(x509Certificate);
        return certificateAlias;
    }

    /**
     * Used to get the certificate alias for a certificate which is get from the Request .
     */
    public static String getAliasFromRequest(String certB64) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(certB64);
            java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) CertificateFactory
                    .getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
            String certificateAlias = LoadKeyStore.trustStore.getCertificateAlias(cert);
            if (certificateAlias != null) {
                return certificateAlias;
            }
            return "";
        } catch (java.security.cert.CertificateException | KeyStoreException e) {
            String msg = "Error while decoding certificate present in the context and validating with the trust store.";
            log.error(msg, e);
            throw ErrorUtils.getBallerinaError(msg, e);
        }
    }
}
