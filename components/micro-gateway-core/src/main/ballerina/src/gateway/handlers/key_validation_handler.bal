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
# + externalKM - Is external key mananager is used or default wso2 key validation service is used.
public type KeyValidationHandler object {

    *http:InboundAuthHandler;

    public OAuth2KeyValidationProvider oauth2KeyValidationProvider;
    public oauth2:InboundOAuth2Provider introspectProvider;
    public boolean externalKM;

    public function __init(OAuth2KeyValidationProvider oauth2KeyValidationProvider, oauth2:InboundOAuth2Provider introspectProvider) {
        self.oauth2KeyValidationProvider = oauth2KeyValidationProvider;
        self.introspectProvider = introspectProvider;
        self.externalKM = getConfigBooleanValue(KM_CONF_INSTANCE_ID, EXTERNAL, DEFAULT_EXTERNAL);
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
        boolean|auth:Error authenticationResult = false;
        if (self.externalKM) {
            authenticationResult = self.introspectProvider.authenticate(credential);
            if (authenticationResult is auth:Error) {
                return prepareAuthenticationError("Failed to authenticate with introspect auth provider.", authenticationResult);
            } else {
                AuthenticationContext authenticationContext = {};
                authenticationContext.authenticated = authenticationResult;
                authenticationContext.keyType = PRODUCTION_KEY_TYPE;
                runtime:Principal? principal = invocationContext?.principal;
                if (principal is runtime:Principal) {
                    authenticationContext.username = principal?.username ?: USER_NAME_UNKNOWN;
                }
                invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
                return authenticationResult;
            }
        } else {
            authenticationResult = self.oauth2KeyValidationProvider.authenticate(credential);
            if (authenticationResult is boolean) {
                if (authenticationResult) {
                    AuthenticationContext authenticationContext = {};
                    authenticationContext = <AuthenticationContext>invocationContext.attributes[
                    AUTHENTICATION_CONTEXT];

                    if (authenticationContext?.callerToken is string && authenticationContext?.callerToken != () 
                            && authenticationContext?.callerToken != "") {
                        printDebug(KEY_AUTHN_FILTER, "Caller token: " + <string>authenticationContext?.
                        callerToken);
                        req.setHeader(jwtheaderName, <string>authenticationContext?.callerToken);
                    }
                }
                return authenticationResult;
            } else {
                return prepareAuthenticationError("Failed to authenticate with key validation auth handler.", authenticationResult);
            }
        }
    }

};
