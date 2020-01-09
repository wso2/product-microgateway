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

import ballerina/auth;
import ballerina/cache;
import ballerina/http;
import ballerina/jwt;
import ballerina/ 'lang\.object as lang;
import ballerina/log;

public type APIGatewayListener object {
    *lang:Listener;

    private int listenerPort = 0;
    private string listenerType = "HTTP";
    public http:Listener httpListener;

    public function __init(int port, http:ListenerConfiguration config) {
        if ((config.secureSocket is ())) {
            self.listenerPort = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTP_PORT, port);
        } else {
            self.listenerPort = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTPS_PORT, port);
            self.listenerType = "HTTPS";
        }
        initiateGatewayConfigurations(config);
        printDebug(KEY_GW_LISTNER, "Initialized gateway configurations for port:" + self.listenerPort.toString());

        self.httpListener = new (self.listenerPort, config = config);

        printDebug(KEY_GW_LISTNER, "Successfully initialized APIGatewayListener for port:" + self.listenerPort.toString());
    }


    public function __start() returns error? {
        error? gwListener = self.httpListener.__start();

        log:printInfo(self.listenerType + " listener is active on port " + self.listenerPort.toString());
        return gwListener;
    }

    public function __gracefulStop() returns error? {
        return self.httpListener.__gracefulStop();
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.httpListener.__attach(s, name);
    }

    public function __immediateStop() returns error? {
        return self.httpListener.__immediateStop();
    }

    public function __detach(service s) returns error? {
        return self.httpListener.__detach(s);
    }


};


public function initiateGatewayConfigurations(http:ListenerConfiguration config) {
    // default should bind to 0.0.0.0, not localhost. Else will not work in dockerized environments.
    config.host = getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HOST, "0.0.0.0");
    initiateKeyManagerConfigurations();
    printDebug(KEY_GW_LISTNER, "Initialized key manager configurations");
    initGatewayCaches();
    printDebug(KEY_GW_LISTNER, "Initialized gateway caches");
    //TODO : migrate this method and re enable
    initializeAnalytics();
    initializegRPCAnalytics();

    //Change the httpVersion
    if (getConfigBooleanValue(HTTP2_INSTANCE_ID, HTTP2_PROPERTY, false)) {
        config.httpVersion = "2.0";
        log:printDebug("httpVersion = " + config.httpVersion);
    }
}

public function getAuthHandlers() returns http:InboundAuthHandler[] {
    //Initializes jwt handler
    jwt:JwtValidatorConfig jwtValidatorConfig = {
        issuer: getConfigValue(JWT_INSTANCE_ID, ISSUER, "https://localhost:9443/oauth2/token"),
        audience: getConfigValue(JWT_INSTANCE_ID, AUDIENCE, "RQIO7ti2OThP79wh3fE5_Zksszga"),
        clockSkewInSeconds: 60,
        trustStoreConfig: {
            trustStore: {
                path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
            },
            certificateAlias: getConfigValue(JWT_INSTANCE_ID, CERTIFICATE_ALIAS, "ballerina")
        },
        jwtCache: jwtCache
    };
    JwtAuthProvider jwtAuthProvider = new (jwtValidatorConfig);
    JWTAuthHandler | JWTAuthHandlerWrapper jwtAuthHandler;
    if (isMetricsEnabled || isTracingEnabled) {
        jwtAuthHandler = new JWTAuthHandlerWrapper(jwtAuthProvider);
    } else {
        jwtAuthHandler = new JWTAuthHandler(jwtAuthProvider);
    }


    // Initializes the key validation handler
    KeyValidationServerConfig keyValidationServerConfig = {
        url: getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, "https://localhost:9443"),
        clientConfig:
        {
            cache: {enabled: false},
            secureSocket: {
                trustStore: {
                    path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                    "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                    password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
                },
                verifyHostname: getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
            }
        }
    };
    OAuth2KeyValidationProvider oauth2KeyValidationProvider = new (keyValidationServerConfig);
    KeyValidationHandler | KeyValidationHandlerWrapper keyValidationHandler;
    if (isMetricsEnabled || isTracingEnabled) {
        keyValidationHandler = new KeyValidationHandlerWrapper(oauth2KeyValidationProvider);
    } else {
        keyValidationHandler = new KeyValidationHandler(oauth2KeyValidationProvider);
    }


    // Initializes the basic auth handler
    auth:BasicAuthConfig basicAuthConfig = {tableName: CONFIG_USER_SECTION};
    BasicAuthProvider | BasicAuthProviderWrapper configBasicAuthProvider;
    if (isMetricsEnabled || isTracingEnabled) {
        configBasicAuthProvider = new BasicAuthProviderWrapper(basicAuthConfig);
    } else {
        configBasicAuthProvider = new BasicAuthProvider(basicAuthConfig);
    }
    http:BasicAuthHandler basicAuthHandler = new (configBasicAuthProvider);

    //Initializes the mutual ssl handler
    MutualSSLHandler | MutualSSLHandlerWrapper mutualSSLHandler;
    if (isMetricsEnabled || isTracingEnabled) {
        mutualSSLHandler = new MutualSSLHandlerWrapper();
    } else {
        mutualSSLHandler = new MutualSSLHandler();
    }

    //Initializes the cookie based handler
    CookieAuthHandler cookieBasedHandler = new;

    return [mutualSSLHandler, cookieBasedHandler, jwtAuthHandler, keyValidationHandler, basicAuthHandler];
}


public function getDefaultAuthorizationFilter() returns OAuthzFilter | OAuthzFilterWrapper {
    int cacheExpiryTime = getConfigIntValue(CACHING_ID, TOKEN_CACHE_EXPIRY, 900000);
    int cacheSize = getConfigIntValue(CACHING_ID, TOKEN_CACHE_CAPACITY, 100);
    float evictionFactor = getConfigFloatValue(CACHING_ID, TOKEN_CACHE_EVICTION_FACTOR, 0.25);
    cache:Cache positiveAuthzCache = new (cacheExpiryTime, cacheSize, evictionFactor);
    cache:Cache negativeAuthzCache = new (cacheExpiryTime, cacheSize, evictionFactor);
    if (isTracingEnabled || isMetricsEnabled) {
        OAuthzFilterWrapper authzFilterWrapper = new (positiveAuthzCache, negativeAuthzCache, ());        //TODO: set the proper scopes
        return authzFilterWrapper;
    } else {
        OAuthzFilter authzFilter = new (positiveAuthzCache, negativeAuthzCache, ());        //TODO: set the proper scopes
        return authzFilter;
    }
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
