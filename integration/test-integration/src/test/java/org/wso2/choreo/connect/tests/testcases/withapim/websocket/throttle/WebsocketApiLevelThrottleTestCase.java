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

package org.wso2.choreo.connect.tests.testcases.withapim.websocket.throttle;

import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.websocket.WsClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Websocket Throttling for APIs in APIM 4.1.0-alpha
 *
 * Supported
 * - Event count throttling via Type REQUESTCOUNTLIMIT
 * - Bandwidth throttling via Type BANDWIDTHLIMIT
 *
 * Not supported
 * - Request count throttling
 */
public class WebsocketApiLevelThrottleTestCase extends ApimBaseTest {
    private static final String BANDWIDTH_THROTTLE_API_NAME = "WebsocketApiLevelBandwidthThrottleAPI";
    private static final String BANDWIDTH_THROTTLE_API_CONTEXT = "websocket-api-level-bandwidth-throttle";

    private static final String EVENT_COUNT_THROTTLE_API_NAME = "WebsocketApiLevelEventCountThrottleAPI";
    private static final String EVENT_COUNT_THROTTLE_API_CONTEXT = "websocket-api-level-event-count-throttle";

    private static final String API_VERSION = "1.0.0";
    private static final String APPLICATION_NAME = "WebsocketApiLevelThrottleApp";

    private static final String BANDWIDTH_THROTTLE_API_POLICY_NAME = "3KbPerMin";
    private static final String EVENT_COUNT_THROTTLE_API_POLICY_NAME = "5ReqPerMin";
    private static final int throttleCount = 5;

    private final Map<String, String> headers = new HashMap<>();
    private String bandwidthThrottleApiId;
    private String eventCountThrottleApiId;
    private String bandwidthThrottleEndpoint;
    private String eventCountThrottleEndpoint;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);

        bandwidthThrottleApiId = ApimResourceProcessor.apiNameToId.get(BANDWIDTH_THROTTLE_API_NAME);
        eventCountThrottleApiId = ApimResourceProcessor.apiNameToId.get(EVENT_COUNT_THROTTLE_API_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        headers.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        bandwidthThrottleEndpoint = Utils.getServiceURLWebSocket(BANDWIDTH_THROTTLE_API_CONTEXT + "/" + API_VERSION);
        eventCountThrottleEndpoint = Utils.getServiceURLWebSocket(EVENT_COUNT_THROTTLE_API_CONTEXT + "/" + API_VERSION);
    }

    @Test(description = "Test API Level Bandwidth Throttling")
    public void testAPILevelBandwidthThrottling() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(bandwidthThrottleApiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(BANDWIDTH_THROTTLE_API_POLICY_NAME);
        APIDTO updatedAPI = publisherRestClient.updateAPI(apidto, bandwidthThrottleApiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), BANDWIDTH_THROTTLE_API_POLICY_NAME, "API tier not updated.");

        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(bandwidthThrottleApiId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME*2, "Couldn't wait until the API was deployed in Choreo Connect");

        WsClient wsClient = new WsClient(bandwidthThrottleEndpoint, headers);
        String largePayload = "a".repeat(1024);
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(largePayload);
        }
        messages.add("close");
        List<String> responses = wsClient.retryConnectUntilDeployed(messages, 0);
        Assert.assertTrue(responses.size() >= 3 && responses.size() < 10,
                "Websocket connection was not throttled on bandwidth.");
    }

    @Test(description = "Test API Level EventCount Throttling")
    public void testAPILevelEventCountThrottling() throws Exception {
        HttpResponse api = publisherRestClient.getAPI(eventCountThrottleApiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(EVENT_COUNT_THROTTLE_API_POLICY_NAME);
        APIDTO updatedAPI = publisherRestClient.updateAPI(apidto, eventCountThrottleApiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), EVENT_COUNT_THROTTLE_API_POLICY_NAME, "API tier not updated.");

        // create Revision and Deploy to Gateway
        PublisherUtils.createAPIRevisionAndDeploy(eventCountThrottleApiId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME*2, "Couldn't wait until the API was deployed in Choreo Connect");

        WsClient wsClient = new WsClient(eventCountThrottleEndpoint, headers);
        Assert.assertTrue(wsClient.isThrottledWebSocket(throttleCount), "Request not throttled by event count");
        // TODO: (suksw) Add testcase for client sent event count throttling
    }
}
