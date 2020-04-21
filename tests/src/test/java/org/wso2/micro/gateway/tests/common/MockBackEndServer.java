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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
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

                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/findByTags", exchange -> {

                byte[] response = ResponseConstants.petByIdResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/store/inventory", exchange -> {

                byte[] response = ResponseConstants.storeInventoryResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/3", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_VALID_JWT_TRANSFORMER.getBytes();
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
            httpServer.createContext(base + "/pet/findByTags", exchange -> {

                byte[] response = ResponseConstants.petByIdResponseV1.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/pet/2", exchange -> {

                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/pet/", exchange -> {

                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            //to test endpoints with security
            String contextWithSecurity2 = "/v2Basic";
            httpServer.createContext(contextWithSecurity2 + "/pet/findByStatus", exchange -> {
                byte[] response;
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46YWRtaW4="))
                {
                    response = ResponseConstants.responseBody.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextWithSecurity2 + "/pet/", exchange -> {
                byte[] response;
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46YWRtaW4="))
                {
                    response = ResponseConstants.petByIdResponse.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextWithSecurity2 + "/pet/findByTags", exchange -> {
                byte[] response;
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46YWRtaW4="))
                {
                    response = ResponseConstants.petByIdResponse.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextWithSecurity2 + "/store/inventory", exchange -> {
                byte[] response;
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46YWRtaW4="))
                {
                    response = ResponseConstants.storeInventoryResponse.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            String contextWithSecurity1 = "/v1Basic";
            httpServer.createContext(contextWithSecurity1 + "/pet/findByStatus", exchange -> {
                byte[] response;
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46YWRtaW4="))
                {
                    response = ResponseConstants.responseBodyV1.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextWithSecurity1 + "/pet/findByTags", exchange -> {
                byte[] response;
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46YWRtaW4="))
                {
                    response = ResponseConstants.petByIdResponseV1.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                            TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/store/inventory", exchange -> {

                byte[] response = ResponseConstants.storeInventoryResponse.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            String contextV3 = "/v3";
            httpServer.createContext(contextV3 + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.responseBodyV1.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextV3 + "/store/order", exchange -> {

                int length;
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                InputStream is =  exchange.getRequestBody();
                byte[] buffer = new byte[1024];
                while ((length = is.read(buffer)) != -1 ) {
                    os.write(buffer, 0, length);
                }
                byte [] response  = os.toByteArray();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextV3 + "/pet/", exchange -> {

                InputStream is =  exchange.getRequestBody();
                byte [] response = IOUtils.toByteArray(is);
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

