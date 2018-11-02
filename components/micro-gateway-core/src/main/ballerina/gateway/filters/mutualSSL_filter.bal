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

@Description { value: "Representation of the MutualSSL filter" }

public type MutualSSLFilter object {

    public json trottleTiers;


    public new(trottleTiers) {}

    @Param { value: "listener: Listner endpoint" }
    @Param { value: "request: Request instance" }
    @Param { value: "context: FilterContext instance" }
    @Return { value: "FilterResult: MTSL result to indicate which folw is selected for request to proceed" }
    public function filterRequest(http:Listener listener, http:Request request, http:FilterContext context) returns
                                                                                                                boolean
    {

        int startingTime = getCurrentTime();
        checkOrSetMessageID(context);
        boolean result = doFilterRequest(listener, request, context);
        return result;
    }

    @Description { value: "representation of Dofilter Request" }
    @Param { value: "listener: Listner endpoint" }
    @Param { value: "request: Request instance" }
    @Param { value: "context: FilterContext instance" }
    @Return { value: "FilterResult: MTSL result to indicate which folw is selected for request to proceed" }
    public function doFilterRequest(http:Listener listener, http:Request request, http:FilterContext context)
                        returns boolean {
        boolean isAuthenticated = false;
        string checkAuthentication = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, "");


        if (checkAuthentication == "require") {
            // get  config for this resource
            AuthenticationContext authenticationContext;
            boolean isSecured = true;


            context.attributes[IS_SECURED] = isSecured;
            int startingTime = getCurrentTime();
            context.attributes[REQUEST_TIME] = startingTime;
            context.attributes[FILTER_FAILED] = false;
            isAuthenticated = true;
            context.attributes[IS_AUTHENTICATED] = isAuthenticated;
            //Set authenticationContext data
            authenticationContext.authenticated = true;
            authenticationContext.tier = UNAUTHENTICATED_TIER;
            authenticationContext.applicationTier = UNLIMITED_TIER;
            context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;

            return isAuthenticated;


        } else {
            //mutual ssl is not anabled and skip this filter
            context.attributes[IS_AUTHENTICATED] = false;
            return true;
        }
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }


};