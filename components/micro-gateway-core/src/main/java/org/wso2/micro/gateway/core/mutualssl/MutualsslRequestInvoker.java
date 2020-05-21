package org.wso2.micro.gateway.core.mutualssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public  class MutualsslRequestInvoker {
    private String trustStorePath;
    private String trustStorePassword;
    public static FileInputStream localTrustStoreStream;

    private static final Logger log = LoggerFactory.getLogger("ballerina");

    public static String getAlias(String certB64, String trustStorePath, String trustStorePassword) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
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