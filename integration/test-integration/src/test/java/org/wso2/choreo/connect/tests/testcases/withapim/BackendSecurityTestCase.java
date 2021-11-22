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
    private String apiId;
    private String applicationId;

    public static final String API_NAME = "BackendSecurityApi";
    private static final String API_CONTEXT = "backend_security";
    public static final String APP_NAME = "backendSecurityApp";
    private static final String API_VERSION = "1.0.0";

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();
    }

    @Test
    public void testBasicAuthSecuredBackend() throws CCTestException, MalformedURLException {

        JSONObject prodEndpoints = new JSONObject();
        prodEndpoints.put("url", new URL(Utils.getDockerMockServiceURLHttp(TestConstant.MOCK_BACKEND_BASEPATH)).toString());
        JSONObject productionSecurity = new JSONObject();
        productionSecurity.put("type", "BASIC");
        productionSecurity.put("username", "admin");
        productionSecurity.put("password", "admin");
        productionSecurity.put("enabled", true);

        JSONObject epSecurity = new JSONObject();
        epSecurity.put("production", productionSecurity);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", prodEndpoints);
        endpointConfig.put("endpoint_security", epSecurity);

        APIOperationsDTO apiOperation = new APIOperationsDTO();
        apiOperation.setVerb("GET");
        apiOperation.setTarget("/echo");
        apiOperation.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperation);

        APIRequest apiRequest = PublisherUtils.createSampleAPIRequest(API_NAME, API_CONTEXT,
                API_VERSION, user.getUserName());
        apiRequest.setEndpoint(endpointConfig);
        apiRequest.setOperationsDTOS(operationsDTOS);
        apiId = PublisherUtils.createAndPublishAPI(apiRequest, publisherRestClient);

        //Create App. Subscribe.
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        AppWithConsumerKey appWithConsumerKey = StoreUtils.createApplicationWithKeys(app, storeRestClient);
        applicationId = appWithConsumerKey.getApplicationId();

        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{}, user, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME * 2, "Interrupted when waiting for the " +
                "subscription to be deployed");

        //Invoke API
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/echo");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint);

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertTrue(respHeaders.containsKey("authorization"), "Backend did not receive auth header");
        Assert.assertEquals(respHeaders.get("authorization"), "Basic YWRtaW46YWRtaW4=",
                "backend basic auth header is incorrect");
    }
}
