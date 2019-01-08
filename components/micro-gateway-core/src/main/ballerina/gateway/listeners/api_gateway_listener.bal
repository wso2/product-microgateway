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


@Description {value:"Representation of an API gateway listener"}
@Field {value:"config: EndpointConfiguration instance"}
@Field {value:"httpListener: HTTP Listener instance"}
public type APIGatewayListener object {
    public EndpointConfiguration config;
    public http:Listener httpListener;


    new () {
        httpListener = new;
    }

    public function init(EndpointConfiguration endpointConfig);

    @Description {value:"Gets called when the endpoint is being initialize during package init time"}
    @Return {value:"Error occured during initialization"}
    public function initEndpoint() returns (error);

    @Description {value:"Gets called every time a service attaches itself to this endpoint. Also happens at package initialization."}
    @Param {value:"ep: The endpoint to which the service should be registered to"}
    @Param {value:"serviceType: The type of the service to be registered"}
    public function register(typedesc serviceType);

    @Description {value:"Starts the registered service"}
    public function start();

    @Description {value:"Returns the connector that client code uses"}
    @Return {value:"The connector that client code uses"}
    public function getCallerActions() returns (http:Connection);

    @Description {value:"Stops the registered service"}
    public function stop();
};

@Description {value:"Configuration for secure HTTP service endpoint"}
@Field {value:"host: Host of the service"}
@Field {value:"port: Port number of the service"}
@Field {value:"keepAlive: The keepAlive behaviour of the connection for a particular port"}
@Field {value:"transferEncoding: The types of encoding applied to the response"}
@Field {value:"chunking: The chunking behaviour of the response"}
@Field {value:"secureSocket: The SSL configurations for the service endpoint"}
@Field {value:"httpVersion: Highest HTTP version supported"}
@Field {value:"requestLimits: Request validation limits configuration"}
@Field {value:"filters: Filters to be applied to the request before dispatched to the actual resource"}
@Field {value:"authProviders: The array of AuthProviders which are used to authenticate the users"}
public type EndpointConfiguration record {
    string host,
    int port =9090,
    http:KeepAlive keepAlive = "AUTO",
    http:ServiceSecureSocket? secureSocket,
    string httpVersion = "1.1",
    http:RequestLimits? requestLimits,
    http:Filter[] filters,
    int timeoutMillis = DEFAULT_LISTENER_TIMEOUT,
    http:AuthProvider[]? authProviders,
    boolean isSecured,
};


function APIGatewayListener::init (EndpointConfiguration endpointConfig) {
    initiateGatewayConfigurations(endpointConfig);
    printDebug(KEY_GW_LISTNER, "Initialized gateway configurations for port:" + endpointConfig.port);
    self.httpListener.init(endpointConfig);
    printDebug(KEY_GW_LISTNER, "Successfully initialized APIGatewayListener for port:" + endpointConfig.port);
}

public function createAuthHandler (http:AuthProvider authProvider) returns http:HttpAuthnHandler {
    if (authProvider.scheme == AUTHN_SCHEME_BASIC) {
        auth:AuthStoreProvider authStoreProvider;
        if (authProvider.authStoreProvider == AUTH_PROVIDER_CONFIG) {
            auth:ConfigAuthStoreProvider configAuthStoreProvider = new;
            authStoreProvider = <auth:AuthStoreProvider>configAuthStoreProvider;
        } else {
        // other auth providers are unsupported yet
            error e = {message: "Invalid auth provider: " + authProvider.authStoreProvider };
            throw e;
        }
        http:HttpBasicAuthnHandler basicAuthHandler = new(authStoreProvider);
        return <http:HttpAuthnHandler>basicAuthHandler;
    } else if(authProvider.scheme == AUTH_SCHEME_JWT){
        auth:JWTAuthProviderConfig jwtConfig = {};
        jwtConfig.issuer = authProvider.issuer;
        jwtConfig.audience = authProvider.audience;
        jwtConfig.certificateAlias = authProvider.certificateAlias;
        jwtConfig.trustStoreFilePath = authProvider.trustStore.path but {() => ""};
        jwtConfig.trustStorePassword = authProvider.trustStore.password but {() => ""};
        auth:JWTAuthProvider jwtAuthProvider = new (jwtConfig);
        http:HttpJwtAuthnHandler jwtAuthnHandler = new(jwtAuthProvider);
        return <http:HttpAuthnHandler> jwtAuthnHandler;
    }  else {
        // TODO: create other HttpAuthnHandlers
        error e = {message:"Invalid auth scheme: " + authProvider.scheme };
        throw e;
    }
}

function initiateGatewayConfigurations(EndpointConfiguration config) {
    if(!config.isSecured) {
        config.port = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTP_PORT, 9090);
    }
    // default should bind to 0.0.0.0, not localhost. Else will not work in dockerized environments.
    config.host = getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HOST, "0.0.0.0");
    intitateKeyManagerConfigurations();
    printDebug(KEY_GW_LISTNER, "Initialized key manager configurations");
    initGatewayCaches();
    printDebug(KEY_GW_LISTNER, "Initialized gateway caches");
    initializeAnalytics();

    //Change the version of http2
    if(getConfigBooleanValue(HTTP2_INSTANCE_ID, HTTP2_PROPERTY, false)) {
        config.httpVersion = "2.0";
        io:println("httpVersion = " + config.httpVersion);
    }
}

public function getAuthProviders() returns http:AuthProvider[] {
    http:AuthProvider jwtAuthProvider = {
        id: AUTH_SCHEME_JWT,
        scheme: AUTH_SCHEME_JWT,
        issuer: getConfigValue(JWT_INSTANCE_ID, ISSUER, "https://localhost:9443/oauth2/token"),
        audience: getConfigValue(JWT_INSTANCE_ID, AUDIENCE, "RQIO7ti2OThP79wh3fE5_Zksszga"),
        certificateAlias: getConfigValue(JWT_INSTANCE_ID, CERTIFICATE_ALIAS, "ballerina"),
        trustStore: {
        path: getConfigValue(JWT_INSTANCE_ID, TRUST_STORE_PATH, "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
        password: getConfigValue(JWT_INSTANCE_ID, TRSUT_STORE_PASSWORD, "ballerina")
        }
    };
    http:AuthProvider basicAuthProvider = {
        id : AUTHN_SCHEME_BASIC,
        scheme: AUTHN_SCHEME_BASIC,
        authStoreProvider: AUTH_PROVIDER_CONFIG
    };
    return [jwtAuthProvider, basicAuthProvider];
}

public function getBasicAuthProvider() returns http:AuthProvider[] {
    http:AuthProvider basicAuthProvider = {
        id: AUTHN_SCHEME_BASIC,
        scheme: AUTHN_SCHEME_BASIC,
        authStoreProvider: AUTH_PROVIDER_CONFIG
    };
    return [basicAuthProvider];
}

public function getJWTAuthProvider() returns http:AuthProvider[] {
    http:AuthProvider jwtAuthProvider = {
        id: AUTH_SCHEME_JWT,
        scheme: AUTH_SCHEME_JWT,
        issuer: getConfigValue(JWT_INSTANCE_ID, ISSUER, "https://localhost:9443/oauth2/token"),
        audience: getConfigValue(JWT_INSTANCE_ID, AUDIENCE, "RQIO7ti2OThP79wh3fE5_Zksszga"),
        certificateAlias: getConfigValue(JWT_INSTANCE_ID, CERTIFICATE_ALIAS, "ballerina"),
        trustStore: {
            path: getConfigValue(JWT_INSTANCE_ID, TRUST_STORE_PATH,
                "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
            password: getConfigValue(JWT_INSTANCE_ID, TRSUT_STORE_PASSWORD, "ballerina")
        }
    };
    return [jwtAuthProvider];
}

public function getDefaultAuthorizationFilter() returns OAuthzFilter {
    cache:Cache authzCache = new(expiryTimeMillis = getConfigIntValue(CACHING_ID, TOKEN_CACHE_EXPIRY, 900000),
        capacity = getConfigIntValue(CACHING_ID, TOKEN_CACHE_CAPACITY, 100), evictionFactor = getConfigFloatValue(
        CACHING_ID, TOKEN_CACHE_EVICTION_FACTOR, 0.25));

    auth:ConfigAuthStoreProvider configAuthStoreProvider = new;
    auth:AuthStoreProvider authStoreProvider = <auth:AuthStoreProvider>configAuthStoreProvider;
    http:HttpAuthzHandler authzHandler = new(authStoreProvider, authzCache);
    http:AuthzFilter authzFilter = new(authzHandler);
    OAuthzFilter authzFilterWrapper = new(authzFilter);
    return authzFilterWrapper;
}

function intitateKeyManagerConfigurations() {
    KeyManagerConf keyManagerConf;
    Credentials credentials;
    keyManagerConf.serverUrl = getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, "https://localhost:9443");
    credentials.username = getConfigValue(KM_CONF_INSTANCE_ID, "username", "admin");
    credentials.password = getConfigValue(KM_CONF_INSTANCE_ID, "password", "admin");
    keyManagerConf.credentials = credentials;
    getGatewayConfInstance().setKeyManagerConf(keyManagerConf);
}


@Description {value:"Gets called every time a service attaches itself to this endpoint. Also happens at package initialization."}
@Param {value:"ep: The endpoint to which the service should be registered to"}
@Param {value:"serviceType: The type of the service to be registered"}
function APIGatewayListener::register (typedesc serviceType) {
    self.httpListener.register(serviceType);
}

@Description {value:"Gets called when the endpoint is being initialize during package init time"}
@Return {value:"Error occured during initialization"}
function APIGatewayListener::initEndpoint() returns (error) {
    return self.httpListener.initEndpoint();
}

@Description {value:"Starts the registered service"}
function APIGatewayListener::start () {
    self.httpListener.start();
}

@Description {value:"Returns the connector that client code uses"}
@Return {value:"The connector that client code uses"}
function APIGatewayListener::getCallerActions () returns (http:Connection) {
    return self.httpListener.getCallerActions();
}

@Description {value:"Stops the registered service"}
function APIGatewayListener::stop () {
    self.httpListener.stop();
}







