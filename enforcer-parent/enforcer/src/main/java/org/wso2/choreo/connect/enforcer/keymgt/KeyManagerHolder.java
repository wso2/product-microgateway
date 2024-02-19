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
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.apimgt.common.gateway.dto.ClaimMappingDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWKSConfigurationDTO;
import org.wso2.choreo.connect.discovery.keymgt.KeyManagerConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ExtendedTokenIssuerDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.discovery.KeyManagerDiscoveryClient;
import org.wso2.choreo.connect.enforcer.dto.IDPEnvironmentDTO;
import org.wso2.choreo.connect.enforcer.util.TLSUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KeyManager holder class for Micro gateway.
 */
public class KeyManagerHolder {

    private static final Logger logger = LogManager.getLogger(KeyManagerHolder.class);

    private static final String X509 = "X.509";
    private static final KeyManagerHolder INSTANCE = new KeyManagerHolder();
    private Map<String, Map<String, ExtendedTokenIssuerDto>> tokenIssuerMap = new ConcurrentHashMap<>();

    private KeyManagerHolder() {}

    public static KeyManagerHolder getInstance() {
        return INSTANCE;
    }

    public void init() {
        KeyManagerDiscoveryClient keyManagerDs = KeyManagerDiscoveryClient.getInstance();
        keyManagerDs.watchKeyManagers();
    }

    public void populateKMIssuerConfiguration(List<KeyManagerConfig> kmIssuers) {
        updateIssuerMap(getAllKmIssuers(kmIssuers));
    }

    private Map<String,  Map<String, ExtendedTokenIssuerDto>> getAllKmIssuers(List<KeyManagerConfig> kmIssuers) {
        Map<String,  Map<String, ExtendedTokenIssuerDto>> kmIssuerMap = new ConcurrentHashMap<>();
        for (KeyManagerConfig keyManagerConfig : kmIssuers) {
            if (!keyManagerConfig.getEnabled()) {
                continue;
            }

            JSONObject configObj = new JSONObject(keyManagerConfig.getConfiguration());
            Map<String, Object> configuration = new HashMap<>();
            Iterator<String> keysItr = configObj.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = configObj.get(key);
                configuration.put(key, value);
            }

            addKMTokenIssuers(keyManagerConfig.getName(), keyManagerConfig.getOrganization(),
                configuration, kmIssuerMap);
        }
        return kmIssuerMap;
    }


    private void addKMTokenIssuers(String keyManagerName, String organization, Map<String, Object> configuration,
                                   Map<String, Map<String, ExtendedTokenIssuerDto>> kmIssuerMap) {
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
                                    e.getMessage());
                        }
                    }
                }

                if (configuration.containsKey(APIConstants.KeyManager.ENVIRONMENTS)) {
                    Object environmentsObject =
                            configuration.get(APIConstants.KeyManager.ENVIRONMENTS);
                    // If environments field is available no values are assigned means that IDP is not allowed
                    // for any environment.
                    if (environmentsObject instanceof JSONArray) {
                        IDPEnvironmentDTO[] environments = null;
                        try {
                            Gson gson = new Gson();
                            environments = gson.fromJson(environmentsObject.toString(),
                                    IDPEnvironmentDTO[].class);
                        } catch (JsonSyntaxException e) {
                            logger.error("Error while parsing environments for issuer " + issuer +
                                    ". Error cause: " + e.getMessage());
                        }
                        if (environments != null) {
                            Set<String> allowedAPIMEnvironments = new HashSet<>();
                            for (IDPEnvironmentDTO environment : environments) {
                                if (environment != null && environment.getApim() != null) {
                                    allowedAPIMEnvironments.addAll(Arrays.asList(environment.getApim()));
                                }
                            }
                            tokenIssuerDto.setEnvironments(allowedAPIMEnvironments);
                        }
                    }
                }
                if (!kmIssuerMap.containsKey(organization)) {
                    kmIssuerMap.put(organization, new ConcurrentHashMap<>());
                }
                Map<String, ExtendedTokenIssuerDto> orgSpecificKMIssuerMap = kmIssuerMap.get(organization);
                orgSpecificKMIssuerMap.put(tokenIssuerDto.getIssuer(), tokenIssuerDto);
            }
        }
    }

    public void updateIssuerMap(Map<String, Map<String, ExtendedTokenIssuerDto>> kmIssuerMap) {
        ArrayList<ExtendedTokenIssuerDto> configIssuerList = ConfigHolder.getInstance().getConfigIssuerList();
        Map<String, Map<String, ExtendedTokenIssuerDto>> tokenIssuerMap = new ConcurrentHashMap<>(kmIssuerMap);
        for (ExtendedTokenIssuerDto configTokenIssuer : configIssuerList) {
            if (!tokenIssuerMap.containsKey(APIConstants.SUPER_TENANT_DOMAIN_NAME)) {
                tokenIssuerMap.put(APIConstants.SUPER_TENANT_DOMAIN_NAME, new ConcurrentHashMap<>());
            }
            Map<String, ExtendedTokenIssuerDto> kmIssuerMapForCarbonSuper =
                    tokenIssuerMap.get(APIConstants.SUPER_TENANT_DOMAIN_NAME);
            if (!kmIssuerMapForCarbonSuper.containsKey(configTokenIssuer.getIssuer())) {
                //add issuer from config if they are not presenting at external km response
                kmIssuerMapForCarbonSuper.put(configTokenIssuer.getIssuer(), configTokenIssuer);
            } else {
                logger.debug("token issuer " + configTokenIssuer.getIssuer() + " already exists in config map. " +
                        "Existing configurations will be replaced by external KeyManager configurations");
            }
        }
        this.tokenIssuerMap = tokenIssuerMap;
    }

    public Map<String, Map<String, ExtendedTokenIssuerDto>> getTokenIssuerMap() {
        return this.tokenIssuerMap;
    }

    public ExtendedTokenIssuerDto getTokenIssuerDTO(String organizationUUID, String issuer) {
        Map<String, Map<String, ExtendedTokenIssuerDto>> tokenIssuerMap = getTokenIssuerMap();
        if (tokenIssuerMap.containsKey(organizationUUID)) {
            Map<String, ExtendedTokenIssuerDto> orgSpecificKMIssuerMap = tokenIssuerMap.get(organizationUUID);
            if (orgSpecificKMIssuerMap.containsKey(issuer)) {
                return orgSpecificKMIssuerMap.get(issuer);
            }
        }
        if (tokenIssuerMap.containsKey(APIConstants.SUPER_TENANT_DOMAIN_NAME)) {
            Map<String, ExtendedTokenIssuerDto> orgSpecificKMIssuerMap =
                    tokenIssuerMap.get(APIConstants.SUPER_TENANT_DOMAIN_NAME);
            if (orgSpecificKMIssuerMap.containsKey(issuer)) {
                String residentKMName = orgSpecificKMIssuerMap.get(issuer).getName();
                if (APIConstants.KeyManager.DEFAULT_KEY_MANAGER.equals(residentKMName)) {
                    return orgSpecificKMIssuerMap.get(issuer);
                }
            }
        }
        return null;
    }
}
