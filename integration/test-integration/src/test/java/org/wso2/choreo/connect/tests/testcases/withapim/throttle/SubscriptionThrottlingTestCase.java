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
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SubscriptionThrottlingTestCase extends ThrottlingBaseTestCase {
    private static final String APPLICATION_NAME = "SubscriptionThrottlingApp";

    private SubscriptionThrottlePolicyDTO requestCountPolicyDTO;
    private final Map<String, String> requestHeaders = new HashMap<>();
    String endpointURL;
    long requestCount = 15L;
    String apiId;
    String applicationId;
    String subscriptionId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // create the application throttling policy DTO with request count limit
        String policyName = "15PerMin";
        String policyTimeUnit = "min";
        Integer policyUnitTime = 1;
        String policyDispName = "15PerMin";
        String policyDesc = "This is a test subscription throttle policy";
        RequestCountLimitDTO reqCountLimit =
                DtoFactory.createRequestCountLimitDTO(policyTimeUnit, policyUnitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, reqCountLimit, null);
        requestCountPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, policyDispName, policyDesc, false, defaultLimit,-1,
                        -1, 100, "min", new ArrayList<>(),
                        true, "", 0);

        // Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                adminRestClient.addSubscriptionThrottlingPolicy(requestCountPolicyDTO);
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        requestCountPolicyDTO = addedPolicy.getData();

        // Get App ID
        applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);

        // Create access token
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, "application/json");

        // Cannot create before defining the policy in this class. Policy name must be included in api.
        apiId = createThrottleApi(policyName, TestConstant.API_TIER.UNLIMITED,
                TestConstant.API_TIER.UNLIMITED);
        // get a predefined api request
        endpointURL = getThrottleAPIEndpoint();

        subscriptionId = StoreUtils.subscribeToAPI(apiId, applicationId, policyName, storeRestClient);
        // this is to wait until policy deployment is complete in case it didn't complete already
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till the policy and API");
    }

    @Test(description = "Test Subscription throttling")
    public void testSubscriptionLevelThrottling() throws Exception {
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, requestCount),
                "Request not throttled by request count condition in subscription tier");
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        storeRestClient.removeApplicationById(applicationId);
        publisherRestClient.deleteAPI(apiId);
        adminRestClient.deleteSubscriptionThrottlingPolicy(requestCountPolicyDTO.getPolicyId());
    }
}
