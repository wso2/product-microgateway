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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.CLIExecutor;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.common.MockEtcdServer;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EtcdSupportTestCase extends BaseTestCase {
    private String jwtTokenProd, jwtTokenSand, balPath, configPath;
    private String etcdUrl = "etcdurl=http://127.0.0.1:3379";
    private String etcdusername = "etcdusername=etcd";
    private String etcdpassword = "etcdpassword=etcd";
    private String pizzaShackProdEtcdKey = "PizzaShackAPI.1.0.0.prod.etcd.key=pizzashackprod";
    private String pizzaShackSandEtcdKey = "PizzaShackAPI.1.0.0.sand.etcd.key=pizzashacksand";
    private String etcdTimer = "etcdTimer=3000";
    private MockEtcdServer mockEtcdServer;
    private final static int MOCK_ETCD_SERVER_PORT = 3379;

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

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);
        jwtTokenSand = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_SANDBOX, 3600);

        //generate apis with CLI and start the micro gateway server
        CLIExecutor cliExecutor;

        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
        isOpen = Utils.isPortOpen(MOCK_ETCD_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_ETCD_SERVER_PORT + " already in use.");
        mockEtcdServer = new MockEtcdServer(MOCK_ETCD_SERVER_PORT);
        mockEtcdServer.start();
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project, "etcd-enable");

        balPath = CLIExecutor.getInstance().getLabelBalx(project);
        configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "default-test-config.conf").getPath();
    }

    @Test(description = "Test Etcd Support Providing all correct arguments")
    public void testEtcdSupport() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrl, "-e", etcdusername, "-e", etcdpassword, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support by changing the api url at the etcd node")
    public void testEtcdSupportApiUrlChanged() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrl, "-e", etcdusername, "-e", etcdpassword, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        //change the prod endpoint url at etcd node
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost("http://localhost:3379/v3alpha/kv/put", "pizzashackprod=https://localhost:9443/echo/newprod", headers);

        //add a sleep to make sure the periodic task capture the new endpoint url
        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_NEW_RESPONSE, 200);

        //change the new value of pizzashackprod key to the initial value
        response = HttpClientRequest
                .doPost("http://localhost:3379/v3alpha/kv/put", "pizzashackprod=https://localhost:9443/echo/prod", headers);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support Providing all correct arguments but provided keys not defined in Etcd Node")
    public void testMissingKeysInEtcd() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrl, "-e", etcdusername, "-e", etcdpassword, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //sandbox key is not present at etcd. So invoke the sandbox endpoint
        invoke(jwtTokenSand, MockHttpServer.SAND_ENDPOINT_RESPONSE, 200);

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        //add a new value to the relevant sandbox key in etcd
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost("http://localhost:3379/v3alpha/kv/put", "pizzashacksand=https://localhost:9443/echo/newsand", headers);

        //add a sleep to make sure the periodic task capture the new endpoint url
        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test the sandbox endpoint
        invoke(jwtTokenSand, MockHttpServer.SAND_ENDPOINT_NEW_RESPONSE, 200);

        //remove the added key-value pair to bring the etcd kv store to the initial state
        response = HttpClientRequest
                .doPost("http://localhost:3379/v3alpha/kv/delete", "pizzashacksand", headers);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support without providing relevant etcd keys")
    public void testWithoutProvidingKeys() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrl, "-e", etcdusername, "-e", etcdpassword, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //add a sleep to check whether the periodic task has been stopped since no etcd keys were provided
        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when etcd authentication fails")
    public void testEtcdAuthenticationFailure() throws Exception {
        String invalidetcdusername = "etcdusername=invalid";
        String invalidetcdpassword = "etcdpassword=invalid";
        String[] args = { "--config", configPath, "-e", etcdUrl, "-e", invalidetcdusername, "-e", invalidetcdpassword, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when incorrect Etcd URL is provided")
    public void testWithIncorrectEtcdUrl() throws Exception {
        String incorrectetcdUrl = "etcdurl=http://127.0.0.1:2389";
        String[] args = { "--config", configPath, "-e", incorrectetcdUrl, "-e", etcdusername, "-e", etcdpassword, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer};
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support without providing Etcd URL")
    public void testWithoutEtcdUrl() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdusername, "-e", etcdpassword, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when etcd credentials are provided, but etcd authentication is disabled")
    public void testCredentialsProvidedEtcdAuthDisabled() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        //disabling the etcd server authentication
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("http://localhost:3379/v3alpha/auth/disable"), headers);

        String[] args = { "--config", configPath, "-e", etcdUrl, "-e", etcdusername, "-e", etcdpassword, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);

        //after testing the prod endpoint, enabling the etcd server authentication
        response = HttpClientRequest
                .doGet(getServiceURLHttp("http://localhost:3379/v3alpha/auth/enable"), headers);
    }

    @Test(description = "Test Etcd Support when etcd credentials are not provided, but etcd authentication is disabled")
    public void testCredentialsNotProvidedEtcdAuthDisabled() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        //disabling the etcd server authentication
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("http://localhost:3379/v3alpha/auth/disable"), headers);

        String[] args = { "--config", configPath, "-e", etcdUrl, "-e", pizzaShackProdEtcdKey, "-e", pizzaShackSandEtcdKey, "-e", etcdTimer };
        microGWServer.startMicroGwServer(balPath, args);

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);

        //after testing the prod endpoint, enabling the etcd server authentication
        response = HttpClientRequest
                .doGet(getServiceURLHttp("http://localhost:3379/v3alpha/auth/enable"), headers);
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

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}


