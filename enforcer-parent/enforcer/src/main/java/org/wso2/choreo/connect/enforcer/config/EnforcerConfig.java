/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.config;

import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.jwttransformer.JWTTransformer;
import org.wso2.choreo.connect.enforcer.config.dto.AdminRestServerDto;
import org.wso2.choreo.connect.enforcer.config.dto.AnalyticsDTO;
import org.wso2.choreo.connect.enforcer.config.dto.AuthHeaderDto;
import org.wso2.choreo.connect.enforcer.config.dto.AuthServiceConfigurationDto;
import org.wso2.choreo.connect.enforcer.config.dto.CacheDto;
import org.wso2.choreo.connect.enforcer.config.dto.CredentialDto;
import org.wso2.choreo.connect.enforcer.config.dto.FilterDTO;
import org.wso2.choreo.connect.enforcer.config.dto.JWTIssuerConfigurationDto;
import org.wso2.choreo.connect.enforcer.config.dto.ManagementCredentialsDto;
import org.wso2.choreo.connect.enforcer.config.dto.MetricsDTO;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottleConfigDto;
import org.wso2.choreo.connect.enforcer.config.dto.TracingDTO;
import org.wso2.choreo.connect.enforcer.jwks.BackendJWKSDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration holder class for Microgateway.
 */
public class EnforcerConfig {

    private AuthServiceConfigurationDto authService;
    private ThrottleConfigDto throttleConfig;
    private TracingDTO tracingConfig;
    private MetricsDTO metricsConfig;
    private JWTConfigurationDto jwtConfigurationDto;
    private CacheDto cacheDto;
    private JWTIssuerConfigurationDto jwtIssuerConfigurationDto;
    private BackendJWKSDto backendJWKSDto;
    private CredentialDto[] jwtUsersCredentials;
    private String publicCertificatePath = "";
    private String privateKeyPath = "";
    private AnalyticsDTO analyticsConfig;
    private Map<String, JWTTransformer> jwtTransformerMap = new HashMap<>();
    private AuthHeaderDto authHeader;
    private ManagementCredentialsDto management;
    private AdminRestServerDto restServer;
    private FilterDTO[] customFilters;

    public AuthServiceConfigurationDto getAuthService() {
        return authService;
    }

    public void setAuthService(AuthServiceConfigurationDto authService) {
        this.authService = authService;
    }

    public ThrottleConfigDto getThrottleConfig() {
        return throttleConfig;
    }

    public void setThrottleConfig(ThrottleConfigDto throttleConfig) {
        this.throttleConfig = throttleConfig;
    }

    public void setTracingConfig(TracingDTO tracingConfig) {
        this.tracingConfig = tracingConfig;
    }

    public TracingDTO getTracingConfig() {
        return tracingConfig;
    }

    public MetricsDTO getMetricsConfig() {
        return metricsConfig;
    }

    public void setMetricsConfig(MetricsDTO metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    public void setJwtConfigurationDto(JWTConfigurationDto jwtConfigurationDto) {
        this.jwtConfigurationDto = jwtConfigurationDto;
    }

    public void setJwtIssuerConfigurationDto(JWTIssuerConfigurationDto jwtIssuerConfigurationDto) {
        this.jwtIssuerConfigurationDto = jwtIssuerConfigurationDto;
    }

    public JWTIssuerConfigurationDto getJwtIssuerConfigurationDto() {
        return jwtIssuerConfigurationDto;
    }

    public BackendJWKSDto getBackendJWKSDto() {
        return backendJWKSDto;
    }

    public void setBackendJWKSDto(BackendJWKSDto backendJWKSDto) {
        this.backendJWKSDto = backendJWKSDto;
    }

    public void setJwtUsersCredentials(CredentialDto[] credentialDtos) {
        this.jwtUsersCredentials = credentialDtos;
    }

    public CredentialDto[] getJwtUsersCredentials() {
        return jwtUsersCredentials;
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

    public AnalyticsDTO getAnalyticsConfig() {
        return analyticsConfig;
    }

    public void setAnalyticsConfig(AnalyticsDTO analyticsConfig) {
        this.analyticsConfig = analyticsConfig;
    }

    public Map<String, JWTTransformer> getJwtTransformerMap() {
        return jwtTransformerMap;
    }

    public void setJwtTransformerMap(Map<String, JWTTransformer> jwtTransformerMap) {
        this.jwtTransformerMap = jwtTransformerMap;
    }

    public AuthHeaderDto getAuthHeader() {
        return authHeader;
    }

    public void setAuthHeader(AuthHeaderDto authHeader) {
        this.authHeader = authHeader;
    }

    public ManagementCredentialsDto getManagement() {
        return management;
    }

    public void setManagement(ManagementCredentialsDto management) {
        this.management = management;
    }

    public AdminRestServerDto getRestServer() {
        return restServer;
    }

    public void setRestServer(AdminRestServerDto restServer) {
        this.restServer = restServer;
    }

    public FilterDTO[] getCustomFilters() {
        return customFilters;
    }

    public void setCustomFilters(FilterDTO[] customFilters) {
        this.customFilters = customFilters;
    }
}

