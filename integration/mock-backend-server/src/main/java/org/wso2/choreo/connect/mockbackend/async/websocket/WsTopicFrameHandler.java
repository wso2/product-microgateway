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
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsTopicFrameHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WsBasicFrameHandler.class);
    private final String topic;

    public WsTopicFrameHandler(String topic) {
        this.topic = topic;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof WebSocketFrame) {
            if (msg instanceof TextWebSocketFrame) {
                log.info("TextWebSocketFrame received. Topic: {} Message: {}", topic,
                        ((TextWebSocketFrame) msg).text());
                ctx.channel().writeAndFlush(
                        new TextWebSocketFrame("Message received: " + ((TextWebSocketFrame) msg).text() +
                                topic));
            } else if (msg instanceof BinaryWebSocketFrame) {
                log.info("BinaryWebSocketFrame received. Topic: {}", topic);
                ctx.channel().writeAndFlush(
                        new BinaryWebSocketFrame(((BinaryWebSocketFrame) msg).content()));
            } else {
                log.info("Unsupported WebSocketFrame");
            }
        }
    }
}
