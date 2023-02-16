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

package org.wso2.choreo.connect.tests.testcases.standalone.apipolicy;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class APIPolicyPerOperationTestCase {
    private static final String REQUEST_HEADER = "Request-header"; // envoy changes request case header to first letter upper and the rest to lower
    private static final String REQUEST_HEADER_VALUE = "Request-header-value";
    private static final String REQUEST_HEADER_BY_POLICY = "Request-Header-By-Policy";
    private static final String REQUEST_HEADER_BY_POLICY_VALUE = "Request-Header-By-Policy-value";
    private static final String RESPONSE_HEADER = "response-header"; // All lower case
    private static final String RESPONSE_HEADER_VALUE = "response-header-value";
    private static final String RESPONSE_HEADER_BY_POLICY = "response-header-by-policy"; // All lower case
    private static final String RESPONSE_HEADER_BY_POLICY_VALUE = "Response-Header-By-Policy-value";
    private static final String ORIGIN_HEADER = "Origin";
    private static final String ACCESS_CONTROL_REQUEST_HEADERS_HEADER = "Access-Control-Request-Headers";
    private static final String ACCESS_CONTROL_REQUEST_METHOD_HEADER = "Access-Control-Request-Method";

    private String jwtTokenProd;
    private static final String basePath = "/api-policy-per-operation";
    private static final String endpoint = basePath + "/echo-full/policies";
    private static final String endpointWithCaptureGroups = basePath + "/echo-full/rewrite-policy-with-capture-groups/shops/shop1234.xyz/pets/pet890/orders";
    private Map<String, String> headers;
    private final String queryParams = "?foo1=bar1&foo2=bar2";

    @BeforeClass(description = "Get Prod token")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @BeforeMethod
    void setClientRequestInfo() {
        headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("Sample1", "Sample Value");
        headers.put("Sample2", "Sample Value");
        headers.put(REQUEST_HEADER, REQUEST_HEADER_VALUE);
        headers.put("Set-headers", RESPONSE_HEADER); // backend will set the header by reading "set_headers"
    }

    @Test
    public void testGET_NoAPIPolicies() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps(endpoint + queryParams), headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getHeaders().getFirst(REQUEST_HEADER), REQUEST_HEADER_VALUE, "Request header must exist");
        Assert.assertEquals(httpResponse.getHeaders().get(RESPONSE_HEADER), RESPONSE_HEADER_VALUE, "Response header must exist");
        Assert.assertFalse(echoResponse.getHeaders().containsKey(REQUEST_HEADER_BY_POLICY), "Request policy header must not exist");
        Assert.assertFalse(httpResponse.getHeaders().containsKey(RESPONSE_HEADER_BY_POLICY), "Response policy header must not exist");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test
    public void testPOST_RequestFlowHeaderUpdate() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doPost(
                Utils.getServiceURLHttps(endpoint + queryParams), "Hello World!", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertFalse(echoResponse.getHeaders().containsKey(REQUEST_HEADER),
                "Request Header has not been removed");
        Assert.assertEquals(httpResponse.getHeaders().get(RESPONSE_HEADER), RESPONSE_HEADER_VALUE,
                "Response header must exist");
        Assert.assertEquals(echoResponse.getHeaders().getFirst(REQUEST_HEADER_BY_POLICY), REQUEST_HEADER_BY_POLICY_VALUE,
                "Request policy header has not been added");
        Assert.assertFalse(httpResponse.getHeaders().containsKey(RESPONSE_HEADER_BY_POLICY),
                "Response policy header must not exist");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test
    public void testDELETE_ResponseFlowHeaderUpdate() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doDelete(
                Utils.getServiceURLHttps(endpoint + queryParams), headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getHeaders().getFirst(REQUEST_HEADER), REQUEST_HEADER_VALUE,
                "Request header must exist");
        Assert.assertFalse(httpResponse.getHeaders().containsKey(RESPONSE_HEADER),
                "Response Header has not been removed");
        Assert.assertFalse(echoResponse.getHeaders().containsKey(REQUEST_HEADER_BY_POLICY),
                "Request policy header must not exist");
        Assert.assertEquals(httpResponse.getHeaders().get(RESPONSE_HEADER_BY_POLICY), RESPONSE_HEADER_BY_POLICY_VALUE,
                "Response policy header has not been added");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test
    public void testPATCH_AddQuery() throws Exception {
        org.apache.http.HttpResponse httpResponse = HttpsClientRequest.doPatch(
                Utils.getServiceURLHttps(endpoint + queryParams), "Hello World!", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getQuery().get("helloQ1"), "worldQ1",
                "Query Param 1 has not been added");
        Assert.assertEquals(echoResponse.getQuery().get("helloQ2"), "worldQ2",
                "Query Param 2 has not been added");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test
    public void testPUT_RewriteMethodAndPath() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doPut(
                Utils.getServiceURLHttps(endpoint + queryParams), "Hello World!", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.GET.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path");
        assertOriginalClientRequestInfo(echoResponse);
    }

    // With capture groups

    @Test(description = "Test rewrite path API Policy with capture groups")
    public void testPUT_WithCaptureGroups_RewriteMethodAndPath_WhenCurrentMethodNotProvided() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doPut(
                Utils.getServiceURLHttps(endpointWithCaptureGroups + queryParams), "Hello World!", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.GET.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/pets/pet890.pets/hello-shops/abcd-shops/shop1234");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite path API Policy with capture groups with trailing slash in path")
    public void testPUT_WithCaptureGroups_WithTrailingSlash_RewritePath() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doPut(
                Utils.getServiceURLHttps(endpointWithCaptureGroups + "/" + queryParams), "Hello World!", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.GET.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/pets/pet890.pets/hello-shops/abcd-shops/shop1234");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite path and discard queries in rewrite path API Policies")
    public void testGET_WithCaptureGroups_RewritePathAndDiscardQueries() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps(endpointWithCaptureGroups + queryParams), headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.DELETE.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/pets/pet890.pets/hello-shops/abcd-shops/shop1234");
        Assert.assertTrue(echoResponse.getQuery().isEmpty(), "Query params has not been discarded");
    }

    @Test(description = "Test rewrite path and discard queries in rewrite path API Policies with trailing slash in path")
    public void testGET_WithCaptureGroups_WithTrailingSlash_RewritePathAndDiscardQueries() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps(endpointWithCaptureGroups + "/" + queryParams), headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.DELETE.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/pets/pet890.pets/hello-shops/abcd-shops/shop1234");
        Assert.assertTrue(echoResponse.getQuery().isEmpty(), "Query params has not been discarded");
    }

    @Test(description = "Test all API Policies together")
    public void testPOST_WithCaptureGroups_AllPoliciesTogether() throws Exception {
        headers.put("foo", "bar"); // this header is validated in OPA policy
        HttpResponse httpResponse = HttpsClientRequest.doPost(
                Utils.getServiceURLHttps(endpointWithCaptureGroups + queryParams), "Hello World!", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);

        Assert.assertFalse(echoResponse.getHeaders().containsKey(REQUEST_HEADER),
                "Request Header has not been removed");
        Assert.assertEquals(echoResponse.getHeaders().getFirst(REQUEST_HEADER_BY_POLICY), REQUEST_HEADER_BY_POLICY_VALUE,
                "Request policy header has not been added");
        Assert.assertFalse(httpResponse.getHeaders().containsKey(RESPONSE_HEADER),
                "Response Header has not been removed");
        Assert.assertEquals(httpResponse.getHeaders().get(RESPONSE_HEADER_BY_POLICY), RESPONSE_HEADER_BY_POLICY_VALUE,
                "Response policy header has not been added");
        Assert.assertEquals(echoResponse.getQuery().get("newQ1"), "newQ1Value",
                "Query param has not been added");
        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.PUT.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/pets/pet890.pets/hello-shops/abcd-shops/shop1234");
        Assert.assertEquals(echoResponse.getData(), "Hello World!");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test CORS Options call when all API Policies together")
    public void testAllPoliciesTogetherWithOptionsCall() throws MalformedURLException, CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put(ORIGIN_HEADER, "https://wso2am:9443");
        headers.put(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST");
        headers.put(ACCESS_CONTROL_REQUEST_HEADERS_HEADER, "internal-key");
        HttpResponse httpResponse = HttpsClientRequest.doOptions(
                Utils.getServiceURLHttps(endpointWithCaptureGroups + queryParams), headers);
        // CORS is disabled in the config TOML file of the test cases, hence testing the CORS fail scenario.
        Assert.assertNotNull(httpResponse);
        Assert.assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_NO_CONTENT);
        Assert.assertNotNull(httpResponse.getHeaders());
        Assert.assertNotNull(httpResponse.getHeaders().get("allow"), "HTTP header 'allow' not found for CORS failure request");
        Assert.assertTrue(httpResponse.getHeaders().get("allow").contains("POST"), "POST value not found in 'allow' header");
    }

    @Test(description = "Test HTTP method rewrite and check if query params exists")
    public void testGET_MethodRewriteOnly() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps(basePath + "/echo-full/method-rewrite-only" + queryParams), headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);
        
        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.POST.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/method-rewrite-only");
        Assert.assertEquals(echoResponse.getQuery().get("foo1"), "bar1",
                "Query Param 1 has not been added");
        Assert.assertEquals(echoResponse.getQuery().get("foo2"), "bar2",
                "Query Param 2 has not been added");
    }

    private void assertOriginalClientRequestInfo(EchoResponse echoResponse) {
        Assert.assertEquals(echoResponse.getHeaders().getFirst("Sample1"), "Sample Value",
                "Original header value in client request is missing");
        Assert.assertEquals(echoResponse.getHeaders().getFirst("Sample2"), "Sample Value",
                "Original header value in client request is missing");
        Assert.assertEquals(echoResponse.getQuery().get("foo1"), "bar1",
                "Original query value in client request is missing");
        Assert.assertEquals(echoResponse.getQuery().get("foo2"), "bar2",
                "Original query value in client request is missing");
    }

}
