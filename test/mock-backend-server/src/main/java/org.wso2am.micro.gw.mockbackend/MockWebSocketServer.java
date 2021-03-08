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

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockWebSocketServer extends Thread{
    private static final Logger logger = Logger.getLogger(MockWebSocketServer.class.getName());
    private final int backendServerPort;
    private final Server server;
    private final boolean secured;
    private final SSLContext sslContext;

    public MockWebSocketServer(int backendServerPort, boolean secured, SSLContext sslContext){
        this.backendServerPort = backendServerPort;
        this.server = new Server(new InetSocketAddress(backendServerPort));
        this.secured = secured;
        this.sslContext = sslContext;
    }

    private class Server extends WebSocketServer{
        public Server(InetSocketAddress address) {
            super(address);
        }
        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            conn.send(message);
        }

        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            logger.log(Level.INFO, "WebSocket connection opened");
            webSocket.send(ResponseConstants.WEBSOCKET_GREETING);
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            logger.log(Level.INFO,"WebSocket connection closed");
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            webSocket.send(s);
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            logger.log(Level.SEVERE, e.toString());
        }

        @Override
        public void onStart() {
            logger.log(Level.INFO, "WebSocket server started on port:"+ getPort());
        }
    }

    @Override
    public void run() {
        if(backendServerPort < 0){
            throw new RuntimeException("Invalid port");
        }
        try{
            if (secured) {
                server.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
            }
            server.start();
        }catch (Exception e){
            logger.log(Level.SEVERE, e.toString());
        }
    }

}
