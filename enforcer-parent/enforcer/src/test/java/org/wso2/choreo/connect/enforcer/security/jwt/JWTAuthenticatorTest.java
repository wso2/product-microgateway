/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.security.jwt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.config.dto.CacheDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigHolder.class})
@PowerMockIgnore("javax.management.*")
public class JWTAuthenticatorTest {

    @Test
    public void isAllowedEnvironmentForIDPTest() {
        PowerMockito.mockStatic(ConfigHolder.class);
        ConfigHolder configHolder = Mockito.mock(ConfigHolder.class);
        EnforcerConfig enforcerConfig = Mockito.mock(EnforcerConfig.class);
        CacheDto cacheDto = Mockito.mock(CacheDto.class);
        Mockito.when(cacheDto.isEnabled()).thenReturn(true);
        Mockito.when(enforcerConfig.getCacheDto()).thenReturn(cacheDto);
        JWTConfigurationDto jwtConfigurationDto = Mockito.mock(JWTConfigurationDto.class);
        Mockito.when(jwtConfigurationDto.isEnabled()).thenReturn(false);
        Mockito.when(enforcerConfig.getJwtConfigurationDto()).thenReturn(jwtConfigurationDto);
        Mockito.when(configHolder.getConfig()).thenReturn(enforcerConfig);
        Mockito.when(ConfigHolder.getInstance()).thenReturn(configHolder);

        JWTAuthenticator jwtAuthenticator = new JWTAuthenticator();
        Set<String> allowedEnvironments = new HashSet<>();

        try {
            jwtAuthenticator.isAllowedEnvironmentForIDP("dev-us-east-azure", null);
        } catch (APISecurityException e) {
            Assert.fail("Exception is not expected when the IDP assigned environments is null");
        }
        try {
            jwtAuthenticator.isAllowedEnvironmentForIDP("dev-us-east-azure", allowedEnvironments);
            Assert.fail("Expected APISecurityException");
        } catch (APISecurityException e) {
            Assert.assertEquals("The access token is not authorized to access the environment.",
                    e.getMessage());
        }
        allowedEnvironments.add("prod-us-east-azure");
        try {
            jwtAuthenticator.isAllowedEnvironmentForIDP("dev-us-east-azure", allowedEnvironments);
            Assert.fail("Expected APISecurityException when the IDP assigned environments does not contain " +
                    "the environment of the API");
        } catch (APISecurityException e) {
            Assert.assertEquals("The access token is not authorized to access the environment.",
                    e.getMessage());
        }
        allowedEnvironments.add("dev-us-east-azure");
        try {
            jwtAuthenticator.isAllowedEnvironmentForIDP("dev-us-east-azure", allowedEnvironments);
        } catch (APISecurityException e) {
            Assert.fail("Exception is not expected when the IDP assigned environments contains the environment " +
                    "of the API");
        }
    }

    @Test
    public void checkTokenEnvAgainstDeploymentTypeTest() {

        PowerMockito.mockStatic(ConfigHolder.class);
        ConfigHolder configHolder = Mockito.mock(ConfigHolder.class);
        EnforcerConfig enforcerConfig = Mockito.mock(EnforcerConfig.class);
        CacheDto cacheDto = Mockito.mock(CacheDto.class);
        Mockito.when(cacheDto.isEnabled()).thenReturn(true);
        Mockito.when(enforcerConfig.getCacheDto()).thenReturn(cacheDto);
        JWTConfigurationDto jwtConfigurationDto = Mockito.mock(JWTConfigurationDto.class);
        Mockito.when(jwtConfigurationDto.isEnabled()).thenReturn(false);
        Mockito.when(enforcerConfig.getJwtConfigurationDto()).thenReturn(jwtConfigurationDto);
        Mockito.when(configHolder.getConfig()).thenReturn(enforcerConfig);
        Mockito.when(ConfigHolder.getInstance()).thenReturn(configHolder);

        JWTAuthenticator jwtAuthenticator = new JWTAuthenticator();


        APIConfig apiConfig = new APIConfig.Builder("testAPI")
                .environmentName("env-1")
                .build();
        validate(jwtAuthenticator, "env-1", apiConfig, false);
        validate(jwtAuthenticator, "env-2", apiConfig, true);
    }

    private static void validate(JWTAuthenticator jwtAuthenticator, String keyEnv,
                                 APIConfig apiConfig, boolean isNotValid) {

        try {
            jwtAuthenticator.checkTokenEnvAgainstDeploymentEnv(keyEnv, apiConfig);
            if (isNotValid) {
                fail("JWT authenticator passed the test when it should have failed");
            }
        } catch (APISecurityException exception) {
            if (isNotValid) {
                assertEquals(APIConstants.StatusCodes.UNAUTHORIZED.getCode(), exception.getStatusCode());
                assertEquals(APISecurityConstants.API_AUTH_KEY_ENVIRONMENT_MISMATCH, exception.getErrorCode());
            } else {
                fail("Expected status code and error code are not thrown");
            }
        }
    }
}
