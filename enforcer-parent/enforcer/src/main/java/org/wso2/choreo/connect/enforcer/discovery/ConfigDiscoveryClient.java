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

package org.wso2.choreo.connect.enforcer.discovery;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.config.enforcer.Config;
import org.wso2.choreo.connect.discovery.service.config.ConfigDiscoveryServiceGrpc;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.discovery.common.XDSCommonUtils;
import org.wso2.choreo.connect.enforcer.discovery.scheduler.XdsSchedulerManager;
import org.wso2.choreo.connect.enforcer.util.GRPCUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

/**
 * Client to communicate with configuration discovery service at the adapter.
 */
public class ConfigDiscoveryClient implements Runnable {
    private static final Logger log = LogManager.getLogger(ConfigDiscoveryClient.class);
    private static ConfigDiscoveryClient instance;
    private final String host;
    private final int port;
    private final CountDownLatch latch;
    private ConfigDiscoveryServiceGrpc.ConfigDiscoveryServiceBlockingStub blockingStub;
    private ManagedChannel channel;
    /**
     * Node struct for the discovery client
     */
    private final Node node;

    private ConfigDiscoveryClient(String host, int port, CountDownLatch latch) {
        this.host = host;
        this.port = port;
        this.latch = latch;
        this.node = XDSCommonUtils.generateXDSNode(AdapterConstants.COMMON_ENFORCER_LABEL);
        initConnection();
    }

    /**
     * Initialize an instance of {@link ConfigDiscoveryClient}.
     *
     * @param latch a latch counter to make calling thread wait for proper
     *              completion of config discovery.
     * @return initialized instance of {@link ConfigDiscoveryClient}
     */
    public static ConfigDiscoveryClient init(@NotNull CountDownLatch latch) {
        if (instance == null) {
            String adsHost = ConfigHolder.getInstance().getEnvVarConfig().getAdapterHost();
            int adsPort = Integer.parseInt(ConfigHolder.getInstance().getEnvVarConfig().getAdapterXdsPort());
            instance = new ConfigDiscoveryClient(adsHost, adsPort, latch);
        }
        return instance;
    }

    /**
     * Retrieves the already initialized instance of this class. Instance must be
     * initialized first by calling {@link ConfigDiscoveryClient#init(CountDownLatch)}
     * before calling this methods. This methods doesn't handled initialization
     * of the instance.
     *
     * @return initialized {@link ConfigDiscoveryClient} instance or {@code null}
     * if the instance is not initialized previously by calling
     * {@link ConfigDiscoveryClient#init(CountDownLatch)}
     */
    public static ConfigDiscoveryClient getInstance() {
        return instance;
    }

    private void initConnection() {
        if (GRPCUtils.isReInitRequired(channel)) {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdownNow();
                do {
                    try {
                        channel.awaitTermination(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        log.error("Config discovery channel shutdown wait was interrupted", e);
                    }
                } while (!channel.isShutdown());
            }
            this.channel = GRPCUtils.createSecuredChannel(log, host, port);
            this.blockingStub = ConfigDiscoveryServiceGrpc.newBlockingStub(channel);
        } else if (channel.getState(true) == ConnectivityState.READY) {
            XdsSchedulerManager.getInstance().stopConfigDiscoveryScheduling();
        }
    }

    public void requestInitConfig() {
        DiscoveryRequest req = DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(Constants.CONFIG_TYPE_URL).build();
        try {
            DiscoveryResponse res = blockingStub.withDeadlineAfter(60, TimeUnit.SECONDS).fetchConfigs(req);
            shutdown();
            XdsSchedulerManager.getInstance().stopConfigDiscoveryScheduling();

            // There's only one config root resource here. Therefore, taking 0 and no need to iterate
            Config config = res.getResources(0).unpack(Config.class);
            ConfigHolder.load(config);
            this.latch.countDown();
        } catch (Exception e) {
            // Catching generic error here to wrap any gRPC communication errors in the runtime
            log.error("Error occurred during Config discovery", e);
            XdsSchedulerManager.getInstance().startConfigDiscoveryScheduling();
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        initConnection();
        requestInitConfig();
    }
}
