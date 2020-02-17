// // Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// //
// // WSO2 Inc. licenses this file to you under the Apache License,
// // Version 2.0 (the "License"); you may not use this file   except
// // in compliance with the License.
// // You may obtain a copy of the License at
// //
// // http://www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing,
// // software distributed under the License is distributed on an
// // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// // KIND, either express or implied.  See the License for the
// // specific language governing permissions and limitations
// // under the License.

// import ballerina/config;
// import ballerina/http;
// import ballerina/runtime;


// public type CookieAuthHandler object {

//     # Checks if the request can be authenticated with the Cookie Auth header.
//     #
//     # + req - The `Request` instance.
//     # + return - Returns `true` if can be authenticated. Else, returns `false`.
//     public function canProcess(http:Request req) returns @tainted boolean {
//         runtime:InvocationContext invocationContext = runtime:getInvocationContext();
//         if (req.hasHeader(COOKIE_HEADER)) {
//             string requiredCookie = config:getAsString(COOKIE_HEADER, "");

//             //extract cookies from the incoming request
//             string authHead = req.getHeader(COOKIE_HEADER);
//             string[] cookies = split(authHead.trim(), ";");
//             foreach var cookie in cookies {
//                 string converted = replaceFirst(cookie, "=", "::");
//                 string[] splitedStrings = split(converted.trim(), "::");
//                 string sessionId = splitedStrings[1];
//                 if (sessionId == requiredCookie) {
//                     invocationContext.attributes[COOKIE_HEADER] = sessionId;
//                     return true;
//                 }
//             }
//         }
//         return false;
//     }

//     # Authenticates the incoming request with the use of credentials passed in the cookie
//     #
//     # + req - The `Request` instance.
//     # + return - Returns `true` if authenticated successfully. Else, returns `false`
//     # or the `AuthenticationError` in case of an error.
//     public function process(http:Request req) returns boolean | http:AuthenticationError {
//         string authHeader = <string>runtime:getInvocationContext().attributes[AUTH_HEADER];
//         string cookieValue = <string>runtime:getInvocationContext().attributes[COOKIE_HEADER];
//         req.setHeader(authHeader, cookieValue);
//         return false;
//     // we always set this handler false , and set the cookie value to authorization header
//     // to be validated by subsequent filters
//     }

// };
