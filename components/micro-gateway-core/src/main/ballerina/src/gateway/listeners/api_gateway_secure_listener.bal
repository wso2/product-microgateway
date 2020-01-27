// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/crypto;
import ballerina/http;
import ballerina/ 'lang\.object as lang;

public type APIGatewaySecureListener object {
    *lang:Listener;
    APIGatewayListener apiGatewayListener;

    public function __init(int port, http:ListenerConfiguration config) {
        initiateGatewaySecureConfigurations(config);
        self.apiGatewayListener = new (getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTPS_PORT, port), config);
    }


    public function __start() returns error? {
        return self.apiGatewayListener.__start();
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.apiGatewayListener.__attach(s, name);
    }

    public function __gracefulStop() returns error? {
        return self.apiGatewayListener.__gracefulStop();
    }

    public function __immediateStop() returns error? {
        return self.apiGatewayListener.__immediateStop();
    }

    public function __detach(service s) returns error? {
        return self.apiGatewayListener.__detach(s);
    }

};

function initiateGatewaySecureConfigurations(http:ListenerConfiguration config) {
    string keyStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PATH,
    "${mgw-runtime.home}/runtime/bre/security/ballerinaKeystore.p12");
    string keyStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID,
    LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina");
    string trustStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID,
    TRUST_STORE_PATH, "${mgw-runtime.home}/runtime/bre/security/ballerinaTruststore.p12");
    string trustStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID,
    TRUST_STORE_PASSWORD, "ballerina");
    string protocolName = getConfigValue(MTSL_CONF_INSTANCE_ID,
    MTSL_CONF_PROTOCOL_NAME, "TLS");
    string defaultProtocolVersions = "TLSv1.2,TLSv1.1";
    string defaultCiphers = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256," +
    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256," +
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA," +
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDH_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA," +
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
    "TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256," +
    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256," +
    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA," +
    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA, TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA," +
    "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA, TLS_EMPTY_RENEGOTIATION_INFO_SCSV";
    string[] protocolVersions = split(getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_PROTOCOL_VERSIONS,
    defaultProtocolVersions), ",");
    string[] ciphers = split(getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_CIPHERS, defaultCiphers), ",");
    string sslVerifyClient = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, "");

    crypto:TrustStore trustStore = {path: trustStorePath, password: trustStorePassword};
    crypto:KeyStore keyStore = {path: keyStorePath, password: keyStorePassword};
    http:Protocols protocol = {name: protocolName, versions: protocolVersions};
    http:ListenerSecureSocket secureSocket = {
        trustStore: trustStore,
        keyStore: keyStore,
        sslVerifyClient: sslVerifyClient,
        ciphers: ciphers
    };
    config.secureSocket = secureSocket;
}
