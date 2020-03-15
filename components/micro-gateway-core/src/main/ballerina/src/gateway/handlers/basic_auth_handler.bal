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

import ballerina/http;
import ballerina/runtime;
import ballerina/stringutils;

# Representation of the basic auth handler
#
# + basicAuthProvider - The reference to the basic auth provider instance
public type BasicAuthHandler object {

    *http:InboundAuthHandler;

    public BasicAuthProvider basicAuthProvider;

    public function __init(BasicAuthProvider basicAuthProvider) {
        self.basicAuthProvider = basicAuthProvider;
    }

    # Checks if the request can be authenticated with the Basic Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString().trim();
        //if '/apikey' resource is called, the handler is hit without the preAuthnFilter.
        if (stringutils:equalsIgnoreCase("", authHeader)) {
            authHeader = DEFAULT_AUTH_HEADER_NAME;
        }
        if (req.hasHeader(authHeader)) {
            string headerValue = req.getHeader(authHeader).toLowerAscii();
            if (headerValue.startsWith(AUTH_SCHEME_BASIC_LOWERCASE)) {
                printDebug(KEY_AUTHN_FILTER, "Request will be authenticated via basicAuth handler");
                return true;
            }
        }
        return false;
    }

    # Authenticates the incoming request with the use of credentials passed as the Basic Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns @tainted boolean | http:AuthenticationError {
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString().trim();
        //if '/apikey' resource is called, the handler is hit without the preAuthnFilter.
        if (stringutils:equalsIgnoreCase("", authHeader)) {
            authHeader = DEFAULT_AUTH_HEADER_NAME;
        }
        string headerValue = req.getHeader(authHeader);
        string credential = headerValue.substring(5, headerValue.length()).trim();
        var authenticationResult = self.basicAuthProvider.authenticate(credential);
        if (authenticationResult is boolean) {
            return authenticationResult;
        } else {
            return prepareAuthenticationError("Failed to authenticate with basic auth handler.", authenticationResult);
        }
    }
};
