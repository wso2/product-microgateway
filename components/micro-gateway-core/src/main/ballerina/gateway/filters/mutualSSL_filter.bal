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
import ballerina/log;
import ballerina/auth;
import ballerina/config;
import ballerina/io;

// MutualSSL filter
public type MutualSSLFilter object {

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        int startingTime = getCurrentTime();
        checkOrSetMessageID(context);
        if(request.mutualSslHandshake["status"] == PASSED) {
            return doMTSLFilterRequest(caller, request, context);
        }
        return true;
    }



    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }
};

function doMTSLFilterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
    boolean isAuthenticated = true;
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
    runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
    context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;

    return isAuthenticated;
}
