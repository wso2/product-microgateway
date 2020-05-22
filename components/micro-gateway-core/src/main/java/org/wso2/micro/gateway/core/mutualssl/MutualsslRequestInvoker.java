package org.wso2.micro.gateway.core.mutualssl;

import org.wso2.micro.gateway.core.Constants;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * This class is invoke for getting the certificate alias for a certificate to validate against per API.
 */
public class MutualsslRequestInvoker {
    public static FileInputStream localTrustStoreStream;

    public static String getAlias(String certB64, String trustStorePath, String trustStorePassword)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        byte[] decoded = Base64.getDecoder().decode(certB64);
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(decoded));
        localTrustStoreStream = new FileInputStream(getKeyStorePath(trustStorePath));
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(localTrustStoreStream, trustStorePassword.toCharArray());
        String certificateAlias = trustStore.getCertificateAlias(cert);
        return certificateAlias;
    }

    /**
     * Used to get the keystore path
     */
    public static String getKeyStorePath(String fullPath) {
        String homePathConst = "\\$\\{mgw-runtime.home}";
        String homePath = System.getProperty(Constants.RUNTIME_HOME_PATH);
        return fullPath.replaceAll(homePathConst, homePath);
    }
}
