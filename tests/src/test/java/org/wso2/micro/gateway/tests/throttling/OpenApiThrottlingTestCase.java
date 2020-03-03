/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.throttling;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;
import org.wso2.micro.gateway.tests.util.HttpResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * This test class is used to test per API/Resource throttling policies.
 */
public class OpenApiThrottlingTestCase extends BaseTestCase {
    private String jwtTokenProd;
    private HttpResponse response;
    private String perResourceUrl = "petstore/v1/pet/1";

    @BeforeClass
    public void start() throws Exception {
        String project = "OpenApiThrottlingProject";
        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(),
                TestConstant.KEY_TYPE_PRODUCTION, 3600);
        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[]{"common_api.yaml"});
    }

    @Test(description = "Test per API throttling")
    public void testPerAPIThrottling() throws Exception {
        response = invokeAndAssert(jwtTokenProd,
                getServiceURLHttp("/petstore/v1/pet/findByStatus?status=available"));
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.PER_API_THROTTLING_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 429, "Too Many Requests");
    }

    @Test(description = "Test per resource throttling")
    public void testPerResourceThrottling() throws Exception {
        response = invokeAndAssert(jwtTokenProd, getServiceURLHttp(perResourceUrl));
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.PER_RESOURCE_THROTTLING_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 429, "Too Many Requests");
    }

    @Test(description = "Test throttling with non existing policy")
    public void testThrottlingWithNonExistingPolicy() throws Exception {
        response = invokeAndAssert(jwtTokenProd,
                getServiceURLHttp("/petstore/v1/store/order/1"));
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.NONEXISTING_THROTTLEPOLICY_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 500, "Internal server error occured");
    }

    @Test(description = "Test resource level throttling for unsecured resource")
    public void testUnsecuredResourceThrottling() throws Exception {
        response = invokeAndAssert(null,
                getServiceURLHttp("/petstore/v1/user/login?username=test?password=test"));
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.PER_RESOURCE_THROTTLING_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 429, "Too Many Requests");
    }

    private HttpResponse invokeAndAssert(String token, String url) throws Exception {
        Map<String, String> headers = new HashMap<>();
        org.wso2.micro.gateway.tests.util.HttpResponse response = null;
        int retryCount;
        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        if (url.contains(perResourceUrl)) {
            retryCount = 5;
        } else {
            retryCount = 10;
        }
        int retry = retryCount;
        while (retry > 0) {
            for (int i = 0; i < retryCount; i++) {
                response = HttpClientRequest.doGet(url, headers);
                Thread.sleep(1000);
                Assert.assertNotNull(response);
                retry--;
            }
        }
        return response;
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
