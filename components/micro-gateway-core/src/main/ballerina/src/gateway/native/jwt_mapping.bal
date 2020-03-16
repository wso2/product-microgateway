// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
public function loadMappingClass(string className) returns string {
    handle class = java:fromString(className);
    string classname= <string> java:toString(jLoadMappingClass(class));
    return classname ;
}

function jLoadMappingClass(handle className) returns handle = @java:Method {
    name: "loadMappingClass",
    class: "org.wso2.micro.gateway.core.mapping.MappingInvoker",
    paramTypes: ["java.lang.String"]
} external;

public function transformJWT(map<any> claims) returns map<any> {
    return jtransformJWT(claims);
}

function jtransformJWT(map<any> claims) returns map<any> = @java:Method {
    name: "transformJWT",
    class: "org.wso2.micro.gateway.core.mapping.MappingInvoker"
} external;
