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

package org.wso2.choreo.connect.enforcer.security.jwt.issuer;

import io.grpc.netty.shaded.io.netty.buffer.ByteBuf;
import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.SimpleChannelInboundHandler;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObject;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpUtil;
import io.grpc.netty.shaded.io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.CredentialDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.security.TokenValidationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * This is the http token server handler implementation.
 */
public class HttpTokenServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final String CONTENT_LENGTH = "content-length";
    private static final String KEEP_ALIVE = "keep-alive";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String CONNECTION = "Connection";
    private static final String CLOSE = "close";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC_VALUE = "Basic";
    private static final String BASIC_LOWER = "basic";

    private static TokenIssuer tokenIssuer;
    private static String username = null;
    private static boolean isAuthorized = false;
    private static final Logger logger = LogManager.getLogger(HttpTokenServerHandler.class);

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        FullHttpResponse response = null;

        if (msg instanceof HttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            boolean keepAlive = HttpUtil.isKeepAlive(req);

            String authHeader = req.headers().get(AUTHORIZATION);

            if (authHeader == null) {
                isAuthorized = false;
                String error = "User is NOT authorized to generate a token. " +
                        "Please provide a valid Authorization header to continue.";
                response = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.UNAUTHORIZED,
                        Unpooled.wrappedBuffer(error.getBytes()));
                response.headers().set(CONTENT_TYPE, TEXT_PLAIN);
                logger.error("User is NOT authorized to generate a token.");
            } else if (authHeader.toLowerCase().startsWith(BASIC_LOWER)) {
                try {
                    // Authorization: Basic base64credentials
                    String base64Credentials = authHeader.substring(BASIC_VALUE.length()).trim();
                    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                    // credentials = username:password
                    final String[] values = credentials.split(":", 2);

                    CredentialDto[] predefinedCredentials = ConfigHolder.getInstance().getConfig()
                            .getJwtUsersCredentials();
                    for (CredentialDto cred: predefinedCredentials) {
                        if (values[0].equals(cred.getUsername()) && values[1].equals(new String(cred.getPwd()))) {
                            username = values[0];
                            isAuthorized = true;
                        } else {
                            isAuthorized = false;
                            String error = "Wrong username or password. " +
                                    "Please provide valid credentials.";
                            response = new DefaultFullHttpResponse(req.protocolVersion(),
                                    HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer(error.getBytes()));
                            response.headers().set(CONTENT_TYPE, TEXT_PLAIN);
                            logger.error("Wrong username or password.");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    String error = "Error occurred while processing the request.";
                    response = new DefaultFullHttpResponse(req.protocolVersion(),
                            HttpResponseStatus.BAD_REQUEST, Unpooled.wrappedBuffer(error.getBytes()));
                    response.headers().set(CONTENT_TYPE, TEXT_PLAIN);
                    logger.error("Error occurred while processing the request.");
                } catch (ArrayIndexOutOfBoundsException e) {
                    String error = "Error occurred while processing the request.";
                    response = new DefaultFullHttpResponse(req.protocolVersion(),
                            HttpResponseStatus.BAD_REQUEST, Unpooled.wrappedBuffer(error.getBytes()));
                    response.headers().set(CONTENT_TYPE, TEXT_PLAIN);
                    logger.error("Error occurred while processing the request.");
                }
            }

            if (isAuthorized) {
                TokenValidationContext validationContext = new TokenValidationContext();
                ByteBuf byteBuf = req.content();
                if (byteBuf != null && byteBuf.isReadable()) {
                    String payload = byteBuf.toString(CharsetUtil.UTF_8);
                    if (!payload.isEmpty()) {
                        String[] bodyParams = payload.split(APIConstants.JwtTokenConstants.PARAM_SEPARATOR);
                        for (String param : bodyParams) {
                            String[] pair = param.split(APIConstants.JwtTokenConstants.PARAM_VALUE_SEPARATOR);
                            if (pair.length == 2 && APIConstants.JwtTokenConstants.SCOPE.equals(pair[0])) {
                                validationContext.setAttribute(APIConstants.JwtTokenConstants.SCOPE, pair[1]);
                            }
                        }
                    }
                }
                tokenIssuer = new JWTIssuerImpl();
                validationContext.setValidationInfoDTO(new APIKeyValidationInfoDTO());
                validationContext.getValidationInfoDTO().setEndUserName(username);
                String jwt = tokenIssuer.generateToken(validationContext);

                response = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(jwt.getBytes()));
                response.headers()
                        .set(CONTENT_TYPE, TEXT_PLAIN)
                        .setInt(CONTENT_LENGTH, response.content().readableBytes());
            }
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
