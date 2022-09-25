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

package org.wso2.choreo.connect.tests.testcases.withapim.graphql;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GraphQLThrottlingTestCase extends GraphQLBaseTest {

    private static final Logger log = LoggerFactory.getLogger(GraphQLThrottlingTestCase.class);
    private final String THROTTLED_IP = "10.100.7.25";
    private final String THROTTLED_HEADER = "cc_integration";
    private final String THROTTLED_QUERY_PARAM = "name";
    private final String THROTTLED_QUERY_PARAM_VALUE = "admin";
    private final String THROTTLED_CLAIM = "ClaimAppForGQL";
    private final String conditionalPolicyName = "GQLAPIPolicyWithConditionLimit";

    private static final String API_NAME = "GraphQLThrottlingAPI";
    private static final String API_CONTEXT = "gqlThrottling";
    private static final String API_VERSION = "1.0.0";
    private static final String APPLICATION_NAME = "GraphQLThrottlingApp";
    private final String API_POLICY_NAME = "GQLAPIPolicyWithDefaultLimit";
    private static final String GQL_QUERY = "{\"variables\":null,\"query\":\"{hero{name}}\"}";
    private final Map<String, String> requestHeaders = new HashMap<>();
    private final long limit5Req = 5L;
    private final long limit10Req = 10L;
    private final long limitNoThrottle = 20L;
    private final long limit1000Req = 1000L;
    private String apiId;
    private String applicationId;
    private String claimApplicationId;
    private String oAuthAppId;
    private String endpointURL;
    private String apiPolicyId;
    private String conditionalPolicyId;
    private String mainAccessToken;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
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
                .createAdvancedThrottlePolicyDTO(API_POLICY_NAME, "", "", false, defaultLimit,
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
                        Utils.createConditionalGroups(limitForConditions, THROTTLED_IP, THROTTLED_HEADER,
                                THROTTLED_QUERY_PARAM, THROTTLED_QUERY_PARAM_VALUE, THROTTLED_CLAIM));
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
        mainAccessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + mainAccessToken);

        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/" + API_VERSION);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");
    }

    @Test(description = "Test API level throttling with default limits")
    public void testGraphQLAPILevelThrottling() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(API_POLICY_NAME);
        APIDTO updatedAPI = publisherRestClient.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), API_POLICY_NAME, "API tier not updated.");

        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Couldn't wait until the API was deployed in Choreo Connect");
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        Assert.assertTrue(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, GQL_QUERY, limit5Req),
                "Request not throttled by request count condition in api tier");
        Utils.delay(40000, "Could not wait until the throttle decision expired");
    }

    @Test(description = "Test Advance throttling for operation", dependsOnMethods = {"testGraphQLAPILevelThrottling"})
    public void testGraphQResourceLevelThrottling() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);

        apidto.getOperations().forEach(op -> {
            if (op.getTarget().equalsIgnoreCase("hero")) {
                op.setThrottlingPolicy(API_POLICY_NAME);
            } else {
                op.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);
            }
        });
       publisherRestClient.updateAPI(apidto, apiId);

        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Couldn't wait until the API was deployed in Choreo Connect");

        String graphqlOAUTHAppName = "GraphQLOauthAPP";
        oAuthAppId= createGraphqlAppAndSubscribeToAPI(apiId, graphqlOAUTHAppName, ApplicationDTO.TokenTypeEnum.OAUTH);

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = StoreUtils.generateKeysForApp(oAuthAppId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, storeRestClient);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        requestHeaders.remove(HttpHeaders.AUTHORIZATION);
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        Assert.assertTrue(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, GQL_QUERY, limit5Req),
                "Request not throttled by request count condition in api tier");
    }

    @Test(description = "Test Advance throttling with IP Condition", dependsOnMethods =
            {"testGraphQResourceLevelThrottling"})
    public void testGraphQThrottlingForMultipleOperations() throws Exception {
        Assert.assertTrue(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, "{\"query\": \"query MyQuery" +
                        " {hero {name}\\n  address {planet village}\\n}\\n\", \"variables\": null, " +
                        "\"operationName\": \"MyQuery\" }", limit5Req),
                "Request not throttled by request count condition in resource level tier");

        // replace with original token so that rest of the test cases will use initial token
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + mainAccessToken);
    }

    @Test(description = "Test Advance throttling with IP Condition",
            dependsOnMethods = {"testGraphQThrottlingForMultipleOperations"})
    public void testGraphQLAPILevelThrottlingWithIpCondition() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apidto.getOperations().forEach(op -> {
            op.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        });
        APIDTO updatedAPI = publisherRestClient.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), APIMIntegrationConstants.API_TIER.UNLIMITED,
                "API tier not updated.");
        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(10000, "Couldn't wait until the API was deployed in Choreo Connect");
        Assert.assertFalse(isGQLAPIRequestsThrottled(endpointURL, requestHeaders,GQL_QUERY, limitNoThrottle),
                "Request was throttled unexpectedly in Unlimited API tier");

        apidto.setApiThrottlingPolicy(conditionalPolicyName);
        updatedAPI = publisherRestClient.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");
        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(10000, "Couldn't wait until the API was deployed in Choreo Connect");
        requestHeaders.put(HttpHeaders.X_FORWARDED_FOR, "192.100.1.24");
        Assert.assertFalse(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, GQL_QUERY, limit10Req),
                "Request shouldn't throttle for an IP not in a condition");

        requestHeaders.put(HttpHeaders.X_FORWARDED_FOR, THROTTLED_IP);
        Assert.assertTrue(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, GQL_QUERY, limit10Req),
                "Request not throttled by request count IP condition in API tier");
        requestHeaders.remove(HttpHeaders.X_FORWARDED_FOR);
    }


    @Test(description = "Test Advance throttling with Header Condition",
            dependsOnMethods = {"testGraphQLAPILevelThrottlingWithIpCondition"})
    public void testGraphQLAPILevelThrottlingWithHeaderCondition() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");

        requestHeaders.put(HttpHeaders.USER_AGENT, "http_client");
        Assert.assertFalse(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, GQL_QUERY, limit10Req),
                "Request shouldn't throttle for a host not in a condition");

        requestHeaders.put(HttpHeaders.USER_AGENT, THROTTLED_HEADER);
        Assert.assertTrue(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, GQL_QUERY, limit10Req),
                "Request not throttled by request count header condition in API tier");
        requestHeaders.remove(HttpHeaders.USER_AGENT);
    }

    @Test(description = "Test Advance throttling with jwt claim Condition",
            dependsOnMethods = {"testGraphQLAPILevelThrottlingWithHeaderCondition"})
    public void testGraphQLAPILevelThrottlingWithJWTClaimCondition() throws Exception {
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
        Assert.assertTrue(isGQLAPIRequestsThrottled(endpointURL, requestHeaders, GQL_QUERY, limit10Req),
                "Request not throttled by request count jwt claim condition in API tier");
        // replace with original token so that rest of the test cases will use initial token
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + origToken);
    }

    public static boolean isGQLAPIRequestsThrottled(String endpointURL, Map<String, String> headers, String query,
                                                    long expectedCount) throws InterruptedException, IOException, URISyntaxException {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResponseAvailable(endpointURL, headers));

        // This buffer is to avoid failures due to delays in evaluating throttle conditions at TM
        // here it sets the final throttle request count twice as the limit set in the policy.
        // it will make sure throttle will happen even if the throttle window passed.
        long throttleBuffer = expectedCount + 2;

        URIBuilder url = new URIBuilder(endpointURL);
        HttpResponse response;
        boolean isThrottled = false;
        for (int j = 0; j <= expectedCount + throttleBuffer; j++) {

            response = HTTPSClientUtils.doPost(url.toString(), headers, query);
            log.info("============== GQL throttling test running -> response: {}, data: {}", response.getResponseCode(),
                    response.getData());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            if (response.getResponseCode() != 200) {
                break;
            }
            Thread.sleep(1000);
        }
        return isThrottled;
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        StoreUtils.removeAllSubscriptionsForAnApp(claimApplicationId, storeRestClient);
        StoreUtils.removeAllSubscriptionsForAnApp(oAuthAppId, storeRestClient);
        storeRestClient.removeApplicationById(applicationId);
        storeRestClient.removeApplicationById(claimApplicationId);
        storeRestClient.removeApplicationById(oAuthAppId);
        publisherRestClient.deleteAPI(apiId);
        adminRestClient.deleteAdvancedThrottlingPolicy(apiPolicyId);
        adminRestClient.deleteAdvancedThrottlingPolicy(conditionalPolicyId);
    }
}
