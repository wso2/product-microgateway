/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;

/**
 * Mock HTTP server for testing Open API tests.
 */
public class EchoService extends Thread {

    private static final Logger log = LoggerFactory.getLogger(EchoService.class);
    private HttpServer httpServer;
    private final int backendServerPort;
    private boolean secured = false;

    public EchoService(int port) {
        this.backendServerPort = port;
    }

    public EchoService(int port, boolean isSecured) {
        this.secured = isSecured;
        this.backendServerPort = port;
    }

    public void run() {
        log.info("Starting EchoService on port: {} TLS: {}", backendServerPort, secured);
        if (backendServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            if (this.secured) {
                httpServer = HttpsServer.create(new InetSocketAddress(backendServerPort), 0);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(
                        Utils.getKeyManagers("echo-service.pkcs12", "backend"), // Created using backendKeystore.pem
                        Utils.getTrustManagers(), null);

                ((HttpsServer)httpServer).setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    public void configure(HttpsParameters params) {
                        try {
                            SSLContext sslContext = SSLContext.getDefault();
                            SSLEngine engine = sslContext.createSSLEngine();

                            SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
                            sslParameters.setCipherSuites(engine.getEnabledCipherSuites());;
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

            // echo request body, request headers in echo response payload
            httpServer.createContext("/", Utils::echoFullRequest);
            httpServer.start();
        } catch (Exception ex) {
            log.error("Error occurred while setting up mock server", ex);
        }
    }

    public void stopIt() {
        httpServer.stop(0);
    }
}
