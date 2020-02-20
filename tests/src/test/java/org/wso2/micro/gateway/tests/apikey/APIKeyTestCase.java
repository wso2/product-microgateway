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
    private String apiKeywithoutAllowedAPIs;

    @BeforeClass
    public void start() throws Exception {
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        apiKeywithoutAllowedAPIs =
                "eyJhbGciOiJSUzI1NiIsICJ0eXAiOiJqd3QiLCAia2lkIjoiYmFsbGVyaW5hIn0.eyJzdWIiOiJhZG1pbiIsICJpc3M" +
                "iOiJodHRwczovL2xvY2FsaG9zdDo5MDk1L2FwaWtleSIsICJpYXQiOjE1ODIwMTczNTMsICJqdGkiOiI5NGFkNTk3NC1lZDI0L" +
                "TRjYmQtOWZlNi1iMGNmMjU5MTQ5ZDEiLCAiYXVkIjoiaHR0cDovL29yZy53c28yLmFwaW1ndC9nYXRld2F5IiwgImtleXR5cGU" +
                "iOiJQUk9EVUNUSU9OIiwgImFsbG93ZWRBUElzIjpbXX0.BaV6hsJUxoVEoZv2DpEg7PaE4z4q4RQ8lMLDLFDROcN5H7V4moTlk" +
                "Gb9tl0p3fZLPq4TGfddi53qiApQsgHG9hdd8XqrlAC47fq2ZHR-VAnWju3N-irqA6q75vfaz3g0X2A2t5e8zzcCt5YkA5ySCjI" +
                "FLHLR3Dxv3XBgpG7n9A8TwVywD64AZAhtnC-wgbX0uuTUSpK7BCPm-s34bChb4asrAKkDYUqzFmZTQDmL6t5lJ7QAhm3UrwuI2" +
                "wCAEZb6hu2YCZsrOLsX76t-2Evkx4Na-3sWIfb_2v5Arbd7PhjS6t0m5wpxCJGChDuP7dLZOXhjZGykg02uUxSt07ys1A";
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
        headers.put("api_key", apiKeywithoutAllowedAPIs);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/pet/1"), headers);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
