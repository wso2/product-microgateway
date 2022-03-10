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

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class InterceptorServiceNotAvailableTestCase extends InterceptorBaseTestCase {
    @BeforeClass
    public void init() {
        apiName = "SwaggerPetstoreRequestIntercept";
    }

    @Override
    public void clearInterceptorStatus() {
        // do not clear interceptor service, since the server is not available here.
    }

    @Test(description = "Test request flow interceptor service not available and only headers are requested")
    public void testRequestFlowInterceptorServiceNotAvailableAndIncludeHeadersOnly() throws Exception {
        // no request body included
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        String basePath = "/intercept-request";
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/headers-only"), "REQUEST_BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 500, "Response code mismatched");

        // test error code
        JSONObject respJSON = new JSONObject(response.getData());
        Assert.assertEquals(respJSON.getString("code"), "103500", "Error code mismatched for interceptor service not available");
    }

    @Test(description = "Test response flow interceptor service not available and only headers are requested")
    public void testResponseFlowInterceptorServiceNotAvailableAndIncludeHeadersOnly() throws Exception {
        // no request body included
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        String basePath = "/intercept-response";
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/headers-only"), "REQUEST_BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 500, "Response code mismatched");

        // test error code
        // We can't update response body, when it is not buffered, so directly send response body to client
        // Should return error code "103500" but return "102503" since interceptor service not available
        JSONObject respJSON = new JSONObject(response.getData());
        Assert.assertEquals(respJSON.getString("code"), "102503", "Error code mismatched for upstream connect error");
    }

    @Test(description = "Test response flow interceptor service not available and response body is included")
    public void testResponseFlowInterceptorServiceNotAvailableAndIncludeBodyIncluded() throws Exception {
        // no request body included
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        String basePath = "/intercept-response";
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                basePath + "/pet/findByStatus/body-included"), "REQUEST_BODY", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 500, "Response code mismatched");

        // test error code
        JSONObject respJSON = new JSONObject(response.getData());
        Assert.assertEquals(respJSON.getString("code"), "103500", "Error code mismatched for interceptor service not available");
    }
}
