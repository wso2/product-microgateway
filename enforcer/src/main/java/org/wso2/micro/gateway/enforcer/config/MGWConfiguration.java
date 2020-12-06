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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.config.enforcer.Config;
import org.wso2.gateway.discovery.config.enforcer.EventHub;
import org.wso2.gateway.discovery.config.enforcer.Issuer;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.discovery.ConfigDiscoveryClient;
import org.wso2.micro.gateway.enforcer.dto.EventHubConfigurationDto;
import org.wso2.micro.gateway.enforcer.dto.JWKSConfigurationDTO;
import org.wso2.micro.gateway.enforcer.dto.TokenIssuerDto;
import org.wso2.micro.gateway.enforcer.exception.DiscoveryException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration holder class for Microgateway.
 */
public class MGWConfiguration {

    private static final Logger logger = LogManager.getLogger(MGWConfiguration.class);

    private static MGWConfiguration mgwConfiguration;
    private static Config config;
    private static Map<String, TokenIssuerDto> issuersMap;
    private static EventHubConfigurationDto eventHubConfiguration;
    private static KeyStore trustStore = null;

    private MGWConfiguration() throws MGWException {
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
    private void init() throws KeyStoreException, DiscoveryException {
        // TODO: praminda load the server details from init config
        ConfigDiscoveryClient cds = new ConfigDiscoveryClient("adapter", 18000);
        config = cds.requestInitConfig();
        //Load Client Trust Store
        loadTrustStore();

        // Read jwt token configuration
        populateJWTIssuerConfiguration();

        // Set Event Hub related configuration.
        populateEventHubConfiguration();
    }

    private void populateJWTIssuerConfiguration() throws KeyStoreException {
        issuersMap = new HashMap<>();
        List<Issuer> jwtIssuers = config.getJwtTokenConfigList();
        for (Issuer issuer : jwtIssuers) {
            TokenIssuerDto issuerDto = new TokenIssuerDto(issuer.getIssuer());

            JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
            jwksConfigurationDTO.setEnabled(StringUtils.isNotEmpty(issuer.getJwksURL()));
            jwksConfigurationDTO.setUrl(issuer.getJwksURL());
            issuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);

            String certificateAlias = issuer.getCertificateAlias();
            if (trustStore.getCertificate(certificateAlias) != null) {
                Certificate issuerCertificate = trustStore.getCertificate(certificateAlias);
                issuerDto.setCertificate(issuerCertificate);
            }

            issuerDto.setName(issuer.getIssuer());
            issuerDto.setConsumerKeyClaim(issuer.getConsumerKeyClaim());
            issuerDto.setValidateSubscriptions(issuer.getValidateSubscription());
            issuersMap.put(issuer.getIssuer(), issuerDto);
        }
    }

    private void populateEventHubConfiguration() {
        EventHub eventHub = config.getEventhub();
        eventHubConfiguration = new EventHubConfigurationDto();
        eventHubConfiguration.setEnabled(eventHub.getEnabled());
        eventHubConfiguration.setServiceUrl(eventHub.getServiceUrl());
        eventHubConfiguration.setUsername(eventHub.getUsername());
        eventHubConfiguration.setPassword(eventHub.getPassword().toCharArray());
        EventHubConfigurationDto.EventHubReceiverConfiguration receiverConfiguration =
                new EventHubConfigurationDto.EventHubReceiverConfiguration();
        Properties properties = new Properties();
        properties.setProperty(Constants.EVENT_HUB_EVENT_LISTENING_ENDPOINT, eventHub.getListenerEndpoint());
        receiverConfiguration.setJmsConnectionParameters(properties);
        eventHubConfiguration.setEventHubReceiverConfiguration(receiverConfiguration);
    }

    private void loadTrustStore() {
        String trustStoreLocation = config.getTruststore().getLocation();
        String trustStorePassword = config.getTruststore().getPassword();
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
