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

package org.wso2.choreo.connect.tests.testcases.standalone.apiDeploy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AllHttpMethodsForWildcardTestCase {
    private String jwtTokenProd;
    private final Map<String, String> headers = new HashMap<>();
    private static final String API_CONTEXT = "/all_http_methods_for_wildcard";

    @BeforeClass(description = "Get Prod token")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
    }

    @Test(description = "test path with trailing slash for GET")
    public void testTrailingSlashGET() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/","Request path mismatched");

        EchoResponse echoResponse0 = Utils.invokeEchoGet(API_CONTEXT, "",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse0.getPath(), "/v2/echo-full","Request path mismatched");

        EchoResponse echoResponse1 = Utils.invokeEchoGet(API_CONTEXT, "/test",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse1.getPath(), "/v2/echo-full/test","Request path mismatched");
    }

    @Test(description = "test path with trailing slash for POST")
    public void testTrailingSlashPOST() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoPost(API_CONTEXT, "/",
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/","Request path mismatched");

        EchoResponse echoResponse0 = Utils.invokeEchoPost(API_CONTEXT, "",
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(echoResponse0.getPath(), "/v2/echo-full","Request path mismatched");

        EchoResponse echoResponse1 = Utils.invokeEchoPost(API_CONTEXT, "/test",
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(echoResponse1.getPath(), "/v2/echo-full/test","Request path mismatched");
    }

    @Test(description = "test path with trailing slash for PUT")
    public void testTrailingSlashPUT() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doPut(
                Utils.getServiceURLHttps(API_CONTEXT + "/"), "Hello", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/","Request path mismatched");

        HttpResponse httpResponse1 = HttpsClientRequest.doPut(
                Utils.getServiceURLHttps(API_CONTEXT + "/test"), "Hello", headers);
        EchoResponse echoResponse1 = Utils.extractToEchoResponse(httpResponse1);
        Assert.assertEquals(echoResponse1.getPath(), "/v2/echo-full/test","Request path mismatched");
    }

    @Test(description = "test path with trailing slash for PATCH")
    public void testTrailingSlashPATCH() throws Exception {
        java.net.http.HttpResponse<String> httpResponse = HttpsClientRequest.doPatch(
                Utils.getServiceURLHttps(API_CONTEXT + "/"), "Hello", headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/","Request path mismatched");

        java.net.http.HttpResponse<String> httpResponse1 = HttpsClientRequest.doPatch(
                Utils.getServiceURLHttps(API_CONTEXT + "/test"), "Hello", headers);
        EchoResponse echoResponse1 = Utils.extractToEchoResponse(httpResponse1);
        Assert.assertEquals(echoResponse1.getPath(), "/v2/echo-full/test","Request path mismatched");
    }

    @Test(description = "test path with trailing slash for DELETE")
    public void testTrailingSlashDELETE() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doDelete(
                Utils.getServiceURLHttps(API_CONTEXT + "/"), headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/","Request path mismatched");

        HttpResponse httpResponse1 = HttpsClientRequest.doDelete(
                Utils.getServiceURLHttps(API_CONTEXT + "/test"), headers);
        EchoResponse echoResponse1 = Utils.extractToEchoResponse(httpResponse1);
        Assert.assertEquals(echoResponse1.getPath(), "/v2/echo-full/test","Request path mismatched");
    }

    @Test(description = "test path with trailing slash for OPTIONS")
    public void testTrailingSlashOPTIONS() throws Exception {
        Object[] expectedAllowArray = {"DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"};

        HttpResponse httpResponse = HttpsClientRequest.doOptions(
                Utils.getServiceURLHttps(API_CONTEXT + "/"), headers);
        Assert.assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_NO_CONTENT, "Response code mismatched");
        Assert.assertNotNull(httpResponse.getHeaders().get("allow"));
        Object[] responseAllowArray = Arrays.stream(httpResponse.getHeaders().get("allow")
                .split(", ")).sorted().toArray();
        Assert.assertEquals(responseAllowArray, expectedAllowArray,"Responded with invalid allow header");

        HttpResponse httpResponse1 = HttpsClientRequest.doOptions(
                Utils.getServiceURLHttps(API_CONTEXT + "/test"), headers);
        Assert.assertEquals(httpResponse1.getResponseCode(), HttpStatus.SC_NO_CONTENT, "Response code mismatched");
        Assert.assertNotNull(httpResponse1.getHeaders().get("allow"));
        Object[] responseAllowArray1 = Arrays.stream(httpResponse1.getHeaders().get("allow")
                .split(", ")).sorted().toArray();
        Assert.assertEquals(responseAllowArray1, expectedAllowArray,"Responded with invalid allow header");
    }

    @Test(description = "test path with trailing slash for HEAD")
    public void testTrailingSlashHEAD() throws Exception {
        HttpResponse httpResponse = HttpsClientRequest.doHead(
                Utils.getServiceURLHttps(API_CONTEXT + "/"), headers);
        EchoResponse echoResponse = Utils.extractToEchoResponse(httpResponse);
        Assert.assertNull(echoResponse, "HEAD request returned a payload");

        HttpResponse httpResponse1 = HttpsClientRequest.doHead(
                Utils.getServiceURLHttps(API_CONTEXT + "/test"), headers);
        EchoResponse echoResponse1 = Utils.extractToEchoResponse(httpResponse1);
        Assert.assertNull(echoResponse1, "HEAD request returned a payload");
    }
}
