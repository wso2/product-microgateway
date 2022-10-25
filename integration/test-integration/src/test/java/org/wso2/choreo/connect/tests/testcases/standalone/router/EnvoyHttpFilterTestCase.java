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

package org.wso2.choreo.connect.tests.testcases.standalone.router;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class EnvoyHttpFilterTestCase {
    private String jwtTokenProd;

    @BeforeClass
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @Test(description = "Test to invoke resource protected with scopes with correct jwt")
    public void checkHeadersSentToBackend() throws Exception {
        HttpResponse response = Utils.invokeApi(jwtTokenProd,"/v2/standard/headers");
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        // Request headers received by the backend
        JSONObject headersSentToBackend = new JSONObject(response.getData());
        Assert.assertEquals(headersSentToBackend.length(), 7, "Unexpected number of headers received by the backend");

        JSONObject headersToBackend = Utils.changeHeadersToLowerCase(headersSentToBackend);

        Assert.assertNotNull(headersToBackend.get("accept"));
        Assert.assertNotNull(headersToBackend.get("x-request-id"));
        Assert.assertNotNull(headersToBackend.get("x-forwarded-proto"));
        Assert.assertNotNull(headersToBackend.get("host"));
        Assert.assertNotNull(headersToBackend.get("pragma"));
        Assert.assertNotNull(headersToBackend.get("user-agent"));
        Assert.assertNotNull(headersToBackend.get("cache-control"));

        Assert.assertFalse(headersToBackend.has("x-envoy-original-path"), "x-envoy-original-path not removed");
        Assert.assertFalse(headersToBackend.has("x-wso2-cluster-header"), "x-wso2-cluster-header not removed");
        Assert.assertFalse(headersToBackend.has("x-envoy-expected-rq-timeout-ms"), "x-envoy-expected-rq-timeout-ms not removed");

        // Response headers received by the client
        Map<String, String> headersToClient = response.getHeaders();
        Assert.assertEquals(headersToClient.size(), 5, "Unexpected number of headers received by the client");

        Assert.assertNotNull(headersToClient.get("date"));      // = Fri, 15 Apr 2022 05:10:41 GMT
        Assert.assertNotNull(headersToClient.get("server"));    // = envoy
        Assert.assertNotNull(headersToClient.get("content-length"));
        Assert.assertNotNull(headersToClient.get("content-type"));
        Assert.assertNotNull(headersToClient.get("vary"));     // header comes due to compression filter to indicate data compression capability
    }

    /*
     * Related to https://github.com/wso2/product-microgateway/issues/3009
     */
    @Test(description = "Confirm header case change - router to backend")
    public void confirmHeaderCaseChange_RouterToBackend() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);

        // Add custom headers
        headers.put("X-Header-One", "X-Header-One-Value");
        headers.put("x-header-two", "x-header-two-value");
        headers.put("X-HEADER-THREE", "X-HEADER-THREE-VALUE");

        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(
                Utils.getServiceURLHttps("/v2/standard/headers"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        JSONObject headersSentToBackend = new JSONObject(response.getData());
        // Headers that were originally sent by the client
        Assert.assertFalse(headersSentToBackend.has("X-Header-One"), "X-Header-One sent to the backend");
        Assert.assertFalse(headersSentToBackend.has("x-header-two"), "x-header-two sent to the backend");
        Assert.assertFalse(headersSentToBackend.has("X-HEADER-THREE"), "X-HEADER-THREE sent to the backend");

        // Headers received by the backend
        // Has got converted to first letter upper case, remaining letters lowercase
        Assert.assertTrue(headersSentToBackend.has("X-header-one"), "X-header-one not sent to the backend");
        Assert.assertTrue(headersSentToBackend.has("X-header-two"), "X-header-two not sent to the backend");
        Assert.assertTrue(headersSentToBackend.has("X-header-three"), "X-header-three not sent to the backend");

        // Case of the value has not changed
        Assert.assertEquals(headersSentToBackend.get("X-header-one"), "X-Header-One-Value",
                "X-header-one header value has changed");
        Assert.assertEquals(headersSentToBackend.get("X-header-two"), "x-header-two-value",
                "X-header-two header value has changed");
        Assert.assertEquals(headersSentToBackend.get("X-header-three"), "X-HEADER-THREE-VALUE",
                "X-header-three header value has changed");
    }

    /*
     * Related to https://github.com/wso2/product-microgateway/issues/3009
     */
    @Test(description = "Confirm header case change - backend to client")
    public void confirmHeaderCaseChange_BackendToClient() throws Exception {
        HttpResponse response = Utils.invokeApi(jwtTokenProd, "/v2/standard/headers-from-backend");
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        Map<String, String> headersToClient = response.getHeaders();
        // Headers that were originally sent by the backend
        Assert.assertFalse(headersToClient.containsKey("X-Header-One"), "X-Header-One sent to the client");
        Assert.assertTrue(headersToClient.containsKey("x-header-two"), "x-header-two not sent to the client");
        Assert.assertFalse(headersToClient.containsKey("X-HEADER-THREE"), "X-HEADER-THREE sent to the client");

        // Headers received by the client
        // Has got converted to all lowercase letters
        Assert.assertTrue(headersToClient.containsKey("x-header-one"), "x-header-one not sent to the client");
        Assert.assertTrue(headersToClient.containsKey("x-header-two"), "x-header-two not sent to the client");
        Assert.assertTrue(headersToClient.containsKey("x-header-three"), "x-header-three not sent to the client");

        // Case of the value has not changed
        Assert.assertEquals(headersToClient.get("x-header-one"), "X-Header-One-Value",
                "X-header-one header value has changed");
        Assert.assertEquals(headersToClient.get("x-header-two"), "x-header-two-value",
                "X-header-two header value has changed");
        Assert.assertEquals(headersToClient.get("x-header-three"), "X-HEADER-THREE-VALUE",
                "X-header-three header value has changed");
    }

    @Test(description = "Confirm that the case of the query parameters have not changed - router to backend")
    public void confirmQueryParamCaseUnchanged() throws Exception {
        Map<String, String> headers = new HashMap<>();
        EchoResponse response = Utils.invokeEchoGet("/v2/standard", "/echo-full?"
                        + "param1=value1&"
                        + "PARAM2=VALUE2&"
                        + "QueryParam3=QueryValue3",
                headers, jwtTokenProd);

        // Query params received by the backend
        Map<String, String> queryParams = response.getQuery();
        Assert.assertNotEquals(queryParams.size(), 0);

        // Case not changed
        Assert.assertTrue(queryParams.containsKey("param1"), "param1 not sent to the client");
        Assert.assertTrue(queryParams.containsKey("PARAM2"), "PARAM2 not sent to the client");
        Assert.assertTrue(queryParams.containsKey("QueryParam3"), "QueryParam3 not sent to the client");

        Assert.assertEquals(queryParams.get("param1"), "value1",
                "param1 value has changed");
        Assert.assertEquals(queryParams.get("PARAM2"), "VALUE2",
                "PARAM2 value has changed");
        Assert.assertEquals(queryParams.get("QueryParam3"), "QueryValue3",
                "QueryParam3 value has changed");
    }
}
