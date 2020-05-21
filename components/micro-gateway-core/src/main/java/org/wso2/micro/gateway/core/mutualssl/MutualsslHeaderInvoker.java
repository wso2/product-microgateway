package org.wso2.micro.gateway.core.mutualssl;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.core.Constants;

import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;


public class MutualsslHeaderInvoker {
    private static final Logger log = LoggerFactory.getLogger("ballerina");
    public static FileInputStream localTrustStoreStream;
    public static boolean isExistCert(String base64EncodedCertificate, String trustStorePath, String trustStorePassword) throws IOException, KeyStoreException, java.security.cert.CertificateException, NoSuchAlgorithmException {
        localTrustStoreStream = new FileInputStream(getKeyStorePath(trustStorePath));
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(localTrustStoreStream, trustStorePassword.toCharArray());
        if (base64EncodedCertificate != null) {
            base64EncodedCertificate = URLDecoder.decode(base64EncodedCertificate).
                    replaceAll("-----BEGIN CERTIFICATE-----\n", "")
                    .replaceAll("-----END CERTIFICATE-----", "");

            byte[] bytes = Base64.decodeBase64(base64EncodedCertificate);
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                X509Certificate x509Certificate = X509Certificate.getInstance(inputStream);
                if (isCertificateExistsInTrustStore(x509Certificate, trustStore)) {
                    return true;
                } else {
                    log.debug("Certificate in Header didn't exist in truststore");
                    return false;
                }
            } catch (IOException | CertificateException e) {
                String msg = "Error while converting into X509Certificate";
                log.error(msg, e);
                return false;
            }


        }
        return false;
    }


    public static boolean isCertificateExistsInTrustStore(X509Certificate certificate, KeyStore truststore) {

        if (certificate != null) {
            try {
                KeyStore trustStore = truststore;
                if (trustStore != null) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    byte[] certificateEncoded = certificate.getEncoded();
                    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(certificateEncoded)) {
                        java.security.cert.X509Certificate x509Certificate =
                                (java.security.cert.X509Certificate) cf.generateCertificate(byteArrayInputStream);
                        String certificateAlias = trustStore.getCertificateAlias(x509Certificate);
                        if (certificateAlias != null) {
                            return true;
                        }
                    }
                }
            } catch (KeyStoreException | CertificateException  | IOException e) {
                String msg = "Error in validating certificate existence";
                log.error(msg, e);
            } catch (java.security.cert.CertificateException e) {
                String msg = "Error in validating certificate existence";
                log.error(msg, e);
            }
        }
        return false;
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