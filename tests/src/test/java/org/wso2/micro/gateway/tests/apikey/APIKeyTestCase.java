/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.tests.apikey;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.json.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.model.*;
import org.wso2.micro.gateway.tests.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

public class APIKeyTestCase extends BaseTestCase {
    private String apikey;
    private String jwtTokenProd;

    @BeforeClass
    public void start() throws Exception {
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        super.init("api-key-project", new String[]{"common_api.yaml"}, null,"confs/api-key.conf");
    }

    @Test(description = "Test to check jwt token is issued successfully")
    private void APIKeyIssueTest() throws Exception {

        String originalInput = "generalUser1:password";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        Map<String, String> headers = new HashMap<>();
        //get token from token endpoint
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + basicAuthToken);
        HttpResponse response = HttpClientRequest
                .doGet("https://localhost:" + TestConstant.GATEWAY_LISTENER_HTTPS_PORT + "/apikey", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        apikey = response.getData();
    }

    @Test(description = "Test to check the apikey auth working", dependsOnMethods = "APIKeyIssueTest")
    private void invokeAPIKeyHeaderTest() throws Exception {

        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put("api_key", apikey);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/pet/1"), headers);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the issued apikey in is working", dependsOnMethods = "APIKeyIssueTest")
    private void invokeAPIKeyNoQueryTest() throws Exception {

        //test endpoint with token
        HttpResponse response =
                HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/store/inventory?api_key=") + apikey );

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check the issued apikey in-query is working", dependsOnMethods = "APIKeyIssueTest")
    private void invokeAPIKeyQueryTest() throws Exception {

        //test endpoint with token
        HttpResponse response =
                HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/pet/1?api_key_query=") + apikey);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check apikey authenticate allowed apis")
    private void validateAPIsAPIKeyTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put("api_key", jwtTokenProd);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/pet/1"), headers);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
