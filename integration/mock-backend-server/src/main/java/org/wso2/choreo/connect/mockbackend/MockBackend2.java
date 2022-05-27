/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public class MockBackend2 extends Thread {
    private static final Logger logger = Logger.getLogger(MockBackend2.class.getName());
    private final int backEndServerPort;

    public MockBackend2(int port) {
        backEndServerPort = port;
    }

    public void run() {
        if (backEndServerPort < 0) {
            throw new RuntimeException("Mock backend server2 port is not defined");
        }
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(backEndServerPort), 0);
            String context = "/v2"; // Same context as the prod and sandbox backends

            // for interceptor dynamic endpoints test cases
            httpServer.createContext(context + "/pet/findByStatus/dynamic-ep-echo", exchange -> {
                byte[] response = ResponseConstants.DYNAMIC_EP_RESPONSE.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            String diffContext = "/v2sand";

            httpServer.createContext(diffContext + "/pet/findByStatus", exchange -> {
                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE_2.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            httpServer.createContext(diffContext + "/pet/patch/same/findByStatus", exchange -> {

                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE_2.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            httpServer.createContext(diffContext + "/pet/patch/diff/findByStatus", exchange -> {

                byte[] response = ResponseConstants.API_SANDBOX_RESPONSE_2.getBytes();
                Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
            });

            httpServer.start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while setting up sandbox server", ex);
        }
    }
}
