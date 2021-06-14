/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.mockbackend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.logging.Level;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * Mock HTTP server for testing Open API tests.
 */
public class MockBackEndServer extends Thread {

    private static final Logger logger = Logger.getLogger(MockBackEndServer.class.getName());
    private HttpServer httpServer;
    private String backEndServerUrl;
    private int backEndServerPort;
    private static boolean retryDone = false;
    private boolean secured = false;
    private boolean mtlsEnabled = false;

    public static void main(String[] args) {
        MockBackEndServer mockBackEndServer = new MockBackEndServer(Constants.MOCK_BACKEND_SERVER_PORT);
        MockSandboxServer mockSandboxServer = new MockSandboxServer(Constants.MOCK_SANDBOX_SERVER_PORT);
        // TODO: (VirajSalaka) start analytics server only when it requires
        MockAnalyticsServer mockAnalyticsServer = new MockAnalyticsServer(Constants.MOCK_ANALYTICS_SERVER_PORT);
        mockBackEndServer.start();
        mockSandboxServer.start();
        mockAnalyticsServer.start();
        if (args.length > 0 && args[0].equals("-tls-enabled")) {
            MockBackEndServer securedMockBackEndServer = new MockBackEndServer(Constants.SECURED_MOCK_BACKEND_SERVER_PORT,
                    true, false);
            MockBackEndServer mtlsMockBackEndServer = new MockBackEndServer(Constants.MTLS_MOCK_BACKEND_SERVER_PORT,
                    true, true);
            securedMockBackEndServer.start();
            mtlsMockBackEndServer.start();
        }
    }

    public MockBackEndServer(int port) {
        this.backEndServerPort = port;
    }

    public MockBackEndServer(int port, boolean isSecured, boolean mtlsEnabled) {
        this.secured = isSecured;
        this.backEndServerPort = port;
        this.mtlsEnabled = mtlsEnabled;
    }

    public void run() {

        if (backEndServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            if (this.secured) {
                httpServer = HttpsServer.create(new InetSocketAddress(backEndServerPort), 0);
                ((HttpsServer)httpServer).setHttpsConfigurator(new HttpsConfigurator(getSslContext()) {
                    public void configure(HttpsParameters params) {
                        try {
                            // initialise the SSL context
                            SSLContext sslContext = SSLContext.getDefault();
                            SSLEngine engine = sslContext.createSSLEngine();
                            params.setNeedClientAuth(mtlsEnabled);
                            params.setCipherSuites(engine.getEnabledCipherSuites());
                            params.setProtocols(engine.getEnabledProtocols());
                            // get the default parameters
                            SSLParameters defaultSSLParameters = sslContext
                                    .getDefaultSSLParameters();
                            params.setSSLParameters(defaultSSLParameters);
                        } catch (Exception ex) {
                            logger.severe("Failed to create HTTPS port");
                        }
                    }
                });
            } else {
                httpServer = HttpServer.create(new InetSocketAddress(backEndServerPort), 0);
            }
            String context = "/v2";
            httpServer.createContext(context + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/", exchange -> {

                byte[] response = ResponseConstants.GET_PET_RESPONSE.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/findByTags", exchange -> {

                byte[] response = ResponseConstants.PET_BY_ID_RESPONSE.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pets/findByTags", exchange -> {

                byte[] response = ResponseConstants.PET_BY_ID_RESPONSE.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/store/inventory", exchange -> {

                byte[] response = ResponseConstants.STORE_INVENTORY_RESPONSE.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/3", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_VALID_JWT_TRANSFORMER.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/store/order/1", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46aGVsbG8="))
                {
                    response = ResponseConstants.STORE_INVENTORY_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                            Constants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                            Constants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/user/john", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46aGVsbG8="))
                {
                    response = ResponseConstants.userResponse.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                            Constants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHZ_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                            Constants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_FORBIDDEN, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            // to test jwt generator
            httpServer.createContext(context + "/jwtheader", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("X-JWT-Assertion")) {
                    response = ResponseConstants.VALID_JWT_RESPONSE.getBytes();
                } else {
                    response = ResponseConstants.INVALID_JWT_RESPONSE.getBytes();
                }
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/jwttoken", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("X-JWT-Assertion")) {
                    String token = exchange.getRequestHeaders().get("X-JWT-Assertion").toString();
                    // token is in the format: [token]
                    token = token.substring(1, (token.length()-1));
                    JSONObject responseJSON = new JSONObject();
                    responseJSON.put("token", token);
                    response = responseJSON.toString().getBytes();
                } else {
                    response = ResponseConstants.INVALID_JWT_RESPONSE.getBytes();
                }
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/removeauthheader", exchange -> {
                byte[] response;
                if (!exchange.getRequestHeaders().containsKey("authHeader")) {
                    response = ResponseConstants.VALID_REMOVE_HEADER_RESPONSE.getBytes();
                } else {
                    response = ResponseConstants.INVALID_REMOVE_HEADER_RESPONSE.getBytes();
                }
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            // For OpenAPI v3 related tests
            httpServer.createContext("/v3" + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/timeout70", exchange -> {
                try {
                    logger.info("Sleeping 70s...");
                    Thread.sleep(70000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/timeout15", exchange -> {
                try {
                    logger.info("Sleeping 15s...");
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.start();
            backEndServerUrl = "http://localhost:" + backEndServerPort;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while setting up mock server", ex);

        }
    }

    public void stopIt() {
        httpServer.stop(0);
    }

    private SSLContext getSslContext() throws Exception {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        // initialise the keystore
        char[] password = "wso2carbon".toCharArray();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream keyStoreIS = Thread.currentThread().getContextClassLoader().getResourceAsStream("wso2carbon.jks");
        keyStore.load(keyStoreIS, password);
        // setup the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, password);
        // setup the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        KeyStore trustStore = KeyStore.getInstance("JKS");
        InputStream trustStoreIS = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("client-truststore.jks");
        trustStore.load(trustStoreIS, password);
        tmf.init(trustStore);
        // setup the HTTPS context and parameters
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }
}
