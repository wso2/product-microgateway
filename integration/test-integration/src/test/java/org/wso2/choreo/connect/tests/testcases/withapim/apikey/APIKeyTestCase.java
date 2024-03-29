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

package org.wso2.choreo.connect.tests.testcases.withapim.apikey;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.*;

public class APIKeyTestCase extends ApimBaseTest {

    private static final String SAMPLE_API_CONTEXT = "apiKey";
    private static final String APP_NAME = "APIKeyTestApp";
    private String apiKey;
    private String apiKeyForIPTest;
    private String apiKeyForRefererTest;
    private String tamperedAPIKey;
    private String endPoint;

    @BeforeClass(description = "Initialise the setup for API key tests")
    void start() throws Exception {
        super.initWithSuperTenant();

        String applicationId = ApimResourceProcessor.applicationNameToId.get(APP_NAME);

        endPoint = Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/1");

        // Obtain API keys
        APIKeyDTO apiKeyDTO = StoreUtils.generateAPIKey(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                storeRestClient);
        apiKey = apiKeyDTO.getApikey();
        tamperedAPIKey = apiKey.substring(0, apiKey.length() - 400);

        APIKeyDTO ipTestAPIKeyDTO = storeRestClient.generateAPIKeys(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                -1, "192.168.1.1", null);
        apiKeyForIPTest = ipTestAPIKeyDTO.getApikey();

        APIKeyDTO refererTestAPIKeyDTO = storeRestClient.generateAPIKeys(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                -1, null, "http://www.abc.com");
        apiKeyForRefererTest = refererTestAPIKeyDTO.getApikey();
    }

    //    Invokes with tampered API key and this will fail.
    @Test(description = "Test to detect wrong API keys")
    public void invokeWithTamperedAPIKey() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", tamperedAPIKey);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(
                Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    // When invoke with original token even though the tampered API key is in the invalid key cache,
    // original token should pass.
    @Test(description = "Test to check the API Key in header is working", dependsOnMethods = "invokeWithTamperedAPIKey")
    public void invokeAPIKeyInHeaderSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", apiKey);
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    //Invoke API by including the API key as a query parameter
    @Test(description = "Test to check the API Key in query param is working")
    public void invokeAPIKeyInQueryParamSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpClientRequest.doGet(
                Utils.getServiceURLHttps(endPoint + "?apikey=" + apiKey), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the API key auth validate invalid signature key")
    public void invokeAPIKeyHeaderInvalidTokenTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", TestConstant.INVALID_JWT_TOKEN);
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    // After invoking with original key, it is cached as a success token. But again using the tampered key should fail.
    @Test(description = "Test to check the APIKey is working", dependsOnMethods = "invokeAPIKeyInHeaderSuccessTest")
    public void invokeAgainWithTamperedAPIKey() throws Exception {
        // Sets header
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", tamperedAPIKey);
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    // Ensures whether a given API key is expired or not
    @Test(description = "Test to check the API key auth validate expired token")
    public void invokeExpiredAPIKeyTest() throws Exception {
        // Sets header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apikey", TestConstant.EXPIRED_API_KEY_TOKEN);
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    @Test(description = "Test to check the API Key for incorrect IP address is not working")
    public void invokeAPIKeyForIncorrectIPAddressTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", apiKeyForIPTest);
        headers.put("permittedIP", "192.168.1.2");
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("Resource forbidden"), "Error response message mismatch");
    }


    @Test(description = "Test to check the API Key for incorrect referer address is not working")
    public void invokeAPIKeyForIncorrectRefererTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", apiKeyForRefererTest);
        headers.put("referer", "http://www.abcd.com");
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("Resource forbidden"), "Error response message mismatch");
    }

    @Test(description = "Test to check the API Key for specific IP address is working")
    public void invokeAPIKeyForIPAddressSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", apiKeyForIPTest);
        headers.put("x-forwarded-for", "192.168.1.1");
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the API Key for specific referer address is working")
    public void invokeAPIKeyForRefererSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", apiKeyForRefererTest);
        headers.put("referer", "http://www.abc.com");
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the backend JWT generation for API Key")
    public void apiKeyBackendJwtGenerationTestWithAPIM() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", apiKey);
        HttpResponse response = HttpClientRequest.doGet(
                Utils.getServiceURLHttps("/apiKey/1.0.0/jwtheader"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.VALID_JWT_RESPONSE, "Response body mismatched");
    }
}
