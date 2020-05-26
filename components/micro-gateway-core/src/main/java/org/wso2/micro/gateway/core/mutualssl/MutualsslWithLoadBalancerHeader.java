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
 * This class is responsible for get the alias from the certificate  in mutual SSL handshake
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
     *
     * @param certificate
     * @param truststore
     * @return
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
