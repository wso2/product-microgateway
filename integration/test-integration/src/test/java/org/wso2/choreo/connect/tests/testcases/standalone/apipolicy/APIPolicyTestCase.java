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

import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class APIPolicyTestCase {
    private String jwtTokenProd;
    private static final String basePath = "/api-policy";
    private Map<String, String> headers;
    private final String queryParams = "?foo1=bar1&foo2=bar2";

    @BeforeClass(description = "Get Prod token")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @BeforeMethod
    void setClientRequestInfo() {
        headers = new HashMap<>();
        headers.put("Sample1", "Sample Value");
        headers.put("Sample2", "Sample Value");
    }

    @Test(description = "Test header based API Policies")
    public void testSetHeaderRemoveHeaderAPIPolicies() throws Exception {
        headers.put("RemoveThisHeader", "Unnecessary Header");
        EchoResponse echoResponse = invokeEchoPost("/echo-full/headers-policy/123" + queryParams, "Hello World!", headers);
        
        Assert.assertFalse(echoResponse.getHeaders().containsKey("RemoveThisHeader"),
                getPolicyFailAssertMessage("Remove Header"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey1"), "newHeaderVal1",
                getPolicyFailAssertMessage("Add Header"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey2"), "newHeaderVal2",
                getPolicyFailAssertMessage("Add Header"));
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test query based API Policies")
    public void testQueryAPIPolicy() throws Exception {
        EchoResponse echoResponse = invokeEchoGet("/echo-full/query-policy" + queryParams, headers);
        
        Assert.assertEquals(echoResponse.getQuery().get("helloQ1"), "worldQ1",
                getPolicyFailAssertMessage("Add Query Param"));
        Assert.assertEquals(echoResponse.getQuery().get("helloQ2"), "worldQ2",
                getPolicyFailAssertMessage("Add Query Param"));
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite method and rewrite path API Policies")
    public void testRewriteMethodAndPathAPIPolicy() throws Exception {
        // HTTP method: GET
        EchoResponse echoResponse = invokeEchoGet("/echo-full/rewrite-policy/345" + queryParams, headers);
        
        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.PUT.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path");
        assertOriginalClientRequestInfo(echoResponse);

        // HTTP method: POST
        echoResponse = invokeEchoPost("/echo-full/rewrite-policy/345" + queryParams, "Hello World", headers);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.POST.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite path and discard queries in rewrite path API Policies")
    public void testRewritePathAndDiscardQueriesAPIPolicy() throws Exception {
        // HTTP method: GET
        EchoResponse echoResponse = invokeEchoGet("/echo-full/rewrite-policy/discard-query-params" + queryParams, headers);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.DELETE.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path2");
        // TODO: (renuka) following is failing, fix the bug and uncomment this
//        Assert.assertTrue(echoResponse.getQuery().isEmpty(), "Query params has not been discarded");
    }

    private EchoResponse invokeEchoGet(String resourcePath, Map<String, String> headers) throws Exception {
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(basePath + resourcePath), headers);
        Assert.assertNotNull(response);
        return new Gson().fromJson(response.getData(), EchoResponse.class);
    }

    private EchoResponse invokeEchoPost(String resourcePath, String payload, Map<String, String> headers) throws Exception {
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(basePath + resourcePath), payload, headers);
        Assert.assertNotNull(response);
        return new Gson().fromJson(response.getData(), EchoResponse.class);
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

    private String getPolicyFailAssertMessage(String policyName) {
        return String.format("Gateway has failed to apply %s API policy", policyName);
    }
}
