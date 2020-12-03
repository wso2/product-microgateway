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
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import static org.wso2am.micro.gw.mockbackend.Constants.CONTENT_TYPE;
import static org.wso2am.micro.gw.mockbackend.Constants.CONTENT_TYPE_APPLICATION_JSON;
import static org.wso2am.micro.gw.mockbackend.Constants.MOCK_BACKEND_SERVER_PORT;

/**
 * Mock HTTP server for testing Open API tests.
 */
public class MockBackEndServer extends Thread {

    //private static final Logger log = LoggerFactory.getLogger(MockBackEndServer.class);
    private HttpServer httpServer;
    private String backEndServerUrl;
    private static int backEndServerPort;
    private static boolean retryDone = false;



    public static void main(String[] args) {
        //backend port
        backEndServerPort = MOCK_BACKEND_SERVER_PORT;
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
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/", exchange -> {

                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/findByTags", exchange -> {

                byte[] response = ResponseConstants.petByIdResponse.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/store/inventory", exchange -> {

                byte[] response = ResponseConstants.storeInventoryResponse.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(context + "/pet/3", exchange -> {

                byte[] response = ResponseConstants.RESPONSE_VALID_JWT_TRANSFORMER.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHZ_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_FORBIDDEN, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            String base = "/v1";
            httpServer.createContext(base + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.responseBodyV1.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/pet/findByStatusNew", exchange -> {
                byte[] response = "{\"error\":true}".getBytes();
                if(exchange.getRequestURI().getQuery().contains("value1=foo&value2=bar")) {
                    response = ResponseConstants.responseBodyV1.getBytes();
                }
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/pet/findByTags", exchange -> {

                byte[] response = ResponseConstants.petByIdResponseV1.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/pet/2", exchange -> {

                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/pet/", exchange -> {

                byte[] response = ResponseConstants.getPetResponse.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
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
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                } else {
                    response = ResponseConstants.AUTHENTICATION_FAILURE_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, response.length);
                }
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(base + "/store/inventory", exchange -> {

                byte[] response = ResponseConstants.storeInventoryResponse.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            String contextV3 = "/v3";
            httpServer.createContext(contextV3 + "/pet/findByStatus", exchange -> {

                byte[] response = ResponseConstants.responseBodyV1.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
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
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextV3 + "/pet/", exchange -> {

                InputStream is =  exchange.getRequestBody();
                byte [] response = IOUtils.toByteArray(is);
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextV3 + "/timeout", exchange -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    //log.error("Error while invoking timeout back end", e);
                }
                byte[] response = ResponseConstants.responseBodyV1.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextV3 + "/retry", exchange -> {
                if (!retryDone) {
                    byte[] response = ResponseConstants.responseBodyV1.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, 0);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                    retryDone = true;
                }
                byte[] response = ResponseConstants.responseBodyV1.getBytes();
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                retryDone = false;
                exchange.close();
            });
            httpServer.createContext(contextV3 + "/circuitBreaker", exchange -> {
                if (exchange.getRequestURI().getQuery() != null && exchange.getRequestURI().getQuery()
                        .contains("cb=true")) {
                    byte[] response = ResponseConstants.ERROR_RESPONSE.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                } else {
                    byte[] response = ResponseConstants.responseBodyV1.getBytes();
                    exchange.getResponseHeaders().set(CONTENT_TYPE,
                            CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            // to test jwt generator
            String contextV4 = "/v4";
            httpServer.createContext(contextV4 + "/jwtheader", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("X-JWT-Assertion")) {
                    response = ResponseConstants.VALID_JWT_RESPONSE.getBytes();
                } else {
                    response = ResponseConstants.INVALID_JWT_RESPONSE.getBytes();
                }
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.createContext(contextV4 + "/jwttoken", exchange -> {
                byte[] response;
                if (exchange.getRequestHeaders().containsKey("X-JWT-Assertion")) {
                    JSONObject responseJSON = new JSONObject();
                    responseJSON.put("token", exchange.getRequestHeaders().get("X-JWT-Assertion").toString());
                    response = responseJSON.toString().getBytes();
                } else {
                    response = ResponseConstants.INVALID_JWT_RESPONSE.getBytes();
                }
                exchange.getResponseHeaders().set(CONTENT_TYPE,
                        CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.start();
            backEndServerUrl = "http://localhost:" + backEndServerPort;
        } catch (Exception e) {
            //log.error("Error occurred while setting up mock server", e);
        }
    }

    public void stopIt() {

        httpServer.stop(0);
    }



}


