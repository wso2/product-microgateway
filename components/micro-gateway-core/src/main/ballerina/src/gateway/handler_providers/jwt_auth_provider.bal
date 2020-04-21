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
import ballerina/jwt;
import ballerina/runtime;
import ballerina/stringutils;

# Represents inbound JWT auth provider.
#
# + jwtValidatorConfig - JWT validator configurations
# + inboundJwtAuthProvider - Reference to b7a inbound auth provider
# + subscriptionValEnabled - Validate subscription
# + claims - JWT claim set
# + className - Transformation class Name
# + classLoaded - Class loaded or not
# + gatewayCache - the `APIGatewayCache instence`
public type JwtAuthProvider object {
    *auth:InboundAuthProvider;

    public jwt:JwtValidatorConfig jwtValidatorConfig;
    public jwt:InboundJwtAuthProvider inboundJwtAuthProvider;
    public boolean subscriptionValEnabled;
    public map<anydata>[] | error claims;
    public string className;
    public boolean classLoaded;
    public APIGatewayCache gatewayCache = new;

    # Provides authentication based on the provided JWT token.
    #
    # + jwtValidatorConfig - JWT validator configurations
    # + subscriptionValEnabled - Validate subscription
    public function __init(jwt:JwtValidatorConfig jwtValidatorConfig, boolean subscriptionValEnabled,
        map<anydata>[] | error claims, string className, boolean classLoaded) {
        self.jwtValidatorConfig = jwtValidatorConfig;
        self.inboundJwtAuthProvider = new (jwtValidatorConfig);
        self.subscriptionValEnabled = subscriptionValEnabled;
        self.claims = claims;
        self.className = className;
        self.classLoaded = classLoaded;
    }

    public function authenticate(string credential) returns @tainted (boolean | auth:Error) {
        //Start a span attaching to the system span.
        int | error | () spanIdAuth = startSpan(JWT_PROVIDER_AUTHENTICATE);
        var handleVar = self.inboundJwtAuthProvider.authenticate(credential);
        //finishing span
        finishSpan(JWT_PROVIDER_AUTHENTICATE, spanIdAuth);
        if (handleVar is boolean) {
            if (!handleVar) {
                setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                return handleVar;
            }
            boolean isBlacklisted = false;
            string? jti = "";
            runtime:InvocationContext invocationContext = runtime:getInvocationContext();
            runtime:AuthenticationContext? authContext = invocationContext?.authenticationContext;
            if (authContext is runtime:AuthenticationContext) {
                string? jwtToken = authContext?.authToken;
                if (jwtToken is string) {
                    boolean isGRPC = invocationContext.attributes.hasKey(IS_GRPC);
                    //Start a new child span for the span.
                    int | error | () spanIdCache = startSpan(JWT_CACHE);
                    var cachedJwt = trap <jwt:CachedJwt>jwtCache.get(jwtToken);
                    //finishing span
                    finishSpan(JWT_CACHE, spanIdCache);
                    if (cachedJwt is jwt:CachedJwt) {
                        printDebug(KEY_JWT_AUTH_PROVIDER, "jwt found from the jwt cache");
                        jwt:JwtPayload jwtPayloadFromCache = cachedJwt.jwtPayload;
                        jti = jwtPayloadFromCache["jti"];
                        if (jti is string) {
                            printDebug(KEY_JWT_AUTH_PROVIDER, "jti claim found in the jwt");
                            printDebug(KEY_JWT_AUTH_PROVIDER, "Checking for the JTI in the gateway invalid revoked token map.");
                            var status = retrieveFromRevokedTokenMap(jti);
                            if (status is boolean) {
                                if (status) {
                                    printDebug(KEY_JWT_AUTH_PROVIDER, "JTI token found in the invalid token map.");
                                    isBlacklisted = true;
                                } else {
                                    printDebug(KEY_JWT_AUTH_PROVIDER, "JTI token not found in the invalid token map.");
                                    isBlacklisted = false;
                                }
                            } else {
                                printDebug(KEY_JWT_AUTH_PROVIDER, "JTI token not found in the invalid token map.");
                                isBlacklisted = false;
                            }
                            if (isBlacklisted) {
                                printDebug(KEY_JWT_AUTH_PROVIDER, "JWT Authentication Handler value for, is token black listed: " + isBlacklisted.toString());
                                printDebug(KEY_JWT_AUTH_PROVIDER, "JWT Token is revoked");
                                setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                                return false;
                            }
                        } else {
                            printDebug(KEY_JWT_AUTH_PROVIDER, "jti claim not found in the jwt");
                        }
                        var jwtTokenClaimCached = self.gatewayCache.retrieveClaimMappingCache(jwtToken);
                        if (jwtTokenClaimCached is runtime:Principal) {
                            invocationContext.principal =  jwtTokenClaimCached;
                            printDebug(KEY_JWT_AUTH_PROVIDER, "Moddified claims in the cache");
                        } else {
                            printDebug(KEY_JWT_AUTH_PROVIDER, "Moddified claims is not in the cache");
                            var result = doMappingContext(invocationContext, self.className, self.claims,
                                self.classLoaded, jwtPayloadFromCache, self.jwtValidatorConfig, self.gatewayCache, authContext);
                            if (result is auth:Error){
                                return result;
                            }
                            jwtToken = authContext?.authToken.toString();
                        }
                        return validateSubscriptions(jwtToken, cachedJwt.jwtPayload, self.subscriptionValEnabled, isGRPC);
                    }
                    printDebug(KEY_JWT_AUTH_PROVIDER, "jwt not found in the jwt cache");
                    (jwt:JwtPayload | error) payload = getDecodedJWTPayload(jwtToken);
                    if (payload is jwt:JwtPayload) {
                        var jwtTokenClaimCached = self.gatewayCache.retrieveClaimMappingCache(jwtToken);
                        if (jwtTokenClaimCached is runtime:Principal) {
                            invocationContext.principal =  jwtTokenClaimCached;
                            printDebug(KEY_JWT_AUTH_PROVIDER, "Moddified claims in the cache");
                        } else {
                            printDebug(KEY_JWT_AUTH_PROVIDER, "Moddified claims is not in the cache");
                            var result = doMappingContext(invocationContext, self.className, self.claims,
                                self.classLoaded, payload, self.jwtValidatorConfig, self.gatewayCache, authContext);
                            if (result is auth:Error){
                                return result;
                            }
                            jwtToken = authContext?.authToken.toString();
                        }
                        return validateSubscriptions(jwtToken, payload, self.subscriptionValEnabled, isGRPC);
                    }
                }
            }
            return handleVar;
        } else {
            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
            return prepareError("Failed to authenticate with jwt auth provider.", handleVar);
        }
    }
};

public function validateSubscriptions(string jwtToken, jwt:JwtPayload payload, boolean subscriptionValEnabled, boolean isGRPC)
        returns @tainted (boolean | auth:Error) {
    boolean subscriptionValidated = false;
    json subscribedAPIList = [];
    map<json>? customClaims = payload?.customClaims;
    //get allowed apis
    if (customClaims is map<json> && customClaims.hasKey(SUBSCRIBED_APIS)) {
        printDebug(KEY_JWT_AUTH_PROVIDER, "subscribedAPIs claim found in the jwt.");
        subscribedAPIList = customClaims.get(SUBSCRIBED_APIS);
    }
    if (subscribedAPIList is json[]) {
        if (subscriptionValEnabled && subscribedAPIList.length() < 1) {
            setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
            return prepareError("SubscribedAPI list is empty.");
        }
        subscriptionValidated = handleSubscribedAPIs(jwtToken, payload, subscribedAPIList, subscriptionValEnabled);
        if (subscriptionValidated || !subscriptionValEnabled || isGRPC) {
            printDebug(KEY_JWT_AUTH_PROVIDER, "Subscriptions validation passed.");
            return true;
        } else {
            setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
            return prepareError("Subscriptions validation failed.");
        }
    }
    setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
    return prepareError("Failed to decode the JWT.");
}

public function doMappingContext(runtime:InvocationContext invocationContext, string className,
    map<anydata>[] | error claims, boolean classLoaded, jwt:JwtPayload jwtPayloadFromCache,
        jwt:JwtValidatorConfig jwtValidatorConfig, APIGatewayCache gatewayCache,
            runtime:AuthenticationContext authContext) returns @tainted (auth:Error)? {
    string payloadIssuer = jwtPayloadFromCache["iss"].toString();
    string payloadAudience = jwtPayloadFromCache["aud"].toString();
    if( jwtValidatorConfig[ISSUER] ==  payloadIssuer &&
        jwtValidatorConfig[AUDIENCE] ==  payloadAudience) {
        map<any>? customClaims = invocationContext["principal"]["claims"];
        if (customClaims is map<any>) {
            if (claims is map<anydata>[] && claims.length() > 0) {
                foreach map<anydata> claim in claims {
                    string remoteClaim = claim["remoteClaim"].toString();
                    string localClaim = claim["localClaim"].toString();
                    if (customClaims is map<anydata>) {
                        if (customClaims.hasKey(remoteClaim)) {
                            customClaims[localClaim] = customClaims[remoteClaim];
                            anydata removedElement = customClaims.remove(remoteClaim);
                        }
                    }
                }
            }
            if (className != "" && classLoaded) {
                map<any>? customClaimsEdited = transformJWTValue(customClaims, className);
                if (customClaimsEdited is map<any>) {
                    customClaims = customClaimsEdited;
                } else {
                    return prepareError("Error while loading the jwttransformer class: " + className);
                }
            }
            if (customClaims["scope"].toString() != "") {
                var result = putScopeValue(customClaims["scope"], invocationContext);
                if (result is auth:Error) {
                    return result;
                }
            }
            runtime:Principal? modifiedPrincipal = invocationContext["principal"];
            string jwtToken = authContext?.authToken.toString();
            if (modifiedPrincipal is runtime:Principal) {
                gatewayCache.addClaimMappingCache(jwtToken, modifiedPrincipal);
            }
        }
    }
}

public function putScopeValue(any scope, runtime:InvocationContext invocationContext) returns @tainted (auth:Error)? {
    if (scope is string && scope != "") {
        string[]? scopes =  stringutils:split(scope.toString(), " ");
        if (scopes is string[]) {
            invocationContext.principal.scopes = scopes;
        } else {
            return prepareError("Scope cannot be change to string array format");
        }
    } else {
        return prepareError("Scope is not a string format.");
    }
}
