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
            "eyJ4NXQiOiJOMkpqTWpOaU0yRXhZalJrTnpaalptWTFZVEF4Tm1GbE5qZzRPV1UxWVdRMll6YzFObVk1TlE9PSIsImtpZCI6Im" +
                    "dhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbkB" +
                    "jYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkbWluIiwidGllclF1b3RhVHlwZSI6bnVsbCwid" +
                    "GllciI6IlVubGltaXRlZCIsIm5hbWUiOiJBUElLZXlUZXN0QXBwIiwiaWQiOjQsInV1aWQiOiIzMjgxNTk0Ni02N2Y" +
                    "yLTRjZmYtYmQxYS0zMjJjZTRmY2U4YTAifSwiaXNzIjoiaHR0cHM6XC9cL2FwaW06OTQ0M1wvb2F1dGgyXC90b2tlb" +
                    "iIsInRpZXJJbmZvIjp7IlVubGltaXRlZCI6eyJ0aWVyUXVvdGFUeXBlIjoicmVxdWVzdENvdW50IiwiZ3JhcGhRTE1" +
                    "heENvbXBsZXhpdHkiOjAsImdyYXBoUUxNYXhEZXB0aCI6MCwic3RvcE9uUXVvdGFSZWFjaCI6dHJ1ZSwic3Bpa2VBc" +
                    "nJlc3RMaW1pdCI6MCwic3Bpa2VBcnJlc3RVbml0IjpudWxsfX0sImtleXR5cGUiOiJQUk9EVUNUSU9OIiwic3Vic2Ny" +
                    "aWJlZEFQSXMiOlt7InN1YnNjcmliZXJUZW5hbnREb21haW4iOiJjYXJib24uc3VwZXIiLCJuYW1lIjoiQVBJS2V5VGV" +
                    "zdEFQSSIsImNvbnRleHQiOiJcL2FwaUtleVwvMS4wLjAiLCJwdWJsaXNoZXIiOiJhZG1pbiIsInZlcnNpb24iOiIxLjA" +
                    "uMCIsInN1YnNjcmlwdGlvblRpZXIiOiJVbmxpbWl0ZWQifV0sInRva2VuX3R5cGUiOiJhcGlLZXkiLCJpYXQiOjE2NDY" +
                    "0MDg3MjEsImp0aSI6IjY0ODI1OWVjLWY5ZmEtNGY5OC1iYWYzLWUyZmYwYzhjZmRhMyJ9.eGrG8v_BUusNTbrIvN0N3m" +
                    "OEHLw8zXU8rbAag1U4JUmSLd2hySoiCqMnXXYLI_U01qfGupET3HD3ZVcNOWqgjMa_c_ABGPrVTLNxKmCTpzRNbNbfex" +
                    "ouvo0b9FcM_5Z_bSgw9kDGqAzdYb8Hi5Ibe2LNDPQf7Kxwc6dd0iKMUvtM9kMdt-IBgRNIDqnx1auSuuuC2SEEQmeDWk" +
                    "uOAvKifyu075Gv7GyiX6vELfH7xxMMZuq0EAKIYXFRmbO6SLQ1JcqFvUW6Une7NAg7Y4TSfx7v6WzL1dTe8pRaGK7CUR" +
                    "f76gJQ9PteAgdzpCkIv2XNKYnoOm6BL0ezv8LUiwJPeA==";

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
