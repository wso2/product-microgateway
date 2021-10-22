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
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockInterceptorServer extends Thread {
    private static final Logger logger = Logger.getLogger(MockInterceptorServer.class.getName());
    private final int serverPort;
    private final HandlerServer handlerServer;
    private volatile InterceptorConstants.Handler handler;
    private volatile String requestFlowRequestBody;
    private volatile String requestFlowResponseBody;
    private volatile String responseFlowRequestBody;
    private volatile String responseFlowResponseBody;

    public MockInterceptorServer(int managerPort, int handlerPort) {
        serverPort = managerPort;
        handlerServer = new HandlerServer(handlerPort);
        clearStatus();
    }

    private void clearStatus() {
        handler = InterceptorConstants.Handler.NONE;
        requestFlowRequestBody = "";
        requestFlowResponseBody = "{}";
        responseFlowRequestBody = "";
        responseFlowResponseBody = "{}";
    }


    @Override
    public void run() {

        if (serverPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }

        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
            String context = "/interceptor";

            // status
            httpServer.createContext(context + "/status", exchange -> {
                JSONObject responseJSON = new JSONObject();
                responseJSON.put(InterceptorConstants.StatusPayload.HANDLER, handler);
                responseJSON.put(InterceptorConstants.StatusPayload.REQUEST_FLOW_REQUEST_BODY, requestFlowRequestBody);
                responseJSON.put(InterceptorConstants.StatusPayload.RESPONSE_FLOW_REQUEST_BODY, responseFlowRequestBody);

                byte[] response = responseJSON.toString().getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            // clear status
            httpServer.createContext(context + "/clear-status", exchange -> {
                clearStatus();
                Utils.send200OK(exchange);
                exchange.close();
            });

            // set response of request flow interceptor
            httpServer.createContext(context + "/request", exchange -> {
                requestFlowResponseBody = Utils.requestBodyToString(exchange);
                Utils.send200OK(exchange);
                exchange.close();
            });

            // set response of response flow interceptor
            httpServer.createContext(context + "/response", exchange -> {
                responseFlowResponseBody = Utils.requestBodyToString(exchange);
                Utils.send200OK(exchange);
                exchange.close();
            });

            httpServer.start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while setting up interceptor server", ex);
        }
    }

    @Override
    public void start() {
        super.start();
        handlerServer.start();
    }

    private class HandlerServer extends Thread {
        private final Logger logger = Logger.getLogger(HandlerServer.class.getName());
        private final int serverPort;

        public HandlerServer(int port) {
            serverPort = port;
        }

        @Override
        public void run() {
            if (serverPort < 0) {
                throw new RuntimeException("Server port is not defined");
            }

            try {
                HttpServer httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);

                // TODO: (renuka) do we want a versioning in interceptors
                String context = "";
                // handle request
                httpServer.createContext(context + "/handle-request", exchange -> {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        Utils.send404NotFound(exchange);
                        return;
                    }

                    logger.info("Called /handle-request of interceptor service");
                    requestFlowRequestBody = Utils.requestBodyToString(exchange);
                    // set which flow has handled by interceptor
                    if (Arrays.asList(InterceptorConstants.Handler.NONE, InterceptorConstants.Handler.REQUEST_ONLY).contains(handler)) {
                        handler = InterceptorConstants.Handler.REQUEST_ONLY;
                    } else {
                        handler = InterceptorConstants.Handler.BOTH;
                    }

                    byte[] response = requestFlowResponseBody.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });

                // handle response
                httpServer.createContext(context + "/handle-response", exchange -> {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        Utils.send404NotFound(exchange);
                        return;
                    }

                    logger.info("Called /handle-response of interceptor service");
                    responseFlowRequestBody = Utils.requestBodyToString(exchange);
                    // set which flow has handled by interceptor
                    if (Arrays.asList(InterceptorConstants.Handler.NONE, InterceptorConstants.Handler.RESPONSE_ONLY).contains(handler)) {
                        handler = InterceptorConstants.Handler.RESPONSE_ONLY;
                    } else {
                        handler = InterceptorConstants.Handler.BOTH;
                    }

                    byte[] response = responseFlowResponseBody.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });

                httpServer.start();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error occurred while setting up interceptor handler server", ex);
            }
        }
    }
}
