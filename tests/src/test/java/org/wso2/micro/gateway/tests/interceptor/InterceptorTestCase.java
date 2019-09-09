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
package org.wso2.micro.gateway.tests.interceptor;

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This test class is used to test per API/Resource interceptors.
 */
public class InterceptorTestCase extends BaseTestCase {
    private String jwtTokenProd;

    @BeforeClass
    public void start() throws Exception {
        String project = "interceptorProject";
        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(),
                TestConstant.KEY_TYPE_PRODUCTION, 3600);
        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[]{"interceptor/interceptor.yaml", "interceptor/validateRequest.bal",
                "interceptor/interceptPerAPIRequest.bal", "interceptor/interceptPerAPIResponse.bal",
                "interceptor/validateResponse.bal", "interceptor/PerAPIInterceptor.yaml"});
    }

    @Test(description = "Test per API throttling")
    public void testPerResourceRequestInterceptor() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("/petstore/v1/store/order"), "{}", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.PER_RESOURCE_REQUEST_INTERCEPTOR_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test per Resource response interceptor")
    public void testPerResourceResponseInterceptor() throws Exception {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> responseHeaders;
        String key = null;

        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/petstore/v1/pet/findByStatus"),
                        headers);
        responseHeaders = response.getHeaders();
        for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            if (entry.getKey().contains(ResponseConstants.RESPONSE_INTERCEPTOR_RESPONSE_HEDAER)) {
                key = entry.getKey();
            }
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(key, ResponseConstants.RESPONSE_INTERCEPTOR_RESPONSE_HEDAER);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test per API request interceptor")
    public void testRequestInterceptorAPILevel() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("/petstore/v3/store/order"), "{}", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.API_REQUEST_INTERCEPTOR_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test per API response interceptor")
    public void testResponseInterceptorAPILevel() throws Exception {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> responseHeaders;
        String key = null;

        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/petstore/v3/pet/findByStatus"),
                        headers);
        responseHeaders = response.getHeaders();
        for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            if (entry.getKey().contains(ResponseConstants.PER_APIRESPONSE_HEADER)) {
                key = entry.getKey();
            }
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(key, ResponseConstants.PER_APIRESPONSE_HEADER);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
