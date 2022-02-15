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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.websocket.WsClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketBasicTestCase extends ApimBaseTest {
    private static final String API_CONTEXT = "websocket-basic";
    private static final String APPLICATION_NAME = "WebSocketBasicApp";
    private final Map<String, String> requestHeaders = new HashMap<>();

    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // Get App ID and API ID
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        endpointURL = Utils.getServiceURLWs(API_CONTEXT);
    }

    @Test(description = "Dummy test")
    public void testConnectionWithPing() throws CCTestException {
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        String[] messagedToSend = { "ping", "close" };
        List<String> responses = wsClient.connectAndSendMessages(messagedToSend);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(responses.get(0), "pong");
    }
}
