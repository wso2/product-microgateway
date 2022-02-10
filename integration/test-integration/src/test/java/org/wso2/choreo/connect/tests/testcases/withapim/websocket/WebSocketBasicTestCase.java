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
import org.wso2.choreo.connect.tests.util.websocket.WebSocketClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketBasicTestCase extends ApimBaseTest {
    private static final String API_NAME = "WebSocketBasicAPI";
    private static final String API_CONTEXT = "websocket-basic";
    private static final String APPLICATION_NAME = "WebSocketBasicApp";
    private final Map<String, String> requestHeaders = new HashMap<>();

    private String apiId;
    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // Get App ID and API ID
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        endpointURL = Utils.getServiceURLHttps(API_CONTEXT);
    }

    @Test(description = "Dummy test")
    public void testConnectionWithPing() throws CCTestException {
        WebSocketClient webSocketClient = new WebSocketClient(endpointURL, requestHeaders);
        String[] messagedToSend = { "ping", "close" };
        List<String> responses = webSocketClient.connectAndSendMessages(messagedToSend);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(responses.get(0), "pong");
    }
}
