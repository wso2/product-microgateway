/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.enforcer.config;

import com.moandjiezana.toml.Toml;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.config.dto.JWKSConfigurationDTO;
import org.wso2.micro.gateway.enforcer.config.dto.TokenIssuerDto;
import org.wso2.micro.gateway.enforcer.constants.ConfigConstants;
import org.wso2.micro.gateway.enforcer.exception.MGWException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

/**
 * Configuration holder class for Microgateway.
 */
public class ConfigHolder {

    private static final Logger logger = LogManager.getLogger(ConfigHolder.class);

    private static ConfigHolder configHolder;
    private Toml configToml;
    EnforcerConfig config;
    private KeyStore trustStore = null;

    private ConfigHolder() throws MGWException {
        try {
            init();
        } catch (KeyStoreException e) {
            throw new MGWException("Error while loading configuration from file", e);
        }
    }

    public static ConfigHolder getInstance() {
        if (configHolder != null) {
            return configHolder;
        }
        try {
            configHolder = new ConfigHolder();
        } catch (MGWException e) {
            logger.error("Error while reading configuration", e);
        }
        return configHolder;
    }


    /**
     * Initialize the configuration provider class by reading the Mgw Configuration file.
     */
    private void init() throws KeyStoreException {
        String home = System.getenv(ConfigConstants.ENFORCER_HOME);
        File file = new File(home + File.separator + ConfigConstants.CONF_DIR + File.separator + "config.toml");
        configToml = new Toml().read(file).getTable(ConfigConstants.CONF_ENFORCER_TABLE);
        config = configToml.to(EnforcerConfig.class);
        //Load Client Trust Store
        loadTrustStore();

        // Read jwt token configuration
        populateJWTIssuerConfiguration();

    }

    private void populateJWTIssuerConfiguration() throws KeyStoreException {
        List<Object> jwtIssuers = configToml.getList(ConfigConstants.JWT_TOKEN_CONFIG);
        for (Object jwtIssuer : jwtIssuers) {
            Map<String, Object> issuer = (Map<String, Object>) jwtIssuer;
            TokenIssuerDto issuerDto = new TokenIssuerDto((String) issuer.get(ConfigConstants.JWT_TOKEN_ISSUER));

            JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
            jwksConfigurationDTO.setEnabled(StringUtils.isNotEmpty(
                    (String) issuer.get(ConfigConstants.JWT_TOKEN_JWKS_URL)));
            jwksConfigurationDTO.setUrl((String) issuer.get(ConfigConstants.JWT_TOKEN_JWKS_URL));
            issuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);

            String certificateAlias = (String) issuer.get(ConfigConstants.JWT_TOKEN_CERTIFICATE_ALIAS);
            if (trustStore.getCertificate(certificateAlias) != null) {
                Certificate issuerCertificate = trustStore.getCertificate(certificateAlias);
                issuerDto.setCertificate(issuerCertificate);
            }

            issuerDto.setName((String) issuer.get(ConfigConstants.JWT_TOKEN_ISSUER_NAME));
            issuerDto.setConsumerKeyClaim((String) issuer.get(ConfigConstants.JWT_TOKEN_CONSUMER_KEY_CLAIM));
            issuerDto.setValidateSubscriptions((boolean) issuer.get(ConfigConstants.JWT_TOKEN_VALIDATE_SUBSCRIPTIONS));
            config.getIssuersMap().put((String) issuer.get(ConfigConstants.JWT_TOKEN_ISSUER), issuerDto);
        }
    }

    private void loadTrustStore() {
        String trustStoreLocation = configToml.getString(ConfigConstants.MGW_TRUST_STORE_LOCATION);
        String trustStorePassword = configToml.getString(ConfigConstants.MGW_TRUST_STORE_PASSWORD);;
        if (trustStoreLocation != null && trustStorePassword != null) {
            try {
                InputStream inputStream = new FileInputStream(new File(trustStoreLocation));
                trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(inputStream, trustStorePassword.toCharArray());
            } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
                logger.error("Error in loading trust store.", e);
            }
        } else {
            logger.error("Error in loading trust store. Configurations are not set.");
        }
    }

    public EnforcerConfig getConfig() {
        return config;
    }

    public void setConfig(EnforcerConfig config) {
        this.config = config;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }
}
