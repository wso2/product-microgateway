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
import io.netty.handler.codec.http.HttpHeaderNames;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class VhostApimTestCase extends ApimBaseTest {
    private final String LOCALHOST = "localhost";
    private final String US_HOST = "us.wso2.com";

    public static final String API_1_NAME = "VHostAPI1";
    public static final String API_2_NAME = "VHostAPI2";
    public static final String APPLICATION_NAME = "VHostApp";

    private String apiId1;
    private String apiContext1;
    private String apiContext2;
    private Map<String, String> requestHeaders1;
    private Map<String, String> requestHeaders2;
    private String api1endpointURL1;
    private String api1endpointURL2;
    private String api2endpointURL1;
    private String api2endpointURL2;

    //TODO: (renuka) Test with multiple gateway environments
    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();

        applicationNameToId = findApplicationId(new String[]{APPLICATION_NAME});
        String applicationId = applicationNameToId.get(APPLICATION_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);

        //Get details of created APIs
        apiNameToInfo = findApiId(new String[]{API_1_NAME, API_2_NAME});
        APIInfoDTO apiInfoDTO1 = apiNameToInfo.get(API_1_NAME);
        APIInfoDTO apiInfoDTO2 = apiNameToInfo.get(API_2_NAME);
        apiId1 = apiInfoDTO1.getId();
        apiContext1 = apiInfoDTO1.getContext();
        apiContext2 = apiInfoDTO2.getContext();

        requestHeaders1 = new HashMap<>();
        requestHeaders1.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders1.put(HttpHeaderNames.HOST.toString(), LOCALHOST);

        requestHeaders2 = new HashMap<>();
        requestHeaders2.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders2.put(HttpHeaderNames.HOST.toString(), US_HOST);

        api1endpointURL1 = Utils.getServiceURLHttps(apiContext1 + "/1.0.0/pet/findByStatus");
        api1endpointURL2 = Utils.getServiceURLHttps(apiContext1 + "/1.0.0/store/inventory");
        api2endpointURL1 = Utils.getServiceURLHttps(apiContext2 + "/1.0.0/pet/findByStatus");
        api2endpointURL2 = Utils.getServiceURLHttps(apiContext2 + "/1.0.0/store/inventory");
    }

    @Test
    public void testAPIsWithDeployedVhost() throws CCTestException {
        // VHOST: localhost, resource: /pet/findByStatus
        testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);

        // VHOST: localhost, resource: /store/inventory
        testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.STORE_INVENTORY_RESPONSE);

        // VHOST: localhost, resource: /pet/findByStatus, requestHeader_host: us.wso2.com
        testInvokeAPI(api1endpointURL1, requestHeaders2, HttpStatus.SC_NOT_FOUND, ResponseConstants.RESPONSE_BODY);

        // VHOST: localhost, resource: /pet/findByStatus
        testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);

        // VHOST: localhost, /store/inventory should be 404 since this resource is not found in the API
        testInvokeAPI(api2endpointURL2, requestHeaders2, HttpStatus.SC_NOT_FOUND, null);
    }

    @Test (dependsOnMethods = {"testAPIsWithDeployedVhost"})
    public void testUndeployAPIsFromVhost() throws Exception {
        // Un deploy API1
        PublisherUtils.undeployAndDeleteAPIRevisions(apiId1, publisherRestClient);

        // VHOST: localhost - 404
        testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_NOT_FOUND, null);
        testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_NOT_FOUND, null);

        // VHOST: localhost, resource: /pet/findByStatus - 200
        testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    @Test (dependsOnMethods = {"testUndeployAPIsFromVhost"})
    public void testRedeployAPIsFromVhost() throws Exception {
        // Redeploy API1
        PublisherUtils.createAPIRevisionAndDeploy(apiId1, LOCALHOST, publisherRestClient);

        // VHOST: localhost - 200
        testInvokeAPI(api1endpointURL1, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);
        testInvokeAPI(api1endpointURL2, requestHeaders1, HttpStatus.SC_SUCCESS, ResponseConstants.STORE_INVENTORY_RESPONSE);

        // VHOST: localhost, resource: /pet/findByStatus - 200
        testInvokeAPI(api2endpointURL1, requestHeaders2, HttpStatus.SC_SUCCESS, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    public static void testInvokeAPI(String endpoint, Map<String,String> headers, int expectedStatusCode,
                                     String expectedResponseBody) throws CCTestException {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResponseAvailable(endpoint, headers));
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), expectedStatusCode,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
        if (expectedStatusCode != HttpStatus.SC_NOT_FOUND) {
            Assert.assertEquals(response.getData(), expectedResponseBody, "Response message mismatched. Endpoint:"
                    + endpoint + " HttpResponse ");
        }
    }
}
