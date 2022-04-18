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

package org.wso2.choreo.connect.tests.testcases.withapim.apikey;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

public class InternalKeyHeaderTestCase extends ApimBaseTest {

    private static final String SAMPLE_API_NAME = "APIKeyHeaderTestAPI";
    private static final String SAMPLE_API_CONTEXT = "apiKeyHeader";
    private static final String SAMPLE_API_VERSION = "1.0.0";
    private static final String APP_NAME = "APIKeyHeaderTestApp";

    protected String apiKey;
    private String endPoint;
    private String internalKey;

    @BeforeClass(description = "Initialise the setup for API key tests")
    void start() throws Exception {
        super.initWithSuperTenant();

        String targetDir = Utils.getTargetDirPath();
        String filePath = targetDir + ApictlUtils.OPENAPIS_PATH + "api_key_openAPI.yaml";

        // enable apikey in apim
        JSONArray securityScheme = new JSONArray();
        securityScheme.put("oauth_basic_auth_api_key_mandatory");
        securityScheme.put("api_key");

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", SAMPLE_API_NAME);
        apiProperties.put("context", "/" + SAMPLE_API_CONTEXT);
        apiProperties.put("version", SAMPLE_API_VERSION);
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("securityScheme", securityScheme);
        String apiId = PublisherUtils.createAPIUsingOAS(apiProperties, filePath, publisherRestClient);

        publisherRestClient.changeAPILifeCycleStatus(apiId, "Publish");

        //Create and subscribe to app
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        String applicationId = StoreUtils.createApplication(app, storeRestClient);

        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);

        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);

        endPoint = Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/1");

        // Obtain internal key
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> internalApiKeyDTO =
                publisherRestClient.generateInternalApiKey(apiId);
        internalKey = internalApiKeyDTO.getData().getApikey();

        // Obtain API key
        APIKeyDTO apiKeyDTO = StoreUtils.generateAPIKey(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                storeRestClient);
        apiKey = apiKeyDTO.getApikey();

        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");
    }

    @Test(description = "Test to check the API Key in header is working")
    public void invokeAPIKeyWithSimilarHeaderSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", apiKey);
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(),
                com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_OK,
                "Response code mismatched");
    }

    @Test(description = "Test to check the Internal Key in header is working")
    public void invokeInternalAPIKeyWithSimilarHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", internalKey);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
}
