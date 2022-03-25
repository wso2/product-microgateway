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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InterceptorRequestFlowTestcase extends InterceptorBaseTestCase {
    @BeforeClass
    public void init() {
        apiName = "SwaggerPetstoreRequestIntercept";
        basePath = "/intercept-request";
        expectedHandler = InterceptorConstants.Handler.REQUEST_ONLY;
        statusBodyType = InterceptorConstants.StatusPayload.REQUEST_FLOW_REQUEST_BODY;
    }

    @DataProvider(name = "requestBodyProvider")
    public Object[][] requestBodyProvider() {
        String clientReqBody = "{\"name\": \"foo\", \"age\": 16}";
        String interceptorRespBody = "<student><name>Foo</name><age type=\"Y\">16</age></student>";

        // {clientReqBody, interceptorRespBody, isOmitInterceptorRespBody, expectedBody}
        return new Object[][]{
                // non-empty body from interceptor - means update request to backend
                {clientReqBody, interceptorRespBody, false, interceptorRespBody},
                // empty request from client and non-empty body from interceptor - means update request to backend
                {"", interceptorRespBody, false, interceptorRespBody},
                // empty response body from interceptor - means update request to backend as empty
                {clientReqBody, "", false, ""},
                // null response body from interceptor (i.e. {"body": null}) - means do not update request to backend
                {clientReqBody, null, false, clientReqBody},
                // no response from interceptor (i.e. {}) - means do not update request to backend
                {clientReqBody, null, true, clientReqBody}
        };
    }

    @DataProvider(name = "directRespondRequestBodyProvider")
    public Object[][] directRespondRequestBodyProvider() {
        String clientReqBody = "{\"name\": \"foo\", \"age\": 16}";
        String interceptorRespBody = "{\"message\": \"This is direct responded\"}";

        // {clientReqBody, interceptorRespBody, isOmitInterceptorRespBody, clientRespBody}
        return new Object[][]{
                // non-empty body from interceptor - means update request to client
                {clientReqBody, interceptorRespBody, false, interceptorRespBody},
                // empty request from client and non-empty body from interceptor - means update request to client
                {"", interceptorRespBody, false, interceptorRespBody},
                // empty response body from interceptor - means update request to client as empty
                {clientReqBody, "", false, ""},
                // null response body from interceptor (i.e. {"body": null}) - means do not update request to client
                {clientReqBody, null, false, ""},
                // no response from interceptor (i.e. {}) - means do not update request to client
                {clientReqBody, null, true, ""}
        };
    }

    @Test(
            description = "Test request body and headers to backend service with request flow interception",
            dataProvider = "requestBodyProvider"
    )
    public void testRequestToBackendServiceInRequestFlowInterception(
            String clientReqBody, String interceptorRespBody, boolean isOmitInterceptorRespBody, String expectedBody)
            throws Exception {

        // JSON request to XML backend
        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        if (!isOmitInterceptorRespBody) {
            if (interceptorRespBody != null) {
                interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString(interceptorRespBody.getBytes()));
            } else {
                interceptorRespBodyJSON.put("body", (String) null);
            }
        }
        interceptorRespBodyJSON.put("headersToAdd", Collections.singletonMap("foo-add", "Header_newly_added"));
        Map<String, String> headersToReplace = new HashMap<>();
        headersToReplace.put("foo-update", "Header_Updated");
        headersToReplace.put("foo-update-not-exist", "Header_Updated_New_Val");
        headersToReplace.put("x-wso2-cluster-header", "foo-invalid-attempt"); // this header should be discarded
        headersToReplace.put("x-wso2-foo-header", "foo-invalid-update"); // this header should be discarded
        headersToReplace.put("x-envoy-foo-header", "foo-invalid-update"); // this header should be discarded
        headersToReplace.put("content-type", "application/xml");
        interceptorRespBodyJSON.put("headersToReplace", headersToReplace);
        interceptorRespBodyJSON.put("headersToRemove", Collections.singletonList("foo-remove"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/json");
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/echo/123"), clientReqBody, headers);

        Assert.assertNotNull(response);
        int expectedRespCode = StringUtils.isEmpty(expectedBody) ? HttpStatus.SC_NO_CONTENT : HttpStatus.SC_OK;
        Assert.assertEquals(response.getResponseCode(), expectedRespCode, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
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

        // discarded headers
        Assert.assertFalse(respHeaders.containsKey("x-wso2-foo-header"), "Failed to remove discarded header");
        Assert.assertFalse(respHeaders.containsKey("x-envoy-foo-header"), "Failed to remove discarded header");

        // test body
        Assert.assertEquals(response.getData(), expectedBody);
    }

    @Test(
            description = "Direct respond when response interception is enabled",
            dataProvider = "directRespondRequestBodyProvider"
    )
    public void directRespondWhenResponseInterceptionEnabled(
            String clientReqBody, String interceptorRespBody, boolean isOmitInterceptorRespBody, String clientRespBody)
            throws Exception {

        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        interceptorRespBodyJSON.put("directRespond", true);
        if (!isOmitInterceptorRespBody) {
            if (interceptorRespBody != null) {
                interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString(interceptorRespBody.getBytes()));
            } else {
                interceptorRespBodyJSON.put("body", (String) null);
            }
        }
        // only headersToAdd is considered when direct respond
        Map<String, String> headersToAdd = new HashMap<>();
        headersToAdd.put("foo-add", "Header_newly_added");
        headersToAdd.put("content-type", "application/json");
        interceptorRespBodyJSON.put("headersToAdd", headersToAdd);
        interceptorRespBodyJSON.put("headersToReplace", Collections.singletonMap("foo-ignored", "Header_not_added"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-client-header", "Header_discard_when_respond");
        headers.put("content-type", "application/json");
        // this is not an echo server, so if request goes to backend it will respond with different payload.
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/resp-intercept-enabled"), clientReqBody, headers);

        Assert.assertNotNull(response);
        int expectedRespCode = StringUtils.isEmpty(clientRespBody) ? HttpStatus.SC_NO_CONTENT : HttpStatus.SC_OK;
        Assert.assertEquals(response.getResponseCode(), expectedRespCode, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertFalse(respHeaders.containsKey("foo-client-header"), "Responding client headers back");
        Assert.assertFalse(respHeaders.containsKey("foo-ignored"), "Should only support add headers");
        Assert.assertEquals(respHeaders.get("foo-add"), "Header_newly_added",
                "Failed to add new header");
        Assert.assertEquals(respHeaders.get("content-type"), "application/json",
                "Failed to replace header");
        // test body
        Assert.assertEquals(response.getData(), clientRespBody);
    }

    @DataProvider(name = "directRespondProvider")
    public Object[][] directRespondProvider() {
        // {interceptorRespBody}
        return new Object[][]{
                {"UPDATED BODY"},
                {""},
        };
    }

    @Test(
            description = "Test direct respond with different response code",
            dataProvider = "directRespondProvider"
    )
    public void testDirectRespondWithResponseCode(String interceptorRespBody) throws Exception {
        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString(interceptorRespBody.getBytes()));
        interceptorRespBodyJSON.put("directRespond", true);
        interceptorRespBodyJSON.put("responseCode", 202);
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/resp-intercept-enabled"), "INITIAL BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 202, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        Assert.assertEquals(response.getData(), interceptorRespBody);
    }

    @Test(description = "Test dynamic endpoints")
    public void testDynamicEndpoints() throws Exception {
        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        // test updating body is also work with dynamic endpoint
        interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString("UPDATED BODY".getBytes()));
        interceptorRespBodyJSON.put("headersToReplace", Collections.singletonMap("x-wso2-cluster-header", "foo-invalid-attempt"));
        interceptorRespBodyJSON.put("dynamicEndpoint", Collections.singletonMap("endpointName", "myDynamicEndpoint"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/dynamic-ep-echo"), "INITIAL BODY", headers);
        Assert.assertNotNull(response);

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        Assert.assertEquals(response.getData(), "UPDATED BODY");
    }

    @Test(description = "Enforcer denied request when response interceptor enabled")
    public void enforcerDeniedRequestWhenResponseInterceptionEnabled() throws Exception {
        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer INVALID-XXX-XXX-XXX-XXX-XXX-XXX-TOKEN");
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/resp-intercept-enabled"), headers);
        Assert.assertNotNull(response);

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.NONE); // no interceptor handle the request
    }
}
