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
package org.wso2.micro.gateway.tests.common;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import io.netty.handler.codec.http.HttpHeaderNames;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.TokenManagementConstants;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * Mock HTTP server for testing Open API tests.
 */
public class MockBackEndServer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(MockHttpServer.class);
    private HttpsServer httpServer;
    private String backEndServerUrl;
    private static int backEndServerPort;

    public static void main(String[] args) {

        MockBackEndServer mockBackEndServer = new MockBackEndServer(backEndServerPort);
        mockBackEndServer.start();
    }

    public MockBackEndServer(int port) {

        backEndServerPort = port;
    }

    public void run() {

        if (backEndServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            httpServer = HttpsServer.create(new InetSocketAddress(backEndServerPort), 0);
            httpServer.setHttpsConfigurator(new HttpsConfigurator(getSslContext()) {
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
            String context = "/v2";
            httpServer.createContext(context + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.responseBody.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/", exchange -> {

                byte[] response = ResponseConstants.petByIdResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
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
            httpServer.createContext(base + "/pet/", exchange -> {

                byte[] response = ResponseConstants.petByIdResponseV1.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.start();
            backEndServerUrl = "http://localhost:" + backEndServerPort;
        } catch (Exception e) {
            log.error("Error occurred while setting up mock server", e);
        }
    }

    public void stopIt() {

        httpServer.stop(0);
    }


    private SSLContext getSslContext() throws Exception {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        // initialise the keystore
        char[] password = "wso2carbon".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream("wso2carbon.jks");
        ks.load(fis, password);
        // setup the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);
        // setup the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        // setup the HTTPS context and parameters
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

}

