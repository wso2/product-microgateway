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
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.websocket.WsClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketTimeoutTestCase extends ApimBaseTest {
    private static final String API_CONTEXT = "websocket-timeout";
    private static final String API_VERSION = "1.0.0";
    private static final String APPLICATION_NAME = "WebSocketTimeoutApp";
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

    @Test(description = "Test websocket connection idle timeout")
    public void testConnectionIdleTimeout() throws Exception {
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        wsClient.setMeasureDuration();
        List<String> messagesToSend = new ArrayList<>();
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 0);
        Assert.assertNotNull(responses);
        int duration = Integer.parseInt(responses.get(0).split(":")[1]);
        Assert.assertTrue(duration < 18);
    }
}
