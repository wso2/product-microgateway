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

public class InterceptorResponseFlowTestcase extends InterceptorBaseTestCase {
    private final boolean isRequestFlow = false;

    @BeforeClass
    public void init() {
        apiName = "SwaggerPetstoreResponseIntercept";
        basePath = "/intercept-response";
        expectedHandler = InterceptorConstants.Handler.RESPONSE_ONLY;
        statusBodyType = InterceptorConstants.StatusPayload.RESPONSE_FLOW_REQUEST_BODY;
    }

    @DataProvider(name = "requestBodyProvider")
    public Object[][] requestBodyProvider() {
        String clientReqBody = "{\"name\": \"foo\", \"age\": 16}";
        String interceptorRespBody = "<student><name>Foo</name><age type=\"Y\">16</age></student>";

        // {clientReqBody, interceptorRespBody, isOmitInterceptorRespBody, reqToBackend}
        return new Object[][]{
                // non empty body from interceptor - means update request to backend
                {clientReqBody, interceptorRespBody, false, interceptorRespBody},
                // empty response body from interceptor - means update request to backend as empty
                {clientReqBody, "", false, ""},
                // null response body from interceptor (i.e. {"body": null}) - means do not update request to backend
                {clientReqBody, null, false, clientReqBody},
                // no response from interceptor (i.e. {}) - means do not update request to backend
                {clientReqBody, null, true, clientReqBody}
        };
    }

    @Test(
            description = "Test response body and headers to client with response flow interception",
            dataProvider = "requestBodyProvider"
    )
    public void testResponseToClientInResponseFlowInterception(
            String clientReqBody, String intRespBody, boolean isOmitIntRespBody, String expectedRespToClient)
            throws Exception {

        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        if (!isOmitIntRespBody) {
            if (intRespBody != null) {
                interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString(intRespBody.getBytes()));
            } else {
                interceptorRespBodyJSON.put("body", (String) null);
            }
        }
        interceptorRespBodyJSON.put("headersToAdd", Collections.singletonMap("foo-add", "Header_newly_added"));
        Map<String, String> headersToReplace = new HashMap<>();
        headersToReplace.put("foo-update", "Header_Updated");
        headersToReplace.put("foo-update-not-exist", "Header_Updated_New_Val");
        headersToReplace.put("content-type", "application/xml");
        interceptorRespBodyJSON.put("headersToReplace", headersToReplace);
        interceptorRespBodyJSON.put("headersToRemove", Collections.singletonList("foo-remove"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), isRequestFlow);

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
        int expectedRespCode = StringUtils.isEmpty(expectedRespToClient) ? HttpStatus.SC_NO_CONTENT : HttpStatus.SC_OK;
        Assert.assertEquals(response.getResponseCode(), expectedRespCode, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.RESPONSE_ONLY);

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertFalse(respHeaders.containsKey("foo-remove"), "Failed to remove header");
        Assert.assertEquals(respHeaders.get("foo-add"), "Header_newly_added", "Failed to add new header");
        Assert.assertEquals(respHeaders.get("foo-update"), "Header_Updated", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("foo-update-not-exist"), "Header_Updated_New_Val", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("content-type"), "application/xml", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("foo-keep"), "Header_to_be_kept", "Failed to keep original header");
        // test body
        Assert.assertEquals(response.getData(), expectedRespToClient);
    }

    @Test(description = "Test update response code")
    public void testUpdateResponseCode() throws Exception {
        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString("Not Found".getBytes()));
        interceptorRespBodyJSON.put("responseCode", 404);
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), isRequestFlow);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/update-status-code"), "REQUEST-BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 404, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.RESPONSE_ONLY);
    }

    @Test(description = "Test updating response body when it is not included - invalid operation")
    public void testInvalidOperationUpdateResponseBody() throws Exception {
        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString("INVALID-UPDATE-BODY-OPERATION".getBytes()));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), isRequestFlow);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/echo/both-intercept/resp-body-not-included"), "REQUEST-BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.BOTH);

        // test body: should be equal to the backend response
        Assert.assertEquals(response.getData(), "REQUEST-BODY", "Response body mismatched");
    }

    @Test(description = "Test non JSON response body from interceptor service")
    public void testNonJSONBody() throws Exception {
        // setting response body of interceptor service
        setResponseOfInterceptor("<non>JSON</non>", isRequestFlow);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/echo/123"), "REQUEST-BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.RESPONSE_ONLY);

        // test error code
        JSONObject respJSON = new JSONObject(response.getData());
        Assert.assertEquals(respJSON.getString("code"), "103501", "Error code mismatched for base64 decode error");
    }
}
