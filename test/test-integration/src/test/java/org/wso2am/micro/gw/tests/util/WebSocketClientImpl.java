/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2am.micro.gw.tests.util;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class WebSocketClientImpl extends WebSocketClient {
    private boolean echoReceived = false;
    private TimerTask webSocketTimer;
    public WebSocketClientImpl(URI serverUri, Object lock, Timer timer) {
        super(serverUri);
        this.webSocketTimer = new WebSocketTimer(lock,timer);
    }

    private class WebSocketTimer extends TimerTask{
        private final Object lock;
        private int counter;
        private final Timer timer;
        protected WebSocketTimer(Object lock, Timer timer) {
            this.lock = lock;
            this.timer = timer;
        }

        @Override
        public boolean cancel() {
            return super.cancel();
        }

        @Override
        public void run() {
            if(echoReceived){
                synchronized (lock){
                    lock.notifyAll();
                }
                timer.cancel();
            }
            counter++;
            if(counter == 5){
                synchronized (lock){
                    lock.notifyAll();
                }
                timer.cancel();
            }
        }
    }

    @Override
    public boolean connectBlocking(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return super.connectBlocking(timeout, timeUnit);
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void send(String text) {
        super.send(text);
    }

    @Override
    public void send(byte[] data) {
        super.send(data);
    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }

    @Override
    public boolean isClosed() {
        return super.isClosed();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String s) {
        if(s.equals(TestConstant.MOCK_WEBSOCKET_HELLO)){
            echoReceived = true;
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {

    }

    public boolean isEchoReceived() {
        return echoReceived;
    }

    public TimerTask getWebSocketTimer(){
        return webSocketTimer;
    }
}
