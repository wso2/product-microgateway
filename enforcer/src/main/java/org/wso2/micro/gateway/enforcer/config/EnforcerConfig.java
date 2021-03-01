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

import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.micro.gateway.enforcer.config.dto.AuthServiceConfigurationDto;
import org.wso2.micro.gateway.enforcer.config.dto.CacheDto;
import org.wso2.micro.gateway.enforcer.config.dto.CredentialDto;
import org.wso2.micro.gateway.enforcer.config.dto.EventHubConfigurationDto;
import org.wso2.micro.gateway.enforcer.config.dto.TokenIssuerDto;
import org.wso2.micro.gateway.enforcer.dto.ThrottleAgentConfigDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration holder class for Microgateway.
 */
public class EnforcerConfig {

    private AuthServiceConfigurationDto authService;
    private EventHubConfigurationDto eventHub;
    private Map<String, TokenIssuerDto> issuersMap = new HashMap<>();
    private CredentialDto apimCredentials;
    private ThrottleAgentConfigDTO throttleAgentConfig;
    private JWTConfigurationDto jwtConfigurationDto;
    private CacheDto cacheDto;
    private String publicCertificatePath = "";
    private String privateKeyPath = "";

    public AuthServiceConfigurationDto getAuthService() {
        return authService;
    }

    public void setAuthService(AuthServiceConfigurationDto authService) {
        this.authService = authService;
    }

    public EventHubConfigurationDto getEventHub() {
        return eventHub;
    }

    public void setEventHub(EventHubConfigurationDto eventHub) {
        this.eventHub = eventHub;
    }

    public Map<String, TokenIssuerDto> getIssuersMap() {
        return issuersMap;
    }

    public void setIssuersMap(Map<String, TokenIssuerDto> issuersMap) {
        this.issuersMap = issuersMap;
    }

    public CredentialDto getApimCredentials() {
        return apimCredentials;
    }

    public void setApimCredentials(CredentialDto apimCredentials) {
        this.apimCredentials = apimCredentials;
    }

    public ThrottleAgentConfigDTO getThrottleAgentConfig() {
        return throttleAgentConfig;
    }

    public void setThrottleAgentConfig(ThrottleAgentConfigDTO throttleAgentConfig) {
        this.throttleAgentConfig = throttleAgentConfig;
    }

    public void setJwtConfigurationDto(JWTConfigurationDto jwtConfigurationDto) {
        this.jwtConfigurationDto = jwtConfigurationDto;
    }

    public JWTConfigurationDto getJwtConfigurationDto() {
        return jwtConfigurationDto;
    }

    public CacheDto getCacheDto() {
        return cacheDto;
    }

    public void setCacheDto(CacheDto cacheDto) {
        this.cacheDto = cacheDto;
    }

    public void setPublicCertificatePath(String certPath) {
        this.publicCertificatePath = certPath;
    }

    public String getPublicCertificatePath() {
        return publicCertificatePath;
    }

    public void setPrivateKeyPath(String keyPath) {
        this.privateKeyPath = keyPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }
}

