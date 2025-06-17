/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.server;

import io.grpc.netty.shaded.io.netty.channel.ChannelInitializer;
import io.grpc.netty.shaded.io.netty.channel.ChannelPipeline;
import io.grpc.netty.shaded.io.netty.channel.socket.SocketChannel;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObjectAggregator;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpServerCodec;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.admin.AdminServerHandler;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.jwks.JWKSRequestHandler;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;
import org.wso2.choreo.connect.enforcer.mcp.request.McpRequestHandler;
import org.wso2.choreo.connect.enforcer.security.jwt.issuer.HttpTokenServerHandler;

/**
 Channel Initializer for the utility rest server
 Add Handlers to the pipeline which only respond to messages with a specific context.
 eg: "/jwks"
 Fire next channel read if context doesn't match
 */
public class RestServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final Logger logger = LogManager.getLogger(RestServerInitializer.class);
    private static final int MAX_CONTENT_LENGTH = 1048576; // 1 MB
    private static final String NETTY_CONTENT_LENGTH = "NETTY_CONTENT_LENGTH";
    private final SslContext sslCtx;
    public RestServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();

        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec());
        if (System.getenv(NETTY_CONTENT_LENGTH) == null) {
            p.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        } else {
            p.addLast(new HttpObjectAggregator(
                    Integer.parseInt(System.getenv(NETTY_CONTENT_LENGTH))));
        }
        if (enforcerConfig.getJwtConfigurationDto().isEnabled()) {
            p.addLast(new JWKSRequestHandler());
        }
        if (enforcerConfig.getJwtIssuerConfigurationDto().isEnabled()) {
            p.addLast(new HttpTokenServerHandler());
        }
        // This handler will act as the upstream for all MCP APIs
        boolean mcpEnabled = Boolean.parseBoolean(System.getenv(McpConstants.MCP_ENABLED));
        if (mcpEnabled) {
            logger.info("Initializing MCP handler...");
            p.addLast(new McpRequestHandler());
        }
        if (enforcerConfig.getRestServer().isEnable()) {
            p.addLast(new AdminServerHandler());
        }
    }
}
