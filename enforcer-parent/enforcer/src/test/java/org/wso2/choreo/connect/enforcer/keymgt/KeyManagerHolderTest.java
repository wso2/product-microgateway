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

package org.wso2.choreo.connect.enforcer.keymgt;

import org.checkerframework.checker.units.qual.A;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.choreo.connect.discovery.keymgt.KeyManagerConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ExtendedTokenIssuerDto;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigHolder.class})
@PowerMockIgnore("javax.management.*")
public class KeyManagerHolderTest {

    @Test
    public void testPopulateKMIssuerConfiguration() {
        List<KeyManagerConfig> keyManagerConfigList = new ArrayList<>();
        String asgardeoConfiguration = "{\"claim_mappings\":[]," +
                "\"authorize_endpoint\":\"https://dev.api.asgardeo.io/t/malinthaa/oauth2/authorize\"," +
                "\"grant_types\":[],\"enable_oauth_app_creation\":true," +
                "\"certificate_value\":\"https://dev.api.asgardeo.io/t/malinthaa/oauth2/jwks\"," +
                "\"enable_token_generation\":true," +
                "\"issuer\":\"https://dev.api.asgardeo.io/t/malinthaa/oauth2/token\"," +
                "\"enable_map_oauth_consumer_apps\":false," +
                "\"enable_token_hash\":false,\"revoke_endpoint\":" +
                "\"https://dev.api.asgardeo.io/t/malinthaa/oauth2/revoke\"," +
                "\"self_validate_jwt\":true," +
                "\"scopes_claim\":\"scope\"," +
                "\"enable_token_encryption\":false," +
                "\"client_registration_endpoint\":" +
                "\"https://dev.api.asgardeo.io/t/malinthaa/api/server/v1\"," +
                "\"consumer_key_claim\":\"azp\",\"certificate_type\":\"JWKS\"," +
                "\"token_endpoint\":\"https://dev.api.asgardeo.io/t/malinthaa/oauth2/token\", " +
                "\"environments\":[{" +
                "                \"choreo\": \"Production\",\n" +
                "                \"apim\": [\n" +
                "                    \"Production and Sandbox\",\n" +
                "                    \"sandbox-prod\",\n" +
                "                    \"Prod-Internal\",\n" +
                "                    \"production-us-east-azure\",\n" +
                "                    \"production-sandbox-us-east-azure\",\n" +
                "                    \"production-internal-us-east-azure\"\n" +
                "                ]" +
                "            }]" +
                "}";

        KeyManagerConfig keyManagerConfig = KeyManagerConfig.newBuilder().setName("Asgardeo").setType("DIRECT")
                .setEnabled(true).setTenantDomain("carbon.super").setConfiguration(asgardeoConfiguration)
                .setOrganization("fooOrg").build();
        keyManagerConfigList.add(keyManagerConfig);

        PowerMockito.mockStatic(ConfigHolder.class);
        ConfigHolder configHolder = Mockito.mock(ConfigHolder.class);
        ExtendedTokenIssuerDto publisherIssuerDTO =
                new ExtendedTokenIssuerDto("https://localhost:9443/publisher");
        publisherIssuerDTO.setName("APIM Publisher");
        publisherIssuerDTO.setValidateSubscriptions(true);

        ExtendedTokenIssuerDto residentKMFromConfig =
                new ExtendedTokenIssuerDto("https://localhost:9443/oauth2/token");
        residentKMFromConfig.setName("Resident Key Manager");
        residentKMFromConfig.setValidateSubscriptions(true);
        ArrayList<ExtendedTokenIssuerDto> keyManagerListFromConfig = new ArrayList<>();
        keyManagerListFromConfig.add(publisherIssuerDTO);
        keyManagerListFromConfig.add(residentKMFromConfig);
        Mockito.when(configHolder.getConfigIssuerList()).thenReturn(keyManagerListFromConfig);
        Mockito.when(ConfigHolder.getInstance()).thenReturn(configHolder);

        KeyManagerHolder.getInstance().populateKMIssuerConfiguration(keyManagerConfigList);

        Assert.assertNotNull( "Token issuer map is null", KeyManagerHolder.getInstance().getTokenIssuerMap());
        Assert.assertEquals("Token issuer map size is not 2 as two orgs available", 2,
                KeyManagerHolder.getInstance().getTokenIssuerMap().size());
        Assert.assertNotNull("Token issuer map does not contain keyManager under fooOrg",
                KeyManagerHolder.getInstance().getTokenIssuerMap().get("fooOrg"));

        ExtendedTokenIssuerDto asgardeoIssuer = KeyManagerHolder.getInstance().getTokenIssuerMap().get("fooOrg")
                .get("https://dev.api.asgardeo.io/t/malinthaa/oauth2/token");
        Assert.assertNotNull(asgardeoIssuer);
        Assert.assertEquals("Asgardeo", asgardeoIssuer.getName());
        Assert.assertEquals("scope", asgardeoIssuer.getScopesClaim());
        Assert.assertEquals("azp", asgardeoIssuer.getConsumerKeyClaim());
        Assert.assertTrue(asgardeoIssuer.isValidateSubscriptions());
        Assert.assertNotNull(asgardeoIssuer.getJwksConfigurationDTO());
        Assert.assertEquals("https://dev.api.asgardeo.io/t/malinthaa/oauth2/jwks",
                asgardeoIssuer.getJwksConfigurationDTO().getUrl());
        Assert.assertTrue(asgardeoIssuer.getJwksConfigurationDTO().isEnabled());
        Assert.assertFalse(asgardeoIssuer.getEnvironments().isEmpty());
        Assert.assertEquals(6, asgardeoIssuer.getEnvironments().size());
        Assert.assertTrue(asgardeoIssuer.getEnvironments().contains("Production and Sandbox"));
        Assert.assertTrue(asgardeoIssuer.getEnvironments().contains("sandbox-prod"));
        Assert.assertTrue(asgardeoIssuer.getEnvironments().contains("Prod-Internal"));
        Assert.assertTrue(asgardeoIssuer.getEnvironments().contains("production-us-east-azure"));
        Assert.assertTrue(asgardeoIssuer.getEnvironments().contains("production-sandbox-us-east-azure"));
        Assert.assertTrue(asgardeoIssuer.getEnvironments().contains("production-internal-us-east-azure"));

        Assert.assertNotNull("Token issuer map does not contain keyManager under carbon.super",
                KeyManagerHolder.getInstance().getTokenIssuerMap().get("carbon.super"));
        ExtendedTokenIssuerDto publisherIssuer = KeyManagerHolder.getInstance().getTokenIssuerMap().get("carbon.super")
                .get("https://localhost:9443/publisher");
        Assert.assertNotNull(publisherIssuer);
        Assert.assertEquals("APIM Publisher", publisherIssuer.getName());
        Assert.assertTrue(publisherIssuer.isValidateSubscriptions());

        ExtendedTokenIssuerDto residentKeyManager = KeyManagerHolder.getInstance()
                .getTokenIssuerMap().get("carbon.super").get("https://localhost:9443/oauth2/token");
        Assert.assertNotNull(residentKeyManager);
        Assert.assertEquals("Resident Key Manager", residentKeyManager.getName());
        Assert.assertTrue(residentKeyManager.isValidateSubscriptions());
        Assert.assertFalse(residentKeyManager.getJwksConfigurationDTO().isEnabled());

        Assert.assertEquals(residentKeyManager, KeyManagerHolder.getInstance().getTokenIssuerDTO("carbon.super",
                "https://localhost:9443/oauth2/token"));
        Assert.assertEquals(publisherIssuer, KeyManagerHolder.getInstance().getTokenIssuerDTO("carbon.super",
                "https://localhost:9443/publisher"));
        Assert.assertEquals(asgardeoIssuer, KeyManagerHolder.getInstance().getTokenIssuerDTO("fooOrg",
                "https://dev.api.asgardeo.io/t/malinthaa/oauth2/token"));

        List<KeyManagerConfig> keyManagerConfigList2 = new ArrayList<>();
        String newResidentKeyManagerConfiguration = "{\"claim_mappings\":[]," +
                "\"authorize_endpoint\":\"https://localhost:9443/oauth2/authorize\"," +
                "\"grant_types\":[],\"enable_oauth_app_creation\":true," +
                "\"certificate_value\":\"https://localhost:9443/oauth2/jwks\"," +
                "\"enable_token_generation\":true," +
                "\"issuer\":\"https://localhost:9443/oauth2/token\"," +
                "\"enable_map_oauth_consumer_apps\":false," +
                "\"enable_token_hash\":false,\"revoke_endpoint\":" +
                "\"https://localhost:9443/oauth2/revoke\"," +
                "\"self_validate_jwt\":true," +
                "\"scopes_claim\":\"scope\"," +
                "\"enable_token_encryption\":false," +
                "\"client_registration_endpoint\":\"https://localhost:9443/api/server/v1\"," +
                "\"consumer_key_claim\":\"azp\",\"certificate_type\":\"JWKS\"," +
                "\"token_endpoint\":\"https://localhost:9443/oauth2/token\"}";
        KeyManagerConfig residentKMConfig = KeyManagerConfig.newBuilder().setName("Resident Key Manager")
                .setType("DIRECT").setEnabled(true).setTenantDomain("carbon.super")
                .setConfiguration(newResidentKeyManagerConfiguration)
                .setOrganization("carbon.super").build();
        keyManagerConfigList2.add(residentKMConfig);

        KeyManagerHolder.getInstance().populateKMIssuerConfiguration(keyManagerConfigList2);
        Assert.assertNotNull( "Token issuer map is null", KeyManagerHolder.getInstance().getTokenIssuerMap());
        Assert.assertEquals("Token issuer map size is not 1 as fooOrg keymanager is removed", 1,
                KeyManagerHolder.getInstance().getTokenIssuerMap().size());
        Assert.assertNull("Token issuer config under fooOrg should be deleted",
                KeyManagerHolder.getInstance().getTokenIssuerMap().get("fooOrg"));

        residentKeyManager = KeyManagerHolder.getInstance()
                .getTokenIssuerMap().get("carbon.super").get("https://localhost:9443/oauth2/token");
        Assert.assertNotNull(residentKeyManager);
        Assert.assertEquals("Resident Key Manager", residentKeyManager.getName());
        Assert.assertTrue(residentKeyManager.isValidateSubscriptions());
        Assert.assertTrue(residentKeyManager.getJwksConfigurationDTO().isEnabled());
        Assert.assertEquals("https://localhost:9443/oauth2/jwks",
                residentKeyManager.getJwksConfigurationDTO().getUrl());


        Assert.assertEquals(residentKeyManager, KeyManagerHolder.getInstance().getTokenIssuerDTO("carbon.super",
                "https://localhost:9443/oauth2/token"));
        Assert.assertEquals(publisherIssuer, KeyManagerHolder.getInstance().getTokenIssuerDTO("carbon.super",
                "https://localhost:9443/publisher"));
        Assert.assertNull(KeyManagerHolder.getInstance().getTokenIssuerDTO("fooOrg",
                "https://dev.api.asgardeo.io/t/malinthaa/oauth2/token"));
    }
}
