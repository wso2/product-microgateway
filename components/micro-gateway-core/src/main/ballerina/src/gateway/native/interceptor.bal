
// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
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

import ballerina/java;
import ballerina/http;

# Compresses a directory.
#
# + className - Full qualified class name of the java interceptor
# + return - Index of the intyerceptor in the interceptor array.
public function loadInterceptorClass(string className) returns int {
    handle class = java:fromString(className);
    return jLoadInterceptorClass(class);
}

public function initiateInterceptorArray(int numberOfResources) {
    jInitiateInterceptorArray(4 * numberOfResources);
}

public function invokeRequestInterceptor(int index, http:Caller caller, http:Request request) returns boolean {
    return jInvokeRequestInterceptor(index, caller, request);
}

public function invokeResponseInterceptor(int index, http:Caller caller, http:Response response) returns boolean {
    return jInvokeResponseInterceptor(index, caller, response);
}

function jInvokeRequestInterceptor(int index, http:Caller caller, http:Request request) returns boolean = @java:Method {
    name: "invokeRequestInterceptor",
    class: "org.wso2.micro.gateway.core.interceptors.InterceptorInvoker"
} external;

function jInvokeResponseInterceptor(int index, http:Caller caller, http:Response response) returns boolean = @java:Method {
    name: "invokeResponseInterceptor",
    class: "org.wso2.micro.gateway.core.interceptors.InterceptorInvoker"
} external;


function jLoadInterceptorClass(handle className) returns int = @java:Method {
    name: "loadInterceptorClass",
    class: "org.wso2.micro.gateway.core.interceptors.InterceptorInvoker"
} external;

function jInitiateInterceptorArray(int arraySize) = @java:Method {
    name: "initiateInterceptorArray",
    class: "org.wso2.micro.gateway.core.interceptors.InterceptorInvoker"
} external;

