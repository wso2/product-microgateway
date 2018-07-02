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
    private String jwtToken, jwtToken2, token1, token2, continueOnQuotaToken;
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
        subPolicyContinueOnLimit.setRequestCount(5);
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
        jwtToken = getJWT(api, application, subscriptionPolicy.getPolicyName(), TestConstant.KEY_TYPE_PRODUCTION);
        jwtToken2 = getJWT(api, application2, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION);
        continueOnQuotaToken = getJWT(api, application3, subPolicyContinueOnLimit.getPolicyName(),
                TestConstant.KEY_TYPE_PRODUCTION);

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

        //generate apis with CLI and start the micro gateway server
        super.init(label, project);
    }

    @Test(description = "Test subscription throttling with a JWT and oauth2 token")
    public void testSubscriptionThrottling() throws Exception {
        responseCode = invokeAndAssert(jwtToken, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
        responseCode = invokeAndAssert(token1, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
    }

    @Test(description = "Test application throttling with a JWT and oauth2 token")
    public void testApplicationThrottling() throws Exception {
        responseCode = invokeAndAssert(jwtToken2, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
        responseCode = invokeAndAssert(token2, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
    }

    @Test(description = "Test throttling with non auth mode")
    public void testApplicationThrottlingInNonAuthMode() throws Exception {
        responseCode = invokeAndAssert(null, getServiceURLHttp("/pizzashack/1.0.0/noauth"));
        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
    }

    @Test(description = "test subscription policy with stop on quota is false")
    public void testSubscriptionThrottlingWithStopOnQuotaFalse() throws Exception {
        responseCode = invokeAndAssert(continueOnQuotaToken, getServiceURLHttp("/pizzashack/1.0.0/menu"));
        Assert.assertEquals(responseCode, 200, "Request should not throttled out");
    }

    private int invokeAndAssert(String token, String url) throws Exception {
        Map<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        }
        int retry = 15;
        int responseCode = -1;
        while (retry > 0) {
            for (int i = 0; i < 15; i++) {
                org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.doGet(url, headers);
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
