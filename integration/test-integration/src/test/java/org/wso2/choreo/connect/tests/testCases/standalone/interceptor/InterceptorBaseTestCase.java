/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testCases.standalone.interceptor;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorBaseTestCase {
    static final String INVOCATION_CONTEXT = "invocationContext";

    String jwtTokenProd;
    String apiName;
    String basePath;
    InterceptorConstants.Handler expectedHandler;
    String statusBodyType;

    @BeforeClass(description = "initialise the setup")
    void setup() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @BeforeMethod(description = "clear the status of interceptor management service")
    void clearInterceptorStatus() throws Exception {
        HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/clear-status"));
    }

    public void testRequestToInterceptorService() throws Exception {
        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/xml");
        String body = "<student><name>Foo</name><age type=\"Y\">16</age></student>";
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/echo/123"), body, headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = new JSONObject(getInterceptorStatus());
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, expectedHandler); // check Request Only, Response Only, Both or None

        JSONObject interceptRespBodyJSON = new JSONObject(status.getString(statusBodyType));
        // invocation context
        testInvocationContext(interceptRespBodyJSON, Arrays.asList("GET", "POST"), "POST", basePath + "/echo/123", "/echo/{id}");
        // headers
        headers.remove(HttpHeaderNames.AUTHORIZATION.toString()); // check without auth header
        testInterceptorHeaders(interceptRespBodyJSON, headers, true);
        // body
        testInterceptorBody(interceptRespBodyJSON, body, true);
    }

    public void testResponseFromInterceptorService(
            String intReqBody, String intRespBody, boolean isOmitIntRespBody, String expectedPayload) throws Exception {

        // JSON request to XML backend
        // setting response body of interceptor service
        JSONObject intRespBodyJSON = new JSONObject();
        if (!isOmitIntRespBody) {
            if (intRespBody != null) {
                intRespBodyJSON.put("body", Base64.getEncoder().encodeToString(intRespBody.getBytes()));
            } else {
                intRespBodyJSON.put("body", (String) null);
            }
        }
        intRespBodyJSON.put("headersToAdd", Collections.singletonMap("foo-add", "Header_newly_added"));
        Map<String, String> headersToReplace = new HashMap<>();
        headersToReplace.put("foo-update", "Header_Updated");
        headersToReplace.put("foo-update-not-exist", "Header_Updated_New_Val");
        headersToReplace.put("content-type", "application/xml");
        intRespBodyJSON.put("headersToReplace", headersToReplace);
        intRespBodyJSON.put("headersToRemove", Collections.singletonList("foo-remove"));
        setResponseOfInterceptor(intRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/json");
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/echo/123"), intReqBody, headers);

        Assert.assertNotNull(response);
        int expectedRespCode = StringUtils.isEmpty(expectedPayload) ? HttpStatus.SC_NO_CONTENT : HttpStatus.SC_OK;
        Assert.assertEquals(response.getResponseCode(), expectedRespCode, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = new JSONObject(getInterceptorStatus());
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertFalse(respHeaders.containsKey("foo-remove"), "Failed to remove header");
        Assert.assertEquals(respHeaders.get("foo-add"), "Header_newly_added", "Failed to add new header");
        Assert.assertEquals(respHeaders.get("foo-update"), "Header_Updated", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("foo-update-not-exist"), "Header_Updated_New_Val", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("content-type"), "application/xml", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("foo-keep"), "Header_to_be_kept", "Failed to keep original header");
        // test body
        Assert.assertEquals(response.getData(), expectedPayload);
    }

    String getInterceptorStatus() throws Exception {
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/status"));
        Assert.assertNotNull(response, "Invalid response from interceptor status");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        return response.getData();
    }

    void setResponseOfInterceptor(String responseBody, boolean isRequestFlow) throws Exception {
        String servicePath = isRequestFlow ? "interceptor/request" : "interceptor/response";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        HttpResponse response = HttpClientRequest.doPost(Utils.getMockInterceptorManagerHttp(servicePath),
                responseBody, headers);
        Assert.assertNotNull(response, "Invalid response when updating response body of interceptor");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    void testInterceptorHandler(String actualHandler, InterceptorConstants.Handler expectedHandler) {
        Assert.assertEquals(actualHandler, expectedHandler.toString(), "Invalid interceptor handler");
    }

    void testInvocationContext(JSONObject bodyJSON, List<String> supportedMethods, String method, String path, String pathTemplate) {
        JSONObject invocationCtx = bodyJSON.getJSONObject(INVOCATION_CONTEXT);
        Assert.assertNotNull(invocationCtx, "Interceptor invocation context not found");

        Assert.assertEquals(invocationCtx.getString("apiName"), apiName, "API name mismatch");
        Assert.assertEquals(invocationCtx.getString("apiVersion"), "1.0.5", "API version mismatch");
        Assert.assertEquals(invocationCtx.getString("scheme"), "https", "Scheme mismatch");
        Assert.assertEquals(invocationCtx.getString("vhost"), "localhost", "Vhost mismatch");
        Assert.assertEquals(invocationCtx.getString("method"), method, "HTTP method mismatch");
        Assert.assertEquals(invocationCtx.getString("path"), path, "Resource path mismatch");
        Assert.assertEquals(invocationCtx.getString("pathTemplate"), pathTemplate, "Resource path template mismatch");
        Assert.assertEquals(invocationCtx.getString("basePath"), basePath, "Base path mismatch");

        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("source")), "Source not found");
        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("requestId")), "Request ID not found");
        Assert.assertEquals(Arrays.asList(invocationCtx.getString("supportedMethods").split(" ")), supportedMethods, "HTTP supported method mismatch");
    }

    void testInterceptorHeaders(JSONObject bodyJSON, Map<String, String> expectedHeaders, boolean isRequestFlow) {
        String jsonKey = isRequestFlow ? "requestHeaders" : "responseHeaders";
        JSONObject headersJSON = bodyJSON.getJSONObject(jsonKey);
        expectedHeaders.forEach((key, value) -> {
            String actualVal = headersJSON.getString(key);
            Assert.assertEquals(actualVal, value, String.format("Header mismatch for header key: %s", key));
        });
    }

    void testInterceptorBody(JSONObject bodyJSON, String expectedBody, boolean isRequestFlow) {
        String jsonKey = isRequestFlow ? "requestBody" : "responseBody";
        String base64EncodedBody = Base64.getEncoder().encodeToString(expectedBody.getBytes());
        Assert.assertEquals(bodyJSON.getString(jsonKey), base64EncodedBody, "Request body mismatch");
    }
}
