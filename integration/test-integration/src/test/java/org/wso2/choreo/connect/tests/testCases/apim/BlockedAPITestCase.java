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

package org.wso2.choreo.connect.tests.testCases.apim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.APIMLifecycleBaseTest;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Test case to check the behaviour when APIs are blocked from APIM publisher lifecycle tab.
 *
 */
public class BlockedAPITestCase extends APIMLifecycleBaseTest {
    private APIRequest apiRequest;
    private String apiId;
    private String applicationId;
    private Map<String, String> requestHeaders;
    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.init();

        // Creating the application
        APIMLifecycleBaseTest.ApplicationCreationResponse appCreationResponse = createApplicationWithKeys(
                "SubscriptionValidationTestApp", restAPIStore);
        applicationId = appCreationResponse.getApplicationId();

        // create the request headers after generating the access token
        String accessToken = generateUserAccessToken(appCreationResponse.getConsumerKey(),
                appCreationResponse.getConsumerSecret(),
                new String[]{"PRODUCTION"}, user, restAPIStore);
        requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        // get a predefined api request
        apiRequest = getAPIRequest(TestConstant.SAMPLE_API_NAME);
        apiRequest.setProvider(user.getUserName());

        // create and publish the api
        apiId = createAndPublishAPIWithoutRequireReSubscription(apiRequest, restAPIPublisher);

        endpointURL = Utils.getServiceURLHttps(TestConstant.SAMPLE_API_CONTEXT + "/1.0.0/pet/findByStatus");
        HttpResponse subscriptionResponse = subscribeToAPI(apiId, applicationId,
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, restAPIStore);

        assertEquals(subscriptionResponse.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Subscribing to the API request not successful " + getAPIIdentifierStringFromAPIRequest(
                        apiRequest));
    }

    @Test(description = "Send a request to a subscribed REST API in a published state")
    public void testPublishedStateAPI() throws IOException, MicroGWTestException, InterruptedException {
        Thread.sleep(3000);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatched. Response Data: " + response.getData());
    }

    @Test(description = "Send a request to a blocked  REST API and check 700700 error code is received", dependsOnMethods = "testPublishedStateAPI")
    public void testBlockedStateAPI() throws IOException, MicroGWTestException, InterruptedException {
        changeLCStateAPI(apiId, APILifeCycleAction.BLOCK.getAction(), restAPIPublisher, false);
        Thread.sleep(3000);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SERVICE_UNAVAILABLE,
                "Expected error code is 503, but received the code : " + response.getResponseCode());
        Assert.assertTrue(response.getData().contains("700700") && response.getData().contains("API blocked"),
                "Response message mismatched. Expected the error code 700700, but Response Data: " + response.getData());
    }

    @Test(description = "Re publish the blocked API and test", dependsOnMethods = "testBlockedStateAPI")
    public void testRePublishAPI() throws IOException, MicroGWTestException, InterruptedException {
        changeLCStateAPI(apiId, APILifeCycleAction.RE_PUBLISH.getAction(), restAPIPublisher, false);
        Thread.sleep(3000);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatched. Response Data: " + response.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
