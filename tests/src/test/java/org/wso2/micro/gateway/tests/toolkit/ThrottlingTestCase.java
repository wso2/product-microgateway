/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.tests.toolkit;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.common.model.ApplicationPolicy;
import org.wso2.micro.gateway.tests.common.model.SubscriptionPolicy;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.util.HashMap;
import java.util.Map;

public class ThrottlingTestCase extends BaseTestCase {
    private String jwtToken, jwtToken2, jwtToken3, token1, token2, continueOnQuotaToken, noSubPolicyJWT, noAppPolicyJWT,
            noSubPolicyToken, noAppPolicyToken;
    private int responseCode;

    @BeforeClass
    public void start() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        API api = new API();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        api.setEndpoint(getMockServiceURLHttp("/echo"));
        api.setVersion("1.0.0");
        api.setProvider("admin");
        //Register API with label
        pub.addApi(label, api);

        //Define application(Unlimited) info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        ApplicationPolicy applicationPolicy = new ApplicationPolicy();
        applicationPolicy.setPolicyName("10MinAppPolicy");
        applicationPolicy.setRequestCount(5);
        pub.addApplicationPolicy(applicationPolicy);

        //Define 10req application
        ApplicationDTO application2 = new ApplicationDTO();
        application2.setName("jwtApp2");
        application2.setTier(applicationPolicy.getPolicyName());
        application2.setId((int) (Math.random() * 1000));

        SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();
        subscriptionPolicy.setPolicyName("10MinSubPolicy");
        subscriptionPolicy.setRequestCount(5);
        pub.addSubscriptionPolicy(subscriptionPolicy);

        SubscriptionPolicy subPolicyContinueOnLimit = new SubscriptionPolicy();
        subPolicyContinueOnLimit.setPolicyName("allowOnLimitExceed");
        subPolicyContinueOnLimit.setRequestCount(10);
        subPolicyContinueOnLimit.setStopOnQuotaReach(false);
        pub.addSubscriptionPolicy(subPolicyContinueOnLimit);


        ApplicationDTO application3 = new ApplicationDTO();
        application3.setName("jwtApp3");
        application3.setTier("Unlimited");
        application3.setId((int) (Math.random() * 1000));

        //Register a token with key validation info
        jwtToken = getJWT(api, application, subscriptionPolicy.getPolicyName(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        jwtToken2 = getJWT(api, application2, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);
        continueOnQuotaToken = getJWT(api, application3, subPolicyContinueOnLimit.getPolicyName(),
                TestConstant.KEY_TYPE_PRODUCTION, 3600);
        //set security schemas
        String security = "oauth2";

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

        noSubPolicyJWT = getJWT(api, application, "SubPolicyNotExist", TestConstant.KEY_TYPE_PRODUCTION, 3600);
        noAppPolicyJWT = getJWT(api, appWithNonExistPolicy, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);

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

        //generate apis with CLI and start the micro gateway server
        super.init(label, project, security);
    }

    @Test(description = "Test subscription throttling with a JWT and oauth2 token")
    public void testSubscriptionThrottling() throws Exception {
        responseCode = invokeAndAssert(jwtToken, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with jwt token");
        responseCode = invokeAndAssert(token1, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with oauth token");
    }

    @Test(description = "Test application throttling with a JWT and oauth2 token")
    public void testApplicationThrottling() throws Exception {
        responseCode = invokeAndAssert(jwtToken2, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with jwt token");
        responseCode = invokeAndAssert(token2, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out with oauth token");
    }

//    @Test(description = "Test throttling with non auth mode")
//    public void testApplicationThrottlingInNonAuthMode() throws Exception {
//        responseCode = invokeAndAssert(null, getServiceURLHttp("/pizzashack/1.0.0/noauth"));
//        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
//    }

    @Test(description = "test subscription policy with stop on quota is false")
    public void testSubscriptionThrottlingWithStopOnQuotaFalse() throws Exception {
        responseCode = invokeAndAssert(continueOnQuotaToken, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 200, "Request should not throttled out");
    }

    @Test(description = "test throttling with non exist subscription policy")
    public void testThrottlingWithNonExistSubscriptionPolicy() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + noSubPolicyJWT);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 500, "Request should not successful with JWT.");
        Assert.assertTrue(response.getData().contains("\"code\":900809"),
                "Error response should have errorcode 900809 in JWT.");

        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + noSubPolicyToken);
        response = HttpClientRequest.doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 500, "Request should not successful with oauth.");
        Assert.assertTrue(response.getData().contains("\"code\":900809"),
                "Error response should have errorcode 900809 in oauth.");
    }

    @Test(description = "test throttling with non exist application policy")
    public void testThrottlingWithNonExistApplicationPolicy() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + noAppPolicyJWT);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 500, "Request should not successful with JWT.");
        Assert.assertTrue(response.getData().contains("\"code\":900809"),
                "Error response should have errorcode 900809 in JWT.");

        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + noAppPolicyToken);
        response = HttpClientRequest.doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 500, "Request should not successful with oauth.");
        Assert.assertTrue(response.getData().contains("\"code\":900809"),
                "Error response should have errorcode 900809 in oauth.");
    }

    private int invokeAndAssert(String token, String url) throws Exception {
        Map<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }

        int retry = 9;
        int responseCode = -1;
        while (retry > 0) {
            for (int i = 0; i < 9; i++) {
                org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
                Thread.sleep(1000);
                Assert.assertNotNull(response);
                responseCode = response.getResponseCode();
                retry--;
            }
        }
        return responseCode;
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
