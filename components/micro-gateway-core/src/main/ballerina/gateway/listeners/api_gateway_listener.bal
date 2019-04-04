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

public type APIGatewayListener object {
    *AbstractListener;

    public http:Listener httpListener;


    public function __init(http:ServiceEndpointConfiguration config) {
        int port = 9090;
        if((config.secureSocket is ())){
            port = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTP_PORT, 9090);
        } else {
            port = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTPS_PORT, 9095);
        }

        initiateGatewayConfigurations(config);
        printDebug(KEY_GW_LISTNER, "Initialized gateway configurations for port:" + port);
        self.httpListener = new(port, config = config);
        printDebug(KEY_GW_LISTNER, "Successfully initialized APIGatewayListener for port:" + port);
    }


    public function __start() returns error? {
        return self.httpListener.__start();
    }

    public function __stop() returns error? {
        return self.httpListener.__stop();
    }

    public function __attach(service s, map<any> annotationData) returns error? {
        return self.httpListener.__attach(s, annotationData);
    }



};

public function createAuthHandler(http:AuthProvider authProvider) returns http:HttpAuthnHandler {
    if (authProvider.scheme == AUTHN_SCHEME_BASIC) {
        auth:AuthStoreProvider authStoreProvider;
        if (authProvider.authStoreProvider == AUTH_PROVIDER_CONFIG) {
            auth:ConfigAuthStoreProvider configAuthStoreProvider = new;
            authStoreProvider = configAuthStoreProvider;
        } else {
            // other auth providers are unsupported yet
            string errMessage = "Invalid auth provider: " + <string>authProvider.authStoreProvider;
            error e = error("Invalid auth provider: " + authProvider.authStoreProvider);
            panic e;
        }
        http:HttpBasicAuthnHandler basicAuthHandler = new(authStoreProvider);
        return basicAuthHandler;
    } else if (authProvider.scheme == AUTH_SCHEME_JWT) {
        auth:JWTAuthProviderConfig jwtConfig = {};
        jwtConfig.issuer = authProvider.issuer;
        jwtConfig.audience = authProvider.audience;
        jwtConfig.certificateAlias = authProvider.certificateAlias;
        jwtConfig.trustStoreFilePath = authProvider.trustStore.path  ?: "";
        jwtConfig.trustStorePassword = authProvider.trustStore.password ?: "";
        auth:JWTAuthProvider jwtAuthProvider = new(jwtConfig);
        http:HttpJwtAuthnHandler jwtAuthnHandler = new(jwtAuthProvider);
        return jwtAuthnHandler;
    } else {
        // TODO: create other HttpAuthnHandlers
        error e = error( "Invalid auth scheme: " + authProvider.scheme);
        panic e;
    }
}

public function initiateGatewayConfigurations(http:ServiceEndpointConfiguration config) {
    // default should bind to 0.0.0.0, not localhost. Else will not work in dockerized environments.
    config.host = getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HOST, "0.0.0.0");
    initiateKeyManagerConfigurations();
    printDebug(KEY_GW_LISTNER, "Initialized key manager configurations");
    initGatewayCaches();
    printDebug(KEY_GW_LISTNER, "Initialized gateway caches");
    initializeAnalytics();

    //Change the httpVersion
    if (getConfigBooleanValue(HTTP2_INSTANCE_ID, HTTP2_PROPERTY, false)) {
        config.httpVersion = "2.0";
        log:printDebug("httpVersion = " + config.httpVersion);
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
            path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
            password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRSUT_STORE_PASSWORD, "ballerina")
        }
    };
    http:AuthProvider basicAuthProvider = {
        id: AUTHN_SCHEME_BASIC,
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
            path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
            password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRSUT_STORE_PASSWORD, "ballerina")
        }
    };
    return [jwtAuthProvider];
}

public function getDefaultAuthorizationFilter() returns OAuthzFilter {
    int cacheExpiryTime = getConfigIntValue(CACHING_ID, TOKEN_CACHE_EXPIRY, 900000);
    int cacheSize = getConfigIntValue(CACHING_ID, TOKEN_CACHE_CAPACITY, 100);
    float evictionFactor = getConfigFloatValue(CACHING_ID, TOKEN_CACHE_EVICTION_FACTOR, 0.25);
    cache:Cache positiveAuthzCache = new(expiryTimeMillis = cacheExpiryTime,
        capacity = cacheSize, evictionFactor = evictionFactor);
    cache:Cache negativeAuthzCache = new(expiryTimeMillis = cacheExpiryTime,
        capacity = cacheSize, evictionFactor = evictionFactor);

    auth:ConfigAuthStoreProvider configAuthStoreProvider = new;
    auth:AuthStoreProvider authStoreProvider = configAuthStoreProvider;
    http:HttpAuthzHandler authzHandler = new(authStoreProvider, positiveAuthzCache, negativeAuthzCache);
    http:AuthzFilter authzFilter = new(authzHandler);
    OAuthzFilter authzFilterWrapper = new(authzFilter);
    return authzFilterWrapper;
}

function initiateKeyManagerConfigurations() {
    KeyManagerConf keyManagerConf = {};
    Credentials credentials = {};
    keyManagerConf.serverUrl = getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, "https://localhost:9443");
    credentials.username = getConfigValue(KM_CONF_INSTANCE_ID, USERNAME, "admin");
    credentials.password = getConfigValue(KM_CONF_INSTANCE_ID, PASSWORD, "admin");
    keyManagerConf.credentials = credentials;
    getGatewayConfInstance().setKeyManagerConf(keyManagerConf);
}
