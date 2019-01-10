/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.micro.gateway.tests.common.HTTP2Server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * HTTP/1.1 handler that responds with a "HTTP/1.1 connection"
 */
public class Http1Handler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String establishApproach;
    private static final Log log = LogFactory.getLog(Http1Handler.class);

    public Http1Handler(String establishApproach) {
        this.establishApproach = checkNotNull(establishApproach, "establishApproach");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if (HttpUtil.is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
        }
        boolean keepAlive = HttpUtil.isKeepAlive(req);

        ByteBuf content = ctx.alloc().buffer();
        content.writeBytes(Http2Handler.RESPONSE_BYTES.duplicate());
        ByteBufUtil.writeAscii(content, " - established via " + req.protocolVersion() + " (" + establishApproach + ")");

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(response);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage());
        ctx.close();
    }
}