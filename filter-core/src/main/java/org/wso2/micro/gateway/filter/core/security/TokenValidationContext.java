/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.filter.core.security;

import org.wso2.micro.gateway.filter.core.dto.APIKeyValidationInfoDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set if data to be used in order to validate a token.
 */
public class TokenValidationContext {

    private String context;
    private String version;
    private String accessToken;
    private String requiredAuthenticationLevel;
    private String clientDomain;
    private String matchingResource;
    private String httpVerb;
    private Map<String, Object> attributeMap = new HashMap<String, Object>();
    private String cacheKey;
    private APIKeyValidationInfoDTO validationInfoDTO;
    private boolean isCacheHit;
    private AccessTokenInfo tokenInfo;
    private String authorizationCode;
    private String tenantDomain;
    private List<String> keyManagers = new ArrayList<>();

    public AccessTokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public void setTokenInfo(AccessTokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }

    public boolean isCacheHit() {
        return isCacheHit;
    }

    public void setCacheHit(boolean isCacheHit) {
        this.isCacheHit = isCacheHit;
    }

    public APIKeyValidationInfoDTO getValidationInfoDTO() {
        return validationInfoDTO;
    }

    public void setValidationInfoDTO(APIKeyValidationInfoDTO validationInfoDTO) {
        this.validationInfoDTO = validationInfoDTO;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRequiredAuthenticationLevel() {
        return requiredAuthenticationLevel;
    }

    public void setRequiredAuthenticationLevel(String requiredAuthenticationLevel) {
        this.requiredAuthenticationLevel = requiredAuthenticationLevel;
    }

    public String getClientDomain() {
        return clientDomain;
    }

    public void setClientDomain(String clientDomain) {
        this.clientDomain = clientDomain;
    }

    public String getMatchingResource() {
        return matchingResource;
    }

    public void setMatchingResource(String matchingResource) {
        this.matchingResource = matchingResource;
    }

    public String getHttpVerb() {
        return httpVerb;
    }

    public void setHttpVerb(String httpVerb) {
        this.httpVerb = httpVerb;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public void setAttribute(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributeMap.get(key);
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }

    public List<String> getKeyManagers() {

        return keyManagers;
    }

    public void setKeyManagers(List<String> keyManagers) {

        this.keyManagers = keyManagers;
    }
}

