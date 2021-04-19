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
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.APIMLifecycleBaseTest;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class SubscriptionValidationTestCase extends APIMLifecycleBaseTest {

    private APIRequest apiRequest;
    private String apiId;
    private String applicationId;
    private Map<String, String> requestHeaders;
    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.init();

        // Creating the application
        ApplicationCreationResponse appCreationResponse = createApplicationWithKeys(
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
    }

    @Test(description = "Send a request to a unsubscribed REST API and check if the API invocation is forbidden")
    public void testAPIsForInvalidSubscription() throws MicroGWTestException, MalformedURLException {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                isResponseAvailable(endpointURL, requestHeaders));

        org.wso2.choreo.connect.tests.util.HttpResponse response;
        try {
            response = HttpsClientRequest.doGet(endpointURL, requestHeaders);
        } catch (IOException e) {
            throw new MicroGWTestException("Error occurred while invoking the endpoint: " + endpointURL, e);
        }
        Assert.assertTrue(response.getResponseCode() == HttpStatus.SC_FORBIDDEN && response.getData()
                                  .contains(TestConstant.RESOURCE_FORBIDDEN_CODE),
                          "The user invoking the API should not be granted access to the required resource. Response "
                                  + "Data:" + response.getData());
    }

    @Test(description = "Send a request to a subscribed REST API returning 200 and check if the expected result is "
            + "received", dependsOnMethods = "testAPIsForInvalidSubscription")
    public void testAPIsForValidSubscription() throws IOException, MicroGWTestException {
        HttpResponse subscriptionResponse = subscribeToAPI(apiId, applicationId,
                                                           TestConstant.SUBSCRIPTION_TIER.UNLIMITED, restAPIStore);

        assertEquals(subscriptionResponse.getResponseCode(), HttpStatus.SC_SUCCESS,
                     "Subscribing to the API request not successful " + getAPIIdentifierStringFromAPIRequest(
                             apiRequest));

        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                isResponseAvailable(endpointURL, requestHeaders));
        HttpClientRequest.doGet("http://localhost:2399/analytics/clear", new HashMap<>());
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest.doGet(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                            "Valid subscription should be able to invoke the associated API");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                            "Response message mismatched. Response Data: " + response.getData());
        try {
            // To publish analytics it takes at most one second.
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        org.wso2.choreo.connect.tests.util.HttpResponse analyticsResponse =
                HttpClientRequest.doGet("http://localhost:2399/analytics/get", new HashMap<>());
        Assert.assertNotNull(analyticsResponse);
        Assert.assertTrue(analyticsResponse.getData().contains(TestConstant.SAMPLE_API_NAME),
                analyticsResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
