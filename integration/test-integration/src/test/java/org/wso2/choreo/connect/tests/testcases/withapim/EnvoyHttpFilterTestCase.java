/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class EnvoyHttpFilterTestCase extends ApimBaseTest {
    private static final String API_CONTEXT = "envoy_http_filter_api";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "CommonApp";
    String accessTokenProd;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APP_NAME);
        accessTokenProd = StoreUtils.generateUserAccessTokenProduction(apimServiceURLHttps, applicationId,
                user, storeRestClient);
    }

    @Test(description = "Check the headers sent to the backend")
    public void checkHeadersSentToBackend() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessTokenProd);

        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/" + API_VERSION + "/headers");
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");

        // Request headers received by the backend
        JSONObject headersSentToBackend = new JSONObject(response.getData());
        Assert.assertEquals(headersSentToBackend.length(), 10, "Unexpected number of headers received by the backend");

        JSONObject headersToBackend = Utils.changeHeadersToLowerCase(headersSentToBackend);

        Assert.assertNotNull(headersToBackend.get("x-trace-key"));
        Assert.assertNotNull(headersToBackend.get("accept"));
        Assert.assertNotNull(headersToBackend.get("x-request-id"));
        Assert.assertNotNull(headersToBackend.get("x-jwt-assertion"));
        Assert.assertNotNull(headersToBackend.get("x-forwarded-proto"));
        Assert.assertNotNull(headersToBackend.get("host"));
        Assert.assertNotNull(headersToBackend.get("pragma"));
        Assert.assertNotNull(headersToBackend.get("user-agent"));
        Assert.assertNotNull(headersToBackend.get("cache-control"));
        Assert.assertNotNull(headersToBackend.get("x-wso2-ratelimit-api-policy"));

        Assert.assertFalse(headersToBackend.has("x-envoy-original-path"), "x-envoy-original-path not removed");
        Assert.assertFalse(headersToBackend.has("x-wso2-cluster-header"), "x-wso2-cluster-header not removed");
        Assert.assertFalse(headersToBackend.has("x-envoy-expected-rq-timeout-ms"), "x-envoy-expected-rq-timeout-ms not removed");

        // Response headers received by the client
        Map<String, String> headersToClient = response.getHeaders();
        Assert.assertEquals(headersToClient.size(), 4, "Unexpected number of headers received by the client");

        Assert.assertNotNull(headersToClient.get("date"));
        Assert.assertNotNull(headersToClient.get("server"));
        Assert.assertNotNull(headersToClient.get("content-length"));
        Assert.assertNotNull(headersToClient.get("content-type"));
    }
}
