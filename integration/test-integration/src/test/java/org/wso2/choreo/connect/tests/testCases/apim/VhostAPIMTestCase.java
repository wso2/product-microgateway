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
import io.netty.handler.codec.http.HttpHeaderNames;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.APIMLifecycleBaseTest;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class VhostAPIMTestCase extends APIMLifecycleBaseTest {

    private APIRequest apiRequest1;
    private APIRequest apiRequest2;
    private String apiId1;
    private String apiId2;
    private String applicationId;
    private Map<String, String> requestHeaders1;
    private Map<String, String> requestHeaders2;
    private String api1endpointURL1;
    private String api1endpointURL2;
    private String api2endpointURL1;
    private String api2endpointURL2;

    private final String LOCALHOST = "localhost";
    private final String US_HOST = "us.wso2.com";

    //TODO: (renuka) Test with multiple gateway environments
    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.init();

        // Creating the application
        ApplicationCreationResponse appCreationResponse = createApplicationWithKeys(
                "VhostTestApp", restAPIStore);
        applicationId = appCreationResponse.getApplicationId();

        // create the request headers after generating the access token
        String accessToken = generateUserAccessToken(appCreationResponse.getConsumerKey(),
                appCreationResponse.getConsumerSecret(),
                new String[]{"PRODUCTION"}, user, restAPIStore);

        requestHeaders1 = new HashMap<>();
        requestHeaders1.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders1.put(HttpHeaderNames.HOST.toString(), LOCALHOST);

        requestHeaders2 = new HashMap<>();
        requestHeaders2.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders2.put(HttpHeaderNames.HOST.toString(), US_HOST);

        // get a predefined api request
        apiRequest1 = getAPIRequest(TestConstant.SAMPLE_API_NAME);
        apiRequest1.setProvider(user.getUserName());

        // get a predefined api request
        apiRequest2 = getAPIRequest(TestConstant.SAMPLE_API2_NAME);
        apiRequest2.setProvider(user.getUserName());

        // create and publish the api
        apiId1 = createAndPublishAPIWithoutRequireReSubscription(apiRequest1, LOCALHOST, restAPIPublisher);
        apiId2 = createAndPublishAPIWithoutRequireReSubscription(apiRequest2, US_HOST, restAPIPublisher);

        api1endpointURL1 = Utils.getServiceURLHttps(TestConstant.SAMPLE_API_CONTEXT + "/1.0.0/pet/findByStatus");
        api1endpointURL2 = Utils.getServiceURLHttps(TestConstant.SAMPLE_API_CONTEXT + "/1.0.0/store/inventory");
        api2endpointURL1 = Utils.getServiceURLHttps(TestConstant.SAMPLE_API2_CONTEXT + "/1.0.0/pet/findByStatus");
        api2endpointURL2 = Utils.getServiceURLHttps(TestConstant.SAMPLE_API2_CONTEXT + "/1.0.0/store/inventory");

        HttpResponse subscriptionResponse1 = subscribeToAPI(apiId1, applicationId,
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, restAPIStore);
        assertEquals(subscriptionResponse1.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Subscribing to the API request not successful " + getAPIIdentifierStringFromAPIRequest(
                        apiRequest1));
        HttpResponse subscriptionResponse2 = subscribeToAPI(apiId2, applicationId,
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, restAPIStore);
        assertEquals(subscriptionResponse1.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Subscribing to the API request not successful " + getAPIIdentifierStringFromAPIRequest(
                        apiRequest2));
    }

    @Test
    public void testAPIsWithDeployedVhost() throws IOException {
        // VHOST: localhost, resource: /pet/findByStatus
        testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);

        // VHOST: localhost, resource: /store/inventory
        testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.STORE_INVENTORY_RESPONSE);

        // VHOST: localhost, resource: /pet/findByStatus
        testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);

        // VHOST: localhost, /store/inventory should be 404 since this resource is not found in the API
        testInvokeAPI(api2endpointURL2, requestHeaders2, HttpStatus.SC_NOT_FOUND, null);
    }

    @Test (dependsOnMethods = {"testAPIsWithDeployedVhost"})
    public void testUndeployAPIsFromVhost() throws Exception {
        // Un deploy API1
        undeployAndDeleteAPIRevisions(apiId1, restAPIPublisher);

        // VHOST: localhost - 404
        testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_NOT_FOUND, null);
        testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_NOT_FOUND, null);

        // VHOST: localhost, resource: /pet/findByStatus - 200
        testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    @Test (dependsOnMethods = {"testUndeployAPIsFromVhost"})
    public void testRedeployAPIsFromVhost() throws Exception {
        // Redeploy API1
        createAPIRevisionAndDeploy(apiId1, LOCALHOST, restAPIPublisher);

        // VHOST: localhost - 200
        testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);
        testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.STORE_INVENTORY_RESPONSE);

        // VHOST: localhost, resource: /pet/findByStatus - 200
        testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    private void testInvokeAPI(String endpoint, Map<String,String> headers, int statusCode, String responseBody) throws IOException {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                isResponseAvailable(endpoint, headers));
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), statusCode,
                "Status code mismatched. Response code: " + response.getResponseCode());
        if (statusCode != HttpStatus.SC_NOT_FOUND) {
            Assert.assertEquals(response.getData(), responseBody,
                    "Response message mismatched. Response Data: " + response.getData());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
