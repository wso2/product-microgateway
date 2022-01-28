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

import com.google.common.net.HttpHeaders;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendSecurityTestCase extends ApimBaseTest {
    public static final String API_NAME = "BackendSecurityApi";
    private static final String API_CONTEXT = "backend_security";
    public static final String APP_NAME = "BackendSecurityApp";

    private String prodAccessToken;
    private String sandAccessToken;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();

        String applicationId = ApimResourceProcessor.applicationNameToId.get(APP_NAME);
        prodAccessToken = StoreUtils.generateUserAccessTokenProduction(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        sandAccessToken = StoreUtils.generateUserAccessTokenSandbox(apimServiceURLHttps, applicationId, user,
                storeRestClient);
    }

    @Test(description = "oauth2 secured resource backend basic auth")
    public void testBasicAuthBackendForSecuredResource() throws CCTestException, MalformedURLException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + prodAccessToken);
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/echo");
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint);

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertTrue(respHeaders.containsKey("authorization"), "Backend did not receive auth header");
        Assert.assertEquals(respHeaders.get("authorization"), "Basic YWRtaW46YWRtaW4=",
                "backend basic auth header is incorrect");
    }

    @Test(description = "test env variables is working")
    public void testBasicAuthBackendForSecuredResourceFromEnv() throws CCTestException, MalformedURLException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + sandAccessToken);
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/echo");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint);

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertTrue(respHeaders.containsKey("authorization"), "Backend did not receive auth header");
        Assert.assertEquals(respHeaders.get("authorization"), "Basic am9objpsb2Nr",
                "backend basic auth header is incorrect " + sandAccessToken);
    }

    @Test(description = "test non secured resource with backend basic auth")
    public void testBasicAuthBackendForNonSecuredResource() throws CCTestException, MalformedURLException {
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/echo2");
        HttpResponse response = HttpsClientRequest.doGet(endpoint);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint);

        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertTrue(respHeaders.containsKey("authorization"), "Backend did not receive auth header");
        Assert.assertEquals(respHeaders.get("authorization"), "Basic YWRtaW46YWRtaW4=",
                "backend basic auth header is incorrect");
    }
}
