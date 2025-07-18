/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.mcp.request;

import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import io.grpc.netty.shaded.io.netty.handler.codec.TooLongFrameException;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpContent;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderNames;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderValues;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaders;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpVersion;
import io.grpc.netty.shaded.io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;
import org.wso2.choreo.connect.enforcer.mcp.response.McpResponseDto;
import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP Request Handler for MCP Proxies
 */
public class McpRequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(McpRequestHandler.class);
    private static final String mcp = "/mcp";
    private static final String wellKnown = "/.well-known/oauth-authorization-server";

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof TooLongFrameException) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            String path = req.uri().split("\\?")[0];
            switch (path) {
                case mcp:
                    if (req.method() == HttpMethod.POST) {
                        handleMcpRequest(ctx, msg);
                        ReferenceCountUtil.release(req);
                        return;
                    }
                    break;
                case wellKnown:
                    if (req.method() == HttpMethod.GET) {
                        handleWellKnownRequest(ctx, msg);
                        ReferenceCountUtil.release(req);
                        return;
                    }
                    break;
                default:
                    ctx.fireChannelRead(msg);
                    return;
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleMcpRequest(ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest req = (FullHttpRequest) msg;
        FullHttpResponse res;
        HttpHeaders headers = req.headers();
        if (!isHeaderValid(req, McpConstants.VHOST_HEADER) || !isHeaderValid(req, McpConstants.BASEPATH_HEADER)
                || !isHeaderValid(req, McpConstants.VERSION_HEADER)) {
            res = new DefaultFullHttpResponse(req.protocolVersion(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            return;
        }
        String vhostHeader = headers.get(McpConstants.VHOST_HEADER);
        String basepathHeader = headers.get(McpConstants.BASEPATH_HEADER);
        String versionHeader = headers.get(McpConstants.VERSION_HEADER);
        String apikey = APIFactory.getInstance().getApiKey(vhostHeader, basepathHeader, versionHeader);
        API matchedAPI = APIFactory.getInstance().getMatchedAPIByKey(apikey);
        if (matchedAPI == null) {
            logger.error("API not found for the given API key");
            res = new DefaultFullHttpResponse(req.protocolVersion(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            return;
        }
        if (matchedAPI.getAPIConfig() == null) {
            logger.error("API configuration not found for the API");
            res = new DefaultFullHttpResponse(req.protocolVersion(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            return;
        }
        Map<String, String> additionalHeaders = new HashMap<>();
        String apiName = matchedAPI.getAPIConfig().getName();
        String authHeaderName;
        String testKeyName = ConfigHolder.getInstance().getConfig().getAuthHeader()
                .getTestConsoleHeaderName().toLowerCase();
        authHeaderName = matchedAPI.getAPIConfig().getAuthHeader();
        StringBuilder tokenHeader = new StringBuilder();
        if (headers.get(authHeaderName) != null) {
            tokenHeader.append(HttpHeaderNames.AUTHORIZATION).append(":").append(headers.get(authHeaderName));
            additionalHeaders.put(McpConstants.PAYLOAD_AUTH, tokenHeader.toString());
        } else if (headers.get(testKeyName) != null) {
            tokenHeader.append(testKeyName).append(":").append(headers.get(testKeyName));
            additionalHeaders.put(McpConstants.PAYLOAD_AUTH, tokenHeader.toString());
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Authorization header is not available for the API: {}", apiName);
            }

        }
        // Forward the backendJWT
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
        String backendJWTHeader = enforcerConfig.getJwtConfigurationDto().getJwtHeader();
        if (headers.get(backendJWTHeader) != null) {
            additionalHeaders.put(McpConstants.PAYLOAD_BACKEND_JWT,
                    backendJWTHeader + ":" + headers.get(backendJWTHeader));
        }

        // Handle mcp-session-id header
        String sessionId = null;
        if (headers.contains(McpConstants.MCP_SESSION_ID_HEADER)) {
            sessionId = headers.get(McpConstants.MCP_SESSION_ID_HEADER);
            if (sessionId == null || sessionId.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("mcp-session-id header is empty for API: {}", apiName);
                }
            } else {
                additionalHeaders.put(McpConstants.MCP_SESSION_ID_HEADER, sessionId);
            }
        }

        HttpContent requestContent = (HttpContent) msg;
        if (requestContent.content().isReadable()) {
            String body = requestContent.content().toString(StandardCharsets.UTF_8);
            McpResponseDto mcpResponse = McpRequestProcessor.processRequest(matchedAPI, body, additionalHeaders);
            if (mcpResponse != null) {
                res = new DefaultFullHttpResponse(req.protocolVersion(),
                        HttpResponseStatus.valueOf(mcpResponse.getStatusCode()),
                        Unpooled.wrappedBuffer(mcpResponse.getResponse().getBytes(StandardCharsets.UTF_8)));
                res.headers()
                        .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
                if (sessionId != null && !sessionId.isEmpty()) {
                    res.headers().set(McpConstants.ACCESS_CONTROL_EXPOSE_HEADERS, McpConstants.MCP_SESSION_ID_HEADER);
                    res.headers().set(McpConstants.MCP_SESSION_ID_HEADER, sessionId);
                }
                if (mcpResponse.getSessionId() != null && !mcpResponse.getSessionId().isEmpty()) {
                    res.headers().set(McpConstants.ACCESS_CONTROL_EXPOSE_HEADERS, McpConstants.MCP_SESSION_ID_HEADER);
                    res.headers().set(McpConstants.MCP_SESSION_ID_HEADER, mcpResponse.getSessionId());
                }
            } else {
                res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.NO_CONTENT);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Received empty request body for API: {}", apiName);
            }
            String jsonResponse = PayloadGenerator
                    .getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Request body not found");
            res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(jsonResponse.getBytes(StandardCharsets.UTF_8)));
            res.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
            if (sessionId != null && !sessionId.isEmpty()) {
                res.headers().set(McpConstants.ACCESS_CONTROL_EXPOSE_HEADERS, McpConstants.MCP_SESSION_ID_HEADER);
                res.headers().set(McpConstants.MCP_SESSION_ID_HEADER, sessionId);
            }
        }
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void handleWellKnownRequest(ChannelHandlerContext ctx, Object msg) {
        // Send a 501 Not Implemented response as this is not implemented yet
        FullHttpRequest req = (FullHttpRequest) msg;
        FullHttpResponse res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.NOT_IMPLEMENTED);
        ChannelFuture cf = ctx.writeAndFlush(res);
        cf.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private boolean isHeaderValid(FullHttpRequest req, String headerName) {
        HttpHeaders headers = req.headers();
        if (headers.contains(headerName)) {
            String headerValue = headers.get(headerName);
            if (headerValue != null && !headerValue.isEmpty()) {
                return true;
            } else {
                logger.error("Header {} is empty", headerName);
            }
        } else {
            logger.error("Header {} is missing", headerName);
        }
        return false;
    }
}
