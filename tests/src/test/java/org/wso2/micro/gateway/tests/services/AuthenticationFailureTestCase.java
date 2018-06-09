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
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.IntegrationTestCase;
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

    @Test(description = "Test without auth header")
    public void testWithoutAuthHeader() throws Exception {
        try {
            String relativePath = new File(
                    "src" + File.separator + "test" + File.separator + "resources" + File.separator + "apis"
                            + File.separator + "pizza_shack_api.bal").getAbsolutePath();
            startServer(relativePath);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);
            HttpResponse response = HttpClientRequest
                    .doGet(microGWServer.getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
            Assert.assertNotNull(response);
            Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");
            Assert.assertTrue(response.getData().contains("No authorization header was provided"),
                    "Message " + "content mismatched");
        } finally {
            microGWServer.stopServer();
        }
    }



    private void startServer(String balFile) throws Exception {
        microGWServer = ServerInstance.initMicroGwServer(TestConstant.GATEWAY_LISTENER_PORT);
        microGWServer.startMicroGwServer(balFile);
    }


}

