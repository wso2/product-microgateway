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

package org.wso2.choreo.connect.tests.testCases.apim.endpoints;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.APIMLifecycleBaseTest;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case to check the behaviour when APIs are blocked from APIM publisher lifecycle tab.
 *
 */
public class PrototypeEndpointTestCase extends APIMLifecycleBaseTest {
    private Map<String, String> requestHeaders;
    private APIRequest apiRequest;
    private String apiId;
    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.init();
        // get a predefined api request
        apiRequest = getAPIRequest(TestConstant.SAMPLE_API_NAME);
        apiRequest.setProvider(user.getUserName());

        requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer undefined");

        // create and publish the api
        apiId = createAndDeployAPI(apiRequest, "localhost", restAPIPublisher);

        endpointURL = Utils.getServiceURLHttps(TestConstant.SAMPLE_API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    @Test(description = "Send a request to a prototyped Endpoint")
    public void testPrototypedEndpointAPI() throws IOException, MicroGWTestException, InterruptedException, ApiException {
        changeLCStateAPI(apiId, APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction(), restAPIPublisher, false);
        Thread.sleep(3000);
        HttpResponse apiDetails = restAPIPublisher.getAPI(apiId);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Expected error code is 200, but received the code : " + response.getResponseCode());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
