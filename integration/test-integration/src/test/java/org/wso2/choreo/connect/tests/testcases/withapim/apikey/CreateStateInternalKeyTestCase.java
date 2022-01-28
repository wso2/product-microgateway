/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * This test class covers API invocations in create state using internal keys.
 */
public class CreateStateInternalKeyTestCase extends ApimBaseTest {
    private static final String SAMPLE_API_NAME = "createStateInternalKeyTestAPI";
    private static final String SAMPLE_API_CONTEXT = "createStateInternalKey";
    private static final String SAMPLE_API_VERSION = "1.0.0";

    private String endPoint;
    private String internalKey;
    private String apiId;

    @BeforeClass(description = "Initialise the setup for create state internal key tests")
    void start() throws Exception {
        super.initWithSuperTenant();

        String targetDir = Utils.getTargetDirPath();
        String filePath = targetDir + ApictlUtils.OPENAPIS_PATH + "api_key_openAPI.yaml";

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", SAMPLE_API_NAME);
        apiProperties.put("context", "/" + SAMPLE_API_CONTEXT);
        apiProperties.put("version", SAMPLE_API_VERSION);
        apiProperties.put("provider", user.getUserName());
        apiId = PublisherUtils.createAPIUsingOAS(apiProperties, filePath, publisherRestClient);

        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);

        endPoint = Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/1");

        // Obtain internal key
        ApiResponse<APIKeyDTO> internalApiKeyDTO = publisherRestClient.generateInternalApiKey(apiId);
        internalKey = internalApiKeyDTO.getData().getApikey();

        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");
    }

    @Test(description = "Test to check the create state's API invocations using internal Key ")
    public void invokeInternalAPIKeyWithSimilarHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", internalKey);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.GET_PET_RESPONSE, "Response body mismatched");
    }

    @AfterClass
    public void clean() throws Exception {
        publisherRestClient.deleteAPI(apiId);
    }
}
