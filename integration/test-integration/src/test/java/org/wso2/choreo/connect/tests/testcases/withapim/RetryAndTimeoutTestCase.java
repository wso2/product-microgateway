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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetryAndTimeoutTestCase extends ApimBaseTest {
    static final String RETRY_API_CONTEXT = "retry";
    static final String TIMEOUT_API_CONTEXT = "timeout";
    static final String RETRY_APP_NAME = "RetryApp";
    static final String TIMEOUT_APP_NAME = "TimeoutApp";

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();
    }

    @Test
    public void testRetry() throws CCTestException, MalformedURLException {
        String applicationId = ApimResourceProcessor.applicationNameToId.get(RETRY_APP_NAME);
        String accessToken = StoreUtils.generateUserAccessTokenProduction(apimServiceURLHttps,
                applicationId, user, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME*2, "Interrupted when waiting for the " +
                "token event to be deployed");

        //Invoke RetryAPI
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        String endpoint = Utils.getServiceURLHttps(RETRY_API_CONTEXT + "/1.0.0/retry-four");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

    @Test
    public void testTimeout() throws CCTestException, MalformedURLException {
        String applicationId = ApimResourceProcessor.applicationNameToId.get(TIMEOUT_APP_NAME);
        String accessToken = StoreUtils.generateUserAccessTokenProduction(apimServiceURLHttps,
                applicationId, user, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME*2, "Interrupted when waiting for the " +
                "token event to be deployed");

        //Invoke TimeoutAPI
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        String endpoint = Utils.getServiceURLHttps(TIMEOUT_API_CONTEXT + "/1.0.0/delay-4");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_GATEWAY_TIMEOUT,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }
}
