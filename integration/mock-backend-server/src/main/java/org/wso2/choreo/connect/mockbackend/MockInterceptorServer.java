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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockInterceptorServer extends Thread {
    private static final Logger logger = Logger.getLogger(MockInterceptorServer.class.getName());
    private int serverPort;
    private HttpServer httpServer;
    private HandlerServer handlerServer;
    private volatile String requestFlowRequestBody = "";
    private volatile String requestFlowResponseBody = "";

    public MockInterceptorServer(int managerPort, int handlerPort) {
        serverPort = managerPort;
        handlerServer = new HandlerServer(handlerPort);
    }

    @Override
    public void run() {

        if (serverPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
            String context = "/interceptor";

            // status
            httpServer.createContext(context + "/status", exchange -> {
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("status", "NONE");
                responseJSON.put("requestFlowRequestBody", requestFlowRequestBody);

                byte[] response = responseJSON.toString().getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            // set request interceptor
            httpServer.createContext(context + "/request", exchange -> {
                requestFlowResponseBody = getRequestBody(exchange);

                byte[] response = "{\"status\":\"OK\"}".getBytes();
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
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

    public class HandlerServer extends Thread {
        private final Logger logger = Logger.getLogger(HandlerServer.class.getName());
        private int serverPort;
        private HttpServer httpServer;
        private volatile ArrayList<String> list = new ArrayList<>();

        public HandlerServer(int port) {
            serverPort = port;
        }

        @Override
        public void run() {
            if (serverPort < 0) {
                throw new RuntimeException("Server port is not defined");
            }

            try {
                httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);

                // TODO: (renuka) do we want a versioning in interceptors
                String context = "";
                httpServer.createContext(context + "/handle-request", exchange -> {
                    requestFlowRequestBody = getRequestBody(exchange);


                    byte[] response = requestFlowResponseBody.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
                httpServer.createContext(context + "/clear", exchange -> {

                    list.clear();
                    byte[] response = "cleared".getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, "text/plain");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });

                httpServer.createContext(context + "/get", exchange -> {

//                String records = new Gson().toJson(list);
                    String records = new JSONArray(list).toString();
                    byte[] response = records.getBytes();
                    exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, "application/json");
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

    private static String getRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        InputStreamReader isReader = new InputStreamReader(inputStream);
        //Creating a BufferedReader object
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
            sb.append(str);
        }
        return sb.toString();
    }
}
