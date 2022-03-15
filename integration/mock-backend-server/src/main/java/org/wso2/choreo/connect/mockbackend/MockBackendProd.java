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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

/**
 * Mock HTTP server for testing Open API tests.
 */
public class MockBackendProd extends Thread {

    private static final Logger log = LoggerFactory.getLogger(MockBackendProd.class);
    private HttpServer httpServer;
    private final int backendServerPort;
    private boolean secured = false;
    private boolean mtlsEnabled = false;
    private int retryCountEndpointTwo = 0;
    private int retryCountEndpointThree = 0;
    private int retryCountEndpointFour = 0;

    public MockBackendProd(int port) {
        this.backendServerPort = port;
    }

    public MockBackendProd(int port, boolean isSecured, boolean mtlsEnabled) {
        this.secured = isSecured;
        this.backendServerPort = port;
        this.mtlsEnabled = mtlsEnabled;
    }

    public void run() {
        log.info("Starting MockBackendProd on port: {} TLS: {} mTLS: {}", backendServerPort, secured, mtlsEnabled);
        if (backendServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            if (this.secured) {
                httpServer = HttpsServer.create(new InetSocketAddress(backendServerPort), 0);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(
                        Utils.getKeyManagers("backendKeystore.pkcs12", "backend"), // Created using backendKeystore.pem
                        Utils.getTrustManagers(), null);

                ((HttpsServer)httpServer).setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    public void configure(HttpsParameters params) {
                        try {
                            SSLContext sslContext = SSLContext.getDefault();
                            SSLEngine engine = sslContext.createSSLEngine();

                            SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
                            sslParameters.setCipherSuites(engine.getEnabledCipherSuites());
                            sslParameters.setNeedClientAuth(mtlsEnabled);
                            sslParameters.setProtocols(engine.getEnabledProtocols());
                            params.setSSLParameters(sslParameters);
                        } catch (Exception ex) {
                            log.error("Failed to create HTTPS port");
                        }
                    }
                });
            } else {
                httpServer = HttpServer.create(new InetSocketAddress(backendServerPort), 0);
            }
            String context = "/v2";
            httpServer.createContext(context + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/pet/", exchange -> {

                byte[] response = ResponseConstants.GET_PET_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/pet/findByTags", exchange -> {

                byte[] response = ResponseConstants.PET_BY_ID_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/pets/findByTags", exchange -> {

                byte[] response = ResponseConstants.PET_BY_ID_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/store/inventory", exchange -> {

                byte[] response = ResponseConstants.STORE_INVENTORY_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/pet/3", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_VALID_JWT_TRANSFORMER.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/store/order/1", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46aGVsbG8="))
                {
                    response = ResponseConstants.STORE_INVENTORY_RESPONSE.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_UNAUTHORIZED, response, exchange);
                }
            });
            httpServer.createContext(context + "/user/john", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46aGVsbG8="))
                {
                    response = ResponseConstants.userResponse.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                } else {
                    response = ResponseConstants.AUTHZ_FAILURE_RESPONSE.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_FORBIDDEN, response, exchange);
                }
            });
            // to test jwt generator
            httpServer.createContext(context + "/jwtheader", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("X-JWT-Assertion")) {
                    response = ResponseConstants.VALID_JWT_RESPONSE.getBytes();
                } else {
                    response = ResponseConstants.INVALID_JWT_RESPONSE.getBytes();
                }
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
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
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/removeauthheader", exchange -> {
                byte[] response;
                if (!exchange.getRequestHeaders().containsKey("authHeader")) {
                    response = ResponseConstants.VALID_REMOVE_HEADER_RESPONSE.getBytes();
                } else {
                    response = ResponseConstants.INVALID_REMOVE_HEADER_RESPONSE.getBytes();
                }
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            // For OpenAPI v3 related tests
            httpServer.createContext("/v3" + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            // For Timeout tests
            httpServer.createContext(context + "/delay-17", exchange -> {
                try {
                    log.info("Sleeping 17s...");
                    Thread.sleep(17000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/delay-8", exchange -> {
                try {
                    log.info("Sleeping 8s...");
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/delay-5", exchange -> {
                try {
                    log.info("Sleeping 5s...");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/delay-4", exchange -> {
                try {
                    log.info("Sleeping 4s...");
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            // For retry tests
            // Mock backend must be restarted if the retry tests are run again, against the already used resources.
            httpServer.createContext(context + "/retry-four", exchange -> {
                retryCountEndpointFour += 1;
                if (retryCountEndpointFour < 4) { // returns a x04 status
                    byte[] response = ResponseConstants.GATEWAY_ERROR.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, response, exchange);
                } else {
                    byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });
            httpServer.createContext(context + "/retry-three", exchange -> {
                retryCountEndpointThree += 1;
                if (retryCountEndpointThree < 3) { // returns a x03 status
                    byte[] response = ResponseConstants.GATEWAY_ERROR.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_UNAVAILABLE, response, exchange);
                } else {
                    byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });
            httpServer.createContext(context + "/retry-two", exchange -> {
                retryCountEndpointTwo += 1;
                if (retryCountEndpointTwo < 2) { // returns a x02 status
                    byte[] response = ResponseConstants.GATEWAY_ERROR.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_BAD_GATEWAY, response, exchange);
                } else {
                    byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });
            httpServer.createContext(context + "/req-cb", exchange -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.error("Error occurred while thread sleep", e);
                }
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/headers", exchange -> {
                JSONObject responseJSON = new JSONObject();
                exchange.getRequestHeaders().forEach((key,values) -> {
                    values.forEach(value -> {
                        responseJSON.put(key, value);
                    });
                });
                byte[] response = responseJSON.toString().getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/headers/23.api", exchange -> {
                JSONObject responseJSON = new JSONObject();
                exchange.getRequestHeaders().forEach((key,values) -> {
                    values.forEach(value -> {
                        responseJSON.put(key, value);
                    });
                });
                byte[] response = responseJSON.toString().getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            // the context "/echo" is used for "/echo-request", "/echo-response" as well in interceptor & request body passing tests.
            // sent request headers in response headers <- this is because in interceptor tests it is required to test
            //                                             response flow headers to interceptor service
            // sent request body in response body
            httpServer.createContext(context + "/echo", Utils::echo);
            httpServer.createContext(context + "/echo2", Utils::echo);

            // echo request body, request headers in echo response payload
            httpServer.createContext(context + "/echo-full", Utils::echoFullRequest);

            httpServer.start();
        } catch (Exception ex) {
            log.error("Error occurred while setting up mock server", ex);
        }
    }

    public void stopIt() {
        httpServer.stop(0);
    }
}
