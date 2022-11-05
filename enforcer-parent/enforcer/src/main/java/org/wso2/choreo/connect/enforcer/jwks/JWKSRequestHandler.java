/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.jwks;

import com.nimbusds.jose.jwk.JWKSet;
import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.SimpleChannelInboundHandler;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObject;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
/**
 * JWKS Request Handler for Backend JWTs
 */
public class JWKSRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final String CONTENT_LENGTH = "content-length";
    private static final String CONNECTION = "Connection";
    private static final String CLOSE = "close";
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final Logger logger = LogManager.getLogger(JWKSRequestHandler.class);
    private static final String route = "/jwks";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        FullHttpResponse res = null;
        FullHttpRequest req = null;
        BackendJWKSDto backendJWKSDto = ConfigHolder.getInstance().getConfig().getBackendJWKSDto();
        JWKSet jwks = backendJWKSDto.getJwks();
        if (msg instanceof HttpRequest) {
            req = (FullHttpRequest) msg;
            String path = req.uri().split("\\?")[0];
            if (!(req.method().equals(HttpMethod.GET) && path.equals(route))) {
                ctx.fireChannelRead(msg);
                return;
            }
            res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(jwks.toJSONObject().toString().getBytes()));
            res.headers()
                    .set(CONNECTION, CLOSE)
                    .set(CONTENT_TYPE, APPLICATION_JSON)
                    .setInt(CONTENT_LENGTH, res.content().readableBytes());
            ChannelFuture f = ctx.write(res);
            f.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            //TODO: keep alive
        }
    }

    @Override
    public void channelReadComplete(io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause);
        ctx.close();
    }
}
