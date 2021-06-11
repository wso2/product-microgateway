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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.withapim.throttle;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ConditionalGroupDTO;
import org.wso2.am.integration.clients.admin.api.dto.HeaderConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.IPConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.JWTClaimsConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.QueryParameterConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class AdvanceThrottlingTestCase extends ThrottlingBaseTestCase {
    private final String THROTTLED_IP = "10.100.1.22";
    private final String THROTTLED_HEADER = "cc_integration";
    private final String THROTTLED_QUERY_PARAM = "name";
    private final String THROTTLED_QUERY_PARAM_VALUE = "admin";
    private final String THROTTLED_CLAIM = "ClaimApp";
    private static final String API_NAME = "AdvancedThrottlingApi";
    private static final String API_CONTEXT = "advanced_throttling";
    private static final String APPLICATION_NAME = "AdvanceThrottlingApp";
    private final Map<String, String> requestHeaders = new HashMap<>();
    private final String apiPolicyName = "APIPolicyWithDefaultLimit";
    private final String conditionalPolicyName = "APIPolicyWithConditionLimit";
    private final long limit5Req = 5L;
    private final long limit10Req = 10L;
    private final long limitNoThrottle = 20L;
    private final long limit1000Req = 1000L;
    private String apiId;
    private String applicationId;
    private String claimApplicationId;
    private String endpointURL;
    private String apiPolicyId;
    private String conditionalPolicyId;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        //Since this is the first apim testcase wait till all resources are deployed
        Utils.delay(30000, "Interrupted while waiting for DELETE and" +
                " CREATE events to be deployed");

        super.initWithSuperTenant();
        RequestCountLimitDTO threePerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, limit5Req);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, threePerMin, null);
        RequestCountLimitDTO thousandPerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, limit1000Req);
        ThrottleLimitDTO defaultLimitForConditions =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, thousandPerMin, null);
        RequestCountLimitDTO tenPerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, limit10Req);
        ThrottleLimitDTO limitForConditions =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, tenPerMin, null);

        // create the advanced throttling policy with no conditions
        AdvancedThrottlePolicyDTO apiPolicyDto = DtoFactory
                .createAdvancedThrottlePolicyDTO(apiPolicyName, "", "", false, defaultLimit,
                        new ArrayList<>());
        ApiResponse<AdvancedThrottlePolicyDTO> addedApiPolicy =
                adminRestClient.addAdvancedThrottlingPolicy(apiPolicyDto);

        // assert the status code and policy ID
        Assert.assertEquals(addedApiPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedApiPolicyDto = addedApiPolicy.getData();
        apiPolicyId = addedApiPolicyDto.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        // create the advanced throttling policy with conditions
        AdvancedThrottlePolicyDTO conditionPolicyDto = DtoFactory
                .createAdvancedThrottlePolicyDTO(conditionalPolicyName, "", "", false, defaultLimitForConditions,
                        createConditionalGroups(limitForConditions));
        ApiResponse<AdvancedThrottlePolicyDTO> addedConditionalPolicy =
                adminRestClient.addAdvancedThrottlingPolicy(conditionPolicyDto);

        // assert the status code and policy ID
        Assert.assertEquals(addedConditionalPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedConditionPolicyDto = addedConditionalPolicy.getData();
        conditionalPolicyId = addedConditionPolicyDto.getPolicyId();
        Assert.assertNotNull(conditionalPolicyId, "The policy ID cannot be null or empty");

        // Get App ID and API ID
        applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        // Create access token
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    @Test(description = "Test API level throttling with default limits")
    public void testAPILevelThrottling() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(apiPolicyName);
        APIDTO updatedAPI = publisherRestClient.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), apiPolicyName, "API tier not updated.");

        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Couldn't wait until the API was deployed in Choreo Connect");
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, limit5Req),
                "Request not throttled by request count condition in api tier");
        Utils.delay(40000, "Could not wait until the throttle decision expired");
    }

    @Test(description = "Test Advance throttling with IP Condition", dependsOnMethods = {"testAPILevelThrottling"})
    public void testAPILevelThrottlingWithIpCondition() throws Exception {
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

        apidto.setApiThrottlingPolicy(conditionalPolicyName);
        updatedAPI = publisherRestClient.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");
        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(10000, "Couldn't wait until the API was deployed in Choreo Connect");
        requestHeaders.put(HttpHeaders.X_FORWARDED_FOR, "192.100.1.24");
        Assert.assertFalse(isThrottled(endpointURL, requestHeaders, null, limit10Req),
                "Request shouldn't throttle for an IP not in a condition");

        requestHeaders.put(HttpHeaders.X_FORWARDED_FOR, THROTTLED_IP);
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, limit10Req),
                "Request not throttled by request count IP condition in API tier");
        requestHeaders.remove(HttpHeaders.X_FORWARDED_FOR);
    }

    @Test(description = "Test Advance throttling with Header Condition",
            dependsOnMethods = {"testAPILevelThrottlingWithIpCondition"})
    public void testAPILevelThrottlingWithHeaderCondition() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");

        requestHeaders.put(HttpHeaders.USER_AGENT, "http_client");
        Assert.assertFalse(isThrottled(endpointURL, requestHeaders, null, limit10Req),
                "Request shouldn't throttle for a host not in a condition");

        requestHeaders.put(HttpHeaders.USER_AGENT, THROTTLED_HEADER);
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, limit10Req),
                "Request not throttled by request count header condition in API tier");
        requestHeaders.remove(HttpHeaders.USER_AGENT);
    }

    @Test(description = "Test Advance throttling with query param Condition",
            dependsOnMethods = {"testAPILevelThrottlingWithHeaderCondition"})
    public void testAPILevelThrottlingWithQueryCondition() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), conditionalPolicyName, "API tier not updated.");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(THROTTLED_QUERY_PARAM, "foo");
        Assert.assertFalse(isThrottled(endpointURL, requestHeaders, queryParams, limit10Req),
                "Request shouldn't throttle for a query param not in a condition");
        queryParams.put(THROTTLED_QUERY_PARAM, THROTTLED_QUERY_PARAM_VALUE);
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, queryParams, limit10Req),
                "Request not throttled by request count query parameter condition in API tier");
    }

    @Test(description = "Test Advance throttling with jwt claim Condition",
            dependsOnMethods = {"testAPILevelThrottlingWithQueryCondition"})
    public void testAPILevelThrottlingWithJWTClaimCondition() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");

        Application app = new Application(THROTTLED_CLAIM, TestConstant.APPLICATION_TIER.UNLIMITED);
        AppWithConsumerKey appCreationResponse = StoreUtils.createApplicationWithKeys(app, storeRestClient);
        claimApplicationId = appCreationResponse.getApplicationId();
        StoreUtils.subscribeToAPI(apiId, appCreationResponse.getApplicationId(),
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted when waiting for the " +
                "subscription to be deployed");
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appCreationResponse.getConsumerKey(), appCreationResponse.getConsumerSecret(),
                new String[]{}, user, storeRestClient);

        String origToken = requestHeaders.get(HttpHeaders.AUTHORIZATION);
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, limit10Req),
                "Request not throttled by request count jwt claim condition in API tier");
        // replace with original token so that rest of the test cases will use initial token
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + origToken);
    }

    /**
     * Creates a set of conditional groups with a list of conditions
     *
     * @param limit Throttle limit of the conditional group.
     * @return Created list of conditional group DTO
     */
    private List<ConditionalGroupDTO> createConditionalGroups(ThrottleLimitDTO limit) {
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();

        // create an IP condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> ipGrp = new ArrayList<>();
        IPConditionDTO ipConditionDTO = DtoFactory.createIPConditionDTO(IPConditionDTO.IpConditionTypeEnum.IPSPECIFIC,
                THROTTLED_IP, null, null);
        ThrottleConditionDTO ipCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.IPCONDITION, false, null, ipConditionDTO,
                        null, null);
        ipGrp.add(ipCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "IP conditional group", ipGrp, limit));

        // create a header condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> headerGrp = new ArrayList<>();
        HeaderConditionDTO headerConditionDTO =
                DtoFactory.createHeaderConditionDTO(HttpHeaders.USER_AGENT.toLowerCase(Locale.ROOT), THROTTLED_HEADER);
        ThrottleConditionDTO headerCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.HEADERCONDITION, false, headerConditionDTO,
                        null, null, null);
        headerGrp.add(headerCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Header conditional group", headerGrp, limit));

        // create a query parameter condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> queryGrp = new ArrayList<>();
        QueryParameterConditionDTO queryParameterConditionDTO =
                DtoFactory.createQueryParameterConditionDTO(THROTTLED_QUERY_PARAM, THROTTLED_QUERY_PARAM_VALUE);
        ThrottleConditionDTO queryParameterCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.QUERYPARAMETERCONDITION, false, null, null,
                        null, queryParameterConditionDTO);
        queryGrp.add(queryParameterCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Query param conditional group", queryGrp, limit));

        // create a JWT claims condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> claimGrp = new ArrayList<>();
        String claimUrl = "http://wso2.org/claims/applicationname";
        JWTClaimsConditionDTO jwtClaimsConditionDTO =
                DtoFactory.createJWTClaimsConditionDTO(claimUrl, THROTTLED_CLAIM);
        ThrottleConditionDTO jwtClaimsCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.JWTCLAIMSCONDITION, false, null, null,
                        jwtClaimsConditionDTO, null);
        claimGrp.add(jwtClaimsCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "JWT Claim conditional group", claimGrp, limit));

        return conditionalGroups;
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        StoreUtils.removeAllSubscriptionsForAnApp(claimApplicationId, storeRestClient);
        storeRestClient.removeApplicationById(applicationId);
        publisherRestClient.deleteAPI(apiId);
        adminRestClient.deleteAdvancedThrottlingPolicy(apiPolicyId);
        adminRestClient.deleteAdvancedThrottlingPolicy(conditionalPolicyId);
    }
}
