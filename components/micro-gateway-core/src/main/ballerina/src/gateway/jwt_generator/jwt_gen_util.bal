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

import ballerina/runtime;
import ballerina/jwt;
import ballerina/http;

boolean jwtGeneratorClassLoaded = loadJWTGeneratorImpl();
boolean claimRetrieverClassLoaded = loadClaimRetrieverImpl();

# Setting backend JWT header when there is no JWT Token is present.
#
# + req - The `Request` instance.
# + cacheKey - key for the jwt generator cache
# + enabledCaching - jwt generator caching enabled
# + generatedToken - generated Backend JWT
# + return - Returns `true` if the token generation and setting the header completed successfully
function setGeneratedTokenAsHeader(http:Request req,
                                string cacheKey,
                                boolean enabledCaching,
                                handle | error generatedToken)
                                returns @tainted boolean {

    if (generatedToken is error) {
        printError(KEY_JWT_AUTH_PROVIDER, "Token not generated due to error", generatedToken);
        return false;
    } else {
        printDebug(KEY_JWT_AUTH_PROVIDER, "Generated jwt token");
        printDebug(KEY_JWT_AUTH_PROVIDER, "Token: " + generatedToken.toString());

        if (enabledCaching) {
            error? err = jwtGeneratorCache.put(<@untainted>cacheKey, <@untainted>generatedToken.toString());
            if (err is error) {
                printError(KEY_JWT_AUTH_PROVIDER, "Error while adding entry to jwt generator cache", err);
            }
            printDebug(KEY_JWT_AUTH_PROVIDER, "Added to jwt generator token cache.");
        }
        req.setHeader(jwtheaderName, generatedToken.toString());
        return true;
    }
}

# populate and return ClaimsMapDTO object which is required to the further processing of Jwt generator implementation.
# + tokenContextDTO - BackendJWTGenUserContextDTO record which contains payload, issuer and whether claim retrieval enabled.
# + return - ClaimsMapDTO
function createMapFromRetrievedUserClaimsListDTO(BackendJWTGenUserContextDTO tokenContextDTO)
                                                    returns @tainted ClaimsMapDTO {
    ClaimsMapDTO claimsMapDTO = {};
    CustomClaimsMapDTO customClaimsMapDTO = {};

    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
    jwt:JwtPayload? payload = tokenContextDTO.payload;

    runtime:Principal? principal = invocationContext?.principal;
    if (principal is runtime:Principal) {
        string[]? scopes = principal?.scopes;
        if (scopes is string[]) {
            string concatenatedScope = "";
            foreach string scope in scopes {
                concatenatedScope += scope + " ";
            }
            customClaimsMapDTO["scope"] = concatenatedScope.trim();
        }
        map<any>? customClaims = principal?.claims;
        if (customClaims is map<any>) {
            foreach var [key, value] in customClaims.entries() {
                if (value is anydata) {
                    customClaimsMapDTO[key] = value;
                }
            }
        }
        if (tokenContextDTO.remoteUserClaimRetrievalEnabled) {
            UserClaimRetrieverContextDTO? userInfo = generateUserClaimRetrieverContextFromPrincipal(authContext,
                                                                                                    principal,
                                                                                                    tokenContextDTO.issuer,
                                                                                                    tokenContextDTO.payload != ());
            if (userInfo is UserClaimRetrieverContextDTO) {
                RetrievedUserClaimsListDTO ? claimsListDTO = retrieveClaims(userInfo);
                if (claimsListDTO is RetrievedUserClaimsListDTO) {
                    ClaimDTO[] claimList = claimsListDTO.list;
                    foreach ClaimDTO claim in claimList {
                        customClaimsMapDTO[claim.uri] = claim.value;
                    }
                }
            }
        }
    } else {
        printDebug(JWT_GEN_UTIL, "Claim retrieval implementation is not executed due to the unavailability " +
                        "of the principal component");
    }

    ApplicationClaimsMapDTO applicationClaimsMapDTO = {};
    applicationClaimsMapDTO.id = emptyStringIfUnknownValue(authContext.applicationId);
    applicationClaimsMapDTO.owner = emptyStringIfUnknownValue(authContext.subscriber);
    applicationClaimsMapDTO.name = emptyStringIfUnknownValue(authContext.applicationName);
    applicationClaimsMapDTO.tier = emptyStringIfUnknownValue(authContext.applicationTier);

    customClaimsMapDTO.application = applicationClaimsMapDTO;
    claimsMapDTO.iss = tokenContextDTO.issuer;
    claimsMapDTO.sub = emptyStringIfUnknownValue(authContext.username);
    claimsMapDTO.customClaims = customClaimsMapDTO;
    return claimsMapDTO;
}

# Populate Map to keep API related information for JWT generation process.
#
# + return - String map with the properties: apiName, apiVersion, apiTier, apiContext, apiPublisher and
#               subscriberTenantDomain
function createAPIDetailsMap () returns map<string> {
    map<string> apiDetails = {};
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    AuthenticationContext authenticationContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
    APIConfiguration? apiConfig = apiConfigAnnotationMap[<string>invocationContext.attributes[http:SERVICE_NAME]];
    if (apiConfig is APIConfiguration) {
        apiDetails["apiName"] = apiConfig.name;
        apiDetails["apiVersion"] = apiConfig.apiVersion;
        apiDetails["apiTier"] = authenticationContext.tier;
        apiDetails["apiContext"] = <string> invocationContext.attributes[API_CONTEXT];
        apiDetails["apiPublisher"] = apiConfig.publisher;
        apiDetails["subscriberTenantDomain"] = authenticationContext.subscriberTenantDomain;
    }
    return apiDetails;
}

function emptyStringIfUnknownValue (string value) returns string {
    return value != UNKNOWN_VALUE ? value : "";
}

# Load JWT Generator Implementation if the JWT generation is enabled.
# + return - `true` if the class loading is successful.
public function loadJWTGeneratorImpl() returns boolean {
    GatewayConf gatewayConf = getGatewayConfInstance();
    boolean enabledJWTGenerator = gatewayConf.jwtGeneratorConfig.jwtGeneratorEnabled;
    if (enabledJWTGenerator) {
        string generatorClass = gatewayConf.jwtGeneratorConfig.generatorImpl;
        string dialectURI = gatewayConf.jwtGeneratorConfig.dialectURI;
        string signatureAlgorithm = gatewayConf.jwtGeneratorConfig.signingAlgorithm;
        string certificateAlias = gatewayConf.jwtGeneratorConfig.certificateAlias;
        string privateKeyAlias = gatewayConf.jwtGeneratorConfig.privateKeyAlias;
        int tokenExpiry = gatewayConf.jwtGeneratorConfig.tokenExpiry;
        any[] restrictedClaims = gatewayConf.jwtGeneratorConfig.restrictedClaims;
        string keyStoreLocationUnresolved = gatewayConf.listenerConfig.keyStorePath;
        string keyStorePassword = gatewayConf.listenerConfig.keyStorePassword;
        string tokenIssuer = gatewayConf.jwtGeneratorConfig.issuer;
        any[] tokenAudience = gatewayConf.jwtGeneratorConfig.tokenAudience;
        // provide backward compatibility for skew time
        int skewTime = getConfigIntValue(SERVER_CONF_ID,
                                            SERVER_TIMESTAMP_SKEW,
                                            DEFAULT_SERVER_TIMESTAMP_SKEW);
        if (skewTime == DEFAULT_SERVER_TIMESTAMP_SKEW) {
            skewTime = getConfigIntValue(KM_CONF_INSTANCE_ID,
                                            TIMESTAMP_SKEW,
                                            DEFAULT_TIMESTAMP_SKEW);
        }
        boolean enabledCaching = gatewayConf.jwtGeneratorConfig.jwtGeneratorCaching.tokenCacheEnable;
        int cacheExpiry = gatewayConf.jwtGeneratorConfig.jwtGeneratorCaching.tokenCacheExpiryTime;

        return loadJWTGeneratorClass(generatorClass,
                                    dialectURI,
                                    signatureAlgorithm,
                                    keyStoreLocationUnresolved,
                                    keyStorePassword,
                                    certificateAlias,
                                    privateKeyAlias,
                                    tokenExpiry,
                                    restrictedClaims,
                                    enabledCaching,
                                    cacheExpiry,
                                    tokenIssuer,
                                    tokenAudience);
    }
    return false;
}

# Refactoring method for setting JWT header
#
# + tokenContextDTO - BackendJWTGenUserContextDTO record which contains payload, issuer and whether claim retrieval enabled.
# + apiDetails - extracted api details for the current api
# + req - The `Request` instance.
# + cacheKey - key for the jwt generator cache
# + enabledCaching - jwt generator caching enabled
# + return - Returns `true` if the token generation and setting the header completed successfully
public function setJWTHeader(BackendJWTGenUserContextDTO tokenContextDTO,
                            map<string> apiDetails,
                            http:Request req,
                            string cacheKey,
                            boolean enabledCaching) returns @tainted boolean {

    (handle|error) generatedToken;
    jwt:JwtPayload? payload = tokenContextDTO.payload;
    if (payload is jwt:JwtPayload) {
        if (isSelfContainedToken(payload)) {
            generatedToken = generateJWTToken(payload, apiDetails);
            return setGeneratedTokenAsHeader(req, cacheKey, enabledCaching, generatedToken);
        }
    }
    ClaimsMapDTO claimsMapDTO = createMapFromRetrievedUserClaimsListDTO(tokenContextDTO);
    generatedToken = generateJWTTokenFromUserClaimsMap(claimsMapDTO, apiDetails);
    if (generatedToken is error) {
        printError(KEY_JWT_AUTH_PROVIDER, "Token not generated due to error", generatedToken);
        return false;
    }
    return setGeneratedTokenAsHeader(req, cacheKey, enabledCaching, generatedToken);
}

# Set the BackendJWT header after checking in the cache.
# The JWT token is picked from cache if it is available in the cache and the time duration between current time
# and the cached token's expiry time is less than the skew time.
# If the token is not picked from the cache, then it will be generated.
#
# + req - HTTP Request
# + cacheKey - Cache key used for jwt generator cache
# + enabledCaching - True if caching is enabled for backend JWT generation
# + tokenContextDTO - User Context which is used to populate the information required for jwtGenerator Implementation
# + apiDetails - API related information which is used to populate the information required for jwtGenerator Implementation
# + return - Returns `true` if adding the JWT token to the request is successful.
function setJWTTokenWithCacheCheck(http:Request req,
                                    string cacheKey,
                                    int skewTime,
                                    boolean enabledCaching,
                                    BackendJWTGenUserContextDTO tokenContextDTO,
                                    map<string> apiDetails)
                                    returns @tainted boolean {
    boolean status = false;
    if (enabledCaching) {
        var cachedToken = jwtGeneratorCache.get(cacheKey);
        printDebug(KEY_JWT_AUTH_PROVIDER, "Key: " + cacheKey);
        if (cachedToken is string) {
            printDebug(KEY_JWT_AUTH_PROVIDER, "Found in jwt generator cache");
            printDebug(KEY_JWT_AUTH_PROVIDER, "Token: " + cachedToken);
            (jwt:JwtPayload | error) cachedPayload = getDecodedJWTPayload(cachedToken, tokenContextDTO.issuer);
            if (cachedPayload is jwt:JwtPayload) {
                int currentTime = getCurrentTime();
                int? cachedTokenExpiry = cachedPayload?.exp;
                if (cachedTokenExpiry is int) {
                    cachedTokenExpiry = cachedTokenExpiry * 1000;
                    int difference = (cachedTokenExpiry - currentTime);
                    if (difference < skewTime) {
                        printDebug(KEY_JWT_AUTH_PROVIDER, "JWT regenerated because of the skew time");
                        status = setJWTHeader(tokenContextDTO, apiDetails, req, cacheKey, enabledCaching);
                    } else {
                        req.setHeader(jwtheaderName, cachedToken);
                        status = true;
                    }
                } else {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "Failed to read exp from cached token");
                    return false;
                }
            }
        } else {
            printDebug(KEY_JWT_AUTH_PROVIDER, "Could not find in the jwt generator cache");
            status = setJWTHeader(tokenContextDTO, apiDetails, req, cacheKey, enabledCaching);
        }
    } else {
        printDebug(KEY_JWT_AUTH_PROVIDER, "JWT generator caching is disabled");
        status = setJWTHeader(tokenContextDTO, apiDetails, req, cacheKey, enabledCaching);
    }
    return status;
}
