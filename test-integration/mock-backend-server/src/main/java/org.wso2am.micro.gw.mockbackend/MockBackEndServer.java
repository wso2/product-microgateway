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

package org.wso2am.micro.gw.mockbackend;

import com.sun.net.httpserver.HttpServer;
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
    private static int backEndServerPort;
    private static boolean retryDone = false;



    public static void main(String[] args) {
        //backend port
        backEndServerPort = Constants.MOCK_BACKEND_SERVER_PORT;
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
            httpServer = HttpServer.create(new InetSocketAddress(backEndServerPort), 0);
            String context = "/v2";
            httpServer.createContext(context + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.responseBody.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/", exchange -> {

                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/findByTags", exchange -> {

                byte[] response = ResponseConstants.petByIdResponse.getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/store/inventory", exchange -> {

                byte[] response = ResponseConstants.storeInventoryResponse.getBytes();
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
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
                        exchange.getRequestHeaders().get("Authorization").toString().contains("Basic YWRtaW46aGVsbG8="))
                {
                    response = ResponseConstants.storeInventoryResponse.getBytes();
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
                if(exchange.getRequestHeaders().containsKey("Authorization") &&
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

            httpServer.start();
            backEndServerUrl = "http://localhost:" + backEndServerPort;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while setting up mock server", ex);

        }
    }

    public void stopIt() {

        httpServer.stop(0);
    }
}
