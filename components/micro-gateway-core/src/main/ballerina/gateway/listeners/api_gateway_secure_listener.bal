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
import ballerina/auth;
import ballerina/cache;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;
import ballerina/io;

@Description { value: "Representation of an API gateway secure listener" }
@Field { value: "apiGatewayListener: API Gateway listener instance" }
public type APIGatewaySecureListener object {
    public APIGatewayListener apiGatewayListener;

    new() {
        apiGatewayListener = new;
    }

    public function init(EndpointConfiguration config);

    @Description { value: "Gets called when the endpoint is being initialize during package init time" }
    @Return { value: "Error occured during initialization" }
    public function initEndpoint() returns (error);

    @Description { value:
    "Gets called every time a service attaches itself to this endpoint. Also happens at package initialization." }
    @Param { value: "serviceType: The type of the service to be registered" }
    public function register(typedesc serviceType);

    @Description { value: "Starts the registered service" }
    public function start();

    @Description { value: "Returns the connector that client code uses" }
    @Return { value: "The connector that client code uses" }
    public function getCallerActions() returns (http:Connection);

    @Description { value: "Stops the registered service" }
    public function stop();
};


function APIGatewaySecureListener::init(EndpointConfiguration config) {
    initiateGatewaySecureConfigurations(config);
    self.apiGatewayListener.init(config);

}

@Description { value: "Gets called when the endpoint is being initialize during package init time" }
@Return { value: "Error occured during initialization" }
function APIGatewaySecureListener::initEndpoint() returns (error) {
    return self.apiGatewayListener.initEndpoint();
}

@Description { value:
"Gets called every time a service attaches itself to this endpoint. Also happens at package initialization." }
@Param { value: "ep: The endpoint to which the service should be registered to" }
@Param { value: "serviceType: The type of the service to be registered" }
function APIGatewaySecureListener::register(typedesc serviceType) {
    self.apiGatewayListener.register(serviceType);
}

@Description { value: "Starts the registered service" }
function APIGatewaySecureListener::start() {
    self.apiGatewayListener.start();
}

@Description { value: "Returns the connector that client code uses" }
@Return { value: "The connector that client code uses" }
function APIGatewaySecureListener::getCallerActions() returns (http:Connection) {
    return self.apiGatewayListener.getCallerActions();
}

@Description { value: "Stops the registered service" }
function APIGatewaySecureListener::stop() {
    self.apiGatewayListener.stop();
}

function initiateGatewaySecureConfigurations(EndpointConfiguration config) {
    config.port = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTPS_PORT, 9095);
    string keyStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PATH,
        "${ballerina.home}/bre/security/ballerinaKeystore.p12");
    string keyStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID,
        LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina");
    string trustStorePath = getConfigValue(JWT_INSTANCE_ID,
        TRUST_STORE_PATH, "${ballerina.home}/bre/security/ballerinaTruststore.p12");
    string trustStorePassword = getConfigValue(JWT_CONFIG_INSTANCE_ID,
        TRSUT_STORE_PASSWORD, "ballerina");
    string protocolName = getConfigValue(MTSL_CONF_INSTANCE_ID,
        MTSL_CONF_PROTOCOL_NAME, "TLS");
    string[] protocolVersions = ["TLSv1.2", "TLSv1.1"];
    string[] ciphers = ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256"
    ,
    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"
    ,
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", " TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA", " TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
    , "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", " TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256"
    , "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA", " TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
    "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", " TLS_EMPTY_RENEGOTIATION_INFO_SCSV"];

    string sslVerifyClient = getConfigValue(MTSL_CONF_INSTANCE_ID,
        MTSL_CONF_SSLVERIFYCLIENT, "");


    http:TrustStore trustStore = { path: trustStorePath, password: trustStorePassword };
    http:KeyStore keyStore = { path: keyStorePath, password: keyStorePassword };
    http:Protocols protocol = { name: protocolName, versions: protocolVersions };
    http:ServiceSecureSocket secureSocket = { trustStore: trustStore, keyStore: keyStore,
        sslVerifyClient: sslVerifyClient };
    config.secureSocket = secureSocket;
    config.isSecured = true;

}
