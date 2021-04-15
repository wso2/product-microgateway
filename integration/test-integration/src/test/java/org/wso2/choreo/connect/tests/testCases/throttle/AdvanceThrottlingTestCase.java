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

package org.wso2.choreo.connect.tests.testCases.throttle;

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
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class AdvanceThrottlingTestCase extends ThrottlingBaseTestCase {
    private final String THROTTLED_IP = "10.100.1.22";
    private final String THROTTLED_HEADER = "cc_integration";
    private final String THROTTLED_QUERY_PARAM = "name";
    private final String THROTTLED_QUERY_PARAM_VALUE = "admin";
    private final String THROTTLED_CLAIM = "ClaimApp";
    private final Map<String, String> requestHeaders = new HashMap<>();
    private final String apiPolicyName = "APIPolicyWithDefaultLimit";
    private final String conditionalPolicyName = "APIPolicyWithConditionLimit";
    private final long limit5Req = 5L;
    private final long limit10Req = 10L;
    private final long limitNoThrottle = 20L;
    private final long limit1000Req = 1000L;
    private String apiId;
    private String endpointURL;
    private String apiPolicyId;
    private String conditionalPolicyId;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.init();
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
                restAPIAdmin.addAdvancedThrottlingPolicy(apiPolicyDto);

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
                restAPIAdmin.addAdvancedThrottlingPolicy(conditionPolicyDto);

        // assert the status code and policy ID
        Assert.assertEquals(addedConditionalPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedConditionPolicyDto = addedConditionalPolicy.getData();
        conditionalPolicyId = addedConditionPolicyDto.getPolicyId();
        Assert.assertNotNull(conditionalPolicyId, "The policy ID cannot be null or empty");

        // creating the application
        ApplicationDTO app = new ApplicationDTO();
        app.setName("AdvanceThrottlingApp");
        app.setDescription("Test Application for AdvanceThrottling");
        app.setThrottlingPolicy(TestConstant.APPLICATION_TIER.UNLIMITED);
        app.setTokenType(ApplicationDTO.TokenTypeEnum.JWT);
        ApplicationCreationResponse appCreationResponse = createApplicationWithKeys(app, restAPIStore);
        String applicationId = appCreationResponse.getApplicationId();
        // create the request headers after generating the access token
        String accessToken = generateUserAccessToken(appCreationResponse.getConsumerKey(),
                appCreationResponse.getConsumerSecret(), new String[]{}, user, restAPIStore);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        apiId = createThrottleApi(TestConstant.API_TIER.UNLIMITED, TestConstant.API_TIER.UNLIMITED,
                TestConstant.API_TIER.UNLIMITED);
        // get a predefined api request
        endpointURL = getThrottleAPIEndpoint();

        HttpResponse subscriptionResponse = subscribeToAPI(apiId, applicationId,
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, restAPIStore);

        assertEquals(subscriptionResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to subscribe to the API");
        // this is to wait until policy deployment is complete in case it didn't complete already
        Thread.sleep(TestConstant.DEPLOYMENT_WAIT_TIME);
    }

    @Test(description = "Test API level throttling with default limits")
    public void testAPILevelThrottling() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(apiPolicyName);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), apiPolicyName, "API tier not updated.");

        // create Revision and Deploy to Gateway
        createAPIRevisionAndDeploy(apiId, restAPIPublisher);
        waitForXdsDeployment();
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, limit5Req),
                "Request not throttled by request count condition in api tier");
        Thread.sleep(40000); // wait until throttle decision expires
    }

    @Test(description = "Test Advance throttling with IP Condition", dependsOnMethods = {"testAPILevelThrottling"})
    public void testAPILevelThrottlingWithIpCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apidto.getOperations().forEach(op -> op.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED));
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), APIMIntegrationConstants.API_TIER.UNLIMITED,
                "API tier not updated.");
        // create Revision and Deploy to Gateway
        createAPIRevisionAndDeploy(apiId, restAPIPublisher);
        waitForXdsDeployment();
        Assert.assertFalse(isThrottled(endpointURL, requestHeaders, null, limitNoThrottle),
                "Request was throttled unexpectedly in Unlimited API tier");

        apidto.setApiThrottlingPolicy(conditionalPolicyName);
        updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");
        // create Revision and Deploy to Gateway
        createAPIRevisionAndDeploy(apiId, restAPIPublisher);
        waitForXdsDeployment();
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
        HttpResponse api = restAPIPublisher.getAPI(apiId);
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
        HttpResponse api = restAPIPublisher.getAPI(apiId);
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
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");

        ApplicationDTO app = new ApplicationDTO();
        app.setName(THROTTLED_CLAIM);
        app.setDescription("Test Application for JWT condition");
        app.setThrottlingPolicy(TestConstant.APPLICATION_TIER.UNLIMITED);
        app.setTokenType(ApplicationDTO.TokenTypeEnum.JWT);
        ApplicationCreationResponse appCreationResponse = createApplicationWithKeys(app, restAPIStore);
        HttpResponse subscriptionResponse = subscribeToAPI(apiId, appCreationResponse.getApplicationId(),
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, restAPIStore);
        assertEquals(subscriptionResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to subscribe to the API");
        String accessToken = generateUserAccessToken(appCreationResponse.getConsumerKey(),
                appCreationResponse.getConsumerSecret(), new String[]{}, user, restAPIStore);

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


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        restAPIAdmin.deleteAdvancedThrottlingPolicy(apiPolicyId);
        restAPIAdmin.deleteAdvancedThrottlingPolicy(conditionalPolicyId);
    }
}
