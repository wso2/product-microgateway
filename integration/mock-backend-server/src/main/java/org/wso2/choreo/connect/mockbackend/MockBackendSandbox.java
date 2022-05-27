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
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockBackendSandbox extends Thread {
    private static final Logger logger = Logger.getLogger(MockBackendSandbox.class.getName());
    private final int backEndServerPort;
    private int retryCountEndpointTwo = 0;
    private int retryCountEndpointThree = 0;
    private int retryCountEndpointSeven = 0;

    public MockBackendSandbox(int port) {
        backEndServerPort = port;
    }

    public void run() {
        if (backEndServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(backEndServerPort), 0);
            String context = "/v2";

            httpServer.createContext(context + "/pet/findByStatus", exchange -> {
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/pets/findByStatus", exchange -> {
                byte[] response = ResponseConstants.PET_BY_ID_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            httpServer.createContext(context + "/pet/patch/diff/findByStatus", exchange -> {
                byte[] response = ResponseConstants.RESPONSE_BODY.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            httpServer.createContext(context + "/pets/findByTags", exchange -> {
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            // for interceptor dynamic endpoints test cases
            httpServer.createContext(context + "/pet/findByStatus/dynamic-ep-echo", Utils::echo);

            // For Timeout tests
            httpServer.createContext(context + "/delay-8", exchange -> {
                try {
                    logger.info("Sleeping 8s...");
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/delay-5", exchange -> {
                try {
                    logger.info("Sleeping 5s...");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/delay-4", exchange -> {
                try {
                    logger.info("Sleeping 4s...");
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            // For retry tests
            // Mock backend must be restarted if the retry tests are run again, against the already used resources.
            httpServer.createContext(context + "/retry-seven", exchange -> {
                retryCountEndpointSeven += 1;
                if (retryCountEndpointSeven < 7) { // returns a x04 status
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
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_FORBIDDEN, response, exchange);
                } else {
                    byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });
            httpServer.createContext(context + "/retry-two", exchange -> {
                retryCountEndpointTwo += 1;
                if (retryCountEndpointTwo < 2) { // returns a x02 status
                    byte[] response = ResponseConstants.GATEWAY_ERROR.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_PAYMENT_REQUIRED, response, exchange);
                } else {
                    byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                    Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
                }
            });
            httpServer.createContext(context + "/req-cb", exchange -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Error occurred while thread sleep", e);
                }
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });
            httpServer.createContext(context + "/echo", Utils::echo);

            httpServer.start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while setting up sandbox server", ex);

        }
    }
}
