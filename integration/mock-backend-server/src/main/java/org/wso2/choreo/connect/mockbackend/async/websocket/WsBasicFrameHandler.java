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
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsBasicFrameHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WsBasicFrameHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object frame) {
        if (frame instanceof WebSocketFrame) {
            if (frame instanceof TextWebSocketFrame) {
                String msg = ((TextWebSocketFrame) frame).text();
                if (msg.length() > 100) {
                    log.info("TextWebSocketFrame with a large payload received.");
                    ctx.channel().writeAndFlush(
                            new TextWebSocketFrame("Message large payload received."));
                    return;
                }
                if (msg.startsWith("send me.")) {
                    // This is to ask the server to send the specified number of frames
                    // The msg is expected to be one in the following formats
                    // - "send me. small frames. 5"
                    // - "send me. large frames. 5"
                    String[] s = msg.split("\\.");
                    int numberOfTimes = Integer.parseInt(s[2].trim());
                    String msgType = s[1];
                    if ("large frames".equals(msgType.trim())) {
                        String largePayload = "a".repeat(1024);
                        sendMultipleFrames(ctx, largePayload, numberOfTimes);
                    } else {
                        sendMultipleFrames(ctx, "", numberOfTimes);
                    }
                }
                // Echo the received message for verification
                log.info("TextWebSocketFrame received. Message: {}", msg);
                ctx.channel().writeAndFlush(
                        new TextWebSocketFrame("Message received: " + ((TextWebSocketFrame) frame).text()));
            } else if (frame instanceof BinaryWebSocketFrame) {
                log.info("BinaryWebSocketFrame received.");
                ctx.channel().writeAndFlush(
                        new BinaryWebSocketFrame(((BinaryWebSocketFrame) frame).content()));
            } else {
                log.info("Unsupported WebSocketFrame");
            }
        }
    }

    public void sendMultipleFrames(ChannelHandlerContext ctx, String msgToSend, int numberOfTimes) {
        for (int i = 0; i < numberOfTimes; i++) {
            log.info("Sending message {}", i);
            ctx.channel().writeAndFlush(
                    new TextWebSocketFrame("Message " + i + msgToSend));
            try {
                // TODO: (suksw) Remove the following once https://github.com/wso2/product-microgateway/issues/2706 is fixed
                Thread.sleep(800);
            } catch (InterruptedException ex) {
                log.info("Interrupted while waiting before sending the next message");
            }
        }
        ctx.channel().writeAndFlush(new CloseWebSocketFrame());
    }
}
