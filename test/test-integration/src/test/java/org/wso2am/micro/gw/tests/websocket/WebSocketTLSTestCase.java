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
package org.wso2am.micro.gw.tests.websocket;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.common.model.ApplicationDTO;
import org.wso2am.micro.gw.tests.util.ApiDeployment;
import org.wso2am.micro.gw.tests.util.ApiProjectGenerator;
import org.wso2am.micro.gw.tests.util.TestConstant;
import org.wso2am.micro.gw.tests.util.WebSocketClientImpl;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class WebSocketTLSTestCase extends BaseTestCase {
    protected String jwtTokenProd;

    @BeforeClass
    public void beforeClass() throws Exception{
        super.startMGW(null, true);

        String prodSandApiZipfile = ApiProjectGenerator.createApictlProjZip("apis/openApis/mockWebSocketAPITLS.yaml", null,
                "backendtls/backend.crt");
        ApiDeployment.deployAPI(prodSandApiZipfile);

        API api = new API();
        api.setName("EchoWebSocketWSS");
        api.setContext("echowebsocketwss/1.0");
        api.setProdEndpoint("wss://mockBackend:"+ TestConstant.SECURE_MOCK_WEB_SOCKET_PORT);
        api.setVersion("1.0");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600, null);
    }

    @Test(description = "Test WebSocket endpoints by sending a request")
    public void invokeWebSocketEndpointsTLS() throws Exception {
        Object lock = new Object();
        Timer timer = new Timer();

        WebSocketClientImpl webSocketClient = new WebSocketClientImpl(getMockServiceURLWebSocket("/echowebsocketwss/1.0"),
                lock, timer);
        webSocketClient.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        boolean isConnected = webSocketClient.connectBlocking(TestConstant.MOCK_WEBSOCKET_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        Assert.assertTrue(isConnected, "Unable to connect to mock websocket endpoint");
        webSocketClient.send(TestConstant.MOCK_WEBSOCKET_HELLO);
        timer.schedule(webSocketClient.getWebSocketTimer(), 0, TestConstant.MOCK_WEBSOCKET_ECHO_CHECK_INTERVAL);
        synchronized (lock){
            try {
                lock.wait();
            }catch (InterruptedException e){
                throw new Exception(e);
            }
        }
        Assert.assertTrue(webSocketClient.isEchoReceived(), "Unable to capture websocket echo");


    }
}
