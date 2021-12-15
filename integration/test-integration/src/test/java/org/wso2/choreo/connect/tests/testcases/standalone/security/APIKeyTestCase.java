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
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

public class APIKeyTestCase extends ApimBaseTest {
    private String testAPIKey =
            "eyJ4NXQiOiJOVGRtWmpNNFpEazNOalkwWXpjNU1tWm1PRGd3TVRFM01XWXdOREU1TVdSbFpEZzROemM0WkE9PSIsImtpZCI6Imdhd" +
                    "GV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib" +
                    "24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkbWluIiwidGllclF1b3RhVHlwZSI6bnVsbCwidGllciI6Il" +
                    "VubGltaXRlZCIsIm5hbWUiOiJBUElLZXlUZXN0QXBwIiwiaWQiOjg4LCJ1dWlkIjoiYzcwZmVmZjEtYWZhOS00YTA3LTk" +
                    "0OWEtNjIwNDExZjFjZmVhIn0sImlzcyI6Imh0dHBzOlwvXC9hcGltOjk0NDNcL29hdXRoMlwvdG9rZW4iLCJ0aWVySW5m" +
                    "byI6eyJVbmxpbWl0ZWQiOnsidGllclF1b3RhVHlwZSI6InJlcXVlc3RDb3VudCIsImdyYXBoUUxNYXhDb21wbGV4aXR5I" +
                    "jowLCJncmFwaFFMTWF4RGVwdGgiOjAsInN0b3BPblF1b3RhUmVhY2giOnRydWUsInNwaWtlQXJyZXN0TGltaXQiOjAsIn" +
                    "NwaWtlQXJyZXN0VW5pdCI6bnVsbH19LCJrZXl0eXBlIjoiUFJPRFVDVElPTiIsInN1YnNjcmliZWRBUElzIjpbeyJzdWJ" +
                    "zY3JpYmVyVGVuYW50RG9tYWluIjoiY2FyYm9uLnN1cGVyIiwibmFtZSI6IkFQSUtleVRlc3RBUEkiLCJjb250ZXh0Ijoi" +
                    "XC9hcGlLZXlcLzEuMC4wIiwicHVibGlzaGVyIjoiYWRtaW4iLCJ2ZXJzaW9uIjoiMS4wLjAiLCJzdWJzY3JpcHRpb25Ua" +
                    "WVyIjoiVW5saW1pdGVkIn1dLCJ0b2tlbl90eXBlIjoiYXBpS2V5IiwiaWF0IjoxNjM4MzUzOTA1LCJqdGkiOiJkNjlmND" +
                    "JlNy1mNWExLTRiZDktOTFjZC0zZmZjYjg5NGQ1OTgifQ==.T_V3sqMPSP3sD4a91HM4dbucac-J9PazE0xkv85D2i5V8p" +
                    "oj1H9jBaAWLH1PRdDdPpGuV69px3cRKJugyZ43z8DrAwYsMO4DzC_VYAJjAFCHWwg82vjeLC3gQrv0A85cx1p4jyjWAbx" +
                    "ByLO4351G96ds-yMfKaUF1ZYWzDtnsI1SIzgczXY3OLANdjkNwE0wvlu-UOWSdEEFSdNFDYc4Nn33g2EeL4I9llYltW81" +
                    "weXA0WnOMK4nvKZtrSQmWTIH-RlfJGR07FZRfFeQi3OfQuOR6puYHBx946PqAbIGj5t2IhmaQl_Bun66AJwkd2nalO2bx" +
                    "pNEHoTWCtuHN2zVpQ==";

    @Test(description = "Test to check the API Key in query param is working")
    public void invokeAPIKeyInQueryParamSuccessTest() throws Exception {
        HttpResponse response = HttpClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/1?x-api-key=" + testAPIKey));
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the API Key in query param is working and not work for header")
    public void invokeAPIKeyInHeaderParamFailTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", testAPIKey);
        HttpResponse response = HttpClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check the API Key in api level")
    public void invokeAPIKeyAPILevelTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key-header", testAPIKey);
        HttpResponse response = HttpClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pet/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the API Key fails for only oauth2 secured resource")
    public void invokeAPIKeyOauth2Test() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key-header", testAPIKey);
        HttpResponse response = HttpClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check the backend JWT generation for API Key")
    public void apiKeyBackendJwtGenerationTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key-header", testAPIKey);
        HttpResponse response = HttpClientRequest.doGet(
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
        HttpResponse response = HttpClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
}
