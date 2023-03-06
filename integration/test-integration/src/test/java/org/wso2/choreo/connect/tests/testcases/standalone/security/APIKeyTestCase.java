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

/**
 * This test case runs twice with issuer "APIM Publisher" and issuer "APIM APIkey".
 */
public class APIKeyTestCase extends ApimBaseTest {
    private String testAPIKey =
            "eyJ4NXQiOiJPREUzWTJaaE1UQmpNRE00WlRCbU1qQXlZemxpWVRJMllqUmhZVFpsT0dJeVptVXhOV0UzWVE9PSIsImtpZCI6Imdh"+
            "dGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib24uc3V"+
            "wZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkbWluIiwidGllclF1b3RhVHlwZSI6bnVsbCwidGllciI6IjEwUGVyTWluIiwi"+
            "bmFtZSI6IkFQSUtleVRlc3RBcHAiLCJpZCI6MiwidXVpZCI6ImE1Y2ExYzViLWRiZjEtNDhjMS1hYzA3LWM2MmQ5OTllNjBkZiJ9L"+
            "CJpc3MiOiJodHRwczpcL1wvYXBpbTo5NDQ0XC9vYXV0aDJcL3Rva2VuIiwidGllckluZm8iOnsiVW5saW1pdGVkIjp7InRpZXJRdW"+
            "90YVR5cGUiOiJyZXF1ZXN0Q291bnQiLCJncmFwaFFMTWF4Q29tcGxleGl0eSI6MCwiZ3JhcGhRTE1heERlcHRoIjowLCJzdG9wT25"+
            "RdW90YVJlYWNoIjp0cnVlLCJzcGlrZUFycmVzdExpbWl0IjowLCJzcGlrZUFycmVzdFVuaXQiOm51bGx9fSwia2V5dHlwZSI6IlBS"+
            "T0RVQ1RJT04iLCJwZXJtaXR0ZWRSZWZlcmVyIjoiIiwic3Vic2NyaWJlZEFQSXMiOlt7InN1YnNjcmliZXJUZW5hbnREb21haW4iO"+
            "iJjYXJib24uc3VwZXIiLCJuYW1lIjoiQVBJS2V5VGVzdEFQSSIsImNvbnRleHQiOiJcL2FwaUtleVwvMS4wLjAiLCJwdWJsaXNoZX"+
            "IiOiJhZG1pbiIsInZlcnNpb24iOiIxLjAuMCIsInN1YnNjcmlwdGlvblRpZXIiOiJVbmxpbWl0ZWQifV0sInRva2VuX3R5cGUiOiJ"+
            "hcGlLZXkiLCJwZXJtaXR0ZWRJUCI6IiIsImlhdCI6MTY3ODExOTEwNSwianRpIjoiZjlkOTI5YWYtMjQ0OS00MGFiLTlhNGMtOTEw"+
            "MDc3ZDYxZDY0In0=.mBN71f7UoIED1MbNBCsfdDl7xS4_TCu4EwhB1zOqkNEsi0s8jPqnIFNsPUrUeLp8XVB8BaxXOdqNjRPMUp2R"+
            "0Crtng6B86vABRS2MwsJYn6vLwNvJram4ypkmaGBB9pF8IPmVuTjDbzKWdFZFtg1vHrflpOMXwwX-yLDsEOPzdtmDP8-zm2_W7jCC"+
            "bHlXVnuEbJcihYZmt9YyrUPDNweNS9FBmbpfOUytSFU1mNj12fNIj1Dpw_egrJSVRLR0XrvFSW2Y8BDEj0eo-RHuuf0-Gp-HqQm0b"+
            "ijSY3RiSX-414GgI-3lHUQlrlIowgQGfCUm2djvl6Z5kUAzuWkF0i1Ng==";

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

    @Test(description = "Test to invoke with an APIKey of uppercase")
    public void invokeWithAnApiKeyOfUppercaseInHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-APIKEY", testAPIKey);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to invoke with an APIKey of uppercase")
    public void invokeWithAnApiKeyOfUppercaseInQuery() throws Exception {
        Map<String, String> headers = new HashMap<>();
        HttpResponse response1 = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/1?X-ApiKey-Q=" + testAPIKey), headers);
        Assert.assertNotNull(response1);
        Assert.assertEquals(response1.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        HttpResponse response2 = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/1?x-apikey-q=" + testAPIKey), headers);
        Assert.assertNotNull(response2);
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
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
