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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class APILevelWithoutRateLimitTestCase {
    private String testKey;
    private String endpointURL;
    private Map<String, String> headers = new HashMap<>();

    @BeforeClass
    public void createApiProject() throws Exception {
        API api = new API();
        api.setName("apiLevelWithoutRatelimit");
        api.setContext("v2/apiLevelWithoutRateLimit");
        api.setVersion("1.0.0");
        api.setProvider("admin");

        ApplicationDTO applicationDto = new ApplicationDTO();
        applicationDto.setName("jwtApp");
        applicationDto.setTier("Unlimited");
        applicationDto.setId((int) (Math.random() * 1000));

        testKey = TokenUtil.getJWT(api, applicationDto, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
        endpointURL = Utils.getServiceURLHttps("/v2/apiLevelWithoutRateLimit/pet/3");
        headers.put("Internal-Key", testKey);
    }

    @Test(description = "Test rate-limiting headers with envoy rate-limit service")
    public void testRateLimitingHeadersWithEnvoyRateLimitService() throws Exception {
        HttpResponse response = HTTPSClientUtils.doGet(endpointURL, headers);
        Map<String,String> responseHeadersMap = response.getHeaders();
        Assert.assertTrue(!responseHeadersMap.containsKey("x-ratelimit-limit"), "x-ratelimit-limit available");
        Assert.assertTrue(!responseHeadersMap.containsKey("x-ratelimit-reset"), "x-ratelimit-reset available");
        Assert.assertTrue(!responseHeadersMap.containsKey("x-ratelimit-remaining"), "x-ratelimit-remaining header available");
    }
}
