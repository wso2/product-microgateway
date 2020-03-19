/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.security;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class APIInvokeWithBasicAuthTestCase extends APIInvokeWithOAuthTestCase {

    @Test(description = "Test API invocation with a JWT token")
    public void testApiInvokeFailWithJWT() throws Exception {
        //test  endpoint with jwt token
        invoke(jwtTokenProd, 401);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }
    }

    private void invoke(String token, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/basic-menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    @Test(description = "Test API invocation with Basic Auth")
    public void testApiInvokePassWithBasicAuth() throws Exception {
        //Valid Credentials
        String originalInput = "generalUser1:password";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        //test endpoint
        invokeBasic(basicAuthToken, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        invokeWithLowercaseBasic(basicAuthToken, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        // user stored with sha256 password in the default-test-config.conf
        String sha256HashedUser = "user1:password";
        basicAuthToken = Base64.getEncoder().encodeToString(sha256HashedUser.getBytes());
        invokeBasic(basicAuthToken, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        // user stored with sha512 password in the default-test-config.conf
        String sha512HashedUser = "user2:password1";
        basicAuthToken = Base64.getEncoder().encodeToString(sha512HashedUser.getBytes());
        invokeBasic(basicAuthToken, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
    }

    @Test(description = "Test API invocation with Basic Auth")
    public void testApiInvokeWithoutPassword() throws Exception {
        //Valid Credentials
        String originalInput = "generalUser1: ";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        //test endpoint
        invokeBasic(basicAuthToken, 401);
    }

    @Test(description = "Test API invocation with Basic Auth")
    public void testApiInvokeFailWithInvalidPassword() throws Exception {
        //Valid Credentials
        String originalInput = "generalUser1:Invalid";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        //test endpoint
        invokeBasic(basicAuthToken, 401);
    }

    @Test(description = "Test API invocation with Basic Auth")
    public void testApiInvokeFailWithInvalidFormat() throws Exception {
        //Valid Credentials
        String originalInput = "generalUser1password";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        //test endpoint
        invokeBasic(basicAuthToken, 401);
    }

    private void invokeBasic(String token, String responseData, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/basic-menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), responseData);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    private void invokeWithLowercaseBasic(String token, String responseData, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "basic " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/basic-menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), responseData);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    private void invokeBasic(String token, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/basic-menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

}