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
import ballerina/runtime;
import ballerina/system;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;

// MutualSSL filter
public type MutualSSLFilter object {
    // public variable in order to satisfy filter interface
    public string authRequired;

    public function __init() {
        self.authRequired = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, "");
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        int startingTime = getCurrentTime();
        checkOrSetMessageID(context);
        if(self.authRequired == REQUIRE) {
            return doMTSLFilterRequest(caller, request, context);
        }
        return true;
    }



    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }
};

function doMTSLFilterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
    boolean isAuthenticated = false;
    AuthenticationContext authenticationContext = {};
    boolean isSecured = true;
    printDebug(KEY_AUTHN_FILTER, "Processing request via MutualSSL filter.");

    context.attributes[IS_SECURED] = isSecured;
    int startingTime = getCurrentTime();
    context.attributes[REQUEST_TIME] = startingTime;
    context.attributes[FILTER_FAILED] = false;
    isAuthenticated = true;
    //Set authenticationContext data
    authenticationContext.authenticated = true;
    authenticationContext.username = USER_NAME_UNKNOWN;
    runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
    context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;

    return isAuthenticated;
}
