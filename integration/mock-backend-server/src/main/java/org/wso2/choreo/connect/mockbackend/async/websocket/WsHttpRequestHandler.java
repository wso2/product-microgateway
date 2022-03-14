/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.mockbackend.async.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.mockbackend.async.MockAsyncServer;

public class WsHttpRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockAsyncServer.class);
    private static final String[] topics = new String[2];
    private static final String WEBSOCKET_BASEPATH = "/v2";
    private static final String WS_PATH_NOTIFICATIONS = "/notifications";
    private static final String WS_PATH_ROOM_ID = "/rooms?room=room1";

    public WsHttpRequestHandler() {
        topics[0] = WS_PATH_NOTIFICATIONS;
        topics[1] = WS_PATH_ROOM_ID;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (msg instanceof HttpRequest) {

            HttpRequest httpRequest = (HttpRequest) msg;
            log.info("Http Request Received");

            HttpHeaders headers = httpRequest.headers();
            System.out.println("Connection : " +headers.get("Connection"));
            System.out.println("Upgrade : " + headers.get("Upgrade"));

            if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION)) &&
                    "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {
                if (!((HttpRequest) msg).uri().startsWith(WEBSOCKET_BASEPATH)) {
                    ctx.channel().unsafe().closeForcibly();
                }
                WsServerFrameHandler frameHandler = new WsServerFrameHandler();
                for (String topic: topics) {
                    if (((HttpRequest) msg).uri().equals(WEBSOCKET_BASEPATH + topic)) {
                        frameHandler = new WsServerFrameHandler(":" + topic);
                        break;
                    }
                }
                //Adding new handler to the existing pipeline to handle WebSocket Messages
                ctx.pipeline().replace(this, "websocketHandler", frameHandler);

                log.info("WebSocketHandler added to the pipeline");
                log.info("Opened Channel : " + ctx.channel());
                log.info("Handshaking....");
                //Do the Handshake to upgrade connection from HTTP to WebSocket protocol
                handleHandshake(ctx, httpRequest);
                log.info("Handshake is done");
            }
        } else {
            log.info("Incoming request is unknown");
        }
    }

    /* Do the handshaking for WebSocket request */
    protected void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    protected String getWebSocketURL(HttpRequest req) {
        System.out.println("Req URI : " + req.uri());
        String url =  "ws://" + req.headers().get("Host") + req.uri();
        System.out.println("Constructed URL : " + url);
        return url;
    }
}
