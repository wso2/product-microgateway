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
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class InterceptorBaseTestCase {
    static final String INVOCATION_CONTEXT = "invocationContext";
    static final String INTERCEPTOR_CONTEXT = "interceptorContext";
    static final String RESPONSE_CODE = "responseCode";
    static final String AUTH_CONTEXT = "authenticationContext";

    String jwtTokenProd;
    String apiName;
    String basePath;
    InterceptorConstants.Handler expectedHandler;
    String statusBodyType;

    @BeforeClass(description = "initialise the setup")
    public void setup() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @BeforeMethod(description = "clear the status of interceptor management service")
    public void clearInterceptorStatus() throws Exception {
        HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/clear-status"));
    }

    JSONObject getInterceptorStatus() throws Exception {
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/status"));
        Assert.assertNotNull(response, "Invalid response from interceptor status");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        return new JSONObject(response.getData());
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
}
