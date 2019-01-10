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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.CLIExecutor;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;


/**
 * Testing the pizza_shack api rest for mutualSSL feature
 */

public class MutualSSLTestCase extends BaseTestCase {

    private static final Log log = LogFactory.getLog(MutualSSLTestCase.class);

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
        //set security schemas
        String security = "oauth2";

        CLIExecutor cliExecutor;
        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project,security);

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "mutualSSL-test.conf").getPath();
        String[] args = {"--config", configPath};
        System.out.println("MTSL TEST CASE");
        microGWServer.startMicroGwServer(balPath, args);

    }


    @Test(description = "mutual SSL is properly established with ballerina keystore and trust store")
    public void mutualSSLEstablished() throws Exception {

        String trustStorePath = getClass().getClassLoader()
                .getResource("keyStores" + File.separator + "ballerinaTruststore.p12").getPath();
        Properties systemProps = System.getProperties();
        systemProps.put("javax.net.debug", "ssl");
        systemProps.put("javax.net.ssl.trustStore", trustStorePath);
        systemProps.put("javax.net.ssl.trustStorePassword", "ballerina");
        System.setProperties(systemProps);

        SSLContext sslcontext;
        KeyStore keyStore;
        final char[] P12_PASSWORD = "ballerina".toCharArray();
        final char[] KEY_PASSWORD = "ballerina".toCharArray();

        try {

            String keyPath = getClass().getClassLoader()
                    .getResource("keyStores" + File.separator + "ballerinaKeystore.p12").getPath();
            final InputStream is = new FileInputStream(keyPath);
            keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(is, P12_PASSWORD);
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, KEY_PASSWORD);

            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(kmf.getKeyManagers(), null, new SecureRandom());
        } catch (Exception ex) {
            throw new IllegalStateException("Failure initializing default SSL context", ex);
        }

        SSLSocketFactory sslsocketfactory = sslcontext.getSocketFactory();
        try {

            URL url = new URL("https://localhost:9595/pizzashack/1.0.0/menu");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslsocketfactory);
            InputStream inputstream = conn.getInputStream();
            log.info("Test is working properly");
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String example = bufferedreader.readLine();
            log.info(example);

            while (example != null) {
                log.info("Received " + example);
                break;
            }

        } catch (UnknownHostException e) {
            log.error("An UnknownHostException occurred: ", e);
        } catch (IOException e) {
            log.error("An IOException occurred: " + e);

        }
    }

    @Test(description = "mutual SSL is filed due to bad certificate")
    public void mutualSSLfail() throws Exception {

        String trustStorePath = getClass().getClassLoader()
                .getResource("keyStores" + File.separator + "ballerinaTruststore.p12").getPath();
        Properties systemProps = System.getProperties();
        systemProps.put("javax.net.debug", "ssl");
        systemProps.put("javax.net.ssl.trustStore", trustStorePath);
        systemProps.put("javax.net.ssl.trustStorePassword", "ballerina");
        System.setProperties(systemProps);
        SSLContext sslcontext;
        KeyStore keyStore;
        final char[] P12_PASSWORD = "ballerina".toCharArray();
        final char[] KEY_PASSWORD = "ballerina".toCharArray();

        try {
            String keyPath = getClass().getClassLoader()
                    .getResource("keyStores" + File.separator + "mtsltestFail.p12").getPath();
            final InputStream is = new FileInputStream(keyPath);
            keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(is, P12_PASSWORD);
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, KEY_PASSWORD);
            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
        } catch (Exception ex) {
            throw new IllegalStateException("Failure initializing default SSL context", ex);
        }

        SSLSocketFactory sslsocketfactory = sslcontext.getSocketFactory();
        try {

            URL url = new URL("https://localhost:9595/pizzashack/1.0.0/menu");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslsocketfactory);
            InputStream inputstream = conn.getInputStream();
            log.info("Test is working properly ");
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String string = null;
            while ((string = bufferedreader.readLine()) != null) {
                log.info("Received " + string);
                break;
            }

        } catch (IOException e) {
            String x = e.toString();
            if (x.equalsIgnoreCase("javax.net.ssl.SSLHandshakeException: Received fatal alert: bad_certificate")) {
                log.info("Test is working properly");
            }
        }

    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
