// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


public http:Client keyValidationEndpoint = new (
    getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, "https://localhost:9443"),
    config =
    {cache: { enabled: false },
        secureSocket:{
            trustStore: {
                  path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                      "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                  password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
            },
            verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
        }
    }
);

http:Client analyticsFileUploadEndpoint = new (
    getConfigValue(ANALYTICS, UPLOADING_EP, "https://localhost:9444/analytics/v1.0/usage/upload-file"),
    config =
    {cache: { enabled: false },
      secureSocket:{
          trustStore: {
              path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                  "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
              password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
          },
          verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
      }
    }
);

public function reInitializeClientsWithProxies() {
    boolean proxyEnabled = getConfigBooleanValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, PROXY_ENABLED, false);
    if (proxyEnabled) {
        http:ProxyConfig proxyConfig = {
            host: getConfigValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, PROXY_HOST, ""),
            port: getConfigIntValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, PROXY_PORT, 0),
            userName: getConfigValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, PROXY_USERNAME, ""),
            password: getConfigValue(HTTP_CLIENTS_PROXY_INSTANCE_ID, PROXY_PASSWORD, "")
        };
        reInitializeClientsWithProxyConfig(proxyConfig);
        printDebug(GW_CLIENTS, "Re initializing gateway clients with proxy configurations");
        keyValidationEndpoint = new (
            getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, "https://localhost:9443"),
            config =
                {cache: { enabled: false },
                secureSocket: {
                    trustStore: {
                          path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                              "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                          password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
                    },
                    verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
                },
                proxy: proxyConfig
            }
        );
        analyticsFileUploadEndpoint = new (
            getConfigValue(ANALYTICS, UPLOADING_EP, "https://localhost:9444/analytics/v1.0/usage/upload-file"),
            config =
                {cache: { enabled: false },
                secureSocket:{
                    trustStore: {
                        path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                          "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                        password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
                    },
                    verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
                },
                proxy: proxyConfig
            }
        );
        etcdEndpoint = new (
            retrieveConfig("etcdurl", "http://127.0.0.1:2379"), config = {
                secureSocket: {
                    trustStore: {
                        path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                            "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                        password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
                    },
                    verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
                },
                proxy: proxyConfig
            }
        );
        etcdTokenRevocationEndpoint = new (
            getConfigValue(PERSISTENT_MESSAGE_INSTANCE_ID, PERSISTENT_MESSAGE_HOSTNAME, "https://localhost:2379/v2/keys/jti/"), config = {
                secureSocket: {
                    trustStore: {
                        path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                            "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                        password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
                    },
                    verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
                },
                proxy: proxyConfig
            }
        );
        throttleEndpoint = new(throttleEndpointUrl, config = {
            cache: { enabled: false },
            secureSocket:{
                trustStore: {
                      path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                          "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                      password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
                },
                verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
            },
            proxy: proxyConfig
        });
    }
}
