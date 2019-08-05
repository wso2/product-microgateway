// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/jwt;
import ballerina/cache;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;
import ballerina/io;


# Represents inbound JWT auth provider.
#
# + jwtValidatorConfig - JWT validator configurations
# + inboundJwtAuthProvider - Reference to b7a inbound auth provider
public type JwtAuthProvider object {
    *auth:InboundAuthProvider;

    public jwt:JwtValidatorConfig jwtValidatorConfig;
    public jwt:InboundJwtAuthProvider inboundJwtAuthProvider;

    # Provides authentication based on the provided JWT token.
    #
    # + jwtValidatorConfig - JWT validator configurations
    public function __init(jwt:JwtValidatorConfig jwtValidatorConfig) {
        self.jwtValidatorConfig = jwtValidatorConfig;
        self.inboundJwtAuthProvider = new(jwtValidatorConfig);
    }

    # Authenticate with a JWT token.
    #
    # + credential - Jwt token extracted from the authentication header
    # + return - `true` if authentication is successful, othewise `false` or `auth:Error` occurred during JWT validation
    public function authenticate(string credential) returns @tainted (boolean|auth:Error) {
        var handleVar = self.inboundJwtAuthProvider.authenticate(credential);
        if(handleVar is boolean) {
        if (handleVar) {
            boolean isBlacklisted = false;
            string? jti = "";
            runtime:InvocationContext invocationContext= runtime:getInvocationContext();
            runtime:AuthenticationContext? authContext = invocationContext?.authenticationContext;
            if(authContext is AuthenticationContext){
                string jwtToken = authContext.authToken;
                var cachedJwt = trap <jwt:CachedJwt>jwtCache.get(jwtToken);
                if (cachedJwt is jwt:CachedJwt) {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "jwt found from the jwt cache");
                    jwt:JwtPayload jwtPayloadFromCache = cachedJwt.jwtPayload;
                    jti = jwtPayloadFromCache["jti"];
                    if(jti is string) {
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
                            return false;
                        } else {
                            return true;
                        }

                    } else {
                        printDebug(KEY_JWT_AUTH_PROVIDER, "jti claim not found in the jwt");
                        return handleVar;
                    }

                } else {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "jwt not found in the jwt cache");
                    return handleVar;
                }
            }
        } else {
            return handleVar;
        }
    } else {
        return prepareAuthenticationError("Failed to authenticate with jwt auth provider.", handleVar);
        }

    }    
};
