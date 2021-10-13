/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf;

import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.DataEndpointConstants;

import java.security.KeyStore;

/**
 * Agent configurations for throttle publisher.
 */
public class AgentConfiguration {

    private final String publishingStrategy = DataEndpointConstants.ASYNC_STRATEGY;
    private KeyStore trustStore;
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

    private static AgentConfiguration instance = new AgentConfiguration();

    private AgentConfiguration() {
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    public void setSocketTimeoutMS(int socketTimeoutMS) {
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getKeepAliveTimeInPool() {
        return keepAliveTimeInPool;
    }

    public void setKeepAliveTimeInPool(int keepAliveTimeInPool) {
        this.keepAliveTimeInPool = keepAliveTimeInPool;
    }

    public int getReconnectionInterval() {
        return reconnectionInterval;
    }

    public void setReconnectionInterval(int reconnectionInterval) {
        this.reconnectionInterval = reconnectionInterval;
    }

    public int getMaxTransportPoolSize() {
        return maxTransportPoolSize;
    }

    public void setMaxTransportPoolSize(int maxTransportPoolSize) {
        this.maxTransportPoolSize = maxTransportPoolSize;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public int getEvictionTimePeriod() {
        return evictionTimePeriod;
    }

    public void setEvictionTimePeriod(int evictionTimePeriod) {
        this.evictionTimePeriod = evictionTimePeriod;
    }

    public int getMinIdleTimeInPool() {
        return minIdleTimeInPool;
    }

    public void setMinIdleTimeInPool(int minIdleTimeInPool) {
        this.minIdleTimeInPool = minIdleTimeInPool;
    }

    public int getSecureMaxTransportPoolSize() {
        return secureMaxTransportPoolSize;
    }

    public void setSecureMaxTransportPoolSize(int secureMaxTransportPoolSize) {
        this.secureMaxTransportPoolSize = secureMaxTransportPoolSize;
    }

    public int getSecureMaxIdleConnections() {
        return secureMaxIdleConnections;
    }

    public void setSecureMaxIdleConnections(int secureMaxIdleConnections) {
        this.secureMaxIdleConnections = secureMaxIdleConnections;
    }

    public int getSecureEvictionTimePeriod() {
        return secureEvictionTimePeriod;
    }

    public void setSecureEvictionTimePeriod(int secureEvictionTimePeriod) {
        this.secureEvictionTimePeriod = secureEvictionTimePeriod;
    }

    public int getSecureMinIdleTimeInPool() {
        return secureMinIdleTimeInPool;
    }

    public void setSecureMinIdleTimeInPool(int secureMinIdleTimeInPool) {
        this.secureMinIdleTimeInPool = secureMinIdleTimeInPool;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
    }

    public String getSslEnabledProtocols() {
        return sslEnabledProtocols;
    }

    public void setSslEnabledProtocols(String sslEnabledProtocols) {
        this.sslEnabledProtocols = sslEnabledProtocols;
    }

    public String getCiphers() {
        return ciphers;
    }

    public void setCiphers(String ciphers) {
        this.ciphers = ciphers;
    }

    public String getPublishingStrategy() {
        return publishingStrategy;
    }

    @Override
    public String toString() {
        return ", PublishingStrategy : " + publishingStrategy +
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

    public static AgentConfiguration getInstance() {
        return instance;
    }

    public static synchronized void setInstance(AgentConfiguration agentConfiguration) {
        instance = agentConfiguration;
    }
}
