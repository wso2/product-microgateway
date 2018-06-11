/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.tests.services;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.IntegrationTestCase;
import org.wso2.micro.gateway.tests.context.MicroGWTestException;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Testing the pizza_shack api service for authentication error messages
 */
public class AuthenticationFailureTestCase extends IntegrationTestCase {
    private ServerInstance microGWServer;
    private ServerInstance kmServer;
    private String keyManagerServiceDir = "src" + File.separator + "test" + File.separator + "resources" + File
            .separator + "keyManager";

    @BeforeClass
    private void setup() throws Exception {
        microGWServer = ServerInstance.initMicroGwServer(TestConstant.GATEWAY_LISTENER_PORT);
        kmServer = ServerInstance.initMicroGwServer(TestConstant.KM_LISTENER_PORT);
        String relativePath = new File(
                "src" + File.separator + "test" + File.separator + "resources" + File.separator + "apis"
                        + File.separator + "pizza_shack_api.bal").getAbsolutePath();
        String configPath = new File("src" + File.separator + "test" + File.separator + "resources"
                + File.separator + "confs" + File.separator + "base.conf").getAbsolutePath();
        startServer(relativePath, configPath);
    }

    @Test(description = "Test without auth header")
    public void testWithoutAuthHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);
        HttpResponse response = HttpClientRequest
                .doGet(microGWServer.getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("No authorization header was provided"),
                "Message " + "content mismatched");
    }

    @Test(description = "Test with invalid token")
    public void testWithInvalidToken() throws Exception {
        try {
            String kmServerFile = new File(keyManagerServiceDir + File.separator + "unauthenticated.bal")
                    .getAbsolutePath();
            startKMServer(kmServerFile);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer asds-34234");
            HttpResponse response = HttpClientRequest
                    .doGet(microGWServer.getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
            Assert.assertNotNull(response);
            Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");
            Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Message " + "content mismatched");
        } finally {
            kmServer.stopServer(false);
        }
    }

    @Test(description = "Test with invalid scopes")
    public void testWithInvalidScopes() throws Exception {
        try {
            String kmServerFile = new File(keyManagerServiceDir + File.separator + "unauthorized.bal")
                    .getAbsolutePath();
            startKMServer(kmServerFile);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer asds-34234");
            HttpResponse response = HttpClientRequest
                    .doGet(microGWServer.getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
            Assert.assertNotNull(response);
            Assert.assertEquals(response.getResponseCode(), 403, "Response code mismatched");
            Assert.assertTrue(
                    response.getData().contains("The access token does not allow you to access the requested resource"),
                    "Message content mismatched");
        } finally {
            kmServer.stopServer(false);
        }
    }

    @Test(description = "Test with invalid subscription")
    public void testWithInvalidSubscription() throws Exception {
        try {
            String kmServerFile = new File(keyManagerServiceDir + File.separator + "invalid_subscription.bal")
                    .getAbsolutePath();
            startKMServer(kmServerFile);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer asds-4324");
            HttpResponse response = HttpClientRequest
                    .doGet(microGWServer.getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
            Assert.assertNotNull(response);
            Assert.assertEquals(response.getResponseCode(), 403, "Response code mismatched");
            Assert.assertTrue(response.getData().contains("Resource forbidden"));
        } finally {
            kmServer.stopServer(false);
        }
    }


    private void startServer(String balFile, String configFile) throws Exception {
        microGWServer.startMicroGwServerWithConfigPath(balFile, configFile);
    }

    private void startKMServer(String balFile) throws Exception {
        kmServer.startMicroGwServer(balFile);
    }

    @AfterClass
    private void cleanUp() throws MicroGWTestException {
        microGWServer.stopServer(true);
    }



}

