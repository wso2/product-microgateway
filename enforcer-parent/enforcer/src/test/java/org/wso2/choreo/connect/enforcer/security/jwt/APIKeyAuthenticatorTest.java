/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.choreo.connect.enforcer.security.jwt;

import com.google.common.cache.LoadingCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.config.dto.APIKeyDTO;
import org.wso2.choreo.connect.enforcer.config.dto.CacheDto;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({APIKeyUtils.class, CacheProvider.class, ConfigHolder.class})
public class APIKeyAuthenticatorTest {

    @Before
    public void setup() {
        PowerMockito.mockStatic(ConfigHolder.class);
        ConfigHolder configHolder = PowerMockito.mock(ConfigHolder.class);
        PowerMockito.when(ConfigHolder.getInstance()).thenReturn(configHolder);
        EnforcerConfig enforcerConfig = PowerMockito.mock(EnforcerConfig.class);
        PowerMockito.when(configHolder.getConfig()).thenReturn(enforcerConfig);
        APIKeyDTO apiKeyDTO = PowerMockito.mock(APIKeyDTO.class);
        PowerMockito.when(enforcerConfig.getApiKeyConfig()).thenReturn(apiKeyDTO);
        PowerMockito.when(ConfigHolder.getInstance().getConfig().getApiKeyConfig()
                .getApiKeyInternalHeader()).thenReturn("choreo-api-key");
        CacheDto cacheDto = Mockito.mock(CacheDto.class);
        Mockito.when(cacheDto.isEnabled()).thenReturn(true);
        Mockito.when(enforcerConfig.getCacheDto()).thenReturn(cacheDto);
        JWTConfigurationDto jwtConfigurationDto = Mockito.mock(JWTConfigurationDto.class);
        Mockito.when(jwtConfigurationDto.isEnabled()).thenReturn(false);
        Mockito.when(enforcerConfig.getJwtConfigurationDto()).thenReturn(jwtConfigurationDto);
    }

    @Test
    public void retrieveTokenFromRequestCtxTest_invalidKey() {

        RequestContext.Builder requestContextBuilder = new RequestContext.Builder("/api-key");
        requestContextBuilder.matchedAPI(new APIConfig.Builder("Petstore")
                .basePath("/test")
                .apiType("REST")
                .build());
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("choreo-api-key",
                "chk_eyJrZXkiOiJieTlpYXQ5d3MycDY0dWF6anFkbzQ4cnAyYnY3aWoxdWRuYmRzNzN6ZWx5OWNoZHJ2YiJ97JYpag");
        requestContextBuilder.headers(headersMap);
        RequestContext requestContext = requestContextBuilder.build();

        APIKeyAuthenticator apiKeyAuthenticator = new APIKeyAuthenticator();
        Assert.assertThrows(APISecurityException.class, () ->
                apiKeyAuthenticator.retrieveTokenFromRequestCtx(requestContext));
    }

    @Test
    public void retrieveTokenFromRequestCtxTest_cached_validKey() throws APISecurityException {

        String mockJWT = "eyJrZXkiOiJieTlpYXQ5d3MycDY0dWF6anFkbzQ4cnAyYnY3aWoxdWRuYmRzNzN6ZWx5OWNoZHJ2YiJ97JYPAg";
        PowerMockito.mockStatic(APIKeyUtils.class);
        PowerMockito.when(APIKeyUtils.isValidAPIKey(Mockito.anyString())).thenReturn(true);
        PowerMockito.when(APIKeyUtils.generateAPIKeyHash(Mockito.anyString())).thenReturn("key_hash");
        PowerMockito.when(APIKeyUtils.isJWTExpired(Mockito.anyString())).thenReturn(false);

        PowerMockito.mockStatic(CacheProvider.class);
        LoadingCache gatewayAPIKeyJWTCache = PowerMockito.mock(LoadingCache.class);
        PowerMockito.when(CacheProvider.getGatewayAPIKeyJWTCache()).thenReturn(gatewayAPIKeyJWTCache);
        PowerMockito.when(gatewayAPIKeyJWTCache.getIfPresent(Mockito.anyString())).thenReturn(mockJWT);

        RequestContext.Builder requestContextBuilder = new RequestContext.Builder("/api-key");
        requestContextBuilder.matchedAPI(new APIConfig.Builder("Petstore")
                .basePath("/test")
                .apiType("REST")
                .build());
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("choreo-api-key",
                "chk_eyJhdHRyMSI6InYxIiwiY29ubmVjdGlvbklkIjoiNjAwM2EzYjctYWYwZi00ZmIzLTg1M2UtYTY1NjJiMjM0N" +
                        "WYyIiwia2V5IjoieG5lcGVxZmZ4eWx2Y2Q4a3FnNHprZDFpMHoxMnA2dTBqcW50aDUyM3JlN292a2pudncifQBdZRRQ");
        requestContextBuilder.headers(headersMap);
        RequestContext requestContext = requestContextBuilder.build();

        APIKeyAuthenticator apiKeyAuthenticator = new APIKeyAuthenticator();
        String token = apiKeyAuthenticator.retrieveTokenFromRequestCtx(requestContext);
        Assert.assertEquals(mockJWT, token);
    }

    @Test
    public void retrieveTokenFromRequestCtxTest_validKey() throws APISecurityException {

        PowerMockito.mockStatic(APIKeyUtils.class);
        String mockJWT = "eyJrZXkiOiJieTlpYXQ5d3MycDY0dWF6anFkbzQ4cnAyYnY3aWoxdWRuYmRzNzN6ZWx5OWNoZHJ2YiJ97JYPAg";
        PowerMockito.when(APIKeyUtils.exchangeAPIKeyToJWT(Mockito.anyString())).thenReturn(Optional.of(mockJWT));
        PowerMockito.when(APIKeyUtils.isValidAPIKey(Mockito.anyString())).thenReturn(true);
        PowerMockito.when(APIKeyUtils.generateAPIKeyHash(Mockito.anyString())).thenReturn("key_hash");

        PowerMockito.mockStatic(CacheProvider.class);
        LoadingCache gatewayAPIKeyJWTCache = PowerMockito.mock(LoadingCache.class);
        PowerMockito.when(CacheProvider.getGatewayAPIKeyJWTCache()).thenReturn(gatewayAPIKeyJWTCache);
        PowerMockito.when(gatewayAPIKeyJWTCache.getIfPresent(Mockito.anyString())).thenReturn(null);

        RequestContext.Builder requestContextBuilder = new RequestContext.Builder("/api-key");
        requestContextBuilder.matchedAPI(new APIConfig.Builder("Petstore")
                .basePath("/test")
                .apiType("REST")
                .build());
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("choreo-api-key",
                "chk_eyJhdHRyMSI6InYxIiwiY29ubmVjdGlvbklkIjoiNjAwM2EzYjctYWYwZi00ZmIzLTg1M2UtYTY1NjJiMjM0N" +
                        "WYyIiwia2V5IjoieG5lcGVxZmZ4eWx2Y2Q4a3FnNHprZDFpMHoxMnA2dTBqcW50aDUyM3JlN292a2pudncifQBdZRRQ");
        requestContextBuilder.headers(headersMap);
        RequestContext requestContext = requestContextBuilder.build();

        APIKeyAuthenticator apiKeyAuthenticator = new APIKeyAuthenticator();
        String token = apiKeyAuthenticator.retrieveTokenFromRequestCtx(requestContext);
        Assert.assertEquals(mockJWT, token);
    }
}
