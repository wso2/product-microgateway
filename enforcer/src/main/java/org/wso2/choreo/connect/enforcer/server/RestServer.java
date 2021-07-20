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

package org.wso2.choreo.connect.enforcer.server;

import io.grpc.netty.shaded.io.netty.bootstrap.ServerBootstrap;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.netty.shaded.io.netty.handler.logging.LogLevel;
import io.grpc.netty.shaded.io.netty.handler.logging.LoggingHandler;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.admin.AdminServerInitializer;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.security.jwt.issuer.HttpTokenServerInitializer;

import java.io.File;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

/**
 * TokenServer to handle JWT /testkey endpoint backend HTTPS service.
 */
public class RestServer {

    private static final Logger logger = LogManager.getLogger(RestServer.class);
    static final int TOKEN_PORT = 8082;
    static final int ADMIN_PORT = 9001;

    public void initServer() throws SSLException, CertificateException, InterruptedException {

        // Configure SSL
        final SslContext sslCtx;
        final SslContextBuilder ssl;
        File certFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPublicKeyPath()).toFile();
        File keyFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPrivateKeyPath()).toFile();
        ssl = SslContextBuilder.forServer(certFile, keyFile);
        ssl.trustManager(ConfigHolder.getInstance().getTrustManagerFactory());
        sslCtx = ssl.build();

        // Create the multithreaded event loops for the server
        final EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        final EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        try {
            // A helper class that simplifies server configuration
            ServerBootstrap tokenServer = new ServerBootstrap();
            // Configure the server
            tokenServer.option(ChannelOption.SO_BACKLOG, 1024);
            tokenServer.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpTokenServerInitializer(sslCtx));

            Channel tokenChannel = tokenServer.bind(TOKEN_PORT).sync().channel();
            logger.info("Token endpoint started Listening in port : " + TOKEN_PORT);


            if (ConfigHolder.getInstance().getConfig().getRestServer().isEnable()) {
                ServerBootstrap adminServer = new ServerBootstrap();
                // Configure the server
                adminServer.option(ChannelOption.SO_BACKLOG, 1024);
                adminServer.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new AdminServerInitializer(sslCtx));

                Channel adminChannel = adminServer.bind(ADMIN_PORT).sync().channel();
                logger.info("Admin endpoint started Listening in port : " + ADMIN_PORT);
                adminChannel.closeFuture().sync();
            }

            // Wait until server socket is closed
            tokenChannel.closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
