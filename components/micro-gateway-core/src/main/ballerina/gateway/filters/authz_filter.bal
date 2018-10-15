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

@Description {value:"Representation of the Authorization filter"}
@Field {value:"filterRequest: request filter method which attempts to authorize the request"}
@Field {value:"filterRequest: response filter method (not used this scenario)"}
public type OAuthzFilter object {

    public http:AuthzFilter authzFilter;

    public new(authzFilter) {
    }

    @Description { value: "Filter function implementation which tries to authorize the request" }
    @Param { value: "request: Request instance" }
    @Param { value: "context: FilterContext instance" }
    @Return { value: "FilterResult: Authorization result to indicate if the request can proceed or not" }
    public function filterRequest(http:Listener listener, http:Request request, http:FilterContext context) returns
        boolean {

        string checkAuthentication = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, "");

        if(!checkAuthentication.equalsIgnoreCase("require")){
            //Setting UUID
            int startingTime = getCurrentTime();
            checkOrSetMessageID(context);
            boolean result = doFilterRequest(listener, request, context);
            setLatency(startingTime, context, SECURITY_LATENCY_AUTHZ);
            return result;
        }else{
            // Skip this filter is mutualSSL is enabled.
            return true;
        }
    }

    public function doFilterRequest(http:Listener listener, http:Request request, http:FilterContext context) returns
        boolean {
        printDebug(KEY_AUTHZ_FILTER, "Processing request via Authorization filter.");
        string authScheme = runtime:getInvocationContext().authContext.scheme;
        boolean result = true;
        // scope validation is done in authn filter for oauth2, hence we only need to
        //validate scopes if auth scheme is jwt.
        if (authScheme == AUTH_SCHEME_JWT){
            result = self.authzFilter.filterRequest(listener, request, context);
        }
        printDebug(KEY_AUTHZ_FILTER, "Returned with value: " + result);
        return result;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        int startingTime = getCurrentTime();
        boolean result = doFilterResponse(response, context);
        setLatency(startingTime, context, SECURITY_LATENCY_AUTHZ_RESPONSE);
        return result;
    }

    public function doFilterResponse(http:Response response, http:FilterContext context) returns boolean {
        // In authorization filter we have specifically set the error payload since we are using ballerina in built
        // authzFilter
        if (response.statusCode == FORBIDDEN) {
            match runtime:getInvocationContext().attributes[ERROR_CODE] {
                () => {
                    match context.attributes[ERROR_CODE] {
                        () => {
                            setAuthorizationFailureMessage(response, context);
                        }
                        any code => {// no need to set error codes
                        }
                    }
                }
                any code => {// no need to set error codes

                }
            }

        }
        return true;
    }
};

function setAuthorizationFailureMessage(http:Response response, http:FilterContext context) {
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


