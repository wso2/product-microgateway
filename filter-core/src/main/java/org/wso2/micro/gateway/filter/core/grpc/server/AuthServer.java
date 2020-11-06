/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.filter.core.grpc.server;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.api.APIFactory;
import org.wso2.micro.gateway.filter.core.common.CacheProvider;
import org.wso2.micro.gateway.filter.core.common.ReferenceHolder;
import org.wso2.micro.gateway.filter.core.config.MGWConfiguration;
import org.wso2.micro.gateway.filter.core.keymgt.KeyManagerDataService;
import org.wso2.micro.gateway.filter.core.keymgt.KeyManagerDataServiceImpl;
import org.wso2.micro.gateway.filter.core.listener.GatewayJMSMessageListener;
import org.wso2.micro.gateway.filter.core.subscription.SubscriptionDataHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * gRPC netty based server that handles the incoming requests.
 */
public class AuthServer {

    private static final Logger logger = LogManager.getLogger(AuthServer.class);

    public static void main(String[] args) throws Exception {
        // Create a new server to listen on port 8081
        final EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        final EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        int blockingQueueLength = 1000;
        final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue(blockingQueueLength);
        final Executor executor = new MGWThreadPoolExecutor(400, 500, 30, TimeUnit.SECONDS, blockingQueue);
        Server server = NettyServerBuilder.forPort(8081).maxConcurrentCallsPerConnection(20)
                .keepAliveTime(60, TimeUnit.SECONDS).maxInboundMessageSize(1000000000).bossEventLoopGroup(bossGroup)
                .workerEventLoopGroup(workerGroup).addService(new ExtAuthService())
                .channelType(NioServerSocketChannel.class).executor(executor).build();

        // Load configurations
        KeyManagerDataService keyManagerDataService = new KeyManagerDataServiceImpl();
        MGWConfiguration mgwConfiguration = new MGWConfiguration();
        ReferenceHolder.getInstance().setKeyManagerDataService(keyManagerDataService);
        ReferenceHolder.getInstance().setMGWConfiguration(mgwConfiguration);
        //TODO: Add API is only for testing this has to come via the rest API.
        addAPI();
        CacheProvider.init();

        // Start the server
        server.start();
        logger.info("Sever started Listening in port : " + 8081);

        if (mgwConfiguration.getEventHubConfiguration().isEnabled()) {
            logger.info("Event Hub configuration enabled... Starting JMS listener...");
            GatewayJMSMessageListener.init(mgwConfiguration.getEventHubConfiguration());
        }
        //TODO: Get the tenant domain from config
        SubscriptionDataHolder.getInstance().registerTenantSubscriptionStore("carbon.super");

        // Don't exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }

    private static void addAPI() {
        String apiPath = "/home/wso2/apis/";
        try {
            Files.walk(Paths.get(apiPath)).filter(path -> {
                Path fileName = path.getFileName();
                return fileName != null && (fileName.toString().endsWith(".yaml") || fileName.toString()
                        .endsWith(".json"));
            }).forEach(path -> {
                OpenAPI openAPI = new OpenAPIV3Parser().read(path.toString());
                APIFactory.getInstance().addAPI(openAPI, "http");
            });
        } catch (IOException e) {
            logger.error("Error while reading API files", e);
        }
    }
}
