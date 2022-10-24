package org.wso2.choreo.connect.enforcer.backend.jwt;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wso2.choreo.connect.discovery.config.enforcer.Config;
import org.wso2.choreo.connect.discovery.config.enforcer.JWTGenerator;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;

import java.io.File;

public class BackendJwksTest {

    private final String resourcePath = BackendJwksTest.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();
    private final String keyStore = "keystore";
    private final String keyStoreSingleCert = "singleCert";
    private final String keyStoreMultiCert = "multipleCerts";
    private final String cert = "cert.pem";
    private final String additionalCert = "mg.pem";


    @BeforeClass
    public static void testStart() {

    }

    @Test
    public void DefaultConfig() {

        String certPath = resourcePath + keyStore + File.separator + keyStoreSingleCert + File.separator + cert;
        ConfigHolder configHolder = ConfigHolder.getInstance();
        JWTGenerator jwtGenerator = JWTGenerator.newBuilder()
                .setEnable(true)
                .setPublicCertificatePath(certPath)
                .setJwksEnabled(true)
                .build();
        ConfigHolder.load(Config.newBuilder().setJwtGenerator(jwtGenerator).buildPartial());


        Assert.assertTrue(configHolder.getConfig().getBackendJWKSDto().isEnabled());

        Assert.assertEquals("Number of JWK's", 1, configHolder.getConfig()
                .getBackendJWKSDto().getJwks().getKeys().size());


    }

    @Test
    public void AdditionalCerts() {
        String certPath = resourcePath + keyStore + File.separator + keyStoreSingleCert + File.separator + cert;
        String additionalCertPath = resourcePath + keyStore + File.separator
                + keyStoreMultiCert + File.separator + additionalCert;
        ConfigHolder configHolder = ConfigHolder.getInstance();
        JWTGenerator jwtGenerator = JWTGenerator.newBuilder()
                .setEnable(true)
                .setPublicCertificatePath(certPath)
                .setJwksEnabled(true)
                .addAdditionalJwksCertPaths(additionalCertPath)
                .build();
        ConfigHolder.load(Config.newBuilder().setJwtGenerator(jwtGenerator).buildPartial());


        Assert.assertTrue(configHolder.getConfig().getBackendJWKSDto().isEnabled());

        Assert.assertEquals("Number of JWK's", 2, configHolder.getConfig()
                .getBackendJWKSDto().getJwks().getKeys().size());
    }


}
