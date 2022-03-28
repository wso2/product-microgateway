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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Test case to check the behaviour when APIs are blocked from APIM publisher lifecycle tab.
 *
 */
public class BlockedApiTestCase extends ApimBaseTest {
    private static final String API_NAME = "BlockedAPI";
    private static final String API_CONTEXT = "blocked";
    private static final String APPLICATION_NAME = "BlockedApiApp";
    private final Map<String, String> requestHeaders = new HashMap<>();
    String internalKey;

    private String apiId;
    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // Get App ID and API ID
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        API api = new API();
        api.setContext(API_CONTEXT + "/1.0.0");
        api.setName(API_NAME);
        api.setVersion("1.0.0");
        api.setProvider("admin");

        internalKey = TokenUtil.getJWT(api, null, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    @Test(description = "Send a request to a subscribed REST API in a published state")
    public void testPublishedStateAPI() throws CCTestException, InterruptedException {
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatched. Response Data: " + response.getData());
    }

    @Test(description = "Send a request to a blocked  REST API and check 700700 error code is received",
            dependsOnMethods = "testPublishedStateAPI")
    public void testBlockedStateAPI() throws CCTestException, InterruptedException {
        PublisherUtils.changeLCStateAPI(apiId, APILifeCycleAction.BLOCK.getAction(), publisherRestClient, false);
        Thread.sleep(3000);
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SERVICE_UNAVAILABLE,
                "Expected error code is 503, but received the code : " + response.getResponseCode());
        Assert.assertTrue(response.getData().contains("700700") && response.getData().contains("API blocked"),
                "Response message mismatched. Expected the error code 700700, but Response Data: " + response.getData());
    }

    @Test(description = "Send a request to a blocked  REST API and check 700700 error code is received", dependsOnMethods = "testBlockedStateAPI")
    public void testBlockedStateAPIWithInternalKey() throws CCTestException, InterruptedException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Internal-Key", internalKey);
        Thread.sleep(3000);
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SERVICE_UNAVAILABLE,
                "Expected error code is 503, but received the code : " + response.getResponseCode());
        Assert.assertTrue(response.getData().contains("700700") && response.getData().contains("API blocked"),
                "Response message mismatched. Expected the error code 700700, but Response Data: " + response.getData());
    }

    @Test(description = "Re publish the blocked API and test", dependsOnMethods = {"testBlockedStateAPI",
            "testBlockedStateAPIWithInternalKey"})
    public void testRePublishAPI() throws CCTestException, InterruptedException {
        PublisherUtils.changeLCStateAPI(apiId, APILifeCycleAction.RE_PUBLISH.getAction(), publisherRestClient, false);
        Thread.sleep(3000);
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatched. Response Data: " + response.getData());
    }

    @Test(description = "Re publish the blocked API and test", dependsOnMethods = "testRePublishAPI")
    public void testRePublishAPIWithInternalKey() throws CCTestException, InterruptedException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Internal-Key", internalKey);
        Thread.sleep(3000);
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatched. Response Data: " + response.getData());
    }
}
