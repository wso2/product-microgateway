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

package org.wso2.micro.gateway.tests.security;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * This test class is used to test disable security at API/resource level and usage of custom authentication header
 */
public class DisableSecurityAndCustomAuthHeaderTestCase extends ScopesTestCase {

    @Test(description = "Test Invoking un secured resource which is specified at API level without token")
    public void testDisableSecurityAPILevel() throws Exception {
        //test endpoint without token
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/pet/findByStatus"), new HashMap<>());
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.responseBody);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

    }

    @Test(description = "Test Invoking un secured resource specified at resource level without token")
    public void testDisableSecurityResourceLevel() throws Exception {
        //test endpoint without token
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPut(getServiceURLHttp("petstore/v1/pet/"), "{}", new HashMap<>());
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

    }

    @Test(description = "Test invoking security disabled API level, but resource is secured without access token")
    public void testEnableSecurityOverriddenResourceLevel() throws Exception {
        //test endpoint without token
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/pet/1"), new HashMap<>());
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("900902"),
                "Response should contain code 900902 which is for missing auth header");
        Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");

    }

    @Test(description = "Test Invoking the resource which is secured with auth header 'auth' using auth header 'Authorization'")
    public void testEnableSecurityOverriddenResourceLevelWithWrongAuthHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + jwtTokenProd);
        //test endpoint with token
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("900902"),
                "Response should contain code 900902 which is for missing auth header");
        Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");

    }

    @Test(description = "Test Invoking the resource which is secured with auth header 'auth' using correct header")
    public void testEnableSecurityOverriddenResourceLevelWithCorrectAuthHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("auth", "Bearer " + jwtTokenProd);
        //test endpoint with token
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
