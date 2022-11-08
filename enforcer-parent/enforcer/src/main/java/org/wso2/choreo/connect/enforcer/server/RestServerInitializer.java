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

package org.wso2.choreo.connect.enforcer.server;

import io.grpc.netty.shaded.io.netty.channel.ChannelInitializer;
import io.grpc.netty.shaded.io.netty.channel.ChannelPipeline;
import io.grpc.netty.shaded.io.netty.channel.socket.SocketChannel;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObjectAggregator;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpServerCodec;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.wso2.choreo.connect.enforcer.admin.AdminServerHandler;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.jwks.JWKSRequestHandler;
import org.wso2.choreo.connect.enforcer.security.jwt.issuer.HttpTokenServerHandler;

/**
    Channel Initializer for the utility rest server

    Add Handlers to the pipeline which only respond to messages with a specific context.
    eg: "/jwks"
    Fire next channel read if context doesn't match


 */
public class RestServerInitializer extends ChannelInitializer<SocketChannel> {
    private final SslContext sslCtx;
    public RestServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }
    //TODO: Logging?
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(1048576));
        if (ConfigHolder.getInstance().getConfig().getJwtConfigurationDto().isEnabled()) {
            p.addLast(new JWKSRequestHandler());
        }
        if (ConfigHolder.getInstance().getConfig().getJwtIssuerConfigurationDto().isEnabled()) {
            p.addLast(new HttpTokenServerHandler());
        }
        if (ConfigHolder.getInstance().getConfig().getRestServer().isEnable()) {
            p.addLast(new AdminServerHandler());
        }

        //TODO: Add handler to deal with resource not found
    }
}
