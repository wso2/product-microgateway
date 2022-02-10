package org.wso2.choreo.connect.mockbackend.async.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebSocketFrameHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger("WebSocketServer");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (msg instanceof WebSocketFrame) {
            if (msg instanceof TextWebSocketFrame) {
                log.info("TextWebSocketFrame received. Message: {}", ((TextWebSocketFrame) msg).text());
                ctx.channel().writeAndFlush(
                        new TextWebSocketFrame("Message received: " + ((TextWebSocketFrame) msg).text()));
            } else {
                log.info("Unsupported WebSocketFrame");
            }
        }
    }
}
