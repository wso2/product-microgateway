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

public class MockSandboxServer extends Thread {
    private static final Logger logger = Logger.getLogger(MockSandboxServer.class.getName());
    private int backEndServerPort;
    private HttpServer httpServer;

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
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                        Constants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            httpServer.start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while setting up sandbox server", ex);

        }
    }
}
