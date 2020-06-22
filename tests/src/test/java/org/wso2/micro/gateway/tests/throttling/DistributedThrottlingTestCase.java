/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.tests.throttling;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.common.model.ApplicationPolicy;
import org.wso2.micro.gateway.tests.common.model.SubscriptionPolicy;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DistributedThrottlingTestCase extends BaseTestCase {
    private String jwtToken, jwtToken2, token1, token2, continueOnQuotaToken, noSubPolicyJWT, noAppPolicyJWT,
            noSubPolicyToken, noAppPolicyToken;
    private int responseCode;
    private BinaryTestServer b;
    private BinaryTestServer b2;
    private String project;

    @Override
    protected void init(String label, String project) throws Exception {
        String configPath = "confs/throttle-test-config.conf";
        super.init(label, project, configPath);
    }

    @BeforeClass
    private void start() throws Exception {
        String label = "apimTestLabel";
        project = "apimTestProject";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        API api = new API();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        api.setProdEndpoint(getMockServiceURLHttp("/echo/prod"));
        api.setSandEndpoint(getMockServiceURLHttp("/echo/sand"));
        api.setVersion("1.0.0");
        api.setProvider("admin");
        //Register API with label
        pub.addApi(label, api);

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();
        subscriptionPolicy.setPolicyName("10MinSubPolicy");
        subscriptionPolicy.setRequestCount(10);
        pub.addSubscriptionPolicy(subscriptionPolicy);

        SubscriptionPolicy subPolicyContinueOnLimit = new SubscriptionPolicy();
        subPolicyContinueOnLimit.setPolicyName("allowOnLimitExceed");
        subPolicyContinueOnLimit.setRequestCount(10);
        subPolicyContinueOnLimit.setStopOnQuotaReach(false);
        pub.addSubscriptionPolicy(subPolicyContinueOnLimit);

        ApplicationPolicy applicationPolicy = new ApplicationPolicy();
        applicationPolicy.setPolicyName("10MinAppPolicy");
        applicationPolicy.setRequestCount(10);
        pub.addApplicationPolicy(applicationPolicy);

        ApplicationDTO application2 = new ApplicationDTO();
        application2.setName("jwtApp2");
        application2.setTier(applicationPolicy.getPolicyName());
        application2.setId((int) (Math.random() * 1000));

        ApplicationDTO application3 = new ApplicationDTO();
        application3.setName("jwtApp3");
        application3.setTier("Unlimited");
        application3.setId((int) (Math.random() * 1000));

        //Register a token with key validation info
        jwtToken = getJWT(api, application, subscriptionPolicy.getPolicyName(), TestConstant.KEY_TYPE_PRODUCTION,
                3600);
        jwtToken2 = getJWT(api, application2, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);
        continueOnQuotaToken = getJWT(api, application3, subPolicyContinueOnLimit.getPolicyName(),
                TestConstant.KEY_TYPE_PRODUCTION, 3600);

        KeyValidationInfo info = new KeyValidationInfo();
        info.setApi(api);
        info.setApplication(application);
        info.setAuthorized(true);
        info.setKeyType(TestConstant.KEY_TYPE_PRODUCTION);
        info.setSubscriptionTier(subscriptionPolicy.getPolicyName());
        token1 = pub.getAndRegisterAccessToken(info);

        KeyValidationInfo info2 = new KeyValidationInfo();
        info2.setApi(api);
        info2.setApplication(application2);
        info2.setAuthorized(true);
        info2.setKeyType(TestConstant.KEY_TYPE_PRODUCTION);
        info2.setSubscriptionTier("Unlimited");
        token2 = pub.getAndRegisterAccessToken(info2);

        ApplicationDTO appWithNonExistPolicy = new ApplicationDTO();
        appWithNonExistPolicy.setName("appWithNonExistPolicy");
        appWithNonExistPolicy.setTier("AppPolicyNotExist");
        appWithNonExistPolicy.setId((int) (Math.random() * 1000));

        noSubPolicyJWT = getJWT(api, application, "SubPolicyNotExist", TestConstant.KEY_TYPE_PRODUCTION,
                3600);
        noAppPolicyJWT = getJWT(api, appWithNonExistPolicy, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600);

        KeyValidationInfo info3 = new KeyValidationInfo();
        info3.setApi(api);
        info3.setApplication(application);
        info3.setAuthorized(true);
        info3.setKeyType(TestConstant.KEY_TYPE_PRODUCTION);
        info3.setSubscriptionTier("SubPolicyNotExist");
        noSubPolicyToken = pub.getAndRegisterAccessToken(info3);

        info3.setApplication(appWithNonExistPolicy);
        info.setSubscriptionTier(subscriptionPolicy.getPolicyName());
        noAppPolicyToken = pub.getAndRegisterAccessToken(info3);


        b = new BinaryTestServer();
        b2 = new BinaryTestServer();

        String streamDefinition = readFromInputStream(getClass().getClassLoader()
                .getResourceAsStream("distributedThrottling/stream-definition.json"));
        b.addStreamDefinition(streamDefinition);
        b2.addStreamDefinition(streamDefinition);
        b.startServer(9611, 9711);
        b2.startServer(9612, 9712);

        //generate apis with CLI and start the micro gateway server
        init(label, project);
    }

    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    @Test(description = "Test subscription throttling with a JWT token", priority = 1)
    public void testSubscriptionThrottling() throws Exception {
        responseCode = invokeAndAssert2(jwtToken, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with jwt token");
        responseCode = invokeAndAssert2(token1, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with oauth token");
        TimeUnit.MINUTES.sleep(1);
        responseCode = invokeAPIForOnce(token1, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 200, "Request should not have throttled out with oauth token after" +
                "1 minute time gap" );
    }

    @Test(description = "Test application throttling with a JWT token", priority = 1)
    public void testApplicationThrottlingWithJwtToken() throws Exception {
        responseCode = invokeAndAssert2(jwtToken2, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with jwt token");
    }

    @Test(description = "Test application throttling with oauth2 token", priority = 1)
    public void testApplicationThrottlingWithOauth2Token() throws Exception {
        responseCode = invokeAndAssert2(token2, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with oauth token");
    }

//    @Test(description = "Test throttling with non auth mode" , priority = 1)
//    public void testApplicationThrottlingInNonAuthMode() throws Exception {
//        responseCode = invokeAndAssert2(null, getServiceURLHttp("/pizzashack/1.0.0/noauth"));
//        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
//    }

    @Test(description = "test subscription policy with stop on quota is false", priority = 1)
    public void testSubscriptionThrottlingWithStopOnQuotaFalse() throws Exception {
        responseCode = invokeAndAssert2(continueOnQuotaToken, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 200, "Request should not throttled out");
    }

    @Test(description = "test event publishing over binary connection with one TM", priority = 2)
    public void testEventPublishingWithOneTM() throws Exception {
        String configPath = "confs/throttle-test-binary-publishing-single-tm.conf";
        restartServerSetup(configPath);
        String url = getServiceURLHttp("/pizzashack/1.0.0/menu");
        Map<String, String> headers = new HashMap<>();
        String token = jwtToken;
        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        for (int i = 0; i < 20; i++) {
            org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
            Assert.assertNotNull(response);
        }
        Thread.sleep(1000);
        Assert.assertEquals(b.getNumberOfEventsReceived(), 20, "Event count mismatch");
    }

    @Test(description = "test event publishing over binary connection with multiple TMs", priority = 2)
    public void testEventPublishingMultipleTM() throws Exception {
        String configPath = "confs/throttle-test-binary-publishing-multiple-tm.conf";
        restartServerSetup(configPath);
        String url = getServiceURLHttp("/pizzashack/1.0.0/menu");
        Map<String, String> headers = new HashMap<>();
        String token = jwtToken;
        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        for (int i = 0; i < 20; i++) {
            org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
            Assert.assertNotNull(response);
            Thread.sleep(10);
        }
        Thread.sleep(1000);
        Assert.assertEquals(b.getNumberOfEventsReceived(), 20, "Server 1 : Event count mismatch");
        Assert.assertEquals(b2.getNumberOfEventsReceived(), 20, "Server 2 : Event count mismatch");
    }

    @Test(description = "test event publishing over binary connection with multiple TMs: Loadbalance", priority = 2)
    public void testEventPublishingLoadBalance() throws Exception {
        String configPath = "confs/throttle-test-binary-publishing-loadbalance-tm.conf";
        restartServerSetup(configPath);
        String url = getServiceURLHttp("/pizzashack/1.0.0/menu");
        Map<String, String> headers = new HashMap<>();
        String token = jwtToken;
        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        for (int i = 0; i < 20; i++) {
            org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
            Assert.assertNotNull(response);
        }
        Thread.sleep(1000);
        Assert.assertEquals(b.getNumberOfEventsReceived(), 10, "Server 1 : Event count mismatch");
        Assert.assertEquals(b2.getNumberOfEventsReceived(), 10, "Server 2 : Event count mismatch");
    }

    @Test(description = "test event publishing over binary connection with multiple TMs: Failover", priority = 3)
    public void testEventPublishingFailover() throws Exception {
        String configPath = "confs/throttle-test-binary-publishing-failover-tm.conf";
        restartServerSetup(configPath);
        String url = getServiceURLHttp("/pizzashack/1.0.0/menu");
        Map<String, String> headers = new HashMap<>();
        String token = jwtToken;
        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        for (int i = 0; i < 15; i++) {
            org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
            Assert.assertNotNull(response);
        }
        Assert.assertEquals(b.getNumberOfEventsReceived(), 15, "Server 1 : Event count mismatch");
        Assert.assertEquals( b2.getNumberOfEventsReceived(), 0, "Server 2 : Event count mismatch");
        Thread.sleep(1000);
        b.stopServer();
        Thread.sleep(10000);
        for (int i = 0; i < 15; i++) {
            org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
            Assert.assertNotNull(response);
        }
        Assert.assertEquals(b2.getNumberOfEventsReceived(), 15, "Event count mismatch");
    }

    public void restartServerSetup(String configFilePath) throws Exception {
        microGWServer.stopServer(false);
        b.resetReceivedEvents();
        b2.resetReceivedEvents();
        restartWithDifferentConfig(project, null, configFilePath);
    }

    public int invokeAPIForOnce(String token, String url) throws IOException {
        Map<String, String> headers = new HashMap<>();

        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
        Assert.assertNotNull(response);
        return response.getResponseCode();
    }

    public int invokeAndAssert2(String token, String url) throws Exception {
        Map<String, String> headers = new HashMap<>();

        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        int retry = 20;
        int responseCode = -1;
        while (retry > 0) {
            for (int i = 0; i < 20; i++) {
                org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
                Assert.assertNotNull(response);
                responseCode = response.getResponseCode();
                if (responseCode == 429) {
                    return responseCode;
                }
                retry--;
            }
        }
        return responseCode;
    }

    public void finalize() throws Exception {
        mockHttpServer.stopIt();
        b.stopServer();
        b2.stopServer();
        microGWServer.stopServer(false);
        MockAPIPublisher.getInstance().clear();
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        finalize();
    }
}
