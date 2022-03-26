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
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.websocket.WsClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketScopeTestCase extends ApimBaseTest {
    private static final String API_CONTEXT = "websocket-scope";
    private static final String API_VERSION = "1.0.0";
    private static final String APPLICATION_NAME = "WebSocketScopeApp";

    private String endpointURL;
    private String jwtWithoutScope;
    private String invalidJwt;
    private String jwtWithScope;
    private String jwtWithMultipleScopes;

    @BeforeClass(alwaysRun = true, description = "Create access tokens and define endpoint URL")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();
        endpointURL = Utils.getServiceURLWebSocket(API_CONTEXT + "/" + API_VERSION);

        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        ApplicationKeyDTO appWithConsumerKey = StoreUtils.generateKeysForApp(applicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, storeRestClient);

        jwtWithoutScope = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{}, user, storeRestClient);
        invalidJwt = jwtWithoutScope.substring(0, jwtWithoutScope.length() - 400);
        jwtWithScope = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{"reader"}, user, storeRestClient);
        jwtWithMultipleScopes = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{"reader", "anonymous"}, user, storeRestClient);
    }

    @Test(description = "Test websocket API without scope")
    public void testWithoutScope() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + jwtWithoutScope);
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        List<String> messagesToSend = List.of(new String[]{"ping", "close"});
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 0);
        Assert.assertNotNull(responses);
        Assert.assertNotEquals(responses.get(0), "pong");
        Assert.assertTrue(responses.get(0).contains("403"), "The gateway did not respond with 403");
    }

    @Test(description = "Test websocket API with invalid JWT")
    public void testWithInvalidJwt() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + invalidJwt);
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        List<String> messagesToSend = List.of(new String[]{"ping", "close"});
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 0);
        Assert.assertNotNull(responses);
        Assert.assertTrue(responses.get(0).contains("401"), "The gateway did not respond with 401");
    }

    @Test(description = "Test websocket API with scope")
    public void testWithScope() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + jwtWithScope);
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        List<String> messagesToSend = List.of(new String[]{"ping", "heyyy", "close"});
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 300);
        Assert.assertNotNull(responses);
        Assert.assertEquals(responses.get(0), "pong");
    }

    @Test(description = "Test websocket connection and a ping via Choreo Connect")
    public void testWithMultipleScopes() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + jwtWithMultipleScopes);
        WsClient wsClient = new WsClient(endpointURL, requestHeaders);
        List<String> messagesToSend = List.of(new String[]{"ping", "close"});
        List<String> responses = wsClient.retryConnectUntilDeployed(messagesToSend, 300);
        Assert.assertNotNull(responses);
        Assert.assertEquals(responses.get(0), "pong");
    }
}
