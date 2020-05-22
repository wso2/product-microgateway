// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

public type TMBinaryPublisherConfigDto record {
    string receiverURLGroup = "";
    string authURLGroup = "";
    string username = getConfigValue(BINARY_PUBLISHER_THROTTLE_CONF_INSTANCE_ID, TM_USERNAME, DEFAULT_TM_USERNAME);
    string password = getConfigValue(BINARY_PUBLISHER_THROTTLE_CONF_INSTANCE_ID, TM_PASSWORD, DEFAULT_TM_PASSWORD);
    int maxIdle = getConfigIntValue(BINARY_PUBLISHER_POOL_THROTTLE_CONF_INSTANCE_ID,
        TM_PUBLISHER_POOL_MAX_IDLE, DEFAULT_TM_PUBLISHER_POOL_MAX_IDLE);
    int initIdleCapacity = getConfigIntValue(BINARY_PUBLISHER_POOL_THROTTLE_CONF_INSTANCE_ID,
        TM_PUBLISHER_POOL_INIT_IDLE_CAPACITY, DEFAULT_TM_PUBLISHER_POOL_INIT_IDLE_CAPACITY);
    int corePoolSize = getConfigIntValue(BINARY_PUBLISHER_THREAD_POOL_THROTTLE_CONF_INSTANCE_ID,
        TM_PUBLISHER_THREAD_POOL_CORE_SIZE, DEFAULT_TM_PUBLISHER_THREAD_POOL_CORE_SIZE);
    int maxPoolSize = getConfigIntValue(BINARY_PUBLISHER_THREAD_POOL_THROTTLE_CONF_INSTANCE_ID,
        TM_PUBLISHER_THREAD_POOL_MAXIMUM_SIZE, DEFAULT_TM_PUBLISHER_THREAD_POOL_MAXIMUM_SIZE);
    int keepAliveTime = getConfigIntValue(BINARY_PUBLISHER_THREAD_POOL_THROTTLE_CONF_INSTANCE_ID,
        TM_PUBLISHER_THREAD_POOL_KEEP_ALIVE_TIME, DEFAULT_TM_PUBLISHER_THREAD_POOL_KEEP_ALIVE_TIME);
};

public type TMBinaryAgentConfigDto record {
    int queueSize = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID, TM_AGENT_QUEUE_SIZE,
        DEFAULT_TM_AGENT_QUEUE_SIZE);
    int batchSize = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID, TM_AGENT_BATCH_SIZE,
        DEFAULT_TM_AGENT_BATCH_SIZE);
    int corePoolSize = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID, TM_AGENT_THREAD_POOL_CORE_SIZE,
        DEFAULT_TM_AGENT_THREAD_POOL_CORE_SIZE);
    int socketTimeoutMS = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,  TM_AGENT_SOCKET_TIMEOUT_MS,
        DEFAULT_TM_AGENT_SOCKET_TIMEOUT_MS);
    int maxPoolSize = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID, TM_AGENT_THREAD_POOL_MAXIMUM_SIZE,
        DEFAULT_TM_AGENT_THREAD_POOL_MAXIMUM_SIZE);
    int keepAliveTimeInPool = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_THREAD_POOL_KEEP_ALIVE_TIME, DEFAULT_TM_AGENT_THREAD_POOL_KEEP_ALIVE_TIME);
    int reconnectionInterval = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_RECONNECTION_INTERVAL, DEFAULT_TM_AGENT_RECONNECTION_INTERVAL);
    int maxTransportPoolSize = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_MAX_TRANSPORT_POOL_SIZE, DEFAULT_TM_AGENT_MAX_TRANSPORT_POOL_SIZE);
    int maxIdleConnections = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_EVICTION_TIME_PERIOD, DEFAULT_TM_AGENT_MAX_IDLE_CONNECTIONS);
    int evictionTimePeriod = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_EVICTION_TIME_PERIOD, DEFAULT_TM_AGENT_EVICTION_TIME_PERIOD);
    int minIdleTimeInPool = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_MIN_IDLE_TIME_IN_POOL, DEFAULT_TM_AGENT_MIN_IDLE_TIME_IN_POOL);
    int secureMaxTransportPoolSize = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_SECURE_MAX_TRANSPORT_POOL_SIZE, DEFAULT_TM_AGENT_SECURE_MAX_TRANSPORT_POOL_SIZE);
    int secureMaxIdleConnections = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_SECURE_MAX_IDLE_CONNECTIONS, DEFAULT_TM_AGENT_SECURE_MAX_IDLE_CONNECTIONS);
    int secureEvictionTimePeriod = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_SECURE_EVICTION_TIME_PERIOD, DEFAULT_TM_AGENT_SECURE_EVICTION_TIME_PERIOD);
    int secureMinIdleTimeInPool = getConfigIntValue(BINARY_AGENT_THROTTLE_CONF_INSTANCE_ID,
        TM_AGENT_SECURE_MIN_IDLE_TIME_IN_POOL, DEFAULT_TM_AGENT_SECURE_MIN_IDLE_TIME_IN_POOL);
    //the placeholder replacement is handled via the java implementation
    string trustStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH);
    string trustStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD,
        DEFAULT_TRUST_STORE_PASSWORD);
    string sslEnabledProtocols = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_PROTOCOL_VERSIONS,
        DEFAULT_PROTOCOL_VERSIONS);
    string ciphers = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_CIPHERS, DEFAULT_CIPHERS);
};
