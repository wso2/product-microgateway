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
# + consumerKeyClaim - The claim name in which the consumer key of the application present in the jwt.
# + claims - JWT claim set
# + className - Transformation class Name
# + classLoaded - Class loaded or not
# + gatewayCache - the `APIGatewayCache instence`
public type JwtAuthProvider object {
    *auth:InboundAuthProvider;

    public jwt:JwtValidatorConfig jwtValidatorConfig;
    public jwt:InboundJwtAuthProvider inboundJwtAuthProvider;
    public boolean subscriptionValEnabled;
    public string consumerKeyClaim;
    public map<anydata>[] | error claims;
    public string className;
    public boolean classLoaded;
    public APIGatewayCache gatewayCache = new;

    # Provides authentication based on the provided JWT token.
    #
    # + jwtValidatorConfig - JWT validator configurations
    # + subscriptionValEnabled - Validate subscription
    # + remoteUserClaimRetrievalEnabled - true if the remote user claim retrieval is required
    public function __init(jwt:JwtValidatorConfig jwtValidatorConfig, boolean subscriptionValEnabled, string consumerKeyClaim,
        map<anydata>[] | error claims, string className, boolean classLoaded) {
        self.jwtValidatorConfig = jwtValidatorConfig;
        self.inboundJwtAuthProvider = new (jwtValidatorConfig);
        self.subscriptionValEnabled = subscriptionValEnabled;
        self.consumerKeyClaim = consumerKeyClaim;
        self.claims = claims;
        self.className = className;
        self.classLoaded = classLoaded;
    }

    public function authenticate(string credential) returns @tainted (boolean | auth:Error) {
        //Start a span attaching to the system span.
        int | error | () spanIdAuth = startSpan(JWT_PROVIDER_AUTHENTICATE);
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        var errorCodeSetFromPreviousHandler = invocationContext.attributes[ERROR_CODE];
        if (errorCodeSetFromPreviousHandler is int && errorCodeSetFromPreviousHandler != 900901) {
            //If a previous jwt auth provider(or issuer) has set any error code than the 900901(invalid token code),
            // that means jwt validation has been successful for a previous jwt issuer, but scope or subscription
            // validation has failed. Hence we do not need to continue rest of the jwt auth providers.
            return false;
        }
        var handleVar = self.inboundJwtAuthProvider.authenticate(credential);
        map<anydata>[] | error claimsSet = self.claims;
        //finishing span
        finishSpan(JWT_PROVIDER_AUTHENTICATE, spanIdAuth);
        if (handleVar is boolean) {
            if (!handleVar) {
                setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                return handleVar;
            }
            boolean isBlacklisted = false;
            string? jti = "";

            runtime:AuthenticationContext? authContext = invocationContext?.authenticationContext;
            if (authContext is runtime:AuthenticationContext) {
                string? iss = self.jwtValidatorConfig?.issuer;
                string? jwtToken = authContext?.authToken;
                if (jwtToken is string && iss is string) {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "jwt authenticated from the issuer : " + iss);
                    boolean isGRPC = invocationContext.attributes.hasKey(IS_GRPC);
                    //Start a new child span for the span.
                    int | error | () spanIdCache = startSpan(JWT_CACHE);
                    var jwtPayloadFromCache = trap <jwt:JwtPayload>self.jwtValidatorConfig.jwtCache.get(jwtToken);
                    //finishing span
                    finishSpan(JWT_CACHE, spanIdCache);
                    if (jwtPayloadFromCache is jwt:JwtPayload) {
                        printDebug(KEY_JWT_AUTH_PROVIDER, "jwt found from the jwt cache");
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
                        if(self.className != "" || (claimsSet is map<anydata>[] && claimsSet.length() > 0)) {
                            var jwtTokenClaimCached = self.gatewayCache.retrieveClaimMappingCache(jwtToken);
                            if (jwtTokenClaimCached is runtime:Principal) {
                                invocationContext.principal =  jwtTokenClaimCached;
                                printDebug(KEY_JWT_AUTH_PROVIDER, "Modified claims in the cache");
                            } else {
                                printDebug(KEY_JWT_AUTH_PROVIDER, "Modified claims is not in the cache");
                                var result = doMappingContext(invocationContext, self.className, claimsSet,
                                    self.classLoaded, jwtPayloadFromCache, self.jwtValidatorConfig, self.gatewayCache, authContext);
                                if (result is auth:Error){
                                    return result;
                                }
                                jwtToken = authContext?.authToken.toString();
                            }
                         }
                        return validateSubscriptions(jwtToken, jwtPayloadFromCache, self.subscriptionValEnabled,
                                self.consumerKeyClaim, isGRPC, self.gatewayCache);
                    }
                    printDebug(KEY_JWT_AUTH_PROVIDER, "jwt not found in the jwt cache");
                    (jwt:JwtPayload | error) payload = getDecodedJWTPayload(self.jwtValidatorConfig, jwtToken);
                    if (payload is jwt:JwtPayload) {
                        if(self.className != "" || (claimsSet is map<anydata>[] && claimsSet.length() > 0)) {
                            var jwtTokenClaimCached = self.gatewayCache.retrieveClaimMappingCache(jwtToken);
                            if (jwtTokenClaimCached is runtime:Principal) {
                                invocationContext.principal =  jwtTokenClaimCached;
                                printDebug(KEY_JWT_AUTH_PROVIDER, "Modified claims in the cache");
                            } else {
                                printDebug(KEY_JWT_AUTH_PROVIDER, "Modified claims is not in the cache");
                                var result = doMappingContext(invocationContext, self.className, claimsSet,
                                    self.classLoaded, payload, self.jwtValidatorConfig, self.gatewayCache, authContext);
                                if (result is auth:Error){
                                    return result;
                                }
                                jwtToken = authContext?.authToken.toString();
                            }
                        }
                        return validateSubscriptions(jwtToken, payload, self.subscriptionValEnabled, self.consumerKeyClaim, isGRPC, self.gatewayCache);
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

public function validateSubscriptions(string jwtToken, jwt:JwtPayload payload, boolean subscriptionValEnabled,
                            string consumerKeyClaim, boolean isGRPC, APIGatewayCache gatewayCache) returns @tainted (boolean | auth:Error) {
    boolean subscriptionValidated = false;
    map<json>? customClaims = payload?.customClaims;

    subscriptionValidated = isAllowedKey(jwtToken, payload, subscriptionValEnabled, consumerKeyClaim, gatewayCache);
    if (subscriptionValidated || !subscriptionValEnabled || isGRPC) {
        printDebug(KEY_JWT_AUTH_PROVIDER, "Subscriptions validated.");
        return true;
    } else {
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        var errorcode = invocationContext.attributes[ERROR_CODE];
        if (errorcode is ()) {
            setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
        }
        return prepareError("Subscriptions validation failed.");
    }
}

public function doMappingContext(runtime:InvocationContext invocationContext, string className,
    map<anydata>[] | error claims, boolean classLoaded, jwt:JwtPayload jwtPayloadFromCache,
        jwt:JwtValidatorConfig jwtValidatorConfig, APIGatewayCache gatewayCache,
            runtime:AuthenticationContext authContext) returns @tainted (auth:Error)? {
    string payloadIssuer = jwtPayloadFromCache["iss"].toString();
    string payloadAudience = jwtPayloadFromCache["aud"].toString();
    if (jwtValidatorConfig[ISSUER] ==  payloadIssuer) {
        map<any>? customClaims = invocationContext[PRINCIPAL][ISSUER_CLAIMS];
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
                printDebug(KEY_JWT_AUTH_PROVIDER, "custom claims key value is updated");
            }
            if (className != "" && classLoaded) {
                map<any>|error customClaimsEdited = transformJWTValue(customClaims, className);
                if (customClaimsEdited is map<any>) {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "custom claims value change to validated format");
                    customClaims = customClaimsEdited;
                } else {
                    return prepareError("Error occured while loading the custom method ");
                }
            }
            if (customClaims.hasKey(SCOPE) && customClaims[SCOPE].toString() != "") {
                var result = putScopeValue(customClaims[SCOPE], invocationContext);
                if (result is auth:Error) {
                    return result;
                }
            }
            runtime:Principal? modifiedPrincipal = invocationContext[PRINCIPAL];
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
