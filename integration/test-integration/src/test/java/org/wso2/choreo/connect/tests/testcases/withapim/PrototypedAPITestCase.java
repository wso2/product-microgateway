/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.gson.Gson;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PrototypedAPITestCase extends ApimBaseTest {
    private static final String SAMPLE_API_NAME = "APIMPrototypedEndpointAPI1";
    private static final String SAMPLE_API_CONTEXT = "petstore-prototype";
    private static final String SAMPLE_API_VERSION = "1.0.0";
    private String apiId;

    @BeforeClass(description = "Initialise the setup for API key tests")
    void start() throws Exception {
        super.initWithSuperTenant();
        String apiEndPointUrl = Utils.getDockerMockServiceURLHttp(TestConstant.MOCK_BACKEND_BASEPATH);;
        String apiProvider = "admin";

        APIRequest apiRequest = new APIRequest(SAMPLE_API_NAME, SAMPLE_API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(SAMPLE_API_VERSION);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest.setProvider(apiProvider);

        apiId = PublisherUtils.createAPI(apiRequest, publisherRestClient);

        WorkflowResponseDTO lcChangeResponse = publisherRestClient.changeAPILifeCycleStatus(
                apiId, APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());

        HttpResponse response = publisherRestClient.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(response.getData(), APIDTO.class);
        String endPointString = "{\"implementation_status\":\"prototyped\",\"endpoint_type\":\"http\"," +
                "\"production_endpoints\":{\"config\":null," +
                "\"url\":\"" + apiEndPointUrl + "\"}," +
                "\"sandbox_endpoints\":{\"config\":null,\"url\":\"" + "http://localhost" + "\"}}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);
        apiDto.setEndpointConfig(endpoint);
        publisherRestClient.updateAPI(apiDto);

        Assert.assertTrue(lcChangeResponse.getLifecycleState().getState().equals("Prototyped"),
                SAMPLE_API_NAME + "  status not updated as Prototyped");

        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");
    }

    @Test(description = "Test to check the PrototypedAPI is working")
    public void invokePrototypeAPISuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        org.wso2.choreo.connect.tests.util.HttpResponse response =
                HttpsClientRequest.doGet(
                        Utils.getServiceURLHttps("/petstore-prototype/1.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }
}
