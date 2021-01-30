package org.wso2.micro.gateway.enforcer.security.jwt.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.exception.MGWException;
import org.wso2.micro.gateway.enforcer.util.FilterUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class JWTGenerator {
    private static final Logger log = LogManager.getLogger(JWTGenerator.class);

    public static Certificate getPublicCert() {
        Certificate publicCert = null;
        try {
            String certificateAlias = APIConstants.GATEWAY_PUBLIC_CERTIFICATE_ALIAS;
            publicCert = FilterUtils.getCertificateFromTrustStore(certificateAlias);
            return publicCert;
        } catch (Exception e) {
            log.error("Error in obtaining keystore",e);
        }
        return publicCert;
    }

    public static PrivateKey getPrivateKey() {
        PrivateKey privateKey = null;
        try {
            String strKeyPEM = "";
            BufferedReader br = new BufferedReader(new FileReader("/home/wso2/mg/security/localhost.key"));
            String line;
            while ((line = br.readLine()) != null) {
                strKeyPEM += line + "\n";
            }
            br.close();
            log.debug("private key>>>>>>>>>>");
            log.debug(strKeyPEM);
            strKeyPEM = strKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
            strKeyPEM = strKeyPEM.replaceAll(System.lineSeparator(), "");
            strKeyPEM = strKeyPEM.replace("-----END PRIVATE KEY-----", "");
            log.debug("After removing headers>>>>>>");
            log.debug(strKeyPEM);
            byte[] encoded = Base64.getDecoder().decode(strKeyPEM);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            privateKey = (PrivateKey) rsaPrivateKey;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println(e);
        }
        return privateKey;
    }


}
