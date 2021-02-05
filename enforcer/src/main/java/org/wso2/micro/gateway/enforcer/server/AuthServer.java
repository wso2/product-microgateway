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

package org.wso2.micro.gateway.enforcer.server;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.analytics.AnalyticsFilter;
import org.wso2.micro.gateway.enforcer.api.APIFactory;
import org.wso2.micro.gateway.enforcer.common.CacheProvider;
import org.wso2.micro.gateway.enforcer.common.ReferenceHolder;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.config.dto.AuthServiceConfigurationDto;
import org.wso2.micro.gateway.enforcer.grpc.ExtAuthService;
import org.wso2.micro.gateway.enforcer.keymgt.KeyManagerDataService;
import org.wso2.micro.gateway.enforcer.keymgt.KeyManagerDataServiceImpl;
import org.wso2.micro.gateway.enforcer.listener.GatewayJMSMessageListener;
import org.wso2.micro.gateway.enforcer.subscription.SubscriptionDataHolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

/**
 * gRPC netty based server that handles the incoming requests.
 */
public class AuthServer {

    private static final Logger logger = LogManager.getLogger(AuthServer.class);

    public static void main(String[] args) {
        try {
            KeyManagerDataService keyManagerDataService = new KeyManagerDataServiceImpl();
            // Load configurations
            ConfigHolder configHolder = ConfigHolder.getInstance();
            ReferenceHolder.getInstance().setKeyManagerDataService(keyManagerDataService);
            APIFactory.getInstance().init();

            // Create a new server to listen on port 8081
            Server server = initServer();

            // Enable global filters
            // TODO (amalimatharaarachchi) enable analytics according to config
            boolean analytics = true;
            if (analytics) {
                logger.info("analytics filter enabled");
                new AnalyticsFilter();
            }

            //Initialise cache objects
            CacheProvider.init();

            // Start the server
            server.start();
            logger.info("Sever started Listening in port : " + 8081);

            if (configHolder.getConfig().getEventHub().isEnabled()) {
                logger.info("Event Hub configuration enabled... Starting JMS listener...");
                GatewayJMSMessageListener.init(configHolder.getConfig().getEventHub());
            }
            //TODO: Get the tenant domain from config
            SubscriptionDataHolder.getInstance().getTenantSubscriptionStore().initializeStore();

            // Don't exit the main thread. Wait until server is terminated.
            server.awaitTermination();
        } catch (IOException e) {
            logger.error("Error while starting the enforcer gRPC server.", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.error("Enforcer server main thread interrupted.", e);
            System.exit(1);
        } catch (Exception ex) {
            // printing the stack trace in case logger might not have been initialized
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static Server initServer() throws SSLException {
        File certFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPublicKeyPath()).toFile();
        File keyFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPrivateKeyPath()).toFile();
        final EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        final EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        AuthServiceConfigurationDto authServerConfig = ConfigHolder.getInstance().getConfig().getAuthService();
        AuthServiceConfigurationDto.ThreadPoolConfig threadPoolConfig = authServerConfig.getThreadPool();
        EnforcerWorkerPool enforcerWorkerPool = new EnforcerWorkerPool(threadPoolConfig.getCoreSize(),
                threadPoolConfig.getMaxSize(), threadPoolConfig.getKeepAliveTime(), threadPoolConfig.getQueueSize(),
                Constants.EXTERNAL_AUTHZ_THREAD_GROUP, Constants.EXTERNAL_AUTHZ_THREAD_ID);
        return NettyServerBuilder.forPort(authServerConfig.getPort())
                .keepAliveTime(authServerConfig.getKeepAliveTime(), TimeUnit.SECONDS).bossEventLoopGroup(bossGroup)
                .workerEventLoopGroup(workerGroup).addService(new ExtAuthService())
                .maxInboundMessageSize(authServerConfig.getMaxMessageSize())
                .maxInboundMetadataSize(authServerConfig.getMaxHeaderLimit()).channelType(NioServerSocketChannel.class)
                .executor(enforcerWorkerPool.getExecutor())
                .sslContext(GrpcSslContexts.forServer(certFile, keyFile)
                        .trustManager(ConfigHolder.getInstance().getTrustManagerFactory())
                        .clientAuth(ClientAuth.REQUIRE)
                        .build())
                .build();
    }
}
