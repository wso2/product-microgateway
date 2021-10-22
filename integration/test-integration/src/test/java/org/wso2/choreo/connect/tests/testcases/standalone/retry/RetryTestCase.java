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

package org.wso2.choreo.connect.tests.testcases.standalone.retry;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;

import java.util.HashMap;
import java.util.Map;

public class RetryTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "Get Prod and Sandbox tokens")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Test API Level retry for Production and Sandbox endpoints")
    public void testAPILevelRetryForProdAndSand() throws Exception {
        Map<String, String> prodHeaders = new HashMap<>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-two") , prodHeaders);
        //TODO: Change the basepath in the OpenAPI definition to /retry when the issue
        // https://github.com/wso2/product-microgateway/issues/2308 is fixed

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-two"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Test Resource Level retry for Production and Sandbox endpoints")
    public void testResourceLevelRetryForProdAndSand() throws Exception {
        Map<String, String> prodHeaders = new HashMap<>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-three") , prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-three"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke endpoint that requires more retries than configured for API or Resource")
    public void testGlobalRetryConfigForProd() throws Exception {
        Map<String, String> prodHeaders = new HashMap<>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        prodHeaders.put("x-envoy-max-retries", "10"); // check if external client headers are accepted
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-four"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_GATEWAY_TIMEOUT,
                "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.GATEWAY_ERROR,
                "Response message mismatch.");
    }

    @Test(description = "Test where retry configs have invalid count and status code, thus get replaced by default")
    public void testInvalidRetryConfigReplaceByDefaultForSand() throws Exception {
        Map<String, String> prodHeaders = new HashMap<>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-seven"), prodHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_GATEWAY_TIMEOUT,
                "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.GATEWAY_ERROR,
                "Response message mismatch.");

        HttpResponse sandResponseFinal = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-seven"), prodHeaders);
        Assert.assertEquals(sandResponseFinal.getResponseCode(), HttpStatus.SC_OK,
                "Response code mismatched");
    }
}
