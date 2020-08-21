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
    private boolean isLegacyKM = false;

    public function __init(OAuth2KeyValidationProvider oauth2KeyValidationProvider, oauth2:InboundOAuth2Provider introspectProvider) {
        GatewayConf gatewayConf = getGatewayConfInstance();
        self.oauth2KeyValidationProvider = oauth2KeyValidationProvider;
        self.introspectProvider = introspectProvider;
        self.validateSubscriptions = getConfigBooleanValue(SECURITY_INSTANCE_ID, SECURITY_VALIDATE_SUBSCRIPTIONS, DEFAULT_VALIDATE_SUBSCRIPTIONS);
        self.isLegacyKM = getConfigBooleanValue(KM_CONF_INSTANCE_ID, KM_CONF_IS_LEGACY_KM, DEFAULT_KM_CONF_IS_LEGACY_KM);
        self.issuer = gatewayConf.getKeyManagerConf().issuer;
        self.enabledJWTGenerator = gatewayConf.jwtGeneratorConfig.jwtGeneratorEnabled;
        if (self.enabledJWTGenerator) {
            // provide backward compatibility for skew time
            self.skewTime = getConfigIntValue(SERVER_CONF_ID,
                                                SERVER_TIMESTAMP_SKEW,
                                                DEFAULT_SERVER_TIMESTAMP_SKEW);
            if (self.skewTime == DEFAULT_SERVER_TIMESTAMP_SKEW) {
                self.skewTime = gatewayConf.getKeyManagerConf().timestampSkew;
            }
            self.enabledCaching = gatewayConf.jwtGeneratorConfig.jwtGeneratorCaching.tokenCacheEnable;
            self.classLoaded = jwtGeneratorClassLoaded;
            self.remoteUserClaimRetrievalEnabled = gatewayConf.getKeyManagerConf().remoteUserClaimRetrievalEnabled;
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
        var tokenStatus = retrieveFromRevokedTokenMap(credential);
        if (tokenStatus is boolean && tokenStatus) {
            printDebug(KEY_AUTHN_FILTER, "Token is found in the invalid token map.");
            printDebug(KEY_AUTHN_FILTER, "Oauth Token has been revoked.");
            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
            return false;
        }
        if (self.isLegacyKM) {
            // In legacy mode, use the KeyValidation service for token validation.
            // This is used when API-M versions < 3.2 are being used to support backward compatibility
            authenticationResult = self.oauth2KeyValidationProvider.authenticate(credential);
            if (authenticationResult is auth:Error) {
                return prepareAuthenticationError("Failed to authenticate with key validation service.", authenticationResult);
            } else if (authenticationResult) {
                boolean tokenGenStatus = generateAndSetBackendJwtHeader(credential,
                                                                                  req,
                                                                                  self.enabledJWTGenerator,
                                                                                  self.classLoaded,
                                                                                  self.skewTime,
                                                                                  self.enabledCaching,
                                                                                  self.issuer,
                                                                                  self.remoteUserClaimRetrievalEnabled,
                                                                                  false);
                if (!tokenGenStatus) {
                    printError(KEY_AUTHN_FILTER, "Error while adding the Backend JWT header");
                }
                return authenticationResult;
            } else {
                return authenticationResult;
            }
        } else {
            // With any external key manager or APIM - 3.2.0, introspection endpoint is used to validate the token.
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
                    // If the client id is null and validate subscription is enabled, return auth failure error.
                    if (self.validateSubscriptions && (clientId is () || (clientId is string && clientId == ""))) {
                        setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
                        printError(KEY_VALIDATION_HANDLER,"Subscription validation is enabled but the Client Id is not " +
                        "received from the key manager during the introspection.");
                        return isAllowed;
                    }
                    // If clientID is present, do the subscription validation from the data stores.
                    if (clientId != () && clientId is string) {
                       [authenticationContext, isAllowed] =
                         validateSubscriptionFromDataStores(credential, clientId, apiName, apiVersion,
                         self.validateSubscriptions);
                       authenticationContext.username = principal?.username ?: USER_NAME_UNKNOWN;
                       invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                       invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
                       if (isAllowed) {
                           boolean tokenGenStatus = generateAndSetBackendJwtHeader(credential,
                                                                               req,
                                                                               self.enabledJWTGenerator,
                                                                               self.classLoaded,
                                                                               self.skewTime,
                                                                               self.enabledCaching,
                                                                               self.issuer,
                                                                               self.remoteUserClaimRetrievalEnabled,
                                                                               false);
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
                            boolean tokenGenStatus = generateAndSetBackendJwtHeader(credential,
                                                                                    req,
                                                                                    self.enabledJWTGenerator,
                                                                                    self.classLoaded,
                                                                                    self.skewTime,
                                                                                    self.enabledCaching,
                                                                                    self.issuer,
                                                                                    self.remoteUserClaimRetrievalEnabled,
                                                                                    false);
                            if (!tokenGenStatus) {
                                printError(KEY_AUTHN_FILTER, "Error while adding the Backend JWT header");
                            }
                        }
                        return authenticationResult;
                    }
                }
            }
        }
        // Default return Invalid Credentials.
        setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
        return false;
    }
};
