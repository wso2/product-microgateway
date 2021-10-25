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
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderNames;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockSandboxServer extends Thread {
    private static final Logger logger = Logger.getLogger(MockSandboxServer.class.getName());
    private int backEndServerPort;
    private HttpServer httpServer;
    private int retryCountEndpointTwo = 0;
    private int retryCountEndpointThree = 0;
    private int retryCountEndpointSeven = 0;

    public MockSandboxServer(int port) {
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
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            // For retry tests
            // Mock backend must be restarted if the retry tests are run again, against the already used resources.
            httpServer.createContext(context + "/retry-seven", exchange -> {
                retryCountEndpointSeven += 1;
                if (retryCountEndpointSeven < 7) { // returns a x04 status
                    byte[] response = ResponseConstants.GATEWAY_ERROR.getBytes();
                    respondWithBodyAndClose(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, response, exchange);
                } else {
                    byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                    respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });
            httpServer.createContext(context + "/retry-three", exchange -> {
                retryCountEndpointThree += 1;
                if (retryCountEndpointThree < 3) { // returns a x03 status
                    byte[] response = ResponseConstants.GATEWAY_ERROR.getBytes();
                    respondWithBodyAndClose(HttpURLConnection.HTTP_FORBIDDEN, response, exchange);
                } else {
                    byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                    respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });
            httpServer.createContext(context + "/retry-two", exchange -> {
                retryCountEndpointTwo += 1;
                if (retryCountEndpointTwo < 2) { // returns a x02 status
                    byte[] response = ResponseConstants.GATEWAY_ERROR.getBytes();
                    respondWithBodyAndClose(HttpURLConnection.HTTP_PAYMENT_REQUIRED, response, exchange);
                } else {
                    byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                    respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });

            httpServer.start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while setting up sandbox server", ex);

        }
    }

    private void respondWithBodyAndClose(int statusCode, byte[] response, HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(),
                Constants.CONTENT_TYPE_APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
