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

public class WebSocketSecurityDisabledTestCase extends ApimBaseTest {
    private static final String API_CONTEXT = "websocket-security-disabled";
    private static final String API_VERSION = "1.0.0";

    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "Initialize the API-M client and define endpoint")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();
        endpointURL = Utils.getServiceURLWebSocket(API_CONTEXT + "/" + API_VERSION);
    }

    @Test(description = "Test websocket topic security disabled via publisher")
    public void testTopicSecurityDisabledViaPublisher() throws Exception {
        WsClient wsClient = new WsClient(endpointURL + "/security-disabled-via-apim-publisher", new HashMap<>());
        List<String> messagesToSend = List.of(new String[]{"ping", "close"});
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 0);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(responses.get(0), "pong");
    }

    @Test(description = "Test websocket topic security disabled via extension")
    public void testTopicSecurityDisabledViaExtension() throws Exception {
        WsClient wsClient = new WsClient(endpointURL + "/security-disabled-via-extension", new HashMap<>());
        List<String> messagesToSend = List.of(new String[]{"ping", "close"});
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 0);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(responses.get(0), "pong");
    }

}
