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

package org.wso2.choreo.connect.tests.testcases.withapim.websocket;

import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.websocket.WsClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketBasicTestCase extends ApimBaseTest {
    private static final String API_CONTEXT = "websocket-basic";
    private static final String API_VERSION = "1.0.0";
    private static final String APPLICATION_NAME = "WebSocketBasicApp";
    private final Map<String, String> requestHeaders = new HashMap<>();

    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "Create access token and define endpoint URL")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // Get App ID and API ID
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        endpointURL = Utils.getServiceURLWebSocket(API_CONTEXT + "/" + API_VERSION);
    }

    @Test(description = "Test websocket connection and a ping via Choreo Connect")
    public void testConnectionWithPing() throws Exception {
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        List<String> messagesToSend = List.of(new String[]{"ping", "close"});
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 0);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(responses.get(0), "pong");
    }

    @Test(description = "Test sending a websocket plain text msg via Choreo Connect")
    public void testConnectionWithTextMessage() throws Exception {
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        String msg = "a text msg that is sent via a web socket connection";
        List<String> messagesToSend = new ArrayList<>();
        messagesToSend.add(msg);
        messagesToSend.add(msg);
        messagesToSend.add("close");
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 0);
        Assert.assertEquals(responses.size(), 2);
        Assert.assertEquals(responses.get(0), "Message received: " + msg);
    }

    @Test(description = "Test non existent version of an API", dependsOnMethods = "testConnectionWithPing")
    public void testNonExistentVersion() throws Exception {
        endpointURL = Utils.getServiceURLWebSocket(API_CONTEXT + "/2.0.0");
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        List<String> messagesToSend = List.of(new String[]{"ping", "close"});
        boolean respondedNotFound = false;
        int serverResponse = 0;
        int maxRetryCount = 10;
        int retryCount = 0;
        do {
            retryCount ++;
            try {
                wsClient.connectAndSendMessages(messagesToSend, 0);
            } catch (WebSocketClientHandshakeException e) {
                serverResponse = e.response().status().code();
                if (404 == e.response().status().code()) {
                    respondedNotFound = true;
                }
            }
        } while (maxRetryCount > retryCount && serverResponse == 503);

        Assert.assertTrue(respondedNotFound, "Server responded with " + serverResponse);
    }
}
