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

package org.wso2.micro.gateway.filter.core.config;

import com.moandjiezana.toml.Toml;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.constants.ConfigConstants;
import org.wso2.micro.gateway.filter.core.dto.EventHubConfigurationDto;
import org.wso2.micro.gateway.filter.core.dto.JWKSConfigurationDTO;
import org.wso2.micro.gateway.filter.core.dto.TokenIssuerDto;
import org.wso2.micro.gateway.filter.core.exception.MGWException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration holder class for Microgateway
 */
public class MGWConfiguration {

    private static final Logger logger = LogManager.getLogger(MGWConfiguration.class);

    private static MGWConfiguration mgwConfiguration;
    private static Toml configToml;
    private static Map<String, TokenIssuerDto> issuersMap;
    private static EventHubConfigurationDto eventHubConfiguration;
    private static KeyStore trustStore = null;

    public MGWConfiguration() throws MGWException {
        try {
            init();
        } catch (KeyStoreException e) {
            throw new MGWException("Error while loading configuration from file", e);
        }
    }

    public static MGWConfiguration getInstance() throws MGWException {
        if (mgwConfiguration != null) {
            return mgwConfiguration;
        }
        mgwConfiguration = new MGWConfiguration();
        return mgwConfiguration;
    }


    /**
     * Initialize the configuration provider class by reading the Mgw Configuration file.
     */
    private void init() throws KeyStoreException {
        InputStream configFile = MGWConfiguration.class.getClassLoader().getResourceAsStream("mgw-config.toml");
        configToml = new Toml().read(configFile);

        //Load Client Trust Store
        loadTrustStore();

        // Read jwt token configuration
        populateJWTIssuerConfiguration();

        // Set Event Hub related configuration.
        populateEventHubConfiguration();
    }

    private void populateJWTIssuerConfiguration() throws KeyStoreException {
        issuersMap = new HashMap<>();
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

            issuerDto.setConsumerKeyClaim((String) issuer.get(ConfigConstants.JWT_TOKEN_CONSUMER_KEY_CLAIM));
            issuerDto.setValidateSubscriptions((boolean) issuer.get(ConfigConstants.JWT_TOKEN_VALIDATE_SUBSCRIPTIONS));
            issuersMap.put((String) issuer.get(ConfigConstants.JWT_TOKEN_ISSUER), issuerDto);
        }
    }

    private void populateEventHubConfiguration() {
        eventHubConfiguration = new EventHubConfigurationDto();
        eventHubConfiguration.setEnabled(configToml.getBoolean(ConfigConstants.EVENT_HUB_ENABLE));
        eventHubConfiguration.setServiceUrl(configToml.getString(ConfigConstants.EVENT_HUB_SERVICE_URL));
        eventHubConfiguration.setUsername(configToml.getString(ConfigConstants.EVENT_HUB_USERNAME));
        eventHubConfiguration.setPassword(configToml.getString(ConfigConstants.EVENT_HUB_PASSWORD).toCharArray());
        EventHubConfigurationDto.EventHubReceiverConfiguration receiverConfiguration =
                new EventHubConfigurationDto.EventHubReceiverConfiguration();
        Properties properties = new Properties();
        properties.setProperty(ConfigConstants.EVENT_HUB_EVENT_LISTENING_ENDPOINT,
                configToml.getString(ConfigConstants.EVENT_HUB_EVENT_LISTENING_ENDPOINT));
        receiverConfiguration.setJmsConnectionParameters(properties);
        eventHubConfiguration.setEventHubReceiverConfiguration(receiverConfiguration);
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

    /**
     * Get the issuer configuration for the provided issuer. Returns null if the config not found.
     * @return : JWTIssuerConfig object.
     */
    public Map<String, TokenIssuerDto> getJWTIssuers() {
        return issuersMap;
    }

    public EventHubConfigurationDto getEventHubConfiguration() {
        return eventHubConfiguration;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }


}
