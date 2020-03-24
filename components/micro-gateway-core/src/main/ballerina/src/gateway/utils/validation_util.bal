// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

boolean enableRequestValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, REQUEST_VALIDATION_ENABLED,
    DEFAULT_REQUEST_VALIDATION_ENABLED);
boolean enableResponseValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, RESPONSE_VALIDATION_ENABLED,
    DEFAULT_RESPONSE_VALIDATION_ENABLED);  

function getRequestPathFromFilterContext(http:FilterContext filterContext) returns string {
    any path = filterContext.attributes[REQUEST_PATH];
    string requestPath = "";
    if (path is string) {
        requestPath = path;
    }
    return requestPath;
}

function getRequestMethodFromFilterContext(http:FilterContext filterContext) returns string {
    any method = filterContext.attributes[REQ_METHOD];
    string requestMethod = "";
    if (method is string) {
        requestMethod = method;
    }
    return requestMethod;
}

 # to set the Method and Path properties to the filterContext for the use of request and validation filters.
function setReqPathAndMethodToFilterContext(http:Request request, http:FilterContext filterContext) {
    //It is not required to set these properties, if neither request validation nor response validation is enabled
    if (!enableRequestValidation && !enableResponseValidation) {
        return;
    }
    //getting the method of the request
    string requestMethod = request.method.toLowerAscii();
    filterContext.attributes[REQ_METHOD] = requestMethod;
    //getting the resource Name
    string resourceName = filterContext.getResourceName();
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
    if (httpResourceConfig is http:HttpResourceConfig) {
        string requestPath = httpResourceConfig.path;
        filterContext.attributes[REQUEST_PATH] = requestPath;
    }
} 
