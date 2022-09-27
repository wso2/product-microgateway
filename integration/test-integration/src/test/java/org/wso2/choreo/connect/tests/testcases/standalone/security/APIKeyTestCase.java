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

package org.wso2.choreo.connect.tests.testcases.standalone.security;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class APIKeyTestCase extends ApimBaseTest {
    private String testAPIKey =
            "eyJ4NXQiOiJOMkpqTWpOaU0yRXhZalJrTnpaalptWTFZVEF4Tm1GbE5qZzRPV1UxWVdRMll6YzFObVk1TlE9PS" +
                    "IsImtpZCI6ImdhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1" +
                    "NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkb" +
                    "WluIiwidGllclF1b3RhVHlwZSI6bnVsbCwidGllciI6IjEwUGVyTWluIiwibmFtZSI6IkFQSUtleVR" +
                    "lc3RBcHAiLCJpZCI6MiwidXVpZCI6IjNjMTk3MWYwLWM2N2QtNDBmNC1iYzNmLWUwYjEzN2I5MjlhY" +
                    "iJ9LCJpc3MiOiJodHRwczpcL1wvYXBpbTo5NDQ0XC9vYXV0aDJcL3Rva2VuIiwidGllckluZm8iOns" +
                    "iVW5saW1pdGVkIjp7InRpZXJRdW90YVR5cGUiOiJyZXF1ZXN0Q291bnQiLCJncmFwaFFMTWF4Q29tc" +
                    "GxleGl0eSI6MCwiZ3JhcGhRTE1heERlcHRoIjowLCJzdG9wT25RdW90YVJlYWNoIjp0cnVlLCJzcGl" +
                    "rZUFycmVzdExpbWl0IjowLCJzcGlrZUFycmVzdFVuaXQiOm51bGx9fSwia2V5dHlwZSI6IlBST0RVQ" +
                    "1RJT04iLCJzdWJzY3JpYmVkQVBJcyI6W3sic3Vic2NyaWJlclRlbmFudERvbWFpbiI6ImNhcmJvbi5" +
                    "zdXBlciIsIm5hbWUiOiJBUElLZXlUZXN0QVBJIiwiY29udGV4dCI6IlwvYXBpS2V5XC8xLjAuMCIsI" +
                    "nB1Ymxpc2hlciI6ImFkbWluIiwidmVyc2lvbiI6IjEuMC4wIiwic3Vic2NyaXB0aW9uVGllciI6IlV" +
                    "ubGltaXRlZCJ9XSwidG9rZW5fdHlwZSI6ImFwaUtleSIsImlhdCI6MTY2MTkzODcwOSwianRpIjoiY" +
                    "2U4ZTI2OWUtNzRjZC00NmYyLTgwMGUtMzExZDFkNGUyYmY3In0=.Lry-TGN0RsUbiQDTQKa_YMBxGr" +
                    "RNT7XaGNztdNlkSgtvdC9ArYwvaDhw9KKKJXLIcBzwnmb2CW1u9VbeRNPQfx7QYxI8sC6EijsGY1Ip" +
                    "khTJ37qtA75Kkqqg7hFu9qKkayf_cKwfH0gSsV7B4VOJ7LUahqlCkRZ_358lJjid8JZh8OiAi8lBMU" +
                    "2KM0kz_j66p4AnjDpKs8JxEe3HuP6TrG3KzcGnucXZmDPAVyiUUf0KfAtGrJSXfWrb9r5rW8WAYRqf" +
                    "N4I2du-A0AQPF7Fc1qWexqfE7URVIdb_C96FpMnVxRBF_wGUUrqTy3jTt5HLImz2dwWrjfnWruUWJz" +
                    "Itpg==";

    @Test(description = "Test to check the API Key in query param is working")
    public void invokeAPIKeyInQueryParamSuccessTest() throws Exception {
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/1?x-api-key=" + testAPIKey));
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the API Key in query param is working and not work for header")
    public void invokeAPIKeyInHeaderParamFailTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", testAPIKey);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check the API Key in api level")
    public void invokeAPIKeyAPILevelTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key-header", testAPIKey);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to test subscription validation failing")
    public void invokeDifferentApiWithSameValidApiKey() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", testAPIKey);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/v2/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
    }

    @Test(description = "Test to check the API Key fails for only oauth2 secured resource")
    public void invokeAPIKeyOauth2Test() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key-header", testAPIKey);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check the backend JWT generation for API Key")
    public void apiKeyBackendJwtGenerationTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key-header", testAPIKey);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/jwtheader"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.VALID_JWT_RESPONSE,
                "Response body mismatched");
    }

    @Test(description = "Test to check the oauth2 secured resource")
    public void invokeOauth2Test() throws Exception {
        API api = new API();
        api.setName("APIKeyTestAPI");
        api.setContext("/apiKey/1.0.0");
        api.setVersion("1.0.0");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("APIKeyTestApp");
        application.setTier("Unlimited");
        application.setId(88);
        String jwtToken = TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, "write:pets", false);
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
}
