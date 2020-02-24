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

import ballerina/cache;
import ballerina/http;
import ballerina/ 'lang\.object as lang;
import ballerina/log;
import ballerina/oauth2;
import ballerina/stringutils;

boolean isConfigInitiated = false;
boolean isDebugEnabled = false;

public type APIGatewayListener object {
    *lang:Listener;

    private int listenerPort = 0;
    private string listenerType = "HTTP";
    public http:Listener httpListener;

    public function __init(int port, http:ListenerConfiguration config) {
        // Since http listeners is wrapped inside https listener also, this init method get invoked twice per
        // each listener. This check will make sure that configurations are read only once and respective
        //objects are initialized only once.
        if (!isConfigInitiated) {
            string logLevel = getConfigValue(B7A_LOG, LOG_LEVEL, INFO);
            if (stringutils:equalsIgnoreCase(DEBUG, logLevel) || stringutils:equalsIgnoreCase(TRACE, logLevel)) {
                isDebugEnabled = true;
            }
            initiateGatewayConfigurations(config);
        }
        if ((config.secureSocket is ())) {
            self.listenerPort = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTP_PORT, port);
            //Initiate handlers without listener annotation to make sure that, the handlers get initialized
            //after the gateway cache objects are initialized.
            initiateAuthenticationHandlers(config);
        } else {
            self.listenerPort = getConfigIntValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HTTPS_PORT, port);
            self.listenerType = "HTTPS";
        }
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

function initiateAuthenticationHandlers(http:ListenerConfiguration config) {
    http:ListenerAuth auth = {
         authHandlers: getAuthHandlers(),
         mandateSecureSocket: false,
         position: 2
    };
    config.auth = auth;
}

public function initiateGatewayConfigurations(http:ListenerConfiguration config) {
    config.host = getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HOST, DEFAULT_CONF_HOST);
    initiateKeyManagerConfigurations();
    printDebug(KEY_GW_LISTNER, "Initialized key manager configurations");
    printDebug(KEY_GW_LISTNER, "Initialized gateway caches");
    //TODO : migrate this method and re enable
    initializeAnalytics();
    initializegRPCAnalytics();

    //Change the httpVersion
    if (getConfigBooleanValue(HTTP2_INSTANCE_ID, HTTP2_PROPERTY, DEFAULT_HTTP2_ENABLED)) {
        config.httpVersion = "2.0";
        log:printDebug("httpVersion = " + config.httpVersion);
    }
    isConfigInitiated = true;
}

public function getAuthHandlers(string[] appSecurity = [], boolean appSecurityOptional = false, 
        boolean isMutualSSL = false) returns http:InboundAuthHandler[][] {
    if (authHandlersMap.length() < 1) {
        printDebug(KEY_GW_LISTNER, "Initializing auth handlers");
        initAuthHandlers();
    }
    if (appSecurityOptional) { 
        if (isMutualSSL) {
            // add mutual ssl to the auth handlers
            appSecurity.push(AUTH_SCHEME_MUTUAL_SSL);
        }
        return [getHandlers(appSecurity)];
    }
    // if application security is mandatory, one of application handlers must pass. If mutual ssl enabled. it also should pass.
    // e.g. [mutualssl] && [jwt or basic or ...]
    if (isMutualSSL) {
        return [getHandlers([AUTH_SCHEME_MUTUAL_SSL]), getHandlers(appSecurity)];
    }
    return [getHandlers(appSecurity)];
}

public function getDefaultAuthorizationFilter() returns OAuthzFilter | OAuthzFilterWrapper {
    int cacheExpiryTime = getConfigIntValue(CACHING_ID, TOKEN_CACHE_EXPIRY, DEFAULT_TOKEN_CACHE_EXPIRY);
    int cacheSize = getConfigIntValue(CACHING_ID, TOKEN_CACHE_CAPACITY, DEFAULT_TOKEN_CACHE_CAPACITY);
    float evictionFactor = getConfigFloatValue(CACHING_ID, TOKEN_CACHE_EVICTION_FACTOR, DEFAULT_TOKEN_CACHE_EVICTION_FACTOR);
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
    keyManagerConf.serverUrl = getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, DEFAULT_KM_SERVER_URL);
    credentials.username = getConfigValue(KM_CONF_INSTANCE_ID, USERNAME, DEFAULT_USERNAME);
    credentials.password = getConfigValue(KM_CONF_INSTANCE_ID, PASSWORD, DEFAULT_PASSWORD);
    keyManagerConf.credentials = credentials;
    getGatewayConfInstance().setKeyManagerConf(keyManagerConf);
}

public function getBasicAuthHandler() returns http:InboundAuthHandler[] {
    // Initializes the basic auth handler
    if (authHandlersMap.length() < 1) {
        printDebug(KEY_GW_LISTNER, "Initializing auth handlers");
        initAuthHandlers();
    }
    return [authHandlersMap.get(BASIC_AUTH_HANDLER)];
}

function getOauth2OutboundProvider() returns oauth2:OutboundOAuth2Provider | error {
    oauth2:OutboundOAuth2Provider oauth2Provider = new ();
    http:ClientConfiguration clientConfig = {
        secureSocket: {
            trustStore: {
                path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH),
                password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD)
            },
            verifyHostname: getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
        }
    };
    if (getConfigBooleanValue(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, ENABLED, DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED)) {
        if (getConfigBooleanValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, ENABLED, DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED)) {
            oauth2Provider = new ({
                tokenUrl: getConfigValue(KM_CONF_SECURITY_OAUTH2_INSTANCE_ID, TOKEN_URL, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                username: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, USERNAME, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                password: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, PASSWORD, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                clientId: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, CLIENT_ID, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                clientSecret: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, CLIENT_SECRET,
                    DEFAULT_KM_CONF_SECURITY_OAUTH2),
                scopes: readScpoesAsArray(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, SCOPES),
                credentialBearer: getCredentialBearer(),
                refreshConfig: {
                    refreshUrl: getConfigValue(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, REFRESH_URL,
                        DEFAULT_KM_CONF_SECURITY_OAUTH2),
                    scopes: readScpoesAsArray(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, SCOPES),
                    clientConfig: clientConfig
                },
                clientConfig: clientConfig
            });
        } else if (getConfigBooleanValue(KM_CONF_SECURITY_OAUTH2_DIRECT_INSTANCE_ID, ENABLED,
                DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED)) {
            oauth2Provider = new ({
                accessToken: getConfigValue(KM_CONF_SECURITY_OAUTH2_DIRECT_INSTANCE_ID, ACCESS_TOKEN, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                credentialBearer: getCredentialBearer(),
                refreshConfig: {
                    refreshUrl: getConfigValue(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, REFRESH_URL,
                        DEFAULT_KM_CONF_SECURITY_OAUTH2),
                    refreshToken: getConfigValue(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, REFRESH_TOKEN,
                        DEFAULT_KM_CONF_SECURITY_OAUTH2),
                    clientId: getConfigValue(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, CLIENT_ID, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                    clientSecret: getConfigValue(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, CLIENT_SECRET,
                        DEFAULT_KM_CONF_SECURITY_OAUTH2),
                    scopes: readScpoesAsArray(KM_CONF_SECURITY_OAUTH2_REFRESH_INSTANCE_ID, SCOPES),
                    credentialBearer: getCredentialBearer(),
                    clientConfig: clientConfig
                }
            });
        } else {
            error err = error("Key manager OAuth2 security enabled, but no secirity configurations provided");
            return err;
        }
    } else {
        if (getConfigBooleanValue(KM_CONF_SECURITY_OAUTH2_CLIENT_CREDENTIAL_INSTANCE_ID, ENABLED,
            DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED)) {
            oauth2Provider = new ({
                tokenUrl: getConfigValue(KM_CONF_SECURITY_OAUTH2_INSTANCE_ID, TOKEN_URL, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                clientId: getConfigValue(KM_CONF_SECURITY_OAUTH2_CLIENT_CREDENTIAL_INSTANCE_ID, CLIENT_ID,
                    DEFAULT_KM_CONF_SECURITY_OAUTH2),
                clientSecret: getConfigValue(KM_CONF_SECURITY_OAUTH2_CLIENT_CREDENTIAL_INSTANCE_ID, CLIENT_SECRET,
                    DEFAULT_KM_CONF_SECURITY_OAUTH2),
                scopes: readScpoesAsArray(KM_CONF_SECURITY_OAUTH2_CLIENT_CREDENTIAL_INSTANCE_ID, SCOPES),
                credentialBearer: getCredentialBearer(),
                clientConfig: clientConfig
            });
        } else if (getConfigBooleanValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, ENABLED,
                DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED)) {
            oauth2Provider = new ({
                tokenUrl: getConfigValue(KM_CONF_SECURITY_OAUTH2_INSTANCE_ID, TOKEN_URL, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                username: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, USERNAME, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                password: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, PASSWORD, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                clientId: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, CLIENT_ID, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                clientSecret: getConfigValue(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, CLIENT_SECRET, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                scopes: readScpoesAsArray(KM_CONF_SECURITY_OAUTH2_PASSWORD_INSTANCE_ID, SCOPES),
                credentialBearer: getCredentialBearer(),
                clientConfig: clientConfig
            });
        } else if (getConfigBooleanValue(KM_CONF_SECURITY_OAUTH2_DIRECT_INSTANCE_ID, ENABLED, DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED)) {
            oauth2Provider = new ({
                accessToken: getConfigValue(KM_CONF_SECURITY_OAUTH2_DIRECT_INSTANCE_ID, ACCESS_TOKEN, DEFAULT_KM_CONF_SECURITY_OAUTH2),
                credentialBearer: getCredentialBearer()
            });
        } else {
            error err = error("Key manager OAuth2 security enabled, but no secirity configurations provided");
            return err;
        }
    }
    return oauth2Provider;
}

function readScpoesAsArray(string instanceId, string key) returns string[] {
    string scopes = getConfigValue(instanceId, key, "");
    string[] scopesArray = [];
    if (scopes.length() > 0) {
        scopesArray = split(scopes.trim(), ",");
    }
    return scopesArray;
}

function getCredentialBearer() returns http:CredentialBearer {
    string crednetailBearerString = getConfigValue(KM_CONF_SECURITY_OAUTH2_INSTANCE_ID, CREDENTIAL_BEARER,
        DEFAULT_KM_CONF_SECURITY_OAUTH2_CREDENTIAL_BEARER);
    if (stringutils:equalsIgnoreCase(crednetailBearerString, http:AUTH_HEADER_BEARER)) {
        return http:AUTH_HEADER_BEARER;
    } else if (stringutils:equalsIgnoreCase(crednetailBearerString, http:POST_BODY_BEARER)) {
        return http:POST_BODY_BEARER;
    }
    return http:NO_BEARER;
}
