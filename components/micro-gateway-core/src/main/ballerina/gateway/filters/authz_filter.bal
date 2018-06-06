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

    public {
        http:AuthzFilter authzFilter;
    }

    public new(authzFilter) {
    }

    @Description { value: "Filter function implementation which tries to authorize the request" }
    @Param { value: "request: Request instance" }
    @Param { value: "context: FilterContext instance" }
    @Return { value: "FilterResult: Authorization result to indicate if the request can proceed or not" }
    public function filterRequest(http:Request request, http:FilterContext context) returns http:FilterResult {
        match <boolean> context.attributes[FILTER_FAILED] {
            boolean failed => {
                if (failed) {
                    return createFilterResult(true, 200, "Skipping filter due to parent filter has returned false");
                }
            } error err => {
            //Nothing to handle
        }
        }
        string authScheme = runtime:getInvocationContext().authContext.scheme;
        // scope validation is done in authn filter for oauth2, hence we only need to
        //validate scopes if auth scheme is jwt.
        if (authScheme == AUTH_SCHEME_JWT){
            // todo: send proper error message
            return self.authzFilter.filterRequest(request, context);
        }
        return createFilterResult(true, 200, "Successfully authorized");
    }
};


