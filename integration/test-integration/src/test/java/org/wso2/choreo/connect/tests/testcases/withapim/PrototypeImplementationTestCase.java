/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

public class PrototypeImplementationTestCase extends ApimBaseTest {

    private static final String SAMPLE_API_NAME = "PrototypedImplementationTestAPI";
    private static final String SAMPLE_API_CONTEXT = "prototyped";
    private static final String SAMPLE_API_VERSION = "1.0.0";
    private String endPoint;
    private String apiId;

    @BeforeClass(description = "Initialise the setup for prototype implementation tests")
    void start() throws Exception {
        super.initWithSuperTenant();

        String targetDir = Utils.getTargetDirPath();
        String filePath = targetDir + ApictlUtils.OPENAPIS_PATH + "prototype_openAPI.yaml";

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", SAMPLE_API_NAME);
        apiProperties.put("context", "/" + SAMPLE_API_CONTEXT);
        apiProperties.put("version", SAMPLE_API_VERSION);
        apiProperties.put("provider", user.getUserName());
        apiId = PublisherUtils.createAPIUsingOAS(apiProperties, filePath, publisherRestClient);

        publisherRestClient.changeAPILifeCycleStatus(apiId, "Deploy as a Prototype");

        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);

        endPoint = Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/findByStatus");

        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");
    }

    //    Invokes with tampered API key and this will fail.
    @Test(description = "Test to detect wrong API keys")
    public void invokePrototypedAPIImplementation() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("prototypedValue", "success");
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttps(endPoint), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("x-wso2-header"), "Response header not available");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }
}
