package org.wso2.micro.gateway.core.mutualssl;

import java.io.ByteArrayInputStream;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * This class is for getting the certificate alias for a certificate to validate against per API.
 */
public class MutualsslWithoutLoadBalancerHeader {
    public static String getAlias(String certB64) throws CertificateException, KeyStoreException {
        byte[] decoded = Base64.getDecoder().decode(certB64);
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(decoded));
        String certificateAlias = LoadKeyStore.trustStore.getCertificateAlias(cert);
        if (certificateAlias != null) {
            return certificateAlias;
        } else {
            return "";
        }
    }

}
