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

package org.wso2.micro.gateway.enforcer.security.jwt.issuer;

import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.SimpleChannelInboundHandler;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObject;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.micro.gateway.enforcer.security.TokenValidationContext;

import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * This is the http token server handler implementation.
 */
public class HttpTokenServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static TokenIssuer tokenIssuer;
    private static final Logger logger = LogManager.getLogger(HttpTokenServerHandler.class);

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            tokenIssuer = new JWTIssuerImpl();

            TokenValidationContext validationContext = new TokenValidationContext();
            validationContext.setValidationInfoDTO(new APIKeyValidationInfoDTO());
            validationContext.getValidationInfoDTO().setEndUserName("admin");
            String jwt = tokenIssuer.generateToken(validationContext);

            boolean keepAlive = HttpUtil.isKeepAlive(req);
            FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK,
                    Unpooled.wrappedBuffer(jwt.getBytes()));
            response.headers()
                    .set(CONTENT_TYPE, TEXT_PLAIN)
                    .setInt(CONTENT_LENGTH, response.content().readableBytes());

            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                // Tell the client we're going to close the connection.
                response.headers().set(CONNECTION, CLOSE);
            }

            ChannelFuture f = ctx.write(response);

            if (!keepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause);
        ctx.close();
    }
}
