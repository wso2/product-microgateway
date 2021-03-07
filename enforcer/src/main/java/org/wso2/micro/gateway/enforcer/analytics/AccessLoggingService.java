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

package org.wso2.micro.gateway.enforcer.analytics;

import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
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
import org.wso2.carbon.apimgt.common.gateway.analytics.AnalyticsConfigurationHolder;
import org.wso2.carbon.apimgt.common.gateway.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.gateway.analytics.collectors.impl.GenericRequestDataCollector;
import org.wso2.carbon.apimgt.common.gateway.analytics.exceptions.AnalyticsException;
import org.wso2.micro.gateway.enforcer.server.EnforcerThreadPoolExecutor;
import org.wso2.micro.gateway.enforcer.server.NativeThreadFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    public boolean init() {
        Map<String, String> configuration = new HashMap<>(2);
        configuration.put("auth.api.token", "eyJ4NXQiOiJOVGd5TTJabE9XVTJOV00zWlRVeFpESTRNamsyTWpoa1pEVT" +
                "FOak15TjJZMlpHRTBPV1ExTVRFek9HVmtabVl4WlRVMU9Ea3dOekV6TnpjNU9EVXpaZyIsImtpZCI6Ik5UZ3lNMl" +
                "psT1dVMk5XTTNaVFV4WkRJNE1qazJNamhrWkRVMU5qTXlOMlkyWkdFME9XUTFNVEV6T0dWa1ptWXhaVFUxT0Rrd" +
                "056RXpOemM1T0RVelpnX1JTMjU2IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJ3c28yX2Rldl9hZG1pbiIsImF1dCI" +
                "6IkFQUExJQ0FUSU9OX1VTRVIiLCJpc3MiOiJodHRwczpcL1wvaWQucHJlcHJvZC5jaG9yZW8uZGV2OjQ0M1wvb2F1" +
                "dGgyXC90b2tlbiIsImdyb3VwcyI6WyJJbnRlcm5hbFwvZXZlcnlvbmUiLCJBTkFMWVRJQ1MtQVBJTS5DT01cL21vbmV" +
                "0aXplIl0sImF1ZCI6Im5wRk9oVG1aZjRTSUtUMFJFcGZQaDc0QTJWa2EiLCJlbnZpcm9ubWVudCI6InByb2QiLCJuYmY" +
                "iOjE2MTQxODQ5NTcsImF6cCI6Im5wRk9oVG1aZjRTSUtUMFJFcGZQaDc0QTJWa2EiLCJzY29wZSI6ImFuYWx5dGljc19hc" +
                "GltIG9wZW5pZCIsImN1c3RvbWVySWQiOiJ3c28yIiwiZXhwIjoxNjE1NDgwOTU3LCJpYXQiOjE2MTQxODQ5NTcsIm" +
                "p0aSI6IjFiYzJjMTAxLWM5YTYtNGQwOS1iMjJmLTE0OWQxNmYyZjg1ZSJ9.hKFAsQpV_CTMaE_6l4IcjZ-ZWYQEFP" +
                "Ef_EBrru5D73LYz7qkWAF67rLJqn29DizPf8Q8PsRRoeXiIO8St2hfv7QI6GW35ex1ak0I9g_8228I96WlO6Ihdydo" +
                "NYTi9Mvrq2_ZWzF3RwWBqM6vTSiIdFZB9rSJ54TmeVCQdI0IzKSXGV7LVN-kP6uXSiTF83eqEXNTaVnIqKZ_VEtiti" +
                "VdASW3PU45C1ksD7NLFXjhkJHEUfkuBXXXJJr1oNOdEXMXdoky2kbI0coZQxo0vZOOfxhXoMnE5H9qkyt89qbv13ZR" +
                "7jZx83lcZb4dar8ImvKZVUxkYbUaR49aRZd22jULEQ");
        configuration.put("auth.api.url", "https://analytics-event-auth.st.choreo.dev/auth/v1");
        AnalyticsConfigurationHolder.getInstance().setConfigurations(configuration);
        return startAccessLoggingServer();
    }

    @Override
    public StreamObserver<StreamAccessLogsMessage> streamAccessLogs
            (StreamObserver<StreamAccessLogsResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(StreamAccessLogsMessage message) {
                logger.info("Received msg" + message.toString());
                for (int i = 0; i < message.getHttpLogs().getLogEntryCount(); i++) {
//                    Event event = new Event();
                    HTTPAccessLogEntry logEntry = message.getHttpLogs().getLogEntry(i);
//                    // TODO: (VirajSalaka) Null check
//                    Map<String, Value> fieldsMap = logEntry.getCommonProperties().getMetadata()
//                            .getFilterMetadataMap().get("envoy.filters.http.ext_authz").getFieldsMap();
//
//                    // TODO: (VirajSalaka) Use the map itself
//                    event.setApi(generateAPIFromMetadataMap(fieldsMap));
//                    event.setApplication(generateApplicationFromMetadataMap(fieldsMap));
//                    event.setMetaInfo(generateMetaInfoFromMetadataMap(fieldsMap));
//                    event.setLatencies(generateLatencies(logEntry.getCommonProperties()));
//                    event.setOperation(generateOperation(fieldsMap, logEntry));
//                    event.setTarget(generateTarget(logEntry));

                    AnalyticsDataProvider provider = new MgwAnalyticsProvider(logEntry);
                    GenericRequestDataCollector dataCollector = new GenericRequestDataCollector(provider);
                    try {
                        dataCollector.collectData();
                    } catch (AnalyticsException e) {
                        logger.error("Analtytics Error. ", e);
                    }
                }

            }

            @Override
            public void onError(Throwable throwable) {
                logger.info("Error in receiving access log from envoy" + throwable.getMessage());
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                logger.info("grpc logger completed");
                responseObserver.onNext(StreamAccessLogsResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }

//    private API generateAPIFromMetadataMap(Map<String, Value> fieldsMap) {
//        API api = new API();
//        api.setApiId(getValueAsString(fieldsMap, "ApiId"));
//        api.setApiCreator(getValueAsString(fieldsMap, "ApiCreator"));
//        api.setApiType(getValueAsString(fieldsMap, "ApiType"));
//        api.setApiName(getValueAsString(fieldsMap, "ApiName"));
//        api.setApiVersion(getValueAsString(fieldsMap, "ApiVersion"));
//        api.setApiCreatorTenantDomain(getValueAsString(fieldsMap, "ApiCreatorTenantDomain"));
//        return api;
//    }

//    private Application generateApplicationFromMetadataMap(Map<String, Value> fieldsMap) {
//        Application application = new Application();
//        application.setApplicationOwner(getValueAsString(fieldsMap, "ApplicationOwner"));
//        application.setApplicationId(getValueAsString(fieldsMap, "ApplicationName"));
//        application.setKeyType(getValueAsString(fieldsMap, "ApplicationKeyType"));
//        application.setApplicationId(getValueAsString(fieldsMap, "ApplicationId"));
//        return application;
//    }

//    private MetaInfo generateMetaInfoFromMetadataMap(Map<String, Value> fieldsMap) {
//        MetaInfo metaInfo = new MetaInfo();
//        metaInfo.setCorrelationId(getValueAsString(fieldsMap, "CorrelationId"));
//        metaInfo.setDeploymentId(getValueAsString(fieldsMap, "DeploymentId"));
//        metaInfo.setGatewayType(getValueAsString(fieldsMap, "GatewayType"));
//        metaInfo.setRegionId(getValueAsString(fieldsMap, "RegionId"));
//        return metaInfo;
//    }

//    private Operation generateOperation(Map<String, Value> fieldsMap, HTTPAccessLogEntry logEntry) {
//        Operation operation = new Operation();
//        operation.setApiResourceTemplate(getValueAsString(fieldsMap, "ApiResourceTemplate"));
//        operation.setApiMethod(logEntry.getRequest().getRequestMethod().name());
//        return operation;
//    }

//    private String getValueAsString(Map<String, Value> fieldsMap, String key) {
//        return fieldsMap.get(key).getStringValue();
//    }

//    private Latencies generateLatencies(AccessLogCommon properties) {
//        Latencies latencies = new Latencies();
//        // TODO: (VirajSalaka) If connection error happens these won't be available
//        // TODO: (VirajSalaka) Finalize the correctness after discussion
//        latencies.setBackendLatency(properties.getTimeToFirstUpstreamTxByte().getNanos() / 1000000 -
//                properties.getTimeToLastUpstreamRxByte().getNanos() / 1000000);
//        latencies.setResponseLatency(properties.getTimeToLastDownstreamTxByte().getNanos() / 1000000);
//        latencies.setRequestMediationLatency(properties.getTimeToLastUpstreamRxByte().getNanos() / 1000000);
//        latencies.setResponseMediationLatency(properties.getTimeToLastDownstreamTxByte().getNanos() / 1000000 -
//                properties.getTimeToFirstUpstreamRxByte().getNanos() / 1000000);
//        return latencies;
//    }

//    private Target generateTarget(HTTPAccessLogEntry logEntry) {
//        Target target = new Target();
//        // As response caching is not configured at the moment.
//        target.setResponseCacheHit(false);
//        target.setTargetResponseCode(logEntry.getResponse().getResponseCode().getValue());
//        // TODO: (VirajSalaka) get destination in the format of url
//        // TODO: (VirajSalaka) add backend basepath
//        target.setDestination(logEntry.getCommonProperties().getUpstreamRemoteAddress().getSocketAddress()
//                .getAddress());
//        return target;
//    }

//    public static String getTimeInISO(long time) {
//        OffsetDateTime offsetDateTime = OffsetDateTime
//                .ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC.normalized());
//        return offsetDateTime.toString();
//    }

    private boolean startAccessLoggingServer() {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        final EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        int blockingQueueLength = 1000;
        final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue(blockingQueueLength);
        final Executor executor = new EnforcerThreadPoolExecutor(400, 500, 30, TimeUnit.SECONDS,
                blockingQueue, new NativeThreadFactory(new ThreadGroup("Analytics"), "analytics"));

        Server accessLoggerService = NettyServerBuilder.forPort(18090).maxConcurrentCallsPerConnection(20)
                .keepAliveTime(60, TimeUnit.SECONDS).maxInboundMessageSize(1000000000).bossEventLoopGroup(bossGroup)
                .workerEventLoopGroup(workerGroup).addService(this)
                .channelType(NioServerSocketChannel.class).executor(executor).build();
        // Start the server
        try {
            accessLoggerService.start();
        } catch (IOException e) {
            logger.error("Error while starting the gRPC access logging server", e);
            return false;
        }
        logger.info("Access loggers Sever started Listening in port : " + 18090);
        return true;
    }
}
