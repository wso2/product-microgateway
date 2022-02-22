/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.keymgt;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.apimgt.common.gateway.dto.ClaimMappingDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWKSConfigurationDTO;
import org.wso2.choreo.connect.discovery.keymgt.KeyManagerConfig;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ExtendedTokenIssuerDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.discovery.KeyManagerDiscoveryClient;
import org.wso2.choreo.connect.enforcer.util.TLSUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * KeyManager holder class for Micro gateway.
 */
public class KeyManagerHolder {

    private static final Logger logger = LogManager.getLogger(ConfigHolder.class);

    private static final String X509 = "X.509";
    private static KeyManagerHolder instance;
    private Map<String, ExtendedTokenIssuerDto> tokenIssuerMap = ConfigHolder.getInstance().getConfig().getIssuersMap();

    private KeyManagerHolder() {}

    public static KeyManagerHolder getInstance() {
        if (instance == null) {
            instance = new KeyManagerHolder();
        }
        return instance;
    }

    public void init() {
        KeyManagerDiscoveryClient keyManagerDs = KeyManagerDiscoveryClient.getInstance();
        keyManagerDs.watchKeyManagers();
    }

    public void populateKMIssuerConfiguration(List<KeyManagerConfig> kmIssuers) {
        Map<String, ExtendedTokenIssuerDto> kmIssuerMap =  getAllKmIssuers(kmIssuers);
        updateIssuerMap(kmIssuerMap);
    }

    public Map<String, ExtendedTokenIssuerDto> getAllKmIssuers(List<KeyManagerConfig> kmIssuers) {
        Map<String, ExtendedTokenIssuerDto> kmIssuerMap = new HashMap<>();
        for (KeyManagerConfig keyManagerConfig : kmIssuers) {
            JSONObject configObj = new JSONObject(keyManagerConfig.getConfiguration());
            Map<String, Object> configuration = new HashMap<>();
            Iterator<String> keysItr = configObj.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = configObj.get(key);
                configuration.put(key, value);
            }

            if (keyManagerConfig.getEnabled()) {
                addKMTokenIssuers(keyManagerConfig.getName(), configuration, kmIssuerMap);
            }
        }
        return kmIssuerMap;
    }


    public void addKMTokenIssuers(String keyManagerName, Map<String, Object> configuration,
                                  Map<String, ExtendedTokenIssuerDto> kmIssuerMap) {
        Object selfValidateJWT = configuration.get(APIConstants.KeyManager.SELF_VALIDATE_JWT);
        if (selfValidateJWT != null && (Boolean) selfValidateJWT) {
            Object issuer = configuration.get(APIConstants.KeyManager.ISSUER);
            if (issuer != null) {
                ExtendedTokenIssuerDto tokenIssuerDto = new ExtendedTokenIssuerDto((String) issuer);
                tokenIssuerDto.setName(keyManagerName);
                tokenIssuerDto.setValidateSubscriptions(true);
                Object claimMappings = configuration.get(APIConstants.KeyManager.CLAIM_MAPPING);
                if (claimMappings instanceof JSONArray) {
                    Gson gson = new Gson();
                    ClaimMappingDto[] claimMappingDto = gson.fromJson(claimMappings.toString(),
                            ClaimMappingDto[].class);
                    tokenIssuerDto.addClaimMappings(claimMappingDto);
                }
                Object consumerKeyClaim =
                        configuration.get(APIConstants.KeyManager.CONSUMER_KEY_CLAIM);
                if (consumerKeyClaim instanceof String && StringUtils.isNotEmpty((String) consumerKeyClaim)) {
                    tokenIssuerDto.setConsumerKeyClaim((String) consumerKeyClaim);
                }
                Object scopeClaim =
                        configuration.get(APIConstants.KeyManager.SCOPES_CLAIM);
                if (scopeClaim instanceof String && StringUtils.isNotEmpty((String) scopeClaim)) {
                    tokenIssuerDto.setScopesClaim((String) scopeClaim);
                }
                Object jwksEndpoint = configuration.get(APIConstants.KeyManager.JWKS_ENDPOINT);
                if (jwksEndpoint != null) {
                    if (StringUtils.isNotEmpty((String) jwksEndpoint)) {
                        JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
                        jwksConfigurationDTO.setEnabled(true);
                        jwksConfigurationDTO.setUrl((String) jwksEndpoint);
                        tokenIssuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);
                    }
                }
                Object certificateType = configuration.get(APIConstants.KeyManager.CERTIFICATE_TYPE);
                Object certificateValue = configuration.get(APIConstants.KeyManager.CERTIFICATE_VALUE);
                if (certificateType != null && StringUtils.isNotEmpty((String) certificateType) &&
                        certificateValue != null && StringUtils.isNotEmpty((String) certificateValue)) {
                    if (APIConstants.KeyManager.CERTIFICATE_TYPE_JWKS_ENDPOINT.equals(certificateType)) {
                        JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
                        jwksConfigurationDTO.setEnabled(true);
                        jwksConfigurationDTO.setUrl((String) certificateValue);
                        tokenIssuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);
                    } else {
                        try {
                            byte[] certBytes = Base64.getDecoder().decode(certificateValue.toString());
                            InputStream is = new ByteArrayInputStream(certBytes);
                            Certificate certificate = CertificateFactory.getInstance(X509)
                                    .generateCertificate(is);
                            tokenIssuerDto.setCertificate(TLSUtils.convertCertificate(certificate));
                        } catch (CertificateException e) {
                            logger.error("Error reading the certificate for issuer " + issuer + ". Error cause: " +
                                    e.getMessage(), ErrorDetails.errorLog(LoggingConstants.Severity.MAJOR, 6200));
                        }
                    }
                }
                kmIssuerMap.put(tokenIssuerDto.getIssuer(), tokenIssuerDto);
            }
        }
    }

    public void updateIssuerMap(Map<String, ExtendedTokenIssuerDto> kmIssuerMap) {
        ArrayList<ExtendedTokenIssuerDto> configIssuerList = ConfigHolder.getInstance().getConfigIssuerList();
        for (ExtendedTokenIssuerDto configTokenIssuer : configIssuerList) {
            if (!kmIssuerMap.containsKey(configTokenIssuer.getIssuer())) {
                //add issuer from config if they are not presenting at external km response
                kmIssuerMap.put(configTokenIssuer.getIssuer(), configTokenIssuer);
            } else {
                logger.warn("token issuer " + configTokenIssuer.getIssuer() + " already exists in config map. " +
                        "Existing configurations will be replaced by external KeyManager configurations");
            }
        }
        // add the updated issuer list replacing the existing one
        tokenIssuerMap.clear();
        tokenIssuerMap.putAll(kmIssuerMap);
    }
}
