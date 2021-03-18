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
import ballerina/java;

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
public function loadJWTGeneratorClass(string className, string dialectURI, string signatureAlgorithm,
                                        string keyStorePathUnresolved, string keyStorePassword, string certificateAlias,
                                        string privateKeyAlias, int tokenExpiry, any[] restrictedClaims,
                                        boolean enabledCaching, int cacheExpiry, string tokenIssuer,
                                        any[] tokenAudience) returns boolean {
    handle jClassName = java:fromString(className);
    handle jDialectURI = java:fromString(dialectURI);
    handle jSignatureAlgorithm = java:fromString(signatureAlgorithm);
    handle jCertificateAlias = java:fromString(certificateAlias);
    handle jPrivateKeyAlias = java:fromString(privateKeyAlias);
    handle jKeyStorePath = getKeystoreLocation(java:fromString(keyStorePathUnresolved));
    handle jKeyStorePassword = java:fromString(keyStorePassword);
    handle jTokenIssuer = java:fromString(tokenIssuer);

    //to maintain backward compatibility
    return jLoadJWTGeneratorClass(jClassName, jDialectURI, jSignatureAlgorithm, jKeyStorePath, jKeyStorePassword,
                                    jCertificateAlias, jPrivateKeyAlias, tokenExpiry, restrictedClaims, enabledCaching,
                                    cacheExpiry, jTokenIssuer, tokenAudience);
}

public function loadClaimRetrieverClass (string className, string unresolvedTrustStorePath, string trustStorePassword,
                                        map<any> properties) returns boolean {
    handle trustStorePath = jGetKeystoreLocation(java:fromString(unresolvedTrustStorePath));
    return jLoadClaimRetrieverClass (java:fromString(className), trustStorePath,
                                    java:fromString(trustStorePassword), properties);
}

public function retrieveClaimsFromImpl (UserClaimRetrieverContextDTO userInfo) returns RetrievedUserClaimsListDTO? {
    return jRetrieveClaims(userInfo);
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

# Invoke the interop function to generate JWT token
#
# + jwtInfo - payload of the authentication token
# + apiDetails - details of the subscribed APIS
# + return - Returns the generated JWT token.
public function generateJWTTokenFromUserClaimsMap(ClaimsMapDTO jwtInfo, map<string> apiDetails) returns (handle | error) {
    return jGenerateJWTTokenFromUserClaimsMap(jwtInfo, apiDetails);
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
public function jLoadJWTGeneratorClass(handle className, handle dialectURI, handle signatureAlgorithm,
                                        handle keyStorePath, handle keyStorePassword, handle certificateAlias,
                                        handle privateKeyAlias, int tokenExpiry, any[] restrictedClaims,
                                        boolean enabledCaching, int cacheExpiry, handle tokenIssuer,
                                        any[] tokenAudience) returns boolean = @java:Method {
    name: LOAD_JWT_GENERATOR_METHOD_NAME,
    class: JWT_GENERATOR_INVOKER_CLASS_PATH
} external;

# Interop function to resolves the keystore path
#
# + unresolvedPath - unresolved keystore path
# + return - Returns the resolved keystore path.
public function jGetKeystoreLocation(handle unresolvedPath) returns handle = @java:Method {
    name: INVOKE_GET_KEY_STORE_PATH_METHOD_NAME,
    class: JWT_GENERATOR_INVOKER_CLASS_PATH
} external;

# Interop function to generate JWT token
#
# + jwtInfo - payload of the authentication token
# + apiDetails - details of the subscribed APIS
# + return - Returns the generated JWT token.
public function jGenerateJWTToken(jwt:JwtPayload jwtInfo, map<string> apiDetails) returns (handle | error) = @java:Method {
    name: INVOKE_GENERATE_TOKEN_METHOD_NAME,
    class: JWT_GENERATOR_INVOKER_CLASS_PATH
} external;

# Interop function to generate JWT token
#
# + jwtInfo - payload of the authentication token
# + apiDetails - details of the subscribed APIS
# + return - Returns the generated JWT token.
public function jGenerateJWTTokenFromUserClaimsMap(ClaimsMapDTO jwtInfo, map<string> apiDetails) returns (handle | error) = @java:Method {
    name: INVOKE_GENERATE_TOKEN_METHOD_NAME,
    class: JWT_GENERATOR_INVOKER_CLASS_PATH
} external;

function jLoadClaimRetrieverClass (handle className, handle trustStorePath, handle trustStorePassword,
                                    map<any> configuration) returns boolean = @java:Method {
    name: LOAD_CLAIM_RETRIEVER_CLASS_METHOD_NAME,
    class: JWT_GENERATOR_INVOKER_CLASS_PATH
} external;

function jRetrieveClaims (UserClaimRetrieverContextDTO userInfo) returns RetrievedUserClaimsListDTO? = @java:Method {
    name: GET_RETRIEVED_CLAIMS_METHOD_NAME,
    class: JWT_GENERATOR_INVOKER_CLASS_PATH
} external;
