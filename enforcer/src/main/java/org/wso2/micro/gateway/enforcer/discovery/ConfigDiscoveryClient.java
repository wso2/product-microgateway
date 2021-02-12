/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.micro.gateway.enforcer.discovery;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.config.enforcer.Config;
import org.wso2.gateway.discovery.service.config.ConfigDiscoveryServiceGrpc;
import org.wso2.micro.gateway.enforcer.config.EnvVarConfig;
import org.wso2.micro.gateway.enforcer.constants.AdapterConstants;
import org.wso2.micro.gateway.enforcer.constants.Constants;
import org.wso2.micro.gateway.enforcer.exception.DiscoveryException;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

/**
 * Client to communicate with configuration discovery service at the adapter.
 */
public class ConfigDiscoveryClient {
    private static final Logger log = LogManager.getLogger(ConfigDiscoveryClient.class);
    private final ManagedChannel channel;
    private final ConfigDiscoveryServiceGrpc.ConfigDiscoveryServiceBlockingStub blockingStub;
    private String nodeId;

    public ConfigDiscoveryClient(EnvVarConfig envVarConfig, TrustManagerFactory trustManagerFactory) {
        File certFile = Paths.get(envVarConfig.getEnforcerPublicKeyPath()).toFile();
        File keyFile = Paths.get(envVarConfig.getEnforcerPrivateKeyPath()).toFile();
        SslContext sslContext = null;
        try {
            sslContext = GrpcSslContexts.forClient()
                    .trustManager(trustManagerFactory)
                    .keyManager(certFile, keyFile)
                    .build();
            // TODO: (VirajSalaka) panic the enforcer
        } catch (SSLException e) {
            log.error("Error while generating SSL Context.", e);
        }
        channel = NettyChannelBuilder
                .forAddress(envVarConfig.getAdapterHost(), Integer.parseInt(envVarConfig.getAdapterXdsPort()))
                .useTransportSecurity()
                .sslContext(sslContext)
                .overrideAuthority(envVarConfig.getAdapterHostName())
                .build();
        this.blockingStub = ConfigDiscoveryServiceGrpc.newBlockingStub(channel);
        nodeId = AdapterConstants.COMMON_ENFORCER_LABEL;
    }

    public Config requestInitConfig() throws DiscoveryException {
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(Node.newBuilder().setId(nodeId).build())
                .setTypeUrl(Constants.CONFIG_TYPE_URL).build();
        DiscoveryResponse res = DiscoveryResponse.getDefaultInstance();
        try {
            res = requestConfig(req);

            // Theres only one config root resource here. Therefore taking 0, no need to iterate
            return res.getResources(0).unpack(Config.class);
        } catch (Exception e) {
            // catching generic error here to wrap any grpc communication errors in the runtime
            throw new DiscoveryException("Couldn't fetch init configs", e);
        }
    }

    /**
     * Call a blocking RPC and retry {@code Constats.MAX_XDS_RETRIES} times before failing.
     *
     * @param req {@link DiscoveryRequest} with the request information for the RPC
     * @return Response as a {@link DiscoveryResponse} for the requested RPC
     * @throws DiscoveryException all retries to the grpc server failed or attempt to shutdown the connection failed
     */
    private DiscoveryResponse requestConfig(DiscoveryRequest req) throws DiscoveryException {
        DiscoveryResponse res;
        int retries = 0;
        Exception e = new Exception();

        // We are looking for a runtime exception to retry. Therefore with the `break` statement,
        // the IDE always mark this condition as always true condition.
        while (retries < Constants.MAX_XDS_RETRIES) {
            try {
                res = blockingStub.withDeadlineAfter(10, TimeUnit.SECONDS).fetchConfigs(req);
                shutdown();
                return res;
            } catch (Exception ex) {
                // catching generic error here to wrap any grpc communication errors in the runtime
                e = ex;
            }
            retries++;
        }
        throw new DiscoveryException("Failed " + Constants.MAX_XDS_RETRIES + " retries", e);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
