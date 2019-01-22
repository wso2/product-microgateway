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


@Description { value: "Representation of the Cookie Based authetication logic" }
public type CookieBasedAuth object {

    @Param { value: "request: Request instance" }
    @Return { value: "FilterResult:Validation of cookies in request" }
    public function processRequest( http:Request request) returns string|error {

        //get required cookie as config value
        string requiredCookie = config:getAsString(COOKIE_HEADER, default = "");

        //extract cookies from the incoming request
        string authHead = request.getHeader(COOKIE_HEADER);
        string[] cookies = authHead.trim().split(";");
        foreach cookie in cookies {
            string converted = cookie.replaceFirst("=", "::");
            string[] splitedStrings = converted.trim().split("::");
            string sessionId = splitedStrings[1];
            if (sessionId == requiredCookie) {
                return sessionId;
            }
        }
        error notFound = { message: "No Authorized cookie is found" };
        return notFound;
    }

    @Param { value: "request: Request instance" }
    @Return { value: "FilterResult:Check whether Validation of cookies in request is authenticated" }
    public function isCookieAuthed( http:Request request) returns boolean {

        //get required cookie as config value
        string requiredCookie = config:getAsString(COOKIE_HEADER, default = "");

        //extract cookies from the incoming request
        string authHead = request.getHeader(COOKIE_HEADER);
        string[] cookies = authHead.trim().split(";");
        foreach cookie in cookies {
            string converted = cookie.replaceFirst("=", "::");
            string[] splitedStrings = converted.trim().split("::");
            string sessionId = splitedStrings[1];
            if (sessionId == requiredCookie) {
                return true;
            }
        }
        return false;
    }
};
