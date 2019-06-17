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
package org.wso2.micro.gateway.tests.validation;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.CLIExecutor;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class ValidationTestCase extends BaseTestCase {
    private String prodToken, sandToken, jwtTokenProd, jwtTokenSand, expiringJwtTokenProd, prodEndpoint, sandEndpoint;

    @BeforeClass
    public void start() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
        String security = "oauth2";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        API api = new API();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        api.setProdEndpoint(getMockServiceURLHttp("/echo"));
        api.setSandEndpoint(getMockServiceURLHttp("/echo/invalidResponse"));
        prodEndpoint = api.getProdEndpoint();
        sandEndpoint = api.getSandEndpoint();
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

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 360000);
        jwtTokenSand = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_SANDBOX, 360000);
        //generate apis with CLI and start the micro gateway server
        CLIExecutor cliExecutor;

        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generateFromDefinition(project, "validation" + File.separator + "PizzaShackAPI_swagger.json");

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "validation.conf").getPath();
        String[] args = {"--config", configPath};
        microGWServer.startMicroGwServer(balPath, args);
    }

    @Test(description = "Test valid request and valid response with a JWT token")
    public void testValidRequestAndValidResponseWithJWT() throws Exception {
        //test valid request and valid response with a JWT token
        invokeValidRequestAndValidResponse(jwtTokenProd, MockHttpServer.ECHO_ENDPOINT_RESPONSE, 200);
    }

    @Test(description = "Test invalid request with a JWT token")
    public void testInvalidRequestWithJWT() throws Exception {
        //test invalid request with a JWT token
        invokeInvalidRequest(jwtTokenProd, MockHttpServer.ECHO_ENDPOINT_RESPONSE_FOR_INVALID_REQUEST, 422);
    }

    @Test(description = "Test invalid response with a JWT token")
    public void testInvalidResponseWithJWT() throws Exception {
        //test invalid response with a JWT token
        invokeInvalidResponse(jwtTokenSand, MockHttpServer.ERROR_MESSAGE_FOR_INVALID_RESPONSE, 500);
    }

    private void invokeValidRequestAndValidResponse(String token, String responseData, int responseCode) throws
            Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.ACCEPT.toString(), "application/json");
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("/pizzashack/1.0.0/order"), MockHttpServer.ECHO_ENDPOINT_RESPONSE,
                        headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), responseData);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    private void invokeInvalidRequest(String token, String responseData, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.ACCEPT.toString(), "application/json");
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("/pizzashack/1.0.0/order"), MockHttpServer.INVALID_POSTBODY,
                        headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), responseData);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    private void invokeInvalidResponse(String token, String responseData, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.ACCEPT.toString(), "application/json");
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), responseData);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
