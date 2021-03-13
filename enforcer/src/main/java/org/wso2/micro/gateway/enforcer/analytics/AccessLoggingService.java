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

package org.wso2.micro.gateway.enforcer.analytics;

import io.envoyproxy.envoy.service.accesslog.v3.AccessLogServiceGrpc;
import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsMessage;
import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsResponse;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.config.dto.AnalyticsReceiverConfigDTO;
import org.wso2.micro.gateway.enforcer.server.Constants;
import org.wso2.micro.gateway.enforcer.server.EnforcerThreadPoolExecutor;
import org.wso2.micro.gateway.enforcer.server.NativeThreadFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is the gRPC streaming server written to match with the envoy grpc access logger filter proto file.
 * Envoy proxy call this service.
 * This will gather data required for analytics.
 */
public class AccessLoggingService extends AccessLogServiceGrpc.AccessLogServiceImplBase {

    private static final Logger logger = LogManager.getLogger(AccessLoggingService.class);
    private static final String AUTH_TOKEN_KEY = "auth.api.token";
    private static final String AUTH_URL = "auth.api.url";

    public boolean init() {
        return startAccessLoggingServer();
    }

    @Override
    public StreamObserver<StreamAccessLogsMessage> streamAccessLogs
            (StreamObserver<StreamAccessLogsResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(StreamAccessLogsMessage message) {
                AnalyticsFilter.getInstance().handleMsg(message);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error while receiving access log entries from router. " + throwable.getMessage());
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(StreamAccessLogsResponse.newBuilder().build());
                responseObserver.onCompleted();
                logger.info("Access Log processing is completed.");
            }
        };
    }

    private boolean startAccessLoggingServer() {
        AnalyticsReceiverConfigDTO serverConfig =
                ConfigHolder.getInstance().getConfig().getAnalyticsConfig().getServerConfig();
        final EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        final EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        int blockingQueueLength = serverConfig.getThreadPoolConfig().getQueueSize();
        final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue(blockingQueueLength);
        final Executor executor =
                new EnforcerThreadPoolExecutor(serverConfig.getThreadPoolConfig().getCoreSize(),
                        serverConfig.getThreadPoolConfig().getMaxSize(),
                        serverConfig.getThreadPoolConfig().getKeepAliveTime(),
                        TimeUnit.SECONDS,
                        blockingQueue,
                        new NativeThreadFactory(new ThreadGroup(Constants.ANALYTICS_THREAD_GROUP),
                                Constants.ANALYTICS_THREAD_ID));

        Server accessLoggerService = NettyServerBuilder
                .forPort(serverConfig.getPort())
                .keepAliveTime(serverConfig.getKeepAliveTime(), TimeUnit.SECONDS)
                .maxInboundMessageSize(serverConfig.getMaxMessageSize())
                .bossEventLoopGroup(bossGroup)
                .workerEventLoopGroup(workerGroup)
                .addService(this)
                .channelType(NioServerSocketChannel.class).executor(executor).build();
        // Start the server
        try {
            accessLoggerService.start();
        } catch (IOException e) {
            logger.error("Error while starting the gRPC access logging server", e);
            return false;
        }
        logger.info("Access loggers Sever started Listening in port : " + serverConfig.getPort());
        return true;
    }
}
