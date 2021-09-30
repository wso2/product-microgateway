/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIKeyHeaderTestCase extends ApimBaseTest {

    private static final String SAMPLE_API_NAME = "APIKeyHeaderTestAPI";
    private static final String SAMPLE_API_CONTEXT = "apiKeyHeader";
    private static final String SAMPLE_API_VERSION = "1.0.0";
    private static final String APP_NAME = "APIKeyHeaderTestApp";

    protected String apiKey;
    private String applicationId;
    private String apiId;
    private String endPoint;
    private String internalKey;

    @BeforeClass(description = "Initialise the setup for API key tests")
    void start() throws Exception {
        super.initWithSuperTenant();
        APIRequest apiRequest = new APIRequest(SAMPLE_API_NAME, SAMPLE_API_CONTEXT,
                new URL(Utils.getDockerMockServiceURLHttp(TestConstant.MOCK_BACKEND_BASEPATH)));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion(SAMPLE_API_VERSION);
        apiRequest.setTiersCollection(TestConstant.SUBSCRIPTION_TIER.UNLIMITED);
        apiRequest.setTier(TestConstant.API_TIER.UNLIMITED);
        apiRequest.setApiTier(TestConstant.API_TIER.UNLIMITED);

        // Add api related operations
        APIOperationsDTO findByStatus = new APIOperationsDTO();
        findByStatus.setVerb("GET");
        findByStatus.setTarget("/pet/{petId}");
        findByStatus.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(findByStatus);
        apiRequest.setOperationsDTOS(operationsDTOS);

        // Add security scheme to the API
        List<String> securitySchemeList = new ArrayList<>();
        securitySchemeList.add("api_key");
        apiRequest.setSecurityScheme(securitySchemeList);
        apiId = PublisherUtils.createAndPublishAPI(apiRequest, publisherRestClient);

        //Create and subscribe to app
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        AppWithConsumerKey appWithConsumerKey = StoreUtils.createApplicationWithKeys(app, storeRestClient);
        applicationId = appWithConsumerKey.getApplicationId();
        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);

        endPoint = Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/2");

        // Obtain internal key
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> internalApiKeyDTO =
                publisherRestClient.generateInternalApiKey(apiId);
        internalKey = internalApiKeyDTO.getData().getApikey();

        // Obtain API key
        APIKeyDTO apiKeyDTO = StoreUtils.generateAPIKey(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                storeRestClient);
        apiKey = apiKeyDTO.getApikey();
    }

    @Test(description = "Test to check the API Key in header is working")
    public void invokeAPIKeyWithSimilarHeaderSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("api_key", apiKey);
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(),
                com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_OK,
                "Response code mismatched");
    }

    @Test(description = "Test to check the API Key in header is working")
    public void invokeInternalAPIKeyWithSimilarHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("api_key", internalKey);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
}
