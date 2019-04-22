/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.tests.jwtRevocation;

//uncomment when running this test case only
//import io.ballerina.messaging.broker.EmbeddedBroker;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.CLIExecutor;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.MockETCDServer;
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.ClientHelper;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.testng.Assert.assertEquals;

/**
 * JWT Revocation Test Class
 */
public class JWTRevocationSupportTestCase extends BaseTestCase {

    private String jwtTokenProd;
    private String jti = "2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d";
    private MockETCDServer mockETCDServer;
    private MessageConsumer consumer;

    @BeforeClass
    public void start() throws Exception {
        initializeEtcdServer();

        String security = "oauth2";
        String balPath, configPath = "";
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

        //Generate the relevant JWT tokens
        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);

        //Extract the JWT token
        int firstDotSeparatorIndex = jwtTokenProd.indexOf('.');
        int secondSeparatorIndex = jwtTokenProd.indexOf('.', firstDotSeparatorIndex + 1);
        String JWTToken = jwtTokenProd.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
        byte[] decodedJwt = Base64.decodeBase64(JWTToken.getBytes());
        JSONObject jsonObject = new JSONObject(new String(decodedJwt));
        jti = jsonObject.get("jti").toString();

        //prepareConfigValues();

        //uncomment to run this test case only
        //Initialize the JMS message broker
        //broker = new EmbeddedBroker();
        //startMessageBroker();

        //generate apis with CLI and start the micro gateway server
        CLIExecutor cliExecutor;

        //Initialize the Micro-Gateway Server
        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project, security);

        balPath = CLIExecutor.getInstance().getLabelBalx(project);
        try {
            configPath = getClass().getClassLoader().getResource("confs" + File.separator + "default-test-config.conf")
                    .getPath();
        } catch (NullPointerException e) {
            Assert.fail("Should not throw any exceptions" + e);
        }

        String ballerinaLogging = "b7a.log.level=TRACE";

        //Starting the Micro-Gateway Server
        String[] args = { "--config", configPath, "-e", ballerinaLogging };
        microGWServer.startMicroGwServer(balPath, args);

        //Send Extracted JTI to the jwtRevocation Topic
        publishMessage();

    }

    /**
     * Method to start the mock ETCD server
     */
    private void initializeEtcdServer() {
        int MOCK_ETCD_PORT = 2379;
        boolean isOpen = Utils.isPortOpen(MOCK_ETCD_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_ETCD_PORT + " already in use.");
        mockETCDServer = new MockETCDServer(MOCK_ETCD_PORT);
        mockETCDServer.start();
    }

    @Test(description = "Test to check retireving all revoked keys from ETCD server")
    public void testEtcdAllRevokedTokenLookup()
            throws Exception {

        //test etcd all keys endpoint
        String requestUrl = "https://localhost:2379/v2/keys/jti";
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpClientRequest.doGet(requestUrl, headers);
        Utils.assertResult(response, MockETCDServer.ALL_KEYS_RESPONSE, 200);
        if (response.getResponseCode() != 200) {
            retryPolicy(MockETCDServer.ALL_KEYS_RESPONSE, 200);
        }

    }

    @Test(description = "Test to check revoked token response")
    public void testRevokedTokenResponse() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 401, "Unauthorized");
        Assert.assertTrue(
                response.getData().contains("Invalid Credentials. Make sure you have given the correct access token"),
                "Invalid Credentials");

    }

    @Test(description = "Test to check the msessage content of JMS JWT revoked message")
    public void testJMSRevocationTopicMessage() throws Exception {

        //test JMS message
        createSubscriberJMSConnection();
        publishMessage();
        Message message = consumer.receive();
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            assertEquals(textMessage.getText(), jti);
        } else if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            assertEquals(mapMessage.getString("revokedToken"), jti);
        }
    }

    /**
     * Method to retry ETCD all jti request
     * @param responseData Expected resoponse data
     * @param responseCode Expected resoponse code
     * @throws Exception Error while sending GET request
     */
    private void retryPolicy(String responseData, int responseCode) throws Exception {
        boolean testPassed = false;
        for (int retries = 0; retries < 5; retries++) {
            Utils.delay(1000);
            String requestUrl = "https://localhost:2379/v2/keys/jti";
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
            HttpResponse response = HttpClientRequest.doGet(requestUrl, headers);
            if (response.getData().equals(responseData) && response.getResponseCode() == responseCode) {
                testPassed = true;
                break;
            }
        }

        if (!testPassed) {
            Assert.fail();
        }
    }

    //uncomment to run this test case only
//    private void startMessageBroker() throws Exception {
//        broker.start();
//    }

    /**
     * Method to publish a messege to JwtRevocation topic
     * @throws NamingException Error thrown while handling initial context
     * @throws JMSException Error thrown while creating JMS connection
     */
    private void publishMessage() throws NamingException, JMSException {

        String topicName = "jwtRevocation";
        InitialContext initialContext = ClientHelper.getInitialContextBuilder("admin", "admin", "localhost", "5672")
                .withTopic(topicName).build();
        ConnectionFactory connectionFactory = (ConnectionFactory) initialContext
                .lookup(ClientHelper.CONNECTION_FACTORY);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = (Topic) initialContext.lookup(topicName);
        MessageProducer producer = session.createProducer(topic);
        MapMessage message = session.createMapMessage();
        message.setString("revokedToken", jti);
        message.setString("ttl", "3600");
        producer.send(message);
        connection.close();
    }

    /**
     * Method to create a subscriber for jwtRevocation topic
     * @throws JMSException Error thrown while creating JMS connection
     * @throws NamingException Error thrown while handling initial context
     */
    private void createSubscriberJMSConnection() throws JMSException, NamingException {

        String topicName = "jwtRevocation";
        InitialContext initialContext = ClientHelper.getInitialContextBuilder("admin", "admin", "localhost", "5672")
                .withTopic(topicName).build();
        ConnectionFactory connectionFactory = (ConnectionFactory) initialContext
                .lookup(ClientHelper.CONNECTION_FACTORY);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = (Topic) initialContext.lookup(topicName);
        consumer = session.createDurableSubscriber(topic, "jwtRevocation");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        microGWServer.stopServer(true);
        mockETCDServer.stopIt();
        mockHttpServer.stopIt();
        super.finalize();

    }
}
