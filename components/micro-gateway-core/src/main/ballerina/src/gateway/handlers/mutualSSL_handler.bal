// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Representation of the mutual ssl handler
#
public type MutualSSLHandler object {

    *http:InboundAuthHandler;    

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        if(req.mutualSslHandshake["status"] == PASSED) {
            return true;
        }
        return false;
    }

    # Authenticates the incoming request knowing that mutual ssl has happened at the trasnport layer.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns boolean|http:AuthenticationError {
        int startingTime = getCurrentTime();
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();  
        return doMTSLFilterRequest(req, invocationContext);
    }

};


function doMTSLFilterRequest(http:Request request, runtime:InvocationContext context) returns boolean|http:AuthenticationError {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    boolean|http:AuthenticationError isAuthenticated = true;
    AuthenticationContext authenticationContext = {};
    boolean isSecured = true;
    printDebug(KEY_AUTHN_FILTER, "Processing request via MutualSSL filter.");

    context.attributes[IS_SECURED] = isSecured;
    int startingTime = getCurrentTime();
    context.attributes[REQUEST_TIME] = startingTime;
    context.attributes[FILTER_FAILED] = false;
    //Set authenticationContext data
    authenticationContext.authenticated = true;
    authenticationContext.username = USER_NAME_UNKNOWN;
    invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
    context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;

    return isAuthenticated;
}
