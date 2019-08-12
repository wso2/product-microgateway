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
import ballerina/cache;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;

// authorization filter which wraps the ballerina in built authorization filter.

public type OAuthzFilter object {

    public http:AuthzFilter authzFilter;

    public function __init(cache:Cache positiveAuthzCache, cache:Cache negativeAuthzCache, string[][]? scopes) {
        http:AuthzHandler authzHandler = new(positiveAuthzCache, negativeAuthzCache);
        self.authzFilter = new(authzHandler,scopes);
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns
                                                                                                            boolean
    {

        string checkAuthentication = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, "");

        if (checkAuthentication != "require") {
            //Setting UUID
            int startingTime = getCurrentTime();
            checkOrSetMessageID(context);
            printDebug(KEY_AUTHZ_FILTER, "Processing request via Authorization filter.");
            runtime:AuthenticationContext? authContext = runtime:getInvocationContext()?.authenticationContext;
            boolean result = true;
            if(authContext is AuthenticationContext){
                string authScheme = authContext.scheme;
                // scope validation is done in authn filter for oauth2, hence we only need to
                //validate scopes if auth scheme is jwt.
                if (authScheme == AUTH_SCHEME_JWT){
                    result = self.authzFilter.filterRequest(caller, request, context);
                }
            }
            printDebug(KEY_AUTHZ_FILTER, "Returned with value: " + result.toString());
            setLatency(startingTime, context, SECURITY_LATENCY_AUTHZ);
            return result;
        } else {
            // Skip this filter is mutualSSL is enabled.
            return true;
        }
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        int startingTime = getCurrentTime();
        boolean result = doAuthzFilterResponse(response, context);
        setLatency(startingTime, context, SECURITY_LATENCY_AUTHZ_RESPONSE);
        return result;
    }

};



public function doAuthzFilterResponse(http:Response response, http:FilterContext context) returns boolean {
    // In authorization filter we have specifically set the error payload since we are using ballerina in built
    // authzFilter
    if (response.statusCode == FORBIDDEN) {
        if(runtime:getInvocationContext().attributes[ERROR_CODE] is ()) {
            if(context.attributes[ERROR_CODE] is ()) {
                setAuthorizationFailureMessage(response, context);
            }
        }

    }
    return true;
}


public function setAuthorizationFailureMessage(http:Response response, http:FilterContext context) {
    string errorDescription = INVALID_SCOPE_MESSAGE;
    string errorMesssage = INVALID_SCOPE_MESSAGE;
    int errorCode = INVALID_SCOPE;
    response.setContentType(APPLICATION_JSON);
    json payload = { fault: {
        code: errorCode,
        message: errorMesssage,
        description: errorDescription
    } };
    response.setJsonPayload(payload);
}
