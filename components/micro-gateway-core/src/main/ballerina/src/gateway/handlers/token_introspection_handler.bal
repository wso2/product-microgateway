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
import ballerina/http;
import ballerina/runtime;
import ballerina/oauth2;
import ballerina/jwt;

# Representation of the key validation  handler
#
# + oauth2KeyValidationProvider - The reference to the key validation provider instance
# + introspectProvider - The reference to the standard oauth2 introspect service provider
public type KeyValidationHandler object {

    *http:InboundAuthHandler;

    public OAuth2KeyValidationProvider oauth2KeyValidationProvider;
    public oauth2:InboundOAuth2Provider introspectProvider;
    private boolean validateSubscriptions;
    private boolean enabledJWTGenerator = false;
    private boolean classLoaded = false;
    private int skewTime = 0;
    private boolean enabledCaching = false;
    private string issuer = "";
    private boolean remoteUserClaimRetrievalEnabled = false;

    public function __init(OAuth2KeyValidationProvider oauth2KeyValidationProvider, oauth2:InboundOAuth2Provider introspectProvider) {
        self.oauth2KeyValidationProvider = oauth2KeyValidationProvider;
        self.introspectProvider = introspectProvider;
        self.validateSubscriptions = getConfigBooleanValue(SECURITY_INSTANCE_ID, SECURITY_VALIDATE_SUBSCRIPTIONS, DEFAULT_VALIDATE_SUBSCRIPTIONS);
        self.issuer = getConfigValue(KM_CONF_INSTANCE_ID, KM_CONF_ISSUER, DEFAULT_KM_CONF_ISSUER);
        self.enabledJWTGenerator = getConfigBooleanValue(JWT_GENERATOR_ID,
                                                            JWT_GENERATOR_ENABLED,
                                                            DEFAULT_JWT_GENERATOR_ENABLED);
        if (self.enabledJWTGenerator) {
            // provide backward compatibility for skew time
            self.skewTime = getConfigIntValue(SERVER_CONF_ID,
                                                SERVER_TIMESTAMP_SKEW,
                                                DEFAULT_SERVER_TIMESTAMP_SKEW);
            if (self.skewTime == DEFAULT_SERVER_TIMESTAMP_SKEW) {
                self.skewTime = getConfigIntValue(KM_CONF_INSTANCE_ID,
                                                    TIMESTAMP_SKEW,
                                                    DEFAULT_TIMESTAMP_SKEW);
            }
            self.enabledCaching = getConfigBooleanValue(JWT_GENERATOR_CACHING_ID,
                                                            JWT_GENERATOR_TOKEN_CACHE_ENABLED,
                                                            DEFAULT_JWT_GENERATOR_TOKEN_CACHE_ENABLED);

            self.classLoaded = jwtGeneratorClassLoaded;
            self.remoteUserClaimRetrievalEnabled = getConfigBooleanValue(KM_CONF_INSTANCE_ID,
                                                                        REMOTE_USER_CLAIM_RETRIEVAL_ENABLED,
                                                                        DEFAULT_JWT_REMOTE_USER_CLAIM_RETRIEVAL_ENABLED);
        }
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
                if (splitContent.length() < 3) {
                    printDebug(KEY_AUTHN_FILTER, "Request will authenticated via key validation service");
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
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        string authHeader = invocationContext.attributes[AUTH_HEADER].toString();
        string headerValue = req.getHeader(authHeader);
        string credential = <@untainted>headerValue.substring(6, headerValue.length()).trim();
        string authHeaderName = getAuthorizationHeader(invocationContext);
        APIConfiguration? apiConfig = apiConfigAnnotationMap[<string>invocationContext.attributes[http:SERVICE_NAME]];
        boolean|auth:Error authenticationResult = false;
        authenticationResult = self.introspectProvider.authenticate(credential);
        if (authenticationResult is auth:Error) {
            return prepareAuthenticationError("Failed to authenticate with introspect auth provider.", authenticationResult);
        } else if (!authenticationResult) {
            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
            return authenticationResult;
        } else {
            runtime:Principal? principal = invocationContext?.principal;
            if (principal is runtime:Principal) {
                AuthenticationContext authenticationContext = {};
                authenticationContext.username = principal?.username ?: USER_NAME_UNKNOWN;
                string apiName = "";
                string apiVersion = "";

                if (apiConfig is APIConfiguration) {
                    apiName = apiConfig.name;
                    apiVersion = apiConfig.apiVersion;
                }
                map<any>? claims = principal?.claims;
                any clientId = claims[CLIENT_ID];
                boolean isAllowed = false;
                // If validateSubscription is true and clientID is present, do the subscription validation.
                if (clientId != () && clientId is string && self.validateSubscriptions) {
                   [authenticationContext, isAllowed] =
                     validateSubscriptionFromDataStores(credential, clientId, apiName, apiVersion,
                     self.validateSubscriptions);
                   authenticationContext.username = principal?.username ?: USER_NAME_UNKNOWN;
                   invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                   invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
                   if (isAllowed) {
                       boolean tokenGenStatus = generateAndSetBackendJwtHeaderOpaque (credential,
                                                                           req,
                                                                           authenticationContext,
                                                                           self.enabledJWTGenerator,
                                                                           self.classLoaded,
                                                                           self.skewTime,
                                                                           self.enabledCaching,
                                                                           self.issuer,
                                                                           self.remoteUserClaimRetrievalEnabled);
                       if (!tokenGenStatus) {
                           printError(KEY_AUTHN_FILTER, "Error while adding the Backend JWT header");
                       }
                   }
                   return isAllowed;    
                } else { // Otherwise return the introspection response.
                    authenticationContext.username = principal?.username ?: USER_NAME_UNKNOWN;
                    invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                    invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
                    if (authenticationResult) {
                        boolean tokenGenStatus = generateAndSetBackendJwtHeaderOpaque (credential,
                                                                            req,
                                                                            authenticationContext,
                                                                            self.enabledJWTGenerator,
                                                                            self.classLoaded,
                                                                            self.skewTime,
                                                                            self.enabledCaching,
                                                                            self.issuer,
                                                                            self.remoteUserClaimRetrievalEnabled);
                        if (!tokenGenStatus) {
                            printError(KEY_AUTHN_FILTER, "Error while adding the Backend JWT header");
                        }
                    }
                    return authenticationResult;
                }
            }
        }
        // Default return Invalid Credentials.
        setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
        return false;
    }
};

# Setting backend JWT header when there is no JWT Token is present.
#
# + req - The `Request` instance.
# + authContext - Authentication Context
# + cacheKey - cache Key
# + enabledCaching - enabled backend jwt caching
# + issuer - Issuer of the Key Manager
# + return - Returns `true` if the token generation and setting the header completed successfully
# or the `AuthenticationError` in case of an error.
public function setJWTHeaderForOpaque(http:Request req,
                                AuthenticationContext authContext,
                                string cacheKey,
                                boolean enabledCaching,
                                boolean remoteUserClaimRetrievalEnabled,
                                string issuer)
                                returns @tainted boolean {
    map<string> apiDetails = createAPIDetailsMap(runtime:getInvocationContext());
    (handle|error) generatedToken = generateBackendJWTTokenForOauth(authContext,
                                                                    apiDetails,
                                                                    remoteUserClaimRetrievalEnabled,
                                                                    issuer);
    if (generatedToken is error) {
        return false;
    } else {
        return setGeneratedTokenAsHeader(req, cacheKey, enabledCaching, generatedToken);
    }
}

# Setting backend JWT header when there is no JWT Token is present.
#
# + apiDetails - extracted api details for the current api
# + issuer - Issuer related to API Manager
# + return - JWT Token
# or the `AuthenticationError` in case of an error.
function generateBackendJWTTokenForOauth(AuthenticationContext authContext,
                                        map<string> apiDetails,
                                        boolean remoteUserClaimRetrievalEnabled,
                                        string issuer) returns handle | error {
    (handle|error) generatedToken;

    ClaimsMapDTO claimsMapDTO = createMapFromRetrievedUserClaimsListDTO(authContext, remoteUserClaimRetrievalEnabled, issuer);
    generatedToken = generateJWTTokenFromUserClaimsMap(claimsMapDTO, apiDetails);
    return generatedToken;
}

//todo: refactor the code since there functionalities shared with jwt_auth_handler
# Generate the backend JWT token and set to the header of the outgoing request.
#
# + credential - Credential
# + req - The `Request` instance.
# + enabledJWTGenerator - state of jwt generator
# + classLoaded - whether the class is loaded successfully
# + enabledCaching - jwt generator caching enabled
# + skewTime - skew time to backend
# + issuer - The jwt issuer who issued the token and comes in the iss claim.
# + remoteUserClaimRetrievalEnabled - true if remoteUserClaimRetrieval is enabled
# + return - Returns `true` if the token generation and setting the header completed successfully
# or the `AuthenticationError` in case of an error.
public function generateAndSetBackendJwtHeaderOpaque(string credential,
                                                        http:Request req,
                                                        AuthenticationContext authContext,
                                                        boolean enabledJWTGenerator,
                                                        boolean classLoaded,
                                                        int skewTime,
                                                        boolean enabledCaching,
                                                        string issuer,
                                                        boolean remoteUserClaimRetrievalEnabled)
                                                        returns @tainted boolean {
    if (enabledJWTGenerator) {
        if (classLoaded) {
            boolean status = false;
            string apiName = "";
            string apiVersion = "";
            APIConfiguration? apiConfig = apiConfigAnnotationMap[runtime:getInvocationContext().attributes[http:SERVICE_NAME].toString()];
            if (apiConfig is APIConfiguration) {
                apiName = apiConfig.name;
                apiVersion = apiConfig.apiVersion;
            }
            string cacheKey = credential + apiName + apiVersion;

            if (enabledCaching) {
                var cachedToken = jwtGeneratorCache.get(cacheKey);
                printDebug(KEY_JWT_AUTH_PROVIDER, "Key: " + cacheKey);
                if (cachedToken is string) {
                    printDebug(KEY_JWT_AUTH_PROVIDER, "Found in jwt generator cache");
                    printDebug(KEY_JWT_AUTH_PROVIDER, "Token: " + cachedToken);
                    (jwt:JwtPayload | error) cachedPayload = getDecodedJWTPayload(cachedToken, issuer);
                    if (cachedPayload is jwt:JwtPayload) {
                        int currentTime = getCurrentTime();
                        int? cachedTokenExpiry = cachedPayload?.exp;
                        if (cachedTokenExpiry is int) {
                            cachedTokenExpiry = cachedTokenExpiry * 1000;
                            int difference = (cachedTokenExpiry - currentTime);
                            if (difference < skewTime) {
                                printDebug(KEY_JWT_AUTH_PROVIDER, "JWT regenerated because of the skew time");
                                status = setJWTHeaderForOpaque(req, authContext, cacheKey, enabledCaching,
                                                                remoteUserClaimRetrievalEnabled, issuer);
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
                    status = setJWTHeaderForOpaque(req, authContext, cacheKey, enabledCaching,
                                                        remoteUserClaimRetrievalEnabled, issuer);
                }
            } else {
                printDebug(KEY_JWT_AUTH_PROVIDER, "JWT generator caching is disabled");
                    status = setJWTHeaderForOpaque(req, authContext, cacheKey, enabledCaching,
                                                        remoteUserClaimRetrievalEnabled, issuer);
            }

            return status;
        } else {
            printDebug(KEY_JWT_AUTH_PROVIDER, "Class loading failed");
            return false;
        }
    } else {
        printDebug(KEY_JWT_AUTH_PROVIDER, "JWT Generator is disabled");
        return true;
    }
}
