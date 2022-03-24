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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.mockbackend.async.MockAsyncServer;

public class WsHttpRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockAsyncServer.class);
    private static final String[] topics = new String[3];
    private static String websocketBasepath;
    private static final String WS_PATH_NOTIFICATIONS = "/notifications";
    private static final String WS_PATH_ROOM_ID = "/rooms?room=room1";
    private static final String WS_PATH_NO_MAPPING = "/noMapping";

    public WsHttpRequestHandler(String websocketBasepath) {
        WsHttpRequestHandler.websocketBasepath = websocketBasepath;
        topics[0] = WS_PATH_NOTIFICATIONS;
        topics[1] = WS_PATH_ROOM_ID;
        topics[2] = WS_PATH_NO_MAPPING;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            String uri = ((HttpRequest) msg).uri();
            log.info("Http Request Received");

            ctx.pipeline().remove(this);

            // handle websocket handshake and the control frames (Close, Ping, Pong)
            ctx.pipeline().addLast(new WebSocketServerProtocolHandler(websocketBasepath,
                null, // subprotocols
                true, // allowExtensions
                65536, // maxFrameSize
                false, // allowMaskMismatch
                true, // checkStartsWith,
                15000L // handshakeTimeoutMillis
            ));

            String knownTopic = isKnownTopic(uri);
            if (knownTopic == null) {
                log.info("Adding WsBasicFrameHandler to the pipeline");
                ctx.pipeline().addLast(new WsBasicFrameHandler());
            } else {
                log.info("Adding WsTopicFrameHandler to the pipeline. Topic: {}", knownTopic);
                ctx.pipeline().addLast(new WsTopicFrameHandler(":" + knownTopic));
            }
            ctx.pipeline().fireChannelRead(msg);
        }
    }

    public String isKnownTopic(String uri) {
        for (String topic: topics) {
            if (uri.equals(websocketBasepath + topic)) {
                return topic;
            }
        }
        return null;
    }
}
