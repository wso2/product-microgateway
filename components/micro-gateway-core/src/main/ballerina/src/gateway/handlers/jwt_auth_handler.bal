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

# Representation of the jwt self validating handler
#
# + bearerAuthHandler - The reference to the 'BearerAuthHandler' instance
# + jwtAuthProvider - The reference to the jwt auth provider instance
public type JWTAuthHandler object {

    *http:InboundAuthHandler;

    public http:BearerAuthHandler bearerAuthHandler;
    public JwtAuthProvider jwtAuthProvider;

    public function __init(JwtAuthProvider jwtAuthProvider) {
        self.jwtAuthProvider = jwtAuthProvider;
        self.bearerAuthHandler = new(jwtAuthProvider);
    }

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        if (req.hasHeader(AUTH_HEADER)) {
            string headerValue = http:extractAuthorizationHeaderValue(req);
            if(hasPrefix(headerValue, auth:AUTH_SCHEME_BEARER)) {
                string credential = headerValue.substring(6, headerValue.length()).trim();
                string[] splitContent = split(credential,"\\.");
                if(splitContent.length() == 3) {
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
    public function process(http:Request req) returns boolean|http:AuthenticationError {
        return self.bearerAuthHandler.process(req);
    }

};