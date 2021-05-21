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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class ApplicationThrottlingTestCase extends ThrottlingBaseTestCase {
    private static final String API_NAME = "ApplicationThrottlingApi";
    private static final String API_CONTEXT = "application_throttling";
    private static final String APPLICATION_NAME = "ApplicationThrottlingApp";

    private ApplicationThrottlePolicyDTO requestCountPolicyDTO;
    private final Map<String, String> requestHeaders = new HashMap<>();
    String endpointURL;
    long requestCount = 15L;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // create the application throttling policy DTO with request count limit
        String policyName = "15PerMin";
        String policyTimeUnit = "min";
        Integer policyUnitTime = 1;
        String policyDispName = "15PerMin";
        String policyDesc = "This is a test application throttle policy";
        RequestCountLimitDTO reqCountLimit =
                DtoFactory.createRequestCountLimitDTO(policyTimeUnit, policyUnitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, reqCountLimit, null);
        requestCountPolicyDTO = DtoFactory
                .createApplicationThrottlePolicyDTO(policyName, policyDispName, policyDesc, false, defaultLimit);

        // Add the application throttling policy
        ApiResponse<ApplicationThrottlePolicyDTO> addedPolicy =
                adminRestClient.addApplicationThrottlingPolicy(requestCountPolicyDTO);
        requestCountPolicyDTO = addedPolicy.getData();

        // Get API ID
        String apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        // creating the application
        Application app = new Application(APPLICATION_NAME, policyName);
        String applicationId = StoreUtils.createApplication(app, storeRestClient);

        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED,
                storeRestClient);

        // Create access token
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, "application/json");

        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    @Test(description = "Test application throttling")
    public void testApplicationLevelThrottling() throws Exception {
        Assert.assertTrue(isThrottled(endpointURL, requestHeaders, null, requestCount),
                "Request not throttled by request count condition in application tier");
    }
}
