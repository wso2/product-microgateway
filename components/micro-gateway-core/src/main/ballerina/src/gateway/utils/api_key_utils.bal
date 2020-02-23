// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/jwt;
import ballerina/lang.'int as ints;
import ballerina/runtime;
import ballerina/stringutils;
import ballerina/system;
import ballerina/time;

# Provide self generated jwt as api key.
# + req - http request
# + return - Returns api key.
public function generateAPIKey(http:Request req) returns string | error {
    if (getConfigBooleanValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_ENABLED, DEFAULT_API_KEY_ISSUER_ENABLED)) {
        crypto:KeyStore keyStore = {
            path: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, KEY_STORE_PATH, DEFAULT_KEY_STORE_PATH),
            password: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, KEY_STORE_PASSWORD, DEFAULT_KEY_STORE_PASSWORD)
        };

        jwt:JwtKeyStoreConfig config = {
            keyStore: keyStore,
            keyAlias: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, CERTIFICATE_ALIAS, DEFAULT_API_KEY_ALIAS),
            keyPassword: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, KEY_STORE_PASSWORD, DEFAULT_KEY_STORE_PASSWORD)
        };

        jwt:JwtHeader header = {};
        header.alg = jwt:RS256;
        header.typ = AUTH_SCHEME_JWT;
        header.kid = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, CERTIFICATE_ALIAS, DEFAULT_API_KEY_ALIAS);

        jwt:JwtPayload jwtPayload = {};
        //get authenticated user
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        string username = authContext.username;

        printDebug(API_KEY_UTIL, "API Key claims sub : " + username);
        jwtPayload.sub = username;
        jwtPayload.iss = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, ISSUER, DEFAULT_API_KEY_ISSUER);
        jwtPayload.jti = system:uuid();
        jwtPayload.aud = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, AUDIENCE, DEFAULT_AUDIENCE);
        int currentTime = time:currentTime().time / 1000;        //current time in seconds
        int expiryTime = getExpiryTime(req);

        if (expiryTime > 0) {
            jwtPayload.exp = currentTime + expiryTime;
        }

        jwtPayload.iat = currentTime;

        map<json> customClaims = {};
        json keyType = PRODUCTION_KEY_TYPE;
        customClaims[KEY_TYPE] = keyType;

        json[] apis = [];
        int counter = 1;
        while (true) {
            map<any> apiMap = getConfigMapValue(API_KEY_ISSUER_APIS + "." + counter.toString());
            counter = counter + 1;
            if (apiMap.keys().length() > 0) {
                string name = <string>apiMap.get("name");
                if (!apiMap.hasKey("versions") || stringutils:equalsIgnoreCase("*", <string>apiMap.get("versions"))) {
                    json api = {name: name, 'version: "*"};
                    apis.push(api);
                } else {
                    string allowedVersionsfromConfig = <string>apiMap.get("versions");
                    string[] allowedVersionList = split(allowedVersionsfromConfig, ",");
                    foreach string v in allowedVersionList {
                        json api = {name: name, 'version: v.trim()};
                        apis.push(api);
                    }
                }
            } else {
                break;
            }
        }
        printDebug(API_KEY_UTIL, apis.toJsonString());
        customClaims[ALLOWED_APIS] = apis;
        jwtPayload.customClaims = customClaims;

        printDebug(API_KEY_UTIL, "API Key is being issued.. .");
        string | error apiKey = jwt:issueJwt(header, jwtPayload, config);
        printDebug(API_KEY_UTIL, "API Key issuing process completed");
        return apiKey;
    }
    return "Error: API Key issuer is disabled";
}

public function getExpiryTime(http:Request req) returns @tainted (int) {
    var payload = req.getJsonPayload();
    int expiryTime =
        getConfigIntValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_VALIDITY_TIME, DEFAULT_API_KEY_VALIDITY_TIME);
    printDebug(API_KEY_UTIL, "Validity Period in config: " + expiryTime.toString());

    if (payload is json) {
        map<json> payloadMap = <map<json>>payload;
        if (payloadMap.hasKey(API_KEY_VALIDITY_TIME)) {
            var expiryTimefromPayload = ints:fromString(payloadMap[API_KEY_VALIDITY_TIME].toString());
            if (expiryTimefromPayload is int && expiryTimefromPayload > 0
                    && (expiryTime < 0 || expiryTime > expiryTimefromPayload)) {
                expiryTime = expiryTimefromPayload;
            }
        }
    }
    return expiryTime;
}

# api key authorization
#
# + apiKeyToken - api key token string.
# + return - Returns boolean value.
public function validateAPIKey(string apiKeyToken) returns boolean {
    boolean validated = false;
    boolean validateAllowedAPIs = getConfigBooleanValue(API_KEY_INSTANCE_ID, API_KEY_VALIDATE_ALLOWED_APIS, 
        DEFAULT_VALIDATE_APIS_ENABLED);

    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    runtime:AuthenticationContext? authContext = invocationContext?.authenticationContext;
    if (authContext is runtime:AuthenticationContext) {
        printDebug(API_KEY_UTIL, "Set authContext scheme to " + AUTH_SCHEME_API_KEY);
        authContext.scheme = AUTH_SCHEME_API_KEY;
    }
    //decode jwt
    [jwt:JwtHeader, jwt:JwtPayload] | jwt:Error decodedJWT = jwt:decodeJwt(apiKeyToken);
    if (decodedJWT is error) {
        printError(API_KEY_UTIL, "Error while decoding the JWT token");
        return false;
    }
    //get payload
    (jwt:JwtPayload | error) payload = getDecodedJWTPayload(apiKeyToken);
    if (payload is jwt:JwtPayload) {
        json subscribedAPIList = [];
        //get allowed apis
        map<json>? customClaims = payload?.customClaims;
        if (customClaims is map<json>) {
            if (customClaims.hasKey(SUBSCRIBED_APIS)) {
                printDebug(API_KEY_UTIL, "SubscribedAPIs claim found in the jwt");
                subscribedAPIList = customClaims.get(SUBSCRIBED_APIS);
            } else if (customClaims.hasKey(ALLOWED_APIS)) {
                printDebug(API_KEY_UTIL, "AllowedAPIs claim found in the jwt");
                subscribedAPIList = customClaims.get(ALLOWED_APIS);
            }
        }
        if (subscribedAPIList is json[]) {
            if (validateAllowedAPIs && subscribedAPIList.length() < 1) {
                printError(API_KEY_UTIL, "SubscribedAPI list is empty");
                return false;
            }
            validated = handleSubscribedAPIs(apiKeyToken, payload, subscribedAPIList, validateAllowedAPIs);
            if (validated || !validateAllowedAPIs) {
                printDebug(API_KEY_UTIL, "Subscriptions validated.");
                return true;
            }
        }
    }
    printError(API_KEY_UTIL, "Failed to validate API.");
    return false;
}

public function setAPIKeyAuth(string inName, string name) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    invocationContext.attributes["apiKeyIn"] = inName;
    invocationContext.attributes["apiKeyName"] = name;
}

public function getAPIKeyAuth() returns [string, string] | error {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (invocationContext.attributes.hasKey("apiKeyIn") && invocationContext.attributes.hasKey("apiKeyName")) {
        return [<string>invocationContext.attributes["apiKeyIn"], <string>invocationContext.attributes["apiKeyName"]];
    } else {
        printDebug(API_KEY_UTIL, "API key is missing in invocation context");
        return error("API key is missing in invocation context");
    }
}
