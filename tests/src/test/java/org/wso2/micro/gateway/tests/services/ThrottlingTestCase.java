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
package org.wso2.micro.gateway.tests.services;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.HashMap;
import java.util.Map;

public class ThrottlingTestCase extends BaseTestCase {
    private static final Logger log = LoggerFactory.getLogger(ThrottlingTestCase.class);
    private String label = "apimTestLabel";
    private String project = "apimTestProject";
    private String token, jwtToken, jwtToken2;

    @BeforeClass
    public void start() throws Exception {
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        API api = new API();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        api.setEndpoint("http://localhost:9443/echo");
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

        ApplicationPolicy applicationPolicy = new ApplicationPolicy();
        applicationPolicy.setPolicyName("10MinAppPolicy");
        applicationPolicy.setRequestCount(10);
        pub.addApplicationPolicy(applicationPolicy);

        ApplicationDTO application2 = new ApplicationDTO();
        application2.setName("jwtApp2");
        application2.setTier(applicationPolicy.getPolicyName());
        application2.setId((int) (Math.random() * 1000));

        //Register a token with key validation info
        jwtToken = getJWT(api, application, subscriptionPolicy.getPolicyName());
        jwtToken2 = getJWT(api, application2, "Unlimited");
        //generate apis with CLI and start the micro gateway server
        super.init(label, project);
    }

    @Test(description = "Test subscription throttling with a JWT token")
    public void testSubscriptionThrottlingWithJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        int retry = 15;
        int responseCode = -1;
        while (retry > 0)
            for (int i = 0; i < 15; i++) {
                org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                        .doGet(microGWServer.getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
                Assert.assertNotNull(response);
                responseCode = response.getResponseCode();
                retry--;
            }
        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
    }

    @Test(description = "Test application throttling with a JWT token")
    public void testApplicationThrottlingWithJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken2);
        int retry = 15;
        int responseCode = -1;
        while (retry > 0)
            for (int i = 0; i < 15; i++) {
                org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                        .doGet(microGWServer.getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
                Assert.assertNotNull(response);
                responseCode = response.getResponseCode();
                retry--;
            }
        Assert.assertEquals(responseCode, 429, "Request should have throttled out");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
