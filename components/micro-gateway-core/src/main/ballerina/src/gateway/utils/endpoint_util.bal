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

import ballerina/http;

http:PoolConfiguration sharedPoolConfig = {
    maxActiveConnections: getConfigIntValue(HTTP_CLIENTS_POOL_CONFIG_INSTANCE_ID, HTTP_CLIENTS_MAX_ACTIVE_CONNECTIONS, DEFAULT_HTTP_CLIENTS_MAX_ACTIVE_CONNECTIONS),
    maxIdleConnections: getConfigIntValue(HTTP_CLIENTS_POOL_CONFIG_INSTANCE_ID, HTTP_CLIENTS_MAX_IDLE_CONNECTIONS, DEFAULT_HTTP_CLIENTS_MAX_IDLE_CONNECTIONS),
    waitTimeInMillis: getConfigIntValue(HTTP_CLIENTS_POOL_CONFIG_INSTANCE_ID, HTTP_CLIENTS_WAIT_TIME, DEFAULT_HTTP_CLIENTS_WAIT_TIME),
    maxActiveStreamsPerConnection: getConfigIntValue(HTTP_CLIENTS_POOL_CONFIG_INSTANCE_ID, HTTP_CLIENTS_MAX_ACTIVE_STREAMS, DEFAULT_HTTP_CLIENTS_MAX_ACTIVE_STREAMS)
};

http:ProxyConfig proxyConfig = {
    host: getConfigValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, HTTP_CLIENTS_PROXY_HOST, ""),
    port: getConfigIntValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, HTTP_CLIENTS_PROXY_PORT, 0),
    userName: getConfigValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, HTTP_CLIENTS_PROXY_USERNAME, ""),
    password: getConfigValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, HTTP_CLIENTS_PROXY_PASSWORD, "")
};

boolean proxyEnable = getConfigBooleanValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, HTTP_CLIENTS_PROXY_ENABLE, false);
boolean proxyEnableInternalServices = getConfigBooleanValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, HTTP_CLIENTS_PROXY_ENABLE_INTERNAL_SERVICES, false);

public function getClientPoolConfig(boolean isSharedPool, int maxActiveConnections, int maxIdleConnections,
int waitTimeInMillis, int maxActiveStreamsPerConnection) returns http:PoolConfiguration? {
    if (isSharedPool) {
        printDebug(KEY_UTILS, "Shared endpoint pool config : " + sharedPoolConfig.toString());
        return sharedPoolConfig;
    }
    http:PoolConfiguration perClientPoolConfig = {
        maxActiveConnections: (maxActiveConnections != 0) ? maxActiveConnections :
        DEFAULT_HTTP_CLIENTS_MAX_ACTIVE_CONNECTIONS,
        maxIdleConnections: (maxIdleConnections != -1) ? maxIdleConnections :
        DEFAULT_HTTP_CLIENTS_MAX_IDLE_CONNECTIONS,
        waitTimeInMillis: (waitTimeInMillis != -1) ? waitTimeInMillis : DEFAULT_HTTP_CLIENTS_WAIT_TIME,
        maxActiveStreamsPerConnection: (maxActiveStreamsPerConnection != -1) ? maxActiveStreamsPerConnection :
        DEFAULT_HTTP_CLIENTS_MAX_ACTIVE_STREAMS
    };
    printDebug(KEY_UTILS, "Per client endpoint pool config : " + perClientPoolConfig.toString());
    return perClientPoolConfig;
}

public function getClientProxyConfig() returns http:ProxyConfig? {
    if (proxyEnable) {
        printDebug(KEY_UTILS, "Client proxy config enabled. Proxy config : " + proxyConfig.toString());
        return proxyConfig;
    } else {
        printDebug(KEY_UTILS, "Client proxy config disabled");
        return ();
    }
}

public function getClientProxyForInternalServices() returns http:ProxyConfig? {
    if (proxyEnableInternalServices) {
        printDebug(KEY_UTILS, "Client proxy config for internal services communication enabled. Proxy config : " +
            proxyConfig.toString());
        return proxyConfig;
    } else {
        printDebug(KEY_UTILS, "Client proxy config internal services communication disabled");
        return ();
    }
}

public function getClientsHttpVersion() returns string {
    boolean val = getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, HTTP_CLIENTS_ENABLE_HTTP2, true);
    if (val) {
        return HTTP2;
    } else {
        return HTTP11;
    }

}
