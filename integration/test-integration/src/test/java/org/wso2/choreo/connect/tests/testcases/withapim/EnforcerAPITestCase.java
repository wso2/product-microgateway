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
package org.wso2.choreo.connect.tests.testcases.withapim;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnforcerAPITestCase extends ApimBaseTest {

    private static final String ADMIN_CREDENTIALS = "YWRtaW46YWRtaW4=";
    private static final String API_NAME = "VHostAPI2";
    private static final String API_CONTEXT = "/vhostApi2/1.0.0";
    private static final String NON_EXISTING_CONTEXT = "/not/exists/1.0.0";
    private static final String API_VERSION = "1.0.0";
    private static final String APPLICATION_NAME = "VHostApp";

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();
    }

    @Test
    public void testGetAPIInfo() throws IOException {
        String requestUrl = "https://localhost:9001/api/info?context=" + API_CONTEXT + "&version=" + API_VERSION;
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse httpResponse = HttpClientRequest.doGet(requestUrl, headers);
        Assert.assertNotNull(httpResponse.getData());
        Assert.assertTrue(httpResponse.getData().contains(API_NAME));
        Assert.assertTrue(httpResponse.getData().contains(API_CONTEXT));
        Assert.assertTrue(httpResponse.getData().contains(API_VERSION));
        Assert.assertTrue(httpResponse.getData().contains(APPLICATION_NAME));
    }

    @Test
    public void testGetNonExistingAPIInfo() throws IOException {
        String requestUrl = "https://localhost:9001/api/info?context=" + NON_EXISTING_CONTEXT + "&version=" + API_VERSION;
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse httpResponse = HttpClientRequest.doGet(requestUrl, headers);
        Assert.assertNotNull(httpResponse.getData());
        Assert.assertTrue(httpResponse.getData().contains("No API information found for context " +
                NON_EXISTING_CONTEXT + " and version " + API_VERSION));
    }

    @Test
    public void testGetAllAPIs() throws IOException {
        String requestUrl = "https://localhost:9001/apis";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse httpResponse = HttpClientRequest.doGet(requestUrl, headers);
        Assert.assertNotNull(httpResponse.getData());
        Assert.assertTrue(httpResponse.getData().contains("count"));
    }

    @Test
    public void testQueryAPIs() throws IOException {
        String requestUrl = "https://localhost:9001/apis?context=" + API_CONTEXT;
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse httpResponse = HttpClientRequest.doGet(requestUrl, headers);
        Assert.assertNotNull(httpResponse.getData());
        Assert.assertTrue(httpResponse.getData().contains("\"count\":1"));
        Assert.assertTrue(httpResponse.getData().contains(API_CONTEXT));
        Assert.assertTrue(httpResponse.getData().contains(API_NAME));
    }

    @Test
    public void testGetApplications() throws IOException {
        String requestUrl = "https://localhost:9001/applications?orgId=carbon.super";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse httpResponse = HttpClientRequest.doGet(requestUrl, headers);
        Assert.assertNotNull(httpResponse.getData());
        Assert.assertTrue(httpResponse.getData().contains("count"));
    }

    @Test
    public void testGetSubscriptions() throws IOException {
        String requestUrl = "https://localhost:9001/subscriptions";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse httpResponse = HttpClientRequest.doGet(requestUrl, headers);
        Assert.assertNotNull(httpResponse.getData());
        Assert.assertTrue(httpResponse.getData().contains("count"));
    }

    @Test
    public void testGetApplicationPolicies() throws IOException {
        String appPolicyRequestURL = "https://localhost:9001/throttling_policies/application";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse appPolicyResponse = HttpClientRequest.doGet(appPolicyRequestURL, headers);
        Assert.assertNotNull(appPolicyResponse.getData());
        Assert.assertTrue(appPolicyResponse.getData().contains("count"));

        String requestUrl = "https://localhost:9001/throttling_policies/application";
        Map<String, String> subPolicyRequestURL = new HashMap<>();
        subPolicyRequestURL.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse subPolicyResponse = HttpClientRequest.doGet(requestUrl, subPolicyRequestURL);
        Assert.assertNotNull(subPolicyResponse.getData());
        Assert.assertTrue(subPolicyResponse.getData().contains("count"));
    }

    @Test
    public void testGetSubscriptionPolicies() throws IOException {
        String requestUrl = "https://localhost:9001/throttling_policies/subscription";
        Map<String, String> subPolicyRequestURL = new HashMap<>();
        subPolicyRequestURL.put("Authorization", "Basic " + ADMIN_CREDENTIALS);
        HttpResponse subPolicyResponse = HttpClientRequest.doGet(requestUrl, subPolicyRequestURL);
        Assert.assertNotNull(subPolicyResponse.getData());
        Assert.assertTrue(subPolicyResponse.getData().contains("count"));
    }
}
