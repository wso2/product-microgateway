// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.wso2.micro.gateway.core.mutualssl;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateFactory;

import javax.security.cert.CertificateEncodingException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import static org.wso2.micro.gateway.core.Constants.BEGIN_CERTIFICATE_STRING;
import static org.wso2.micro.gateway.core.Constants.END_CERTIFICATE_STRING;

/**
 * This class is responsible for get the alias from the certificate  in mutual SSL handshake,
 * when the header send by the load balancer.
 */
public class MutualsslWithLoadBalancerHeader {

    public static String getAliasAFromHeaderCert(String base64EncodedCertificate) throws  KeyStoreException,
            java.security.cert.CertificateException, CertificateException {
        base64EncodedCertificate = URLDecoder.decode(base64EncodedCertificate).
                replaceAll(BEGIN_CERTIFICATE_STRING, "")
                .replaceAll(END_CERTIFICATE_STRING, "");
        byte[] bytes = Base64.decodeBase64(base64EncodedCertificate);
        InputStream inputStream = new ByteArrayInputStream(bytes);
        X509Certificate x509Certificate = X509Certificate.getInstance(inputStream);
        if (getAliasFromTrustStore(x509Certificate, LoadKeyStore.trustStore) != null) {
            return getAliasFromTrustStore(x509Certificate, LoadKeyStore.trustStore);
        } else {
            return "";
        }
    }

    /**
     *  Used to get the certificate alias for a certificate exist in the trustore.
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
}
