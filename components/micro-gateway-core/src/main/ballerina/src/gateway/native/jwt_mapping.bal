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

import ballerinax/java;

# Compresses a directory.
#
# + className - Full qualified class name of the java interceptor
# + return - return the string array of scope
public function loadMappingClass(string className) returns boolean  {
    handle class = java:fromString(className);
    return jLoadMappingClass(class) ;
}

public function initiateJwtMap() {
    jinitiateJwtMap();
}

function jLoadMappingClass(handle className) returns boolean = @java:Method {
    name: "loadMappingClass",
    class: "org.wso2.micro.gateway.core.mapping.MappingInvoker"
} external;

public function transformJWTValue(map<any> claims, string className) returns map<any> {
    handle class = java:fromString(className);
    return jtransformJWTValue(claims, class);
}

function jtransformJWTValue(map<any> claims, handle className) returns map<any> = @java:Method {
    name: "transformJWTValue",
    class: "org.wso2.micro.gateway.core.mapping.MappingInvoker"
} external;

function jinitiateJwtMap() = @java:Method {
    name: "initiateJwtMap",
    class: "org.wso2.micro.gateway.core.mapping.MappingInvoker"
} external;
