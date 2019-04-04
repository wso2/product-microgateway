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
import ballerina/log;
import ballerina/config;

public type APIGatewaySecureListener object {
    *AbstractListener;
    APIGatewayListener apiGatewayListener;

    public function __init(http:ServiceEndpointConfiguration config) {
        initiateGatewaySecureConfigurations(config);
        self.apiGatewayListener = new(config);
    }


    public function __start() returns error? {
        return self.apiGatewayListener.__start();
    }

    public function __stop() returns error? {
        return self.apiGatewayListener.__stop();
    }

    public function __attach(service s, map<any> annotationData) returns error? {
        return self.apiGatewayListener.__attach(s, annotationData);
    }

};


function initiateGatewaySecureConfigurations(http:ServiceEndpointConfiguration config) {
    string keyStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PATH,
        "${ballerina.home}/bre/security/ballerinaKeystore.p12");
    string keyStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID,
        LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina");
    string trustStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID,
        TRUST_STORE_PATH, "${ballerina.home}/bre/security/ballerinaTruststore.p12");
    string trustStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID,
        TRSUT_STORE_PASSWORD, "ballerina");
    string protocolName = getConfigValue(MTSL_CONF_INSTANCE_ID,
        MTSL_CONF_PROTOCOL_NAME, "TLS");
    string defaultProtocolVersions = "TLSv1.2,TLSv1.1";
    string defaultCiphers = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
    TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256
    ,
    TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
    ,
    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA,
    TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDH_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
    , TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,
    TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256,
    TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256
    , TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA,
    TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA, TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,
    SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA, TLS_EMPTY_RENEGOTIATION_INFO_SCSV";

    string[] protocolVersions = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_PROTOCOL_VERSIONS,
    defaultProtocolVersions).split(",");
    string[] ciphers = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_CIPHERS, defaultCiphers).split(",");
    string sslVerifyClient = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, "");

    http:TrustStore trustStore = { path: trustStorePath, password: trustStorePassword };
    http:KeyStore keyStore = { path: keyStorePath, password: keyStorePassword };
    http:Protocols protocol = { name: protocolName, versions: protocolVersions };
    http:ServiceSecureSocket secureSocket = { trustStore: trustStore, keyStore: keyStore,
        sslVerifyClient: sslVerifyClient, ciphers: ciphers };
    config.secureSocket = secureSocket;
}
