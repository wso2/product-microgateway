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

import ballerina/auth;
import ballerina/http;
import ballerina/runtime;

# Representation of the key validation  handler
#
# + oauth2KeyValidationProvider - The reference to the key validation provider instance
public type KeyValidationHandler object {

    *http:InboundAuthHandler;

    public OAuth2KeyValidationProvider oauth2KeyValidationProvider;

    public function __init(OAuth2KeyValidationProvider oauth2KeyValidationProvider) {
        self.oauth2KeyValidationProvider = oauth2KeyValidationProvider;
    }

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString();
        if (req.hasHeader(authHeader)) {
            string headerValue = req.getHeader(authHeader);
            if (hasPrefix(headerValue, auth:AUTH_SCHEME_BEARER)) {
                string credential = headerValue.substring(6, headerValue.length()).trim();
                string[] splitContent = split(credential,"\\.");
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
    public function process(http:Request req) returns @tainted boolean|http:AuthenticationError {
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        string authHeader = invocationContext.attributes[AUTH_HEADER].toString();
        string headerValue = req.getHeader(authHeader);
        string credential = <@untainted>headerValue.substring(6, headerValue.length()).trim();
        var authenticationResult = self.oauth2KeyValidationProvider.authenticate(credential);
        if(authenticationResult is boolean) {
            if(authenticationResult) {
                AuthenticationContext authenticationContext = {};
                authenticationContext = <AuthenticationContext>invocationContext.attributes[
                AUTHENTICATION_CONTEXT];

                if (authenticationContext?.callerToken is string && authenticationContext?.callerToken != () && authenticationContext?.callerToken != "") {
                    printDebug(KEY_AUTHN_FILTER, "Caller token: " + <string>authenticationContext?.
                                callerToken);
                    string jwtheaderName = getConfigValue(JWT_CONFIG_INSTANCE_ID, JWT_HEADER, JWT_HEADER_NAME);
                    req.setHeader(jwtheaderName, <string>authenticationContext?.callerToken);
                }
                string authHeaderName = getAuthorizationHeader(invocationContext);
                checkAndRemoveAuthHeaders(req, authHeaderName);
            }
            return authenticationResult;
        } else {
            return prepareAuthenticationError("Failed to authenticate with key validation auth handler.", authenticationResult);
        }

    }

};