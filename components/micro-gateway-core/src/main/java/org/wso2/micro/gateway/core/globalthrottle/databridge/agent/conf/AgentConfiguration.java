/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.core.globalthrottle.databridge.agent.conf;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ballerinalang.jvm.values.api.BMap;
import org.wso2.micro.gateway.core.globalthrottle.databridge.agent.util.DataAgentConstants;
import org.wso2.micro.gateway.core.globalthrottle.databridge.agent.util.DataEndpointConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data agent configuration.
 */
public class AgentConfiguration {
    private static final Logger log = LogManager.getLogger(AgentConfiguration.class);

    private AgentConfiguration() {
    }

    private final String publishingStrategy = DataEndpointConstants.ASYNC_STRATEGY;
    private String trustStorePath;
    private char[] trustStorePassword;
    private int queueSize;
    private int batchSize;
    private int corePoolSize;
    private int socketTimeoutMS;
    private int maxPoolSize;
    private int keepAliveTimeInPool;
    private int reconnectionInterval;
    private int maxTransportPoolSize;
    private int maxIdleConnections;
    private int evictionTimePeriod;
    private int minIdleTimeInPool;
    private int secureMaxTransportPoolSize;
    private int secureMaxIdleConnections;
    private int secureEvictionTimePeriod;
    private int secureMinIdleTimeInPool;
    private String sslEnabledProtocols;
    private String ciphers;

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return String.valueOf(trustStorePassword);
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getKeepAliveTimeInPool() {
        return keepAliveTimeInPool;
    }

    public int getReconnectionInterval() {
        return reconnectionInterval;
    }

    public int getMaxTransportPoolSize() {
        return maxTransportPoolSize;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public int getEvictionTimePeriod() {
        return evictionTimePeriod;
    }

    public int getMinIdleTimeInPool() {
        return minIdleTimeInPool;
    }

    public int getSecureMaxTransportPoolSize() {
        return secureMaxTransportPoolSize;
    }

    public int getSecureMaxIdleConnections() {
        return secureMaxIdleConnections;
    }

    public int getSecureEvictionTimePeriod() {
        return secureEvictionTimePeriod;
    }

    public int getSecureMinIdleTimeInPool() {
        return secureMinIdleTimeInPool;
    }

    public String getSslEnabledProtocols() {
        return sslEnabledProtocols;
    }

    public String getCiphers() {
        return ciphers;
    }

    public String getPublishingStrategy() {
        return publishingStrategy;
    }

    @Override
    public String toString() {
        return ", PublishingStrategy : " + publishingStrategy +
                "TrustSorePath" + trustStorePath +
                "TrustSorePassword" + trustStorePassword +
                "QueueSize" + queueSize +
                "BatchSize" + batchSize +
                "CorePoolSize" + corePoolSize +
                "SocketTimeoutMS" + socketTimeoutMS +
                "MaxPoolSize" + maxPoolSize +
                "KeepAliveTimeInPool" + keepAliveTimeInPool +
                "ReconnectionInterval" + reconnectionInterval +
                "MaxTransportPoolSize" + maxTransportPoolSize +
                "MaxIdleConnections" + maxIdleConnections +
                "EvictionTimePeriod" + evictionTimePeriod +
                "MinIdleTimeInPool" + minIdleTimeInPool +
                "SecureMaxTransportPoolSize" + secureMaxTransportPoolSize +
                "SecureMaxIdleConnections" + secureMaxIdleConnections +
                "SecureEvictionTimePeriod" + secureEvictionTimePeriod +
                "SecureMinIdleTimeInPool" + secureMinIdleTimeInPool +
                "SSLEnabledProtocols" + sslEnabledProtocols +
                "Ciphers" + ciphers;
    }

    private static class InnerAgentConfiguration {
        private static final AgentConfiguration instance = new AgentConfiguration();
    }

    public static AgentConfiguration getInstance() {
        return InnerAgentConfiguration.instance;
    }

    public void setConfiguration(BMap<String, Object> configuration) {
        String trustStorePath = String.valueOf(configuration.get(DataAgentConstants.TRUST_STORE_PATH));
        //TrustStore path provided from the microgateway configuration needs to be preprocessed.
        this.trustStorePath = preProcessTrustStorePath(trustStorePath);
        this.trustStorePassword = String.valueOf(configuration.get(DataAgentConstants.TRUST_STORE_PASSWORD))
                .toCharArray();
        this.sslEnabledProtocols = String.valueOf(configuration.get(DataAgentConstants.SSL_ENABLED_PROTOCOLS));
        this.ciphers = String.valueOf(configuration.get(DataAgentConstants.CIPHERS));

        try {
            this.queueSize = Math.toIntExact((long) configuration.get(DataAgentConstants.QUEUE_SIZE));
            this.batchSize = Math.toIntExact((long) configuration.get(DataAgentConstants.BATCH_SIZE));
            this.corePoolSize = Math.toIntExact((long) configuration.get(DataAgentConstants.CORE_POOL_SIZE));
            this.socketTimeoutMS = Math.toIntExact((long) configuration.get(DataAgentConstants.SOCKET_TIMEOUT_MS));
            this.maxPoolSize = Math.toIntExact((long) configuration.get(DataAgentConstants.MAX_POOL_SIZE));
            this.keepAliveTimeInPool = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.KEEP_ALIVE_TIME_INTERVAL_IN_POOL));
            this.reconnectionInterval = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.RECONNECTION_INTERVAL));
            this.maxTransportPoolSize = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.MAX_TRANSPORT_POOL_SIZE));
            this.maxIdleConnections = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.MAX_IDLE_CONNECTIONS));
            this.evictionTimePeriod = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.EVICTION_TIME_PERIOD));
            this.minIdleTimeInPool = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.MIN_IDLE_TIME_IN_POOL));
            this.secureMaxTransportPoolSize = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.SECURE_MAX_TRANSPORT_POOL_SIZE));
            this.secureMaxIdleConnections = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.SECURE_MAX_IDLE_CONNECTIONS));
            this.secureEvictionTimePeriod = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.SECURE_EVICTION_TIME_PERIOD));
            this.secureMinIdleTimeInPool = Math.toIntExact((long) configuration
                    .get(DataAgentConstants.SECURE_MIN_IDLE_TIME_IN_POOL));
        } catch (ArithmeticException e) {
            log.error("Error while processing the publisher configuration.", e);
        }
    }

    /**
     * The Truststore path provided from the ballerina implementation could be associated with a system property.
     * It needs to substituted with relevant system property.
     * e.g. ${mgw-runtime.home}/runtime/bre/security/ballerinaTruststore.p12
     *
     * @param mgwTrustStorePath trustStorePath as provided by the microgateway configuration
     * @return resolved trustStorePath
     */
    private static String preProcessTrustStorePath(String mgwTrustStorePath) {
        String placeHolderRegex = "\\$\\{.*\\}";
        Pattern placeHolderPattern = Pattern.compile(placeHolderRegex);
        Matcher placeHolderMatcher = placeHolderPattern.matcher(mgwTrustStorePath);
        if (placeHolderMatcher.find()) {
            String placeHolder = placeHolderMatcher.group(0);
            //to remove additional symbols
            String systemPropertyKey = placeHolder.substring(2, placeHolder.length() - 1);
            //To support windows path (replaces \ with \\ internally)
            String replacement = Matcher.quoteReplacement(System.getProperty(systemPropertyKey));
            return placeHolderMatcher.replaceFirst(replacement);
        }
        return mgwTrustStorePath;
    }
}
