// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

map<Client> clientMap = {};
map<FailoverClient> failOverClientMap = {};
map<LoadBalanceClient> loadBalanceClientMap = {};

public function addClientToMap(string clientName, Client gatewayClient) {
    clientMap[clientName] = gatewayClient;
}

public function addFailoverClientToMap(string clientName, FailoverClient failoverClient) {
    failOverClientMap[clientName] = failoverClient;
}

public function addLoadBalanceClientToMap(string clientName, LoadBalanceClient loadBlanceClient) {
    loadBalanceClientMap[clientName] = loadBlanceClient;
}

public function reInitializeClientsWithProxyConfig(http:ProxyConfig proxyConfig) {
    printDebug(GW_CLIENTS, "Re initializing gateway clients of APIs with proxy configurations");
    foreach var (key, gatewayClient) in clientMap {
        http:ClientEndpointConfig clientEPConfig = gatewayClient.config;
        clientEPConfig.proxy = proxyConfig;
        string url = gatewayClient.url;
        gatewayClient.httpClient = new (url, config = clientEPConfig);
        printDebug(GW_CLIENTS, "Re initialized gateway client : " + key);
    }

    foreach var (key, failoverClient) in failOverClientMap {
        http:FailoverClientEndpointConfiguration failoverClientEPConfig = failoverClient.config;
        failoverClientEPConfig.proxy = proxyConfig;
        failoverClient.httpClient = new (failoverClientEPConfig);
        printDebug(GW_CLIENTS, "Re initialized failover gateway client : " + key);
    }

    foreach var (key, loadBalanceClient) in loadBalanceClientMap {
        http:LoadBalanceClientEndpointConfiguration loadBalanceClientEPConfig = loadBalanceClient.config;
        loadBalanceClientEPConfig.proxy = proxyConfig;
        loadBalanceClient.httpClient = new (loadBalanceClientEPConfig);
        printDebug(GW_CLIENTS, "Re initialized load balanced gateway client : " + key);
    }
}
