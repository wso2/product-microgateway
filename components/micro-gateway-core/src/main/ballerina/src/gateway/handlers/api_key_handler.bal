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

import ballerina/http;

# Representation of the api key validating handler
#
# + apiKeyProvider - The reference to the jwt auth provider instance
public type APIKeyHandler object {

    *http:InboundAuthHandler;

    public APIKeyProvider apiKeyProvider;

    public function __init(APIKeyProvider apiKeyProvider) {
        self.apiKeyProvider = apiKeyProvider;
    }

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        return req.hasHeader(API_KEY_HEADER);
    }

    # Authenticates the incoming request with the use of credentials passed as the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns @tainted boolean|http:AuthenticationError {
        string credentials = req.getHeader(API_KEY_HEADER);
        var authenticationResult = self.apiKeyProvider.authenticate(credentials);
        if (authenticationResult is boolean) {
            return authenticationResult;
        } else {
            return prepareAuthenticationError("Failed to authenticate with api key handler.", authenticationResult);
        }
    }
};
