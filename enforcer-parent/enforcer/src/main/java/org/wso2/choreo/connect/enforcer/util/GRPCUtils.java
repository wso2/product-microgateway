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

package org.wso2.choreo.connect.enforcer.util;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;

import java.io.File;
import java.nio.file.Paths;

import javax.net.ssl.SSLException;

/**
 * Utility functions required for gRPC Xds Clients.
 */
public class GRPCUtils {

    public static ManagedChannel createSecuredChannel(Logger logger, String host, int port) {
        File certFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPublicKeyPath()).toFile();
        File keyFile = Paths.get(ConfigHolder.getInstance().getEnvVarConfig().getEnforcerPrivateKeyPath()).toFile();
        SslContext sslContext = null;
        try {
            sslContext = GrpcSslContexts
                    .forClient()
                    .trustManager(ConfigHolder.getInstance().getTrustManagerFactory())
                    .keyManager(certFile, keyFile)
                    .build();
        } catch (SSLException e) {
            logger.error("Error while generating SSL Context.", e);
        }
        return NettyChannelBuilder.forAddress(host, port)
                .useTransportSecurity()
                .sslContext(sslContext)
                .overrideAuthority(ConfigHolder.getInstance().getEnvVarConfig().getAdapterHostName())
                .build();
    }

    public static boolean isReInitRequired(ManagedChannel channel) {
        if (channel != null && (channel.getState(true) == ConnectivityState.CONNECTING
                || channel.getState(true) == ConnectivityState.READY)) {
            return false;
        }
        return true;
    }
}
