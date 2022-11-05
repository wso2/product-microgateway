/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.backend.jwt;

import org.junit.Assert;
import org.junit.Test;
import org.wso2.choreo.connect.discovery.config.enforcer.Config;
import org.wso2.choreo.connect.discovery.config.enforcer.JWTGenerator;
import org.wso2.choreo.connect.discovery.config.enforcer.Keypair;
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

    @Test
    public void DefaultConfig() {
        String certPath = resourcePath + keyStore + File.separator + keyStoreSingleCert + File.separator + cert;
        ConfigHolder configHolder = ConfigHolder.getInstance();
        JWTGenerator jwtGenerator = JWTGenerator.newBuilder()
                .setEnable(true)
                .addKeypairs(Keypair.newBuilder()
                        .setPublicCertificatePath(certPath)
                        .setUseForSigning(true)
                        .buildPartial())
        .build();
        ConfigHolder.load(Config.newBuilder().setJwtGenerator(jwtGenerator).buildPartial());
        Assert.assertTrue(configHolder.getConfig().getJwtConfigurationDto().isEnabled());
        Assert.assertEquals("Failed to generate single JWKS", 1, configHolder.getConfig()
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
                .addKeypairs(Keypair.newBuilder()
                        .setPublicCertificatePath(certPath)
                        .setUseForSigning(true)
                        .buildPartial())
                .addKeypairs(Keypair.newBuilder()
                        .setPublicCertificatePath(additionalCertPath)
                        .setUseForSigning(true)
                        .buildPartial())
                .build();
        ConfigHolder.load(Config.newBuilder().setJwtGenerator(jwtGenerator).buildPartial());
        Assert.assertTrue(configHolder.getConfig().getJwtConfigurationDto().isEnabled());
        Assert.assertEquals("Failed to generate multiple JWKS", 2, configHolder.getConfig()
                .getBackendJWKSDto().getJwks().getKeys().size());
    }
}
