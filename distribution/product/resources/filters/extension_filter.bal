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
import wso2/gateway;

// Extension filter used to send custom error messages and to customizations
@Description {value:"Representation of the Subscription filter"}
@Field {value:"filterRequest: request filter method which attempts to validate the subscriptions"}
public type ExtensionFilter object {

    @Description {value:"filterRequest: Request filter function"}
    public function filterRequest (http:Request request, http:FilterContext context) returns http:FilterResult {
        match <boolean> context.attributes[gateway:FILTER_FAILED] {
            boolean failed => {
                if (failed) {
                    //todo we need to send proper error message once the ballerina respond support comes to the filter
                    int statusCode = check <int>context.attributes[gateway:HTTP_STATUS_CODE];
                    string errorMessage = <string>context.attributes[gateway:ERROR_DESCRIPTION];
                    return gateway:createFilterResult(false, statusCode, errorMessage);
                }
            } error err => {
            //Nothing to handle
        }
        }
        http:FilterResult requestFilterResult = {canProceed:true, statusCode:200, message:"Filters succeeded"};
        return requestFilterResult;
    }

};

