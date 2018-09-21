/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.tests.services;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Testing the pizza_shack api rest for authentication error messages
 */
public class AuthenticationFailureTestCase extends BaseTestCase {
    private String invalidSubscriptionToken, invalidScopeToken;

    @BeforeClass
    private void setup() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        APIDTO api = new APIDTO();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        api.setVersion("1.0.0");
        api.setProvider("admin");
        //Register API with label
        pub.addApi(label, api);

        String response = IOUtils.toString(new FileInputStream(
                getClass().getClassLoader().getResource("keyManager" + File.separator + "invalid_subscription.xml")
                        .getPath()));
        KeyValidationInfo info = new KeyValidationInfo();
        info.setStringResponse(response);
        invalidSubscriptionToken = pub.getAndRegisterAccessToken(info);

        String response1 = IOUtils.toString(new FileInputStream(
                getClass().getClassLoader().getResource("keyManager" + File.separator + "unauthorized.xml").getPath()));
        KeyValidationInfo info1 = new KeyValidationInfo();
        info1.setStringResponse(response1);
        invalidScopeToken = pub.getAndRegisterAccessToken(info1);

        super.init(label, project);
    }

    @Test(description = "Test without auth header")
    public void testWithoutAuthHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("Missing Credentials"), "Message content mismatched");
    }

    @Test(description = "Test with invalid token")
    public void testWithInvalidToken() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer invalid");
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Message " + "content mismatched");
    }

    @Test(description = "Test with invalid scopes")
    public void testWithInvalidScopes() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + invalidScopeToken);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 403, "Response code mismatched");
        Assert.assertTrue(
                response.getData().contains("The access token does not allow you to access the requested resource"),
                "Message content mismatched");
    }

    @Test(description = "Test with invalid subscription")
    public void testWithInvalidSubscription() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + invalidSubscriptionToken);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 403, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("Resource forbidden"));
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}

