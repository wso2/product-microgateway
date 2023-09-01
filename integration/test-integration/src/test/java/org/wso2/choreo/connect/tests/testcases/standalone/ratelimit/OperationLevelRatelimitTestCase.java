/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.choreo.connect.tests.testcases.standalone.ratelimit;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class OperationLevelRatelimitTestCase {
    private String testKey;
    private static Map<String, String> headers = new HashMap<>();

    @BeforeClass
    public void createApiProject() throws Exception {
        API api = new API();
        api.setName("ratelimit");
        api.setContext("v1.0.0/operationLevelRL");
        api.setVersion("1.0.0");
        api.setProvider("admin");

        ApplicationDTO applicationDto = new ApplicationDTO();
        applicationDto.setName("jwtApp");
        applicationDto.setTier("Unlimited");
        applicationDto.setId((int) (Math.random() * 1000));

        testKey = TokenUtil.getJWT(api, applicationDto, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
        headers.put("Internal-Key", testKey);
    }

    @Test(description = "Test operation level 3 permin rate-limiting with envoy rate-limit service")
    public void testRateLimitsWithEnvoyRateLimitService3PerMin() throws Exception {
        String endpointURL = Utils.getServiceURLHttps("/v1.0.0/operationLevelRL/pet/findByStatus");
        Assert.assertTrue(RateLimitUtils.isThrottled(
                        RateLimitUtils.sendMultipleRequests(endpointURL, headers, 4)),
                "Operation level rate-limit 3 per min testcase failed.");
    }

    @Test(description = "Test operation level 5 per min rate-limiting with envoy rate-limit service")
    public void testRateLimitsWithEnvoyRateLimitService5PerMin() throws Exception {
        String requestData = "Payload to create pet 5";
        String endpointURL1 = Utils.getServiceURLHttps("/v1.0.0/operationLevelRL/pet/3");
        String endpointURL2 = Utils.getServiceURLHttps("/v1.0.0/operationLevelRL/pet/4");
        RateLimitUtils.sendMultipleRequests(endpointURL2, headers, 2);
        Assert.assertTrue(RateLimitUtils.isThrottled(
                        RateLimitUtils.sendMultipleRequests(endpointURL1, headers, 4))
                , "Operation level rate-limit 5 per min testcase failed.");
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/v1.0.0/operationLevelRL/pet/5"), requestData, headers);
        Assert.assertNotNull(response, "Response value recieved as null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), requestData, "Request data and response data is not equal ");
    }

    @Test(description = "Test an operation without defining envoy rate-limits")
    public void testOperationWithoutEnvoyRateLimits() throws Exception {
        String endpointURL = Utils.getServiceURLHttps("/v1.0.0/operationLevelRL/pets/findByTags");
        HttpResponse response = HttpsClientRequest.doGet(endpointURL, headers);
        Map<String,String> responseHeadersMap = response.getHeaders();
        Assert.assertTrue(!responseHeadersMap.containsKey("x-ratelimit-limit"), "x-ratelimit-limit header available");
        Assert.assertTrue(!responseHeadersMap.containsKey("x-ratelimit-reset"), "x-ratelimit-reset header available");
        Assert.assertTrue(!responseHeadersMap.containsKey("x-ratelimit-remaining"), "x-ratelimit-remaining header available");
    }
}
