// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Representation of the api key validating handler
#
# + apiKeyProvider - The reference to the jwt auth provider instance
public type APIKeyHandler object {

    *http:InboundAuthHandler;
    public APIKeyProvider apiKeyProvider;

    public function __init(APIKeyProvider apiKeyProvider) {
        self.apiKeyProvider = apiKeyProvider;
    }

    # Checks if the request can be authenticated with the Bearer API Key.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        [string, string] | error apiKey = getAPIKeyAuth();
        if (apiKey is [string, string]) {
            [string, string][inName, name] = <[string, string]> apiKey;
            if (stringutils:equalsIgnoreCase(HEADER, inName)) {
                if (req.hasHeader(name)) {
                    printDebug(KEY_AUTHN_FILTER, "Request will handle through API Key handler");
                    return true;
                }
            } else if (stringutils:equalsIgnoreCase(QUERY, inName)) {
                string? apikeyQuery = req.getQueryParamValue(name);
                if (apikeyQuery is string) {
                    printDebug(KEY_AUTHN_FILTER, "Request will handle through API Key handler");
                    return true;
                }
            }
        }

        return false;
    }

    # Authenticates the incoming request with the use of credentials passed as API Key.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns @tainted boolean | http:AuthenticationError {
        string credentials = "";
        [string, string] | error apiKey = getAPIKeyAuth();
        if (apiKey is [string, string]) {
            [string, string][inName, name] = <[string, string]> apiKey;
            if (stringutils:equalsIgnoreCase(HEADER, inName)) {
                credentials = req.getHeader(name);
            } else {
                string? apikeyQuery = req.getQueryParamValue(name);
                if (apikeyQuery is string) {
                    credentials = apikeyQuery;
                }
            }
        }
        printDebug(KEY_AUTHN_FILTER, "credentials: " + credentials);
        var authenticationResult = self.apiKeyProvider.authenticate(<@untainted>credentials);
        if (authenticationResult is boolean) {
            if(authenticationResult) {
                runtime:InvocationContext invocationContext = runtime:getInvocationContext();
                string resourceName = invocationContext.attributes[http:RESOURCE_NAME].toString();
                http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
                if (httpResourceConfig is http:HttpResourceConfig) {
                    // we are explicitly setting the scopes to principal, because api key auth mechanism should not
                    //enforce the concept of scopes. But ballerina auth module calls the authorization filter
                    //irrespective of the handler type. Hence we need to explicitly skip scope validation
                    //for the api key handler
                    setResourceScopesToPrincipal(httpResourceConfig, invocationContext, credentials);
                }
            }
            return authenticationResult;
        } else {
            return prepareAuthenticationError("Failed to authenticate with api key handler.", authenticationResult);
        }
    }
};
