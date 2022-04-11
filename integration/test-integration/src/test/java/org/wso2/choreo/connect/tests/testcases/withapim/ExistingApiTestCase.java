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
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class ExistingApiTestCase extends ApimBaseTest {
    private static final String API_CONTEXT = "existing_api";
    private static final String APP_NAME = "ExistingApiApp";
    private String applicationId;
    private String accessTokenProd;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();

        applicationId = ApimResourceProcessor.applicationNameToId.get(APP_NAME);
        accessTokenProd = StoreUtils.generateUserAccessTokenProduction(apimServiceURLHttps, applicationId,
                user, storeRestClient);
    }

    @Test
    public void testExistingApiWithProdKey() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessTokenProd);
        headers.put(HttpHeaderNames.HOST.toString(), "localhost");

        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

    @Test
    public void testExistingApiWithSandboxKey() throws Exception {
        String accessToken = StoreUtils.generateUserAccessTokenSandbox(apimServiceURLHttps, applicationId,
                user, storeRestClient);

        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        headers.put(HttpHeaderNames.HOST.toString(), "localhost");

        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
    }

    @Test
    public void checkHeadersSentToBackend() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessTokenProd);

        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/headers");
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + endpoint + " HttpResponse ");
        JSONObject responseJSON = new JSONObject(response.getData());
        Assert.assertEquals(responseJSON.length(), 10, "Unexpected number of headers received by the backend");

        Assert.assertNotNull(responseJSON.get("X-trace-key"));
        Assert.assertNotNull(responseJSON.get("Accept"));
        Assert.assertNotNull(responseJSON.get("X-request-id"));
        Assert.assertNotNull(responseJSON.get("X-jwt-assertion"));
        Assert.assertNotNull(responseJSON.get("X-forwarded-proto"));
        Assert.assertNotNull(responseJSON.get("Host"));
        Assert.assertNotNull(responseJSON.get("Pragma"));
        Assert.assertNotNull(responseJSON.get("X-envoy-original-path"));
        Assert.assertNotNull(responseJSON.get("User-agent"));
        Assert.assertNotNull(responseJSON.get("Cache-control"));

        Assert.assertFalse(responseJSON.has("x-wso2-cluster-header"));
        Assert.assertFalse(responseJSON.has("x-envoy-expected-rq-timeout-ms"));
    }
}
