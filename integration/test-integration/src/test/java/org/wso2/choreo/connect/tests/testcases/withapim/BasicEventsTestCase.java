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
package org.wso2.choreo.connect.tests.testcases.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.common.net.HttpHeaders;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicEventsTestCase extends ApimBaseTest {
    private static final String API_NAME = "BasicEventsApi";
    private static final String API_CONTEXT = "basicEvents";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "BasicEventsApp";

    String apiId;
    String revisionUUID;
    String applicationId;
    APIRequest apiRequest;
    Map<String, String> headers;
    String endpoint;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();
    }

    @Test
    public void createDeployPublishAndInvokeAPI() throws Exception {
        //Create, deploy and publish API - only to specifically test events.
        // For other testcases the json is used to create APIs, Apps, Subscriptions
        apiRequest = PublisherUtils.createSampleAPIRequest(API_NAME, API_CONTEXT, API_VERSION, user.getUserName());
        apiId = PublisherUtils.createAPI(apiRequest, publisherRestClient);
        revisionUUID = PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        PublisherUtils.publishAPI(apiId, API_NAME, publisherRestClient);

        //Create App. Subscribe.
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        AppWithConsumerKey appWithConsumerKey = StoreUtils.createApplicationWithKeys(app, storeRestClient);
        applicationId = appWithConsumerKey.getApplicationId();
        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted when waiting for the " +
                "subscription to be deployed");
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{}, user, storeRestClient);

        //Invoke API
        headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

    @Test(dependsOnMethods = "createDeployPublishAndInvokeAPI")
    void undeployAndTestResponse404() throws Exception {
        PublisherUtils.undeployAPI(apiId, revisionUUID, publisherRestClient);
        HttpResponse response = HttpsClientRequest.retryUntil404(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

    @Test(dependsOnMethods = "undeployAndTestResponse404")
    void redeployAndInvokeAPI() throws Exception {
        PublisherUtils.deployRevision(apiId, revisionUUID, "localhost", publisherRestClient);
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

    @Test(dependsOnMethods = "redeployAndInvokeAPI")
    void addNewResourceAndInvokeNewRevision() throws Exception {
        APIOperationsDTO apiOperation = new APIOperationsDTO();
        apiOperation.setVerb("GET");
        apiOperation.setTarget("/pet/findByTags");
        apiOperation.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = apiRequest.getOperationsDTOS();
        operationsDTOS.add(apiOperation);
        apiRequest.setOperationsDTOS(operationsDTOS);
        publisherRestClient.updateAPI(apiRequest, apiId);
        revisionUUID = PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);

        String newResource = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByTags");
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(newResource, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + newResource + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + newResource + " HttpResponse ");
    }

    @Test(dependsOnMethods = "addNewResourceAndInvokeNewRevision")
    void deleteNewResourceAndInvokeNewRevision() throws Exception {
        List<APIOperationsDTO> oldOperationsDTOS = apiRequest.getOperationsDTOS();
        List<APIOperationsDTO> newOperationsDTOS = new ArrayList<>();
        newOperationsDTOS.add(oldOperationsDTOS.get(0));
        apiRequest.setOperationsDTOS(newOperationsDTOS);
        publisherRestClient.updateAPI(apiRequest, apiId);
        revisionUUID = PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);

        String newResource = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByTags");
        HttpResponse response = HttpsClientRequest.retryUntil404(newResource, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + newResource + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,
                "Status code mismatched. Endpoint:" + newResource + " HttpResponse ");

        HttpResponse response2 = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response2, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

    @Test(dependsOnMethods = "deleteNewResourceAndInvokeNewRevision")
    void testDeleteEvents() throws Exception{
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting to subscription delete event");

        HttpResponse response1 = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response1, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response1.getResponseCode(), HttpStatus.SC_FORBIDDEN,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");

        storeRestClient.removeApplicationById(applicationId);
        publisherRestClient.deleteAPI(apiId);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting to API delete event");
        HttpResponse response2 = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response2, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_NOT_FOUND,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

}
