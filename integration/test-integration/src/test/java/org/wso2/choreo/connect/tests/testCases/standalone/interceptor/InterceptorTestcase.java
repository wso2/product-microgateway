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
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorTestcase {
    protected String jwtTokenProd;

    private static final String INVOCATION_CONTEXT = "invocationContext";

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @BeforeMethod(description = "clear the status of interceptor management service")
    void clearInterceptorStatus() throws Exception {
        HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/clear-status"));
    }

    @Test(description = "Test request body of Interceptor Service in Request Flow")
    public void testRequestOfInterceptorServiceInRequestFlow() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/xml");
        String body = "<student><name>Foo</name><age type=\"Y\">16</age></student";
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/intercept-request/echo/123"), body, headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        JSONObject status = new JSONObject(getInterceptorStatus());
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        checkHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        JSONObject reqFlowBodyJSON = new JSONObject(status.getString(InterceptorConstants.StatusPayload.REQUEST_FLOW_REQUEST_BODY));
        // invocation context
        // TODO: (renuka) change lua script: method -> supportedMethods and add current method from headers
        checkInvocationContext(reqFlowBodyJSON, Arrays.asList("GET", "POST"), "POST", "/intercept-request/echo/123", "/echo/{id}");
        // headers
        headers.remove(HttpHeaderNames.AUTHORIZATION.toString()); // check without auth header
        checkHeaders(reqFlowBodyJSON,headers, false);
        // body
        checkBody(reqFlowBodyJSON, body, false);
    }

    private String getInterceptorStatus() throws Exception {
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/status"));
        Assert.assertNotNull(response, "Invalid response from interceptor status");
        return response.getData();
    }

    private void checkHandler(String actualHandler, InterceptorConstants.Handler expectedHandler) {
        Assert.assertEquals(actualHandler, expectedHandler.toString(),
                String.format("Invalid interceptor handler, expected: %s, got: %s", expectedHandler, actualHandler)
        );
    }

    private void checkInvocationContext(JSONObject bodyJSON, List<String> supportedMethods, String method, String path, String pathTemplate) {
        JSONObject invocationCtx = bodyJSON.getJSONObject(INVOCATION_CONTEXT);
        Assert.assertNotNull(invocationCtx, "Interceptor invocation context not found");

        Assert.assertEquals(invocationCtx.getString("apiName"), "SwaggerPetstoreRequestIntercept", "API name mismatch");
        Assert.assertEquals(invocationCtx.getString("apiVersion"), "1.0.5", "API version mismatch");
        Assert.assertEquals(invocationCtx.getString("scheme"), "https", "Scheme mismatch");
        Assert.assertEquals(invocationCtx.getString("vhost"), "localhost", "Vhost mismatch");
//        Assert.assertEquals(invocationCtx.getString("method"), method, "HTTP method mismatch");
        Assert.assertEquals(invocationCtx.getString("path"), path, "Resource path mismatch");
        Assert.assertEquals(invocationCtx.getString("pathTemplate"), pathTemplate, "Resource path template mismatch");
        Assert.assertEquals(invocationCtx.getString("basePath"), "/intercept-request", "Base path mismatch");

        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("source")), "Source not found");
        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("requestId")), "Request ID not found");
        Assert.assertEquals(Arrays.asList(invocationCtx.getString("method").split(" ")), supportedMethods, "HTTP supported method mismatch"); // TODO: change this
    }

    private void checkHeaders(JSONObject bodyJSON, Map<String, String> expectedHeaders, boolean isResponseFlow) {
        String jsonKey = isResponseFlow ? "responseHeaders" : "requestHeaders";
        JSONObject headersJSON = bodyJSON.getJSONObject(jsonKey);
        expectedHeaders.forEach((key, value) -> {
            String actualVal = headersJSON.getString(key);
            Assert.assertEquals(actualVal, value, String.format(
                    "Header mismatch for header key: %s, required: %s but found: %s", key, value, actualVal));
        });
    }

    private void checkBody(JSONObject bodyJSON, String expectedBody, boolean isResponseFlow) {
        String jsonKey = isResponseFlow ? "responseBody" : "requestBody";
        String base64EncodedBody = Base64.getEncoder().encodeToString(expectedBody.getBytes());
        Assert.assertEquals(bodyJSON.getString(jsonKey), base64EncodedBody, "Request body mismatch");
    }
}
