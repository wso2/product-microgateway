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

import ballerina/http;
import ballerina/jwt;
import ballerina/runtime;

# Representation of the jwt self validating handler
#
# + jwtAuthProvider - The reference to the jwt auth provider instance
public type JWTAuthHandler object {

    *http:InboundAuthHandler;

    public JwtAuthProvider jwtAuthProvider;

    public function __init(JwtAuthProvider jwtAuthProvider) {
        self.jwtAuthProvider = jwtAuthProvider;
    }

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString();
        if (req.hasHeader(authHeader)) {
            string headerValue = req.getHeader(authHeader).toLowerAscii();
            if (headerValue.startsWith(AUTH_SCHEME_BEARER_LOWERCASE)) {
                string credential = headerValue.substring(6, headerValue.length()).trim();
                string[] splitContent = split(credential, "\\.");
                if (splitContent.length() == 3) {
                    printDebug(KEY_AUTHN_FILTER, "Request will authenticated via jwt handler");
                    return true;
                }
            }
        }
        return false;
    }

    # Authenticates the incoming request with the use of credentials passed as the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns @tainted boolean | http:AuthenticationError {
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString();
        string headerValue = req.getHeader(authHeader);
        string credential = headerValue.substring(6, headerValue.length()).trim();
        var authenticationResult = self.jwtAuthProvider.authenticate(credential);
        if (authenticationResult is boolean) {
            var generationStatus = generateAndSetBackendJwtHeader(credential, req);
            if (generationStatus is boolean) {
                return authenticationResult;
            } else {
                return prepareAuthenticationError("JWT generation process failed.");
            }
        } else {
            return prepareAuthenticationError("Failed to authenticate with jwt bearer auth handler.", authenticationResult);
        }
    }
};

# Identify the api details from the subscribed apis in the authentication token.
#
# + payload - The payload of the authentication token
# + return - Returns map<string> with the extracted details.
public function checkSubscribedAPIs(jwt:JwtPayload payload) returns map<string> {
    map<string> subscriptionDetails = {
        apiName: "",
        apiContext: "",
        apiVersion: "",
        apiTier: "",
        apiPublisher: "",
        subscriberTenantDomain: ""
    };
    json subscribedAPIList = [];
    map<json>? customClaims = payload?.customClaims;
    //get allowed apis
    if (customClaims is map<json> && customClaims.hasKey(SUBSCRIBED_APIS)) {
        printDebug(KEY_JWT_AUTH_PROVIDER, "subscribedAPIs claim found in the jwt.");
        subscribedAPIList = customClaims.get(SUBSCRIBED_APIS);
    }
    if (subscribedAPIList is json[]) {
        APIConfiguration? apiConfig = apiConfigAnnotationMap[runtime:getInvocationContext().attributes[http:SERVICE_NAME].toString()];
        if (apiConfig is APIConfiguration) {
            string apiName = apiConfig.name;
            string apiVersion = apiConfig.apiVersion;
            printDebug(KEY_JWT_AUTH_PROVIDER, "Current API name: " + apiName + ", current version: " + apiVersion);
            int l = subscribedAPIList.length();
            int index = 0;
            while (index < l) {
                var subscription = subscribedAPIList[index];
                if (subscription.name.toString() == apiName && subscription.'version.toString() == apiVersion) {
                    // API is found in the subscribed APIs
                    if (isDebugEnabled) {
                        printDebug(KEY_JWT_AUTH_PROVIDER, "Found the API in subscribed APIs:" + subscription.name.toString()
                            + " version:" + subscription.'version.toString());
                    }
                    if (subscription.name is json) {
                        subscriptionDetails["apiName"] = subscription.name.toString();
                    }
                    if (subscription.'version is json) {
                        subscriptionDetails["apiVersion"] = subscription.'version.toString();
                    }
                    if (subscription.context is json) {
                        subscriptionDetails["apiContext"] = subscription.context.toString();
                    }
                    if (subscription.subscriptionTier is json) {
                        subscriptionDetails["apiTier"] = subscription.subscriptionTier.toString();
                    }
                    if (subscription.publisher is json) {
                        subscriptionDetails["apiPublisher"] = subscription.publisher.toString();
                    }
                    if (subscription.subscriberTenantDomain is json) {
                        subscriptionDetails["subscriberTenantDomain"] = subscription.subscriberTenantDomain.toString();
                    }
                }
                index += 1;
            }
        }
    }
    return subscriptionDetails;
}

# Generate the backend JWT token and set to the header of the outgoing request.
#
# + credential - Credential
# + req - The `Request` instance.
# + return - Returns `true` if the token generation and setting the header completed successfully
# or the `AuthenticationError` in case of an error.
public function generateAndSetBackendJwtHeader(string credential, http:Request req) returns @tainted (boolean | http:AuthenticationError){
    boolean enabledJWTGenerator = getConfigBooleanValue(JWT_GENERATOR_ID,
                                                        JWT_GENERATOR_ENABLED,
                                                        DEFAULT_JWT_GENERATOR_ENABLED);
    if (enabledJWTGenerator) {
        (boolean | http:AuthenticationError) status = false;
        handle jDialectURI = java:fromString(getConfigValue(JWT_GENERATOR_ID,
                                                            JWT_GENERATOR_DIALECT,
                                                            DEFAULT_JWT_GENERATOR_DIALECT));
        handle jSignatureAlgorithm = java:fromString(getConfigValue(JWT_GENERATOR_ID,
                                                                    JWT_GENERATOR_SIGN_ALGO,
                                                                    DEFAULT_JWT_GENERATOR_SIGN_ALGO));
        int jTokenExpiry = getConfigIntValue(JWT_GENERATOR_ID,
                                                JWT_GENERATOR_TOKEN_EXPIRY,
                                                DEFAULT_JWT_GENERATOR_TOKEN_EXPIRY);
        handle jRestrictedClaims = java:fromString(getConfigValue(JWT_GENERATOR_ID,
                                                                    JWT_GENERATOR_RESTRICTED_CLAIMS,
                                                                    DEFAULT_JWT_GENERATOR_RESTRICTED_CLAIMS));
        handle keyStoreLocationUnresolved = java:fromString(getConfigValue(LISTENER_CONF_INSTANCE_ID,
                                                                            KEY_STORE_PATH,
                                                                            DEFAULT_KEY_STORE_PATH));
        handle jKeyStorePath = getKeystoreLocation(keyStoreLocationUnresolved);
        handle jKeyStorePassword = java:fromString(getConfigValue(LISTENER_CONF_INSTANCE_ID,
                                                                    KEY_STORE_PASSWORD,
                                                                    DEFAULT_KEY_STORE_PASSWORD));
        handle jTokenIssuer = java:fromString(getConfigValue(JWT_GENERATOR_ID,
                                                                JWT_GENERATOR_TOKEN_ISSUER,
                                                                DEFAULT_JWT_GENERATOR_TOKEN_ISSUER));
        handle jTokenAudience = java:fromString(getConfigValue(JWT_GENERATOR_ID,
                                                                JWT_GENERATOR_TOKEN_AUDIENCE,
                                                                DEFAULT_JWT_GENERATOR_TOKEN_AUDIENCE));
        int skewTime = getConfigIntValue(JWT_GENERATOR_ID,
                                            JWT_GENERATOR_SKEW_TIME,
                                            DEFAULT_JWT_GENERATOR_SKEW_TIME);
        (jwt:JwtPayload | error) payload = getDecodedJWTPayload(credential);
        if (payload is jwt:JwtPayload) {
            printDebug(KEY_JWT_AUTH_PROVIDER, "decoded token credential");
            boolean enabledCaching = getConfigBooleanValue(JWT_GENERATOR_CACHING_ID,
                                                            JWT_GENERATOR_TOKEN_CACHE_ENABLED,
                                                            DEFAULT_JWT_GENERATOR_TOKEN_CACHE_ENABLED);
            int cacheExpiry = getConfigIntValue(JWT_GENERATOR_CACHING_ID,
                                                JWT_GENERATOR_TOKEN_CACHE_EXPIRY,
                                                DEFAULT_JWT_GENERATOR_TOKEN_CACHE_EXPIRY);
            // get the subscribedAPI details
            map<string> apiDetails = checkSubscribedAPIs(payload);
            // checking if cache is enabled
            if (enabledCaching) {
                var cachedToken = jwtGeneratorCache.get(credential);
                if (cachedToken is string) {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "Found in jwt generator cache");
                    printDebug(KEY_JWT_AUTH_PROVIDER, "Token: " + cachedToken);
                    (jwt:JwtPayload | error) cachedPayload = getDecodedJWTPayload(cachedToken);
                    if (cachedPayload is jwt:JwtPayload) {
                        int currentTime = getCurrentTime();
                        int? cachedTokenExpiry = cachedPayload?.exp;
                        if (cachedTokenExpiry is int) {
                            cachedTokenExpiry = cachedTokenExpiry * 1000;
                            int difference = (cachedTokenExpiry - currentTime);
                            if (difference < skewTime) {
                                printDebug(KEY_JWT_AUTH_PROVIDER, "JWT regenerated because of the skew time");
                                status =setJWTHeader(jDialectURI, jSignatureAlgorithm, jKeyStorePath, jKeyStorePassword, jTokenExpiry, jRestrictedClaims,
                                            payload, req, credential, enabledCaching, cacheExpiry, jTokenIssuer, jTokenAudience, apiDetails);
                            } else {
                                req.setHeader(jwtheaderName, cachedToken);
                                status = true;
                            }
                        } else {
                            printDebug(KEY_JWT_AUTH_PROVIDER, "Failed to read exp from cached token");
                            return prepareAuthenticationError("Failed to read exp claim from cached token");
                        }
                    }
                } else {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "Could not find in the jwt generator cache");
                    status = setJWTHeader(jDialectURI, jSignatureAlgorithm, jKeyStorePath, jKeyStorePassword, jTokenExpiry, jRestrictedClaims,
                                        payload, req, credential, enabledCaching, cacheExpiry, jTokenIssuer, jTokenAudience, apiDetails);
                }
            } else {
                printDebug(KEY_JWT_AUTH_PROVIDER, "JWT generator caching is disabled");
                status = setJWTHeader(jDialectURI, jSignatureAlgorithm, jKeyStorePath, jKeyStorePassword, jTokenExpiry, jRestrictedClaims,
                                    payload, req, credential, enabledCaching, cacheExpiry, jTokenIssuer, jTokenAudience, apiDetails);
            }
        } else {
            return prepareAuthenticationError("Failed to read JWT token");
        }
        return status;
    } else {
        printDebug(KEY_JWT_AUTH_PROVIDER, "JWT Generator is disabled");
        return true;
    }
}

# Refactoring method for setting JWT header
#
# + jDialectURI - DialectURI for the standard claims
# + jSignatureAlgorithm - Signature algorithm to sign the JWT
# + jKeyStorePath - Keystore path
# + jKeyStorePassword - Keystore password
# + jTokenExpiry - Token expiry value
# + jRestrictedClaims - Restricted claims from the configuration
# + payload - The payload of the authentication token
# + req - The `Request` instance.
# + credential - Credential
# + enabledCaching - jwt generator caching enabled
# + cacheExpiry - jwt generator cache expiry
# + jTokenIssuer - token issuer for the claims
# + jTokenAudience - token audience for the claims
# + apiDetails - extracted api details for the current api
# + return - Returns `true` if the token generation and setting the header completed successfully
# or the `AuthenticationError` in case of an error.
public function setJWTHeader(handle jDialectURI, handle jSignatureAlgorithm, handle jKeyStorePath, handle jKeyStorePassword,
                            int jTokenExpiry, handle jRestrictedClaims, jwt:JwtPayload payload, http:Request req, string credential,
                            boolean enabledCaching, int cacheExpiry, handle jTokenIssuer, handle jTokenAudience, map<string> apiDetails)
                            returns @tainted (boolean | http:AuthenticationError) {
    handle generatorClass = java:fromString(getConfigValue(JWT_GENERATOR_ID,
                                                            JWT_GENERATOR_IMPLEMENTATION,
                                                            DEFAULT_JWT_GENERATOR_IMPLEMENTATION));
    boolean classLoaded = loadJWTGeneratorClass(generatorClass,
                                                jDialectURI,
                                                jSignatureAlgorithm,
                                                jKeyStorePath,
                                                jKeyStorePassword,
                                                jTokenExpiry,
                                                jRestrictedClaims,
                                                enabledCaching,
                                                cacheExpiry,
                                                jTokenIssuer,
                                                jTokenAudience,
                                                apiDetails);
    if (classLoaded) {
        handle generatedToken = generateJWTToken(payload);

        printDebug(KEY_JWT_AUTH_PROVIDER, "Generated jwt token");
        printDebug(KEY_JWT_AUTH_PROVIDER, "Token: " + generatedToken.toString());

        // add to cache if cache enabled
        if (enabledCaching) {
            jwtGeneratorCache.put(credential, generatedToken.toString());
            printDebug(KEY_GW_CACHE, "Added to jwt generator token cache.");
        }
        req.setHeader(jwtheaderName, generatedToken.toString());
        return true;
    } else {
        return prepareAuthenticationError("Failed to load JWT token generator class");
    }
}

# To invoke the interop function to create instance of JWT generator
#
# + className - className for the jwtgenerator implementation
# + dialectURI - DialectURI for the standard claims
# + signatureAlgorithm - Signature algorithm to sign the JWT
# + keyStorePath - Keystore path
# + keyStorePassword - Keystore password
# + tokenExpiry - Token expiry value
# + restrictedClaims - Restricted claims from the configuration
# + enabledCaching - jwt generator caching enabled
# + cacheExpiry - jwt generator cache expiry
# + tokenIssuer - token issuer for the claims
# + tokenAudience - token audience for the claims
# + apiDetails - extracted api details for the current api
# + return - Returns `true` if the class is created successfully. or `false` if unsuccessful.
public function loadJWTGeneratorClass(handle className,
                                        handle dialectURI,
                                        handle signatureAlgorithm,
                                        handle keyStorePath,
                                        handle keyStorePassword,
                                        int tokenExpiry,
                                        handle restrictedClaims,
                                        boolean enabledCaching,
                                        int cacheExpiry,
                                        handle tokenIssuer,
                                        handle tokenAudience,
                                        map<string> apiDetails) returns boolean {
    return jLoadJWTGeneratorClass(className,
                                    dialectURI,
                                    signatureAlgorithm,
                                    keyStorePath,
                                    keyStorePassword,
                                    tokenExpiry,
                                    restrictedClaims,
                                    enabledCaching,
                                    cacheExpiry,
                                    tokenIssuer,
                                    tokenAudience,
                                    apiDetails);
}

# Interop function to create instance of JWTGenerator
#
# + className - className for the jwtgenerator implementation
# + dialectURI - DialectURI for the standard claims
# + signatureAlgorithm - Signature algorithm to sign the JWT
# + keyStorePath - Keystore path
# + keyStorePassword - Keystore password
# + tokenExpiry - Token expiry value
# + restrictedClaims - Restricted claims from the configuration
# + enabledCaching - jwt generator caching enabled
# + cacheExpiry - jwt generator cache expiry
# + tokenIssuer - token issuer for the claims
# + tokenAudience - token audience for the claims
# + apiDetails - extracted api details for the current api
# + return - Returns `true` if the class is created successfully. or `false` if unsuccessful.
public function jLoadJWTGeneratorClass(handle className,
                                        handle dialectURI,
                                        handle signatureAlgorithm,
                                        handle keyStorePath,
                                        handle keyStorePassword,
                                        int tokenExpiry,
                                        handle restrictedClaims,
                                        boolean enabledCaching,
                                        int cacheExpiry,
                                        handle tokenIssuer,
                                        handle tokenAudience,
                                        map<string> apiDetails) returns boolean = @java:Method {
    name: "loadJWTGeneratorClass",
    class: "org.wso2.micro.gateway.core.jwtgenerator.MGWJWTGeneratorInvoker"
} external;

# Invoke the interop function to resolves the keystore path
#
# + unresolvedPath - unresolved keystore path
# + return - Returns the resolved keystore path.
public function getKeystoreLocation(handle unresolvedPath) returns handle {
    return jGetKeystoreLocation(unresolvedPath);
}

# Interop function to resolves the keystore path
#
# + unresolvedPath - unresolved keystore path
# + return - Returns the resolved keystore path.
public function jGetKeystoreLocation(handle unresolvedPath) returns handle = @java:Method {
    name: "invokeGetKeystorePath",
    class: "org.wso2.micro.gateway.core.jwtgenerator.MGWJWTGeneratorInvoker"
} external;

# Invoke the interop function to generate JWT token
#
# + jwtInfo - payload of the authentication token
# + return - Returns the generated JWT token.
public function generateJWTToken(jwt:JwtPayload jwtInfo) returns handle {
    return jGenerateJWTToken(jwtInfo);
}

# Interop function to generate JWT token
#
# + jwtInfo - payload of the authentication token
# + return - Returns the generated JWT token.
public function jGenerateJWTToken(jwt:JwtPayload jwtInfo) returns handle = @java:Method {
    name: "invokeGenerateToken",
    class: "org.wso2.micro.gateway.core.jwtgenerator.MGWJWTGeneratorInvoker"
} external;