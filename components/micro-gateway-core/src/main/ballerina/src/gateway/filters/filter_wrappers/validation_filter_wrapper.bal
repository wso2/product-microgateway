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

// import ballerina/http;

// public type ValidationFilterWrapper object {

//     ValidationFilter validationFilter;

//     public function __init(map<json> openAPIs) {
//         self.validationFilter = new ValidationFilter(openAPIs);
//     }

//     public function filterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext)
//                         returns boolean {
//         //Start a new root span attaching to the system span.
//         int | error | () spanIdReq = startSpan(VALIDATION_FILTER_REQUEST);
//         boolean result = self.validationFilter.filterRequest(caller, request, filterContext);
//         //Finish span.
//         finishSpan(VALIDATION_FILTER_REQUEST, spanIdReq);
//         return result;
//     }

//     public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
//         //Start a new root span attaching to the system span.
//         int | error | () spanIdRes = startSpan(VALIDATION_FILTER_RESPONSE);
//         boolean result = self.validationFilter.filterResponse(response, context);
//         //Finish span.
//         finishSpan(VALIDATION_FILTER_RESPONSE, spanIdRes);
//         return result;
//     }

// };
