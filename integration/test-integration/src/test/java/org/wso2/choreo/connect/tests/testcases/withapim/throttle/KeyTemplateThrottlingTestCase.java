/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.withapim.throttle;

import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.CustomRuleDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.enforcer.security.jwt.SignedJWTInfo;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;


import java.util.HashMap;
import java.util.Map;

public class KeyTemplateThrottlingTestCase extends ThrottlingBaseTestCase{

    private final String customThrottlePolicyName = "customThrottlePolicyName";
    private static final String API_NAME = "KeyTemplateThrottlingAPI";
    private static final String API_CONTEXT = "keytemplate_throttling";
    private static final String APPLICATION_NAME = "KeyTemplateThrottlingApp";
    private final Map<String, String> requestHeaders = new HashMap<>();
    private String apiId;
    private String applicationId;
    private String endpointURL;
    private String subClaim;
    private final long limitNoThrottle = 20L;
    private final long limit5Req = 5L;
    private final static String keyTemplate = "$userId:$apiContext:$apiVersion:$customProperty.fooKey";
    private String siddhiQuery = "FROM\n" +
            "  RequestStream\n" +
            "SELECT\n" +
            "  userId,\n" +
            "  (\n" +
            "    userId == 'admin@carbon.super'\n" +
            "    and apiContext == '/keytemplate_throttling/1.0.0'\n" +
            "    and apiVersion == '1.0.0'\n" +
            "  ) AS isEligible,\n" +
            "  str: concat(\n" +
            "    'admin@carbon.super',\n" +
            "    ':',\n" +
            "    '/keytemplate_throttling/1.0.0:1.0.0',\n" +
            "    ':',\n" +
            "    'fooVal'\n" +
            "  ) as throttleKey\n" +
            "INSERT INTO\n" +
            "  EligibilityStream;\n" +
            "FROM\n" +
            "  EligibilityStream [ isEligible == true ] # throttler: timeBatch(1 min)\n" +
            "SELECT\n" +
            "  throttleKey,\n" +
            "  (count(throttleKey) >= 5) as isThrottled,\n" +
            "  expiryTimeStamp\n" +
            "group by\n" +
            "  throttleKey\n" +
            "INSERT\n" +
            "  ALL EVENTS into ResultStream;\n";

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();

        // Get App ID and API ID
        applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        // Create access token
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put(TestConstant.ENABLE_ENFORCER_CUSTOM_FILTER, TestConstant.ENABLE_ENFORCER_CUSTOM_FILTER);

        SignedJWTInfo signedJWTInfo = JWTUtils.getSignedJwt(accessToken);
        subClaim = signedJWTInfo.getJwtClaimsSet().getSubject();
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    @Test(description = "Test Key Template throttling with custom properties")
    public void testCustomPropertyThrottling() throws Exception {
        // Set all throttling limits to unlimited to throttle based on key templates only.
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apidto.getOperations().forEach(op -> op.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED));
        APIDTO updatedAPI = publisherRestClient.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), APIMIntegrationConstants.API_TIER.UNLIMITED,
                "API tier not updated.");
        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(10000, "Couldn't wait until the API was deployed in Choreo Connect");
        Assert.assertFalse(isThrottled(endpointURL, requestHeaders, null, limitNoThrottle),
                "Request was throttled unexpectedly in Unlimited API tier");

        // Create custom throttle policy using key templates
        siddhiQuery = siddhiQuery.replace("admin", subClaim);
        CustomRuleDTO customRuleDTO = DtoFactory.createCustomThrottlePolicyDTO(customThrottlePolicyName,
                "Custom throttle policy to throttle requests based on custom properties",
                true, siddhiQuery, keyTemplate);
        ApiResponse<CustomRuleDTO> addedCustomThrottlingPolicy = adminRestClient.addCustomThrottlingPolicy(customRuleDTO);

        Assert.assertEquals(addedCustomThrottlingPolicy.getData().getKeyTemplate(), keyTemplate, "Key template should be equal");
        Assert.assertEquals(addedCustomThrottlingPolicy.getData().getSiddhiQuery(), siddhiQuery, "Sidhdhi query should be equal");

        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, limit5Req),
                "Request not throttled by key template condition with custom property");

        adminRestClient.deleteCustomThrottlingPolicy(addedCustomThrottlingPolicy.getData().getPolicyId());
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        storeRestClient.removeApplicationById(applicationId);
        publisherRestClient.deleteAPI(apiId);
    }

}
