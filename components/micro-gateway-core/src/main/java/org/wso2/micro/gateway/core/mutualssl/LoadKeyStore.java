package org.wso2.micro.gateway.core.mutualssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.core.Constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * This class is for load the keystore.
 */
public class LoadKeyStore {
    public static KeyStore trustStore;
    public static FileInputStream localTrustStoreStream;
    private static final Logger log = LoggerFactory.getLogger("ballerina");

    public static void loadKeyStore(String trustStorePath, String trustStorePassword) {
        try {
            localTrustStoreStream = new FileInputStream(getKeyStorePath(trustStorePath));
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(localTrustStoreStream, trustStorePassword.toCharArray());

        } catch (NoSuchAlgorithmException | IOException | KeyStoreException | CertificateException e) {
            String msg = "Error while loading the trustore";
            log.error(msg, e);
        }
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
