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

import org.testng.Assert;
import org.testng.TestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.*;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.listener.TestNGListener;

import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.*;
import java.util.Properties;


/**
 * Testing the pizza_shack api rest for mutualSSL feature
 */

public class MutualSSLTestCase extends BaseTestCase{

    @BeforeClass
    private void setup() throws Exception {
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

        CLIExecutor cliExecutor;

        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project);

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "mutualSSL-test.conf").getPath();
        String[] args = { "--config", configPath };
        System.out.println("MTSL TEST CASE");
        microGWServer.startMicroGwServer(balPath, args);

    }


    @Test(description = "mutual SSL is properly established with ballerina keystore and trust store")
    public void mutualSSLEstablished()throws Exception{

        String trustStorePath = getClass().getClassLoader()
                .getResource("keyStores" + File.separator + "ballerinaTruststore.p12").getPath();
        Properties systemProps = System.getProperties();
        systemProps.put("javax.net.debug","ssl");
        systemProps.put("javax.net.ssl.trustStore", trustStorePath);
        systemProps.put("javax.net.ssl.trustStorePassword","ballerina");
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

            sslcontext=SSLContext.getInstance("TLS");
            sslcontext.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
        } catch (Exception ex) {
            throw new IllegalStateException("Failure initializing default SSL context", ex);
        }

        SSLSocketFactory sslsocketfactory = sslcontext.getSocketFactory();


        try {
//
//            int port= 9595;
//            String host="localhost" ;
//            int connectTimeout=100000;
//            SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket();
//            sslsocket.connect(new InetSocketAddress(host, port), connectTimeout);
//            sslsocket.startHandshake();
            URL url = new URL("https://localhost:9595/pizzashack/1.0.0/menu");
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setSSLSocketFactory(sslsocketfactory);
            InputStream inputstream = conn.getInputStream();
            System.out.println("Test is working properly");
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String example=bufferedreader.readLine();
            System.out.println(example);

        while (example != null) {
            System.out.println("Received " + example);
            break;
        }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    @Test(description = "mutual SSL is filed due to bad certificate")
    public void mutualSSLfail()throws Exception{

        String trustStorePath = getClass().getClassLoader()
                .getResource("keyStores" + File.separator + "ballerinaTruststore.p12").getPath();
        Properties systemProps = System.getProperties();
        systemProps.put("javax.net.debug","ssl");
        systemProps.put("javax.net.ssl.trustStore", trustStorePath);
        systemProps.put("javax.net.ssl.trustStorePassword","ballerina");
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
            sslcontext=SSLContext.getInstance("TLS");
            sslcontext.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
        } catch (Exception ex) {
            throw new IllegalStateException("Failure initializing default SSL context", ex);
        }

        SSLSocketFactory sslsocketfactory = sslcontext.getSocketFactory();


        try {

//            int port= 9595;
//            String host="localhost" ;
//            int connectTimeout=100000;
//            SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket();
//            sslsocket.connect(new InetSocketAddress(host, port), connectTimeout);
//            sslsocket.startHandshake();

            URL url = new URL("https://localhost:9595/pizzashack/1.0.0/menu");
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setSSLSocketFactory(sslsocketfactory);
            InputStream inputstream = conn.getInputStream();
            System.out.println("Test is working properly ");
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String example=bufferedreader.readLine();
            String string = null;
            while ((string = bufferedreader.readLine()) != null) {
            System.out.println("Received " + string);
            break;
        }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            String x =e.toString();
            if(x.equalsIgnoreCase("javax.net.ssl.SSLHandshakeException: Received fatal alert: bad_certificate")){
                System.out.println("Test is working properly");
            }
        }

    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }

}
