// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerinax/'java\.arrays as arrays;

public function split(string str, string delimeter) returns string[] {
    handle delim = java:fromString(delimeter);
    handle rec = java:fromString(str);
    handle arr = jSplit(rec, delim);

    string[] splitArr = [];
    int i = 0;
    while (i < arrays:getLength(arr)) {
        string val = arrays:get(arr, i).toString();
        splitArr[splitArr.length()] = val;
        i = i + 1;
    }
    
    return splitArr;
}

public function replaceAll(string str, string regex, string replacement) returns string {
    handle reg = java:fromString(regex);
    handle rep = java:fromString(replacement);
    handle rec = java:fromString(str);
    handle newStr = jReplaceAll(rec, reg, rep);

    return newStr.toString();
}

public function replaceFirst(string str, string regex, string replacement) returns string {
    handle reg = java:fromString(regex);
    handle rep = java:fromString(replacement);
    handle rec = java:fromString(str);
    handle newStr = jReplaceFirst(rec, reg, rep);

    return newStr.toString();
}

public function contains(string str, string s) returns boolean {
    handle seq = java:fromString(s);
    handle rec = java:fromString(str);
    
    return jContains(rec, seq);
}

public function lastIndexOf(string str, string indexOf) returns int {
    handle index = java:fromString(indexOf);
    handle rec = java:fromString(str);
    
    return jLastIndexOf(rec, index);
}

public function hasSuffix(string str, string suffix) returns boolean {
    handle suf = java:fromString(suffix);
    handle rec = java:fromString(str);
    
    return jEndsWith(rec, suf);
}

public function hasPrefix(string str, string prefix) returns boolean {
    handle pref = java:fromString(prefix);
    handle rec = java:fromString(str);
    
    return jStartsWith(rec, pref);
}

function jSplit(handle receiver, handle delimeter) returns handle = @java:Method {
    name: "split",
    class: "java.lang.String"
} external;

function jReplaceAll(handle receiver, handle regex, handle replacement) returns handle = @java:Method {
    name: "replaceAll",
    class: "java.lang.String"
} external;

function jReplaceFirst(handle receiver, handle regex, handle replacement) returns handle = @java:Method {
    name: "replaceFirst",
    class: "java.lang.String"
} external;

function jContains(handle receiver, handle s) returns boolean = @java:Method {
    name: "contains",
    class: "java.lang.String"
} external;

function jLastIndexOf(handle receiver, handle str) returns int = @java:Method {
    name: "lastIndexOf",
    class: "java.lang.String",
    paramTypes: ["java.lang.String"]
} external;

function jEndsWith(handle receiver, handle suffix) returns boolean = @java:Method {
    name: "endsWith",
    class: "java.lang.String"
} external;

function jStartsWith(handle receiver, handle prefix) returns boolean = @java:Method {
    name: "startsWith",
    class: "java.lang.String",
    paramTypes: ["java.lang.String"]
} external;
