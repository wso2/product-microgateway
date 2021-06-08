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

package org.wso2.choreo.connect.tests.testCases.withAPIM;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VhostAPIMTestCase extends ApimBaseTest {

    private String apiId1;
    private Map<String, String> requestHeaders1;
    private Map<String, String> requestHeaders2;
    private String api1endpointURL1;
    private String api1endpointURL2;
    private String api2endpointURL1;
    private String api2endpointURL2;

    private final String LOCALHOST = "localhost";
    private final String US_HOST = "us.wso2.com";

    public static final String SAMPLE_API_1_NAME = "VHostAPI1";
    public static final String SAMPLE_API_1_CONTEXT = "vhostApi1";
    public static final String SAMPLE_API_2_NAME = "VHostAPI2";
    public static final String SAMPLE_API_2_CONTEXT = "vhostApi2";
    public static final String SAMPLE_API_VERSION = "1.0.0";

    //TODO: (renuka) Test with multiple gateway environments
    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();

        // Creating the application
        AppWithConsumerKey appCreationResponse = StoreUtils.createApplicationWithKeys(sampleApp, storeRestClient);
        String applicationId = appCreationResponse.getApplicationId();

        // create the request headers after generating the access token
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appCreationResponse.getConsumerKey(), appCreationResponse.getConsumerSecret(),
                new String[]{"PRODUCTION"}, user, storeRestClient);

        requestHeaders1 = new HashMap<>();
        requestHeaders1.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders1.put(HttpHeaderNames.HOST.toString(), LOCALHOST);

        requestHeaders2 = new HashMap<>();
        requestHeaders2.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders2.put(HttpHeaderNames.HOST.toString(), US_HOST);

        // get a predefined api request
        APIRequest apiRequest1 = PublisherUtils.createSampleAPIRequest(SAMPLE_API_1_NAME, SAMPLE_API_1_CONTEXT,
                SAMPLE_API_VERSION, user.getUserName());

        // get a predefined api request with different resources
        APIRequest apiRequest2 = createCustomAPIRequest(SAMPLE_API_2_NAME, SAMPLE_API_2_CONTEXT,
                SAMPLE_API_VERSION, user.getUserName());

        // create and publish the api
        apiId1 = PublisherUtils.createAndPublishAPI(apiRequest1, LOCALHOST, publisherRestClient, false);
        String apiId2 = PublisherUtils.createAndPublishAPI(apiRequest2, US_HOST, publisherRestClient, false);

        api1endpointURL1 = Utils.getServiceURLHttps(SAMPLE_API_1_CONTEXT + "/1.0.0/pet/findByStatus");
        api1endpointURL2 = Utils.getServiceURLHttps(SAMPLE_API_1_CONTEXT + "/1.0.0/store/inventory");
        api2endpointURL1 = Utils.getServiceURLHttps(SAMPLE_API_2_CONTEXT + "/1.0.0/pet/findByStatus");
        api2endpointURL2 = Utils.getServiceURLHttps(SAMPLE_API_2_CONTEXT + "/1.0.0/store/inventory");

        StoreUtils.subscribeToAPI(apiId1, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
        StoreUtils.subscribeToAPI(apiId2, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
    }

    @Test
    public void testAPIsWithDeployedVhost() throws CCTestException {
        // VHOST: localhost, resource: /pet/findByStatus
        Utils.testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);

        // VHOST: localhost, resource: /store/inventory
        Utils.testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.STORE_INVENTORY_RESPONSE);

        // VHOST: localhost, resource: /pet/findByStatus
        Utils.testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);

        // VHOST: localhost, /store/inventory should be 404 since this resource is not found in the API
        Utils.testInvokeAPI(api2endpointURL2, requestHeaders2, HttpStatus.SC_NOT_FOUND, null);
    }

    @Test (dependsOnMethods = {"testAPIsWithDeployedVhost"})
    public void testUndeployAPIsFromVhost() throws Exception {
        // Un deploy API1
        PublisherUtils.undeployAndDeleteAPIRevisions(apiId1, publisherRestClient);

        // VHOST: localhost - 404
        Utils.testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_NOT_FOUND, null);
        Utils.testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_NOT_FOUND, null);

        // VHOST: localhost, resource: /pet/findByStatus - 200
        Utils.testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    @Test (dependsOnMethods = {"testUndeployAPIsFromVhost"})
    public void testRedeployAPIsFromVhost() throws Exception {
        // Redeploy API1
        PublisherUtils.createAPIRevisionAndDeploy(apiId1, LOCALHOST, publisherRestClient);

        // VHOST: localhost - 200
        Utils.testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);
        Utils.testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.STORE_INVENTORY_RESPONSE);

        // VHOST: localhost, resource: /pet/findByStatus - 200
        Utils.testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsAndAppsFromStore(storeRestClient);
        PublisherUtils.removeAllApisFromPublisher(publisherRestClient);
    }

    private APIRequest createCustomAPIRequest(String apiName, String apiContext, String apiVersion, String provider)
            throws MalformedURLException, APIManagerIntegrationTestException {
        APIRequest apiRequest = new APIRequest(apiName, apiContext,
                new URL(Utils.getDockerMockService2URLHttp(TestConstant.MOCK_BACKEND_BASEPATH)));
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(provider);
        apiRequest.setTiersCollection(TestConstant.API_TIER.UNLIMITED);
        apiRequest.setTier(TestConstant.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/pet/findByStatus");
        apiOperationsDTO1.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);
        return apiRequest;
    }
}
