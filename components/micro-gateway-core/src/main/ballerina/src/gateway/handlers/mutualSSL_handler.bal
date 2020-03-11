// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file   except
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

# Representation of the mutual ssl handler
#
public type MutualSSLHandler object {
    *http:InboundAuthHandler;

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        return true;
    }

    # Authenticates the incoming request knowing that mutual ssl has happened at the trasnport layer.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns boolean | http:AuthenticationError {
        string|error mutualSSLVerifyClient = getMutualSSL();
        if (mutualSSLVerifyClient is string && stringutils:equalsIgnoreCase(MANDATORY, mutualSSLVerifyClient) 
                && req.mutualSslHandshake[STATUS] != PASSED ) {
            if (req.mutualSslHandshake[STATUS] == FAILED) {
                printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status: FAILED");
            }
            // provided more generic error code to avoid security issues.
            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS); 
            return prepareAuthenticationError("Failed to authenticate with MutualSSL handler");            
        }

        if (req.mutualSslHandshake[STATUS] == PASSED) {
            printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status: PASSED");
            runtime:InvocationContext invocationContext = runtime:getInvocationContext();
            doMTSLFilterRequest(req, invocationContext); 
        }
        return true;
    }
};


function doMTSLFilterRequest(http:Request request, runtime:InvocationContext context) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    AuthenticationContext authenticationContext = {};
    printDebug(KEY_AUTHN_FILTER, "Processing request via MutualSSL filter.");

    context.attributes[IS_SECURED] = true;
    int startingTime = getCurrentTimeForAnalytics();
    context.attributes[REQUEST_TIME] = startingTime;
    context.attributes[FILTER_FAILED] = false;
    //Set authenticationContext data
    authenticationContext.authenticated = true;
    authenticationContext.username = USER_NAME_UNKNOWN;
    invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
    context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
}
