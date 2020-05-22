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

package org.wso2.micro.gateway.core.globalthrottle.databridge.agent.util;

/**
 * Class to define the constants for Data Agent.
 */
public class DataAgentConstants {

    private DataAgentConstants() {
    }

    public static final String TRUST_STORE_PATH = "trustStorePath";
    public static final String TRUST_STORE_PASSWORD = "trustStorePassword";
    public static final String QUEUE_SIZE = "queueSize";
    public static final String BATCH_SIZE = "batchSize";
    public static final String CORE_POOL_SIZE = "corePoolSize";
    public static final String SOCKET_TIMEOUT_MS = "socketTimeoutMS";
    public static final String MAX_POOL_SIZE = "maxPoolSize";
    public static final String KEEP_ALIVE_TIME_INTERVAL_IN_POOL = "keepAliveTimeInPool";
    public static final String RECONNECTION_INTERVAL = "reconnectionInterval";
    public static final String MAX_TRANSPORT_POOL_SIZE = "maxTransportPoolSize";
    public static final String MAX_IDLE_CONNECTIONS = "maxIdleConnections";
    public static final String EVICTION_TIME_PERIOD = "evictionTimePeriod";
    public static final String MIN_IDLE_TIME_IN_POOL = "minIdleTimeInPool";
    public static final String SECURE_MAX_TRANSPORT_POOL_SIZE = "secureMaxTransportPoolSize";
    public static final String SECURE_MAX_IDLE_CONNECTIONS = "secureMaxIdleConnections";
    public static final String SECURE_EVICTION_TIME_PERIOD = "secureEvictionTimePeriod";
    public static final String SECURE_MIN_IDLE_TIME_IN_POOL = "secureMinIdleTimeInPool";
    public static final String SSL_ENABLED_PROTOCOLS = "sslEnabledProtocols";
    public static final String CIPHERS = "ciphers";
}
