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
package org.wso2.micro.gateway.tests.serviceDiscovery;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
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
import org.wso2.micro.gateway.tests.util.EtcdClient;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EtcdSupportTestCase extends BaseTestCase {
    private String jwtTokenProd, jwtTokenSand, balPath, configPath;
    private String etcdUrlParameter;
    private String etcdusername = "root";
    private String etcdpassword = "root";
    private String etcdUsernameParameter;
    private String etcdPasswordParameter;
    private String pizzaShackEndpointSandConfigValue;
    private String base64EncodedpizzaShackEndpointProdPrefix;
    private String base64EncodedpizzaShackEndpointSandPrefix;
    private String pizzaShackProdConfigValue;
    private String pizzaShackProdEtcdKey = "pizzashackprod";
    private String pizzaShackProdParameter;
    private String pizzaShackSandConfigValue;
    private String pizzaShackSandEtcdKey = "pizzashacksand";
    private String pizzaShackSandParameter;
    private String pizzaShackSandNewEndpoint = "https://localhost:9443/echo/newsand";
    private String etcdTimerParameter;
    private String overridingEndpointParameter;
    private String base64EncodedPizzaShackProdKey;
    private String base64EncodedPizzaShackSandKey;
    private String base64EncodedPizzaShackProdValue;
    private String base64EncodedPizzaShackSandValue;
    private String base64EncodedPizzaShackProdNewValue;
    private String base64EncodedPizzaShackSandNewValue;
    private final static String INVALID_URL_AT_ETCD_RESPONSE = "{\"fault\":{\"code\":\"101505\", \"message\":\"Runtime Error\", \"description\":\"URL defined at etcd for key pizzashackprod is invalid\"}}";
    private EtcdClient etcdClient;
    private boolean etcdAuthenticationEnabled = true;

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
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generatePassingFlag(label, project, "etcd-enable");

        balPath = CLIExecutor.getInstance().getLabelBalx(project);
        configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "default-test-config.conf").getPath();

        encodeValuesToBase64();
        prepareConfigValues();
        prepareCLIParameters();
        initializeEtcdServer();
    }

    private void initializeEtcdServer() throws Exception{
        String etcdUrl;
        String etcdrole = "root";
        String etcd_host = System.getenv("ETCD_HOST");
        String etcd_port = System.getenv("PORT");
        etcdClient = new EtcdClient(etcd_host, etcd_port);
        etcdUrl = etcdClient.getEtcdUrl();
        String etcdUrlConfigValue = "etcdurl";
        etcdUrlParameter =  etcdUrlConfigValue + "=" + etcdUrl;

        //create root user in etcd
        etcdClient.createUser(etcdusername, etcdpassword);

        //create root role in etcd
        etcdClient.createRole(etcdrole);

        //grant root role to root user
        etcdClient.addRoleToUser(etcdusername, etcdrole);

        //enable etcd authentication
        etcdClient.enableAuthentication();

        //authenticate root user and retrieve token
        String token = etcdClient.authenticate();

        //add pizzashackprod and corresponding url to etcd. The key and value should be encoded in base64 format
        etcdClient.addKeyValuePair(token, base64EncodedPizzaShackProdKey, base64EncodedPizzaShackProdValue);
    }

    private void encodeValuesToBase64() throws Exception{
        String pizzaShackEndpointProdPrefix = "PizzaShackAPI_1.0.0_prod";
        String pizzaShackEndpointSandPrefix = "PizzaShackAPI_1.0.0_sand";
        String pizzaShackProdEndpoint = "https://localhost:9443/echo/prod";
        String pizzaShackProdNewEndpoint = "https://localhost:9443/echo/newprod";
        String pizzaShackSandEndpoint = "https://localhost:9443/echo/sand";
        base64EncodedPizzaShackProdKey = Utils.encodeValueToBase64(pizzaShackProdEtcdKey);
        base64EncodedPizzaShackSandKey = Utils.encodeValueToBase64(pizzaShackSandEtcdKey);
        base64EncodedPizzaShackProdValue = Utils.encodeValueToBase64(pizzaShackProdEndpoint);
        base64EncodedPizzaShackSandValue = Utils.encodeValueToBase64(pizzaShackSandEndpoint);
        base64EncodedPizzaShackProdNewValue = Utils.encodeValueToBase64(pizzaShackProdNewEndpoint);
        base64EncodedPizzaShackSandNewValue = Utils.encodeValueToBase64(pizzaShackSandNewEndpoint);
        base64EncodedpizzaShackEndpointProdPrefix = Utils.encodeValueToBase64(pizzaShackEndpointProdPrefix);
        base64EncodedpizzaShackEndpointSandPrefix = Utils.encodeValueToBase64(pizzaShackEndpointSandPrefix);
    }

    private void prepareConfigValues(){
        String apiEndpointSuffix = "endpoint_0";
        String etcdKeySuffix = "etcdKey";
        pizzaShackEndpointSandConfigValue = base64EncodedpizzaShackEndpointSandPrefix + "_" + apiEndpointSuffix;
        pizzaShackProdConfigValue = base64EncodedpizzaShackEndpointProdPrefix + "_" + etcdKeySuffix;
        pizzaShackSandConfigValue = base64EncodedpizzaShackEndpointSandPrefix + "_" + etcdKeySuffix;
    }

    private void prepareCLIParameters(){
        String etcdUsernameConfigValue = "etcdusername";
        String etcdPasswordConfigValue = "etcdpassword";
        String etcdTimerConfigValue = "etcdtimer";
        String etcdTimer = "1000";
        etcdUsernameParameter = etcdUsernameConfigValue + "=" + etcdusername;
        etcdPasswordParameter = etcdPasswordConfigValue + "=" + etcdpassword;
        pizzaShackProdParameter = pizzaShackProdConfigValue + "=" + pizzaShackProdEtcdKey;
        pizzaShackSandParameter = pizzaShackSandConfigValue + "=" + pizzaShackSandEtcdKey;
        etcdTimerParameter = etcdTimerConfigValue + "=" + etcdTimer;
        overridingEndpointParameter = pizzaShackEndpointSandConfigValue + "=" + pizzaShackSandNewEndpoint;
    }

    @Test(description = "Test Etcd Support Providing all correct arguments")
    public void testEtcdSupport() throws Exception {

        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        delay();
        //add a sleep to make sure the periodic task capture the new endpoint url
//        try {
//            Thread.sleep(3000);
//        } catch(InterruptedException ex) {
//            Assert.fail("thread sleep interrupted!");
//        }

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support by changing the api url at the etcd node")
    public void testEtcdSupportApiUrlChanged() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        //change the prod endpoint url at etcd node
        String token = etcdClient.authenticate();
        etcdClient.addKeyValuePair(token, base64EncodedPizzaShackProdKey, base64EncodedPizzaShackProdNewValue);

        delay();
        //add a sleep to make sure the periodic task capture the new endpoint url
//        try {
//            Thread.sleep(3000);
//        } catch(InterruptedException ex) {
//            Assert.fail("thread sleep interrupted!");
//        }

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_NEW_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support Providing all correct arguments but provided keys not defined in Etcd Node")
    public void testMissingKeysInEtcd() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //sandbox key is not present at etcd. So invoke the sandbox endpoint
        invoke(jwtTokenSand, MockHttpServer.SAND_ENDPOINT_RESPONSE, 200);

        //add a new value to the relevant sandbox key in etcd
        String token = etcdClient.authenticate();
        etcdClient.addKeyValuePair(token, base64EncodedPizzaShackSandKey, base64EncodedPizzaShackSandNewValue);

        delay();
        //add a sleep to make sure the periodic task capture the new endpoint url
//        try {
//            Thread.sleep(3000);
//        } catch(InterruptedException ex) {
//            Assert.fail("thread sleep interrupted!");
//        }

        //test the sandbox endpoint
        invoke(jwtTokenSand, MockHttpServer.SAND_ENDPOINT_NEW_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support without providing relevant etcd keys")
    public void testWithoutProvidingKeys() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        delay();
        //add a sleep to check whether the periodic task has been stopped since no etcd keys were provided
//        try {
//            Thread.sleep(3000);
//        } catch(InterruptedException ex) {
//            Assert.fail("thread sleep interrupted!");
//        }

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when etcd authentication fails")
    public void testEtcdAuthenticationFailure() throws Exception {
        String invalidetcdusername = "etcdusername=invalid";
        String invalidetcdpassword = "etcdpassword=invalid";
        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", invalidetcdusername, "-e", invalidetcdpassword, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when incorrect Etcd URL is provided")
    public void testWithIncorrectEtcdUrl() throws Exception {
        String incorrectetcdUrl = "etcdurl=http://127.0.0.1:2389";
        String[] args = { "--config", configPath, "-e", incorrectetcdUrl, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter};
        microGWServer.startMicroGwServer(balPath, args);

        //test prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support without providing Etcd URL")
    public void testWithoutEtcdUrl() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support by changing the api url at the etcd node")
    public void testOverridingEndpointUrl() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter, "-e", overridingEndpointParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //test sand endpoint
        invoke(jwtTokenSand, MockHttpServer.SAND_ENDPOINT_NEW_RESPONSE, 200);

        //change the sand endpoint url at etcd node
        String token = etcdClient.authenticate();
        etcdClient.addKeyValuePair(token, base64EncodedPizzaShackSandKey, base64EncodedPizzaShackSandValue);

        //add a sleep to make sure the periodic task capture the new endpoint url
        try {
            Thread.sleep(3000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test the prod endpoint
        invoke(jwtTokenSand, MockHttpServer.SAND_ENDPOINT_RESPONSE, 200);

        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when the URL defined at etcd corresponding to a key is invalid")
    public void testInvalidUrlAtEtcd() throws Exception {
        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //insert an invalid url for the pizzashackprod key at etcd node
        String token = etcdClient.authenticate();
        String invalidUrlValue = "abcd";
        etcdClient.addKeyValuePair(token, base64EncodedPizzaShackProdKey, Utils.encodeValueToBase64(invalidUrlValue));

        //add a sleep to make sure the periodic task capture the new endpoint url
        try {
            Thread.sleep(3000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test the prod endpoint
        invoke(jwtTokenProd, INVALID_URL_AT_ETCD_RESPONSE, 500);

        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when etcd credentials are provided, but etcd authentication is disabled")
    public void testCredentialsProvidedEtcdAuthDisabled() throws Exception {
        //disabling the etcd server authentication
        String token = etcdClient.authenticate();
        etcdClient.disableAuthentication(token);
        etcdAuthenticationEnabled = false;

        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", etcdUsernameParameter, "-e", etcdPasswordParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        //change the prod endpoint url at etcd node
        etcdClient.addKeyValuePair(base64EncodedPizzaShackProdKey, base64EncodedPizzaShackProdNewValue);

        //add a sleep to make sure the periodic task capture the new endpoint url
        try {
            Thread.sleep(3000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_NEW_RESPONSE, 200);
        microGWServer.stopServer(false);
    }

    @Test(description = "Test Etcd Support when etcd credentials are not provided, but etcd authentication is disabled")
    public void testCredentialsNotProvidedEtcdAuthDisabled() throws Exception {
        //disabling the etcd server authentication
        String token = etcdClient.authenticate();
        etcdClient.disableAuthentication(token);
        etcdAuthenticationEnabled = false;

        String[] args = { "--config", configPath, "-e", etcdUrlParameter, "-e", pizzaShackProdParameter, "-e", pizzaShackSandParameter, "-e", etcdTimerParameter };
        microGWServer.startMicroGwServer(balPath, args);

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_RESPONSE, 200);

        //change the prod endpoint url at etcd node
        etcdClient.addKeyValuePair(base64EncodedPizzaShackProdKey, base64EncodedPizzaShackProdNewValue);

        //add a sleep to make sure the periodic task capture the new endpoint url
        try {
            Thread.sleep(3000);
        } catch(InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }

        //test the prod endpoint
        invoke(jwtTokenProd, MockHttpServer.PROD_ENDPOINT_NEW_RESPONSE, 200);
        microGWServer.stopServer(false);
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

    private void delay(){
        long startTime = System.currentTimeMillis();
        for (int count = 0; ;count++) {
            long now = System.currentTimeMillis();
            if(now - startTime >= 2000)
                break;
            // Do nothing
        }
    }

    @AfterMethod
    public void etcdInitialState() throws Exception {
        if(!etcdAuthenticationEnabled) {
            etcdClient.enableAuthentication();
        }
        String token = etcdClient.authenticate();
        etcdClient.addKeyValuePair(token, base64EncodedPizzaShackProdKey, base64EncodedPizzaShackProdValue);
        etcdClient.deleteKeyValuePair(token, base64EncodedPizzaShackSandKey);
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}