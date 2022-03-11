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

package org.wso2.choreo.connect.tests.testcases.standalone.interceptor;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorServiceRequestBodyTestCase extends InterceptorBaseTestCase {
    @DataProvider(name = "interceptFlowProvider")
    public Object[][] interceptFlowProvider() {
        //  {basePath, expectedHandler, isRequestFlow}
        return new Object[][]{
                {"SwaggerPetstoreRequestIntercept", "/intercept-request", InterceptorConstants.Handler.REQUEST_ONLY, true},
                {"SwaggerPetstoreResponseIntercept", "/intercept-response", InterceptorConstants.Handler.RESPONSE_ONLY, false}
        };
    }

    @Test(description = "Test request body to interceptor service", dataProvider = "interceptFlowProvider")
    public void testRequestToInterceptorService(String apiName, String basePath, InterceptorConstants.Handler expectedHandler,
                                                boolean isRequestFlow) throws Exception {
        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-one", "Foo One");
        headers.put("foo-two", "Foo Two");
        headers.put("content-type", "application/xml");
        String body = "<student><name>Foo</name><age type=\"Y\">16</age></student>";
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/echo/123"), body, headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, expectedHandler); // check Request Only, Response Only, Both or None

        String statusBodyType = isRequestFlow ? InterceptorConstants.StatusPayload.REQUEST_FLOW_REQUEST_BODY :
                InterceptorConstants.StatusPayload.RESPONSE_FLOW_REQUEST_BODY;
        JSONObject interceptReqBodyJSON = new JSONObject(status.getString(statusBodyType));

        // invocation context
        testInvocationContext(interceptReqBodyJSON, apiName, basePath, Arrays.asList("GET", "POST"), "POST",
                basePath + "/echo/123", "/echo/{id}");

        // headers
        headers.remove(HttpHeaderNames.AUTHORIZATION.toString()); // check without auth header
        testInterceptorHeaders(interceptReqBodyJSON, headers, true);
        if (!isRequestFlow) { // check both request and response headers in response flow
            testInterceptorHeaders(interceptReqBodyJSON, headers, false);
        }

        // body
        testInterceptorBody(interceptReqBodyJSON, body, isRequestFlow);

        // status code only available in response flow
        if (!isRequestFlow) {
            Assert.assertEquals(interceptReqBodyJSON.getInt(RESPONSE_CODE), 200, "Response code mismatched in request body");
        }
    }

    @Test(description = "Test interceptor context")
    public void testInterceptorContext() throws Exception {
        // JSON request to XML backend
        // setting response body of interceptor service
        JSONObject interceptorReqBodyJSON = new JSONObject();
        JSONObject interceptorContext = new JSONObject();
        interceptorContext.put("foo-key1", "foo-val1");
        interceptorContext.put("foo-key2", "foo-val2");
        interceptorReqBodyJSON.put("interceptorContext", interceptorContext.toString());
        setResponseOfInterceptor(interceptorReqBodyJSON.toString(), true);

        // setting client
        String basePath = "/intercept-response";
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/interceptor-context"), "REQUEST-BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.BOTH);

        // test interceptor context
        String respFlowReqBody = status.getString(InterceptorConstants.StatusPayload.RESPONSE_FLOW_REQUEST_BODY);
        JSONObject contextFromRespFlow = new JSONObject(new JSONObject(respFlowReqBody).getString(INTERCEPTOR_CONTEXT));
        Assert.assertEquals(contextFromRespFlow.getString("foo-key1"), "foo-val1", "Interceptor context read by interceptor service is not matching");
        Assert.assertEquals(contextFromRespFlow.getString("foo-key2"), "foo-val2", "Interceptor context read by interceptor service is not matching");
    }

    void testInvocationContext(JSONObject bodyJSON, String apiName, String basePath, List<String> supportedMethods,
                               String method, String path, String pathTemplate) {
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

        JSONObject authContext = invocationCtx.getJSONObject(AUTH_CONTEXT);
        Assert.assertEquals(authContext.getString("tokenType"), "JWT", "Auth context, token type mismatched");
        Assert.assertEquals(authContext.getString("token"), jwtTokenProd, "Auth context, token mismatched");
        Assert.assertEquals(authContext.getString("keyType"), TestConstant.KEY_TYPE_PRODUCTION, "Auth context, key type mismatched");
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
