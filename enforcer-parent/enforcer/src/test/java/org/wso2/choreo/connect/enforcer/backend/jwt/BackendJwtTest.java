/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.X509CertUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayJWTGeneratorImpl;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;
import org.wso2.choreo.connect.enforcer.util.TLSUtils;
import org.wso2.choreo.connect.enforcer.util.TLSUtilsTest;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

public class BackendJwtTest {
    private static final String keystore = "keystore";
    private static final String publicCert = "mg.pem";
    private static final String privateKey = "mg.key";

    private static final String certPath = BackendJwtTest.class.getProtectionDomain().getCodeSource().
            getLocation().getPath() + keystore + File.separator + publicCert;
    private static final String keyPath = BackendJwtTest.class.getProtectionDomain().getCodeSource().
            getLocation().getPath() + keystore + File.separator + privateKey;
    private static JWTConfigurationDto jwtConfig;
    private static AbstractAPIMgtGatewayJWTGenerator jwtGenerator;
    // Init JWT generator
    @BeforeClass
    public static void start() throws Exception {
        jwtConfig = getConfigDto();
        jwtGenerator = new APIMgtGatewayJWTGeneratorImpl();
        jwtGenerator.setJWTConfigurationDto((jwtConfig));
    }

    // Initialize config object to use for JWT generation
    public static JWTConfigurationDto getConfigDto() {
        JWTConfigurationDto configDto = new JWTConfigurationDto();
        try {
            configDto.setPublicCert(TLSUtils.getCertificateFromFile(certPath));
            configDto.setPrivateKey(JWTUtils.getPrivateKey(keyPath));
        } catch (EnforcerException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
        configDto.setEnabled(true);
        configDto.setEnableUserClaims(false);
        configDto.setGatewayJWTGeneratorImpl("org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayJWTGeneratorImpl");
        configDto.setTtl(3600);
        return configDto;
    }

    // Test whether backend JWT generates and validates
    @Test
    public void validateJWT() throws Exception {
        JWSObject jwsObject;
        JWTInfoDto jwtInfoDto = new JWTInfoDto();
        jwtInfoDto.setJwtValidationInfo(new JWTValidationInfo());
        String jwt = jwtGenerator.generateToken(jwtInfoDto);
        Assert.assertNotNull("JWT Generation failed",jwt);

        jwsObject = JWSObject.parse(jwt);
        Assert.assertNotNull("JWT Couldn't be parsed",jwsObject);

        X509Certificate cert = X509CertUtils.parse(jwtConfig.getPublicCert().getEncoded());
        RSAPublicKey publicKey = RSAKey.parse(cert).toRSAPublicKey();

        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        boolean verifiedSignature = false;
        verifiedSignature = jwsObject.verify(verifier);
        Assert.assertTrue("JWT failed to verify",verifiedSignature);
    }
}
