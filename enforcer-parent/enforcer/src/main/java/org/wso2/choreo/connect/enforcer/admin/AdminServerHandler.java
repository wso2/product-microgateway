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
package org.wso2.choreo.connect.enforcer.admin;

import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpVersion;
import io.grpc.netty.shaded.io.netty.util.CharsetUtil;
import org.apache.http.protocol.HTTP;
import org.wso2.choreo.connect.enforcer.admin.handlers.APIRequestHandler;
import org.wso2.choreo.connect.enforcer.admin.handlers.ApplicationRequestHandler;
import org.wso2.choreo.connect.enforcer.admin.handlers.RequestHandler;
import org.wso2.choreo.connect.enforcer.admin.handlers.RevokedTokensRequestHandler;
import org.wso2.choreo.connect.enforcer.admin.handlers.SubscriptionRequestHandler;
import org.wso2.choreo.connect.enforcer.admin.handlers.ThrottlingPolicyRequestHandler;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ManagementCredentialsDto;
import org.wso2.choreo.connect.enforcer.constants.AdminConstants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.models.ResponsePayload;

import java.util.Base64;

import static org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTConstants.AUTHORIZATION;
/**
 * Netty handler implementation for admin server.
 */
public class AdminServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        FullHttpRequest request;
        ResponsePayload responsePayload;
        boolean isAuthorized = false;

        if (msg instanceof FullHttpRequest) {
            request = (FullHttpRequest) msg;
        } else {
            // return error response;
            String error = AdminConstants.ErrorMessages.INTERNAL_SERVER_ERROR;
            responsePayload = new ResponsePayload();
            responsePayload.setError(true);
            responsePayload.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            responsePayload.setContent(error);
            buildAndSendResponse(ctx, responsePayload);
            return;
        }
        String authHeader = request.headers().get(AUTHORIZATION);

        // Validate from new config
        if (authHeader == null) {
            String error = AdminConstants.ErrorMessages.NO_AUTH_HEADER_ERROR;
            responsePayload = AdminUtils.buildResponsePayload(error, HttpResponseStatus.UNAUTHORIZED, true);
            buildAndSendResponse(ctx, responsePayload);
            return;
        } else if (authHeader.toLowerCase().startsWith(HttpConstants.BASIC_LOWER)) {
            try {
                // Authorization: Basic base64credentials
                String base64Credentials = authHeader.substring(HttpConstants.BASIC_LOWER.length()).trim();
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, CharsetUtil.UTF_8);
                // credentials = username:password
                final String[] values = credentials.split(":", 2);

                ManagementCredentialsDto managementCredentials = ConfigHolder.getInstance().getConfig()
                        .getManagement();
                isAuthorized = values[0].equals(managementCredentials.getUserName())
                        && values[1].equals(new String(managementCredentials.getPassword()));
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                String error = AdminConstants.ErrorMessages.INTERNAL_SERVER_ERROR;
                responsePayload = AdminUtils.buildResponsePayload(error, HttpResponseStatus.UNAUTHORIZED, true);
                buildAndSendResponse(ctx, responsePayload);
                return;
            }
        }

        if (!isAuthorized) {
            String error = AdminConstants.ErrorMessages.UNAUTHORIZED_ERROR;
            responsePayload = AdminUtils.buildResponsePayload(error, HttpResponseStatus.UNAUTHORIZED, true);
            buildAndSendResponse(ctx, responsePayload);
            return;
        }

        if (HttpMethod.GET != request.method()) {
            // return error
            String error = AdminConstants.ErrorMessages.METHOD_NOT_IMPLEMENTED;
            responsePayload = AdminUtils.buildResponsePayload(error, HttpResponseStatus.NOT_IMPLEMENTED, true);
        } else {
            // Check request uri and invoke correct handler
            String[] uriSections = request.uri().split("\\?");
            String[] params = null;
            String baseURI = uriSections[0];
            if (uriSections.length > 1) {
                params = uriSections[1].split("&");
            }
            RequestHandler requestHandler;

            switch (baseURI) {
                case AdminConstants.AdminResources.API_INFO:
                    requestHandler = new APIRequestHandler();
                    responsePayload = requestHandler.handleRequest(params, AdminConstants.API_INFO_TYPE);
                    break;
                case AdminConstants.AdminResources.APIS:
                    requestHandler = new APIRequestHandler();
                    responsePayload = requestHandler.handleRequest(params, AdminConstants.API_TYPE);
                    break;
                case AdminConstants.AdminResources.APPLICATIONS:
                    requestHandler = new ApplicationRequestHandler();
                    responsePayload = requestHandler.handleRequest(params, AdminConstants.APPLICATION_TYPE);
                    break;
                case AdminConstants.AdminResources.SUBSCRIPTIONS:
                    requestHandler = new SubscriptionRequestHandler();
                    responsePayload = requestHandler.handleRequest(params, AdminConstants.SUBSCRIPTION_TYPE);
                    break;
                case AdminConstants.AdminResources.APPLICATION_THROTTLING_POLICIES:
                    requestHandler = new ThrottlingPolicyRequestHandler();
                    responsePayload = requestHandler.handleRequest(params,
                            AdminConstants.APPLICATION_THROTTLING_POLICY_TYPE);
                    break;
                case AdminConstants.AdminResources.SUBSCRIPTION_THROTTLING_POLICIES:
                    requestHandler = new ThrottlingPolicyRequestHandler();
                    responsePayload = requestHandler.handleRequest(params,
                            AdminConstants.SUBSCRIPTION_THROTTLING_POLICY_TYPE);
                    break;
                case AdminConstants.AdminResources.REVOKED_TOKENS:
                    requestHandler = new RevokedTokensRequestHandler();
                    responsePayload = requestHandler.handleRequest(params, AdminConstants.REVOKED_TOKEN_TYPE);
                    break;
                default:
                    String error = AdminConstants.ErrorMessages.RESOURCE_NOT_FOUND_ERROR;
                    responsePayload = AdminUtils.buildResponsePayload(error, HttpResponseStatus.NOT_FOUND, true);
                    break;
            }
        }
        buildAndSendResponse(ctx, responsePayload);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void buildAndSendResponse(ChannelHandlerContext ctx, ResponsePayload response) {
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                response.getStatus(),
                Unpooled.copiedBuffer(response.getContent(), CharsetUtil.UTF_8));
        httpResponse.headers().set(HTTP.CONTENT_TYPE, HttpConstants.APPLICATION_JSON);
        httpResponse.headers().set(HTTP.CONTENT_LEN, httpResponse.content().readableBytes());
        ctx.writeAndFlush(httpResponse);
    }
}
