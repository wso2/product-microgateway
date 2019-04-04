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
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class APIInvokeWithOAuthTestCase extends BaseTestCase {
    private String prodToken, sandToken, jwtTokenProd, jwtTokenSand, expiringJwtTokenProd;

    @BeforeClass
    public void start() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
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

        //Register a production token with key validation info
        KeyValidationInfo info = new KeyValidationInfo();
        info.setApi(api);
        info.setApplication(application);
        info.setAuthorized(true);
        info.setKeyType(TestConstant.KEY_TYPE_PRODUCTION);
        info.setSubscriptionTier("Unlimited");
        //set security schemas
        String security = "oauth2";
        //Register a production token with key validation info
        prodToken = pub.getAndRegisterAccessToken(info);

        //Register a sandbox token with key validation info
        KeyValidationInfo infoSand = new KeyValidationInfo();
        infoSand.setApi(api);
        infoSand.setApplication(application);
        infoSand.setAuthorized(true);
        infoSand.setKeyType(TestConstant.KEY_TYPE_SANDBOX);
        infoSand.setSubscriptionTier("Unlimited");
        sandToken = pub.getAndRegisterAccessToken(infoSand);

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);
        jwtTokenSand = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_SANDBOX, 3600);
        expiringJwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 1);
        //generate apis with CLI and start the micro gateway server
        super.init(label, project, security);
    }

    @Test(description = "Test API invocation with a oauth token")
    public void testApiInvoke() throws Exception {
        //test prod endpoint
        invoke(prodToken, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        //test sand endpoint
        invoke(sandToken, MockHttpServer.SAND_ENDPOINT_RESPONSE, 200);
    }

    @Test(description = "Test API invocation with a JWT token")
    public void testApiInvokeWithJWT() throws Exception {
        //test prod endpoint with jwt token
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        //test sand endpoint
        invoke(jwtTokenSand, MockHttpServer.SAND_ENDPOINT_RESPONSE, 200);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }
        //test invoking with an expired JWT token
        invoke(expiringJwtTokenProd, 401);
    }

    private void invoke(String token, String responseData, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), responseData);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    private void invoke(String token, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

//    @Test(description = "Test API invocation with Basic Auth")
//    public void testApiInvokeFailWithBasicAuth() throws Exception {
//        //Valid Credentials
//        String originalInput = "generalUser1:password";
//        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());
//
//        //test endpoint
//        invokeBasic(basicAuthToken, 401);
//
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException ex) {
//            Assert.fail("thread sleep interrupted!");
//        }
//    }
//
//    private void invokeBasic(String token, int responseCode) throws Exception {
//        Map<String, String> headers = new HashMap<>();
//        //test endpoint with token
//        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + token);
//        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
//                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
//        Assert.assertNotNull(response);
//        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
//    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
