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

package org.wso2.choreo.connect.tests.testcases.withapim.apikey;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.testcases.withapim.throttle.ThrottlingBaseTestCase;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIKeyAppLevelThrottleTestCase extends ApimBaseTest {

    private static final String API_NAME = "APIKeyAppLevelThrottleAPI";
    private static final String API_CONTEXT = "apikey_app_level_throttling";
    private static final String APP_NAME = "apiKeyAppThrottleApp";

    private static final String POLICY_NAME = "app5PerMin";
    private static final String POLICY_TIME_UNIT = "min";
    private static final Integer POLICY_UNIT_TIME = 1;
    private static final long REQUEST_COUNT = 5L;
    private static final String POLICY_DESC = "This is an application level throttle policy";

    private ApplicationThrottlePolicyDTO appThrottlePolicyDTO;
    String apiId;
    String applicationId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // create the throttling policy DTO with request count limit
        RequestCountLimitDTO reqCountLimit = DtoFactory.createRequestCountLimitDTO(POLICY_TIME_UNIT, POLICY_UNIT_TIME,
                REQUEST_COUNT);
        ThrottleLimitDTO defaultLimit = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                reqCountLimit, null);

        // Add the application throttling policy
        appThrottlePolicyDTO = DtoFactory.createApplicationThrottlePolicyDTO(POLICY_NAME, POLICY_NAME,
                POLICY_DESC, false, defaultLimit);
        ApiResponse<ApplicationThrottlePolicyDTO> addedPolicy = adminRestClient.addApplicationThrottlingPolicy(
                    appThrottlePolicyDTO);
        appThrottlePolicyDTO = addedPolicy.getData();

        // Create the app and subscribe
        Application app = new Application(APP_NAME, POLICY_NAME);
        applicationId = StoreUtils.createApplication(app, storeRestClient);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);
        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED,
                storeRestClient);
    }

    @Test(description = "Test application level throttling for API Key")
    public void testApplicationLevelThrottlingForAPIKey() throws Exception {
        APIKeyDTO apiKeyDTO = StoreUtils.generateAPIKey(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                storeRestClient);
        String apiKey = apiKeyDTO.getApikey();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("apikey", apiKey);

        String endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
        Assert.assertTrue(ThrottlingBaseTestCase.isThrottled(endpointURL, requestHeaders, null, REQUEST_COUNT),
                "Request not throttled by request count condition in application tier");
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        storeRestClient.removeApplicationById(applicationId);
        adminRestClient.deleteApplicationThrottlingPolicy(appThrottlePolicyDTO.getPolicyId());
    }
}
