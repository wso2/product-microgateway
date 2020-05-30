// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/jwt;
import ballerinax/java;

# To invoke the interop function to create instance of JWT generator
#
# + className - className for the jwtgenerator implementation
# + dialectURI - DialectURI for the standard claims
# + signatureAlgorithm - Signature algorithm to sign the JWT
# + keyStorePathUnresolved - Keystore path
# + keyStorePassword - Keystore password
# + certificateAlias - Certificate alias
# + privateKeyAlias - Private key alias
# + tokenExpiry - Token expiry value
# + restrictedClaims - Restricted claims from the configuration
# + enabledCaching - jwt generator caching enabled
# + cacheExpiry - jwt generator cache expiry
# + tokenIssuer - token issuer for the claims
# + tokenAudience - token audience for the claims
# + return - Returns `true` if the class is created successfully. or `false` if unsuccessful.
public function loadJWTGeneratorClass(string className,
                                        string dialectURI,
                                        string signatureAlgorithm,
                                        string keyStorePathUnresolved,
                                        string keyStorePassword,
                                        string certificateAlias,
                                        string privateKeyAlias,
                                        int tokenExpiry,
                                        any[] restrictedClaims,
                                        boolean enabledCaching,
                                        int cacheExpiry,
                                        string tokenIssuer,
                                        any[] tokenAudience) returns boolean {
    handle jClassName = java:fromString(className);
    handle jDialectURI = java:fromString(dialectURI);
    handle jSignatureAlgorithm = java:fromString(signatureAlgorithm);
    handle jCertificateAlias = java:fromString(certificateAlias);
    handle jPrivateKeyAlias = java:fromString(privateKeyAlias);
    handle jKeyStorePath = getKeystoreLocation(java:fromString(keyStorePathUnresolved));
    handle jKeyStorePassword = java:fromString(keyStorePassword);
    handle jTokenIssuer = java:fromString(tokenIssuer);
    return jLoadJWTGeneratorClass(jClassName,
                                    jDialectURI,
                                    jSignatureAlgorithm,
                                    jKeyStorePath,
                                    jKeyStorePassword,
                                    jCertificateAlias,
                                    jPrivateKeyAlias,
                                    tokenExpiry,
                                    restrictedClaims,
                                    enabledCaching,
                                    cacheExpiry,
                                    jTokenIssuer,
                                    tokenAudience);
}

# Invoke the interop function to resolves the keystore path
#
# + unresolvedPath - unresolved keystore path
# + return - Returns the resolved keystore path.
public function getKeystoreLocation(handle unresolvedPath) returns handle {
    return jGetKeystoreLocation(unresolvedPath);
}


# Invoke the interop function to generate JWT token
#
# + jwtInfo - payload of the authentication token
# + apiDetails - details of the subscribed APIS
# + return - Returns the generated JWT token.
public function generateJWTToken(jwt:JwtPayload jwtInfo, map<string> apiDetails) returns (handle | error) {
    return jGenerateJWTToken(jwtInfo, apiDetails);
}

# Interop function to create instance of JWTGenerator
#
# + className - className for the jwtgenerator implementation
# + dialectURI - DialectURI for the standard claims
# + signatureAlgorithm - Signature algorithm to sign the JWT
# + keyStorePath - Keystore path
# + keyStorePassword - Keystore password
# + certificateAlias - Certificate alias
# + privateKeyAlias - Private key alias
# + tokenExpiry - Token expiry value
# + restrictedClaims - Restricted claims from the configuration
# + enabledCaching - jwt generator caching enabled
# + cacheExpiry - jwt generator cache expiry
# + tokenIssuer - token issuer for the claims
# + tokenAudience - token audience for the claims
# + return - Returns `true` if the class is created successfully. or `false` if unsuccessful.
public function jLoadJWTGeneratorClass(handle className,
                                        handle dialectURI,
                                        handle signatureAlgorithm,
                                        handle keyStorePath,
                                        handle keyStorePassword,
                                        handle certificateAlias,
                                        handle privateKeyAlias,
                                        int tokenExpiry,
                                        any[] restrictedClaims,
                                        boolean enabledCaching,
                                        int cacheExpiry,
                                        handle tokenIssuer,
                                        any[] tokenAudience) returns boolean = @java:Method {
    name: "loadJWTGeneratorClass",
    class: "org.wso2.micro.gateway.core.jwtgenerator.MGWJWTGeneratorInvoker"
} external;

# Interop function to resolves the keystore path
#
# + unresolvedPath - unresolved keystore path
# + return - Returns the resolved keystore path.
public function jGetKeystoreLocation(handle unresolvedPath) returns handle = @java:Method {
    name: "invokeGetKeystorePath",
    class: "org.wso2.micro.gateway.core.jwtgenerator.MGWJWTGeneratorInvoker"
} external;

# Interop function to generate JWT token
#
# + jwtInfo - payload of the authentication token
# + apiDetails - details of the subscribed APIS
# + return - Returns the generated JWT token.
public function jGenerateJWTToken(jwt:JwtPayload jwtInfo, map<string> apiDetails) returns (handle | error) = @java:Method {
    name: "invokeGenerateToken",
    class: "org.wso2.micro.gateway.core.jwtgenerator.MGWJWTGeneratorInvoker"
} external;
