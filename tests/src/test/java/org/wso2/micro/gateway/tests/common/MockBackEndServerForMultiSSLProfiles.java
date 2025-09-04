/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.tests.common;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.TokenManagementConstants;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.io.FileInputStream;

/**
 * Mock HTTP server for testing Open API tests.
 */
public class MockBackEndServerForMultiSSLProfiles extends Thread {

    private static final Logger log = LoggerFactory.getLogger(MockHttpServer.class);
    private HttpsServer httpServer;
    private HttpsServer incorrectHttpServer;
    private static int backEndServerPort;

    public static void main(String[] args) {

        MockBackEndServerForMultiSSLProfiles mockBackEndServer = new MockBackEndServerForMultiSSLProfiles(backEndServerPort);
        mockBackEndServer.start();
    }

    public MockBackEndServerForMultiSSLProfiles(int port) {

        backEndServerPort = port;
    }

    public void run() {

        if (backEndServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            httpServer = HttpsServer.create(new InetSocketAddress(backEndServerPort), 0);
            incorrectHttpServer = HttpsServer.create(new InetSocketAddress(backEndServerPort + 1), 0);

            httpServer.setHttpsConfigurator(new HttpsConfigurator(
                    getSslContext("keyStores/dynamicSSL/KS.p12", "keyStores/dynamicSSL/TS.p12")) {
                public void configure(HttpsParameters params) {

                    try {
                        // initialise the SSL context
                        SSLContext sslContext = SSLContext.getDefault();
                        SSLEngine engine = sslContext.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        // get the default parameters
                        SSLParameters defaultSSLParameters = sslContext
                                .getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (Exception ex) {
                        log.error("Failed to create HTTPS port");
                    }
                }
            });

            incorrectHttpServer.setHttpsConfigurator(new HttpsConfigurator(
                    getSslContext("keyStores/dynamicSSL/keystore.p12", "keyStores/dynamicSSL/truststore.p12")) {
                public void configure(HttpsParameters params) {

                    try {
                        // initialise the SSL context
                        SSLContext sslContext = SSLContext.getDefault();
                        SSLEngine engine = sslContext.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        // get the default parameters
                        SSLParameters defaultSSLParameters = sslContext
                                .getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (Exception ex) {
                        log.error("Failed to create HTTPS port");
                    }
                }
            });
            String base = "/v1";
            httpServer.createContext(base + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.responseBodyV1.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            incorrectHttpServer.createContext(base + "/pet/2", exchange -> {
                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            incorrectHttpServer.createContext(base + "/pet/", exchange -> {
                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            httpServer.start();
            incorrectHttpServer.start();
        } catch (Exception e) {
            log.error("Error occurred while setting up mock server", e);
        }
    }

    public void stopIt() {

        httpServer.stop(0);
        incorrectHttpServer.stop(0);
    }

    private SSLContext getSslContext(String keystorePath, String truststorePath) throws Exception {
        final char[] password = "ballerina".toCharArray();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        String keyPath = getClass().getClassLoader()
                .getResource(keystorePath).getPath();
        final InputStream is = new FileInputStream(keyPath);
        keyStore.load(is, password);

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        String trustPath = getClass().getClassLoader()
                .getResource(truststorePath).getPath();
        final InputStream tsIn = new FileInputStream(trustPath);
        trustStore.load(tsIn, password);

        String kmAlg = KeyManagerFactory.getDefaultAlgorithm();
        String tmAlg = TrustManagerFactory.getDefaultAlgorithm();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmAlg);
        kmf.init(keyStore, password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmAlg);
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

}

