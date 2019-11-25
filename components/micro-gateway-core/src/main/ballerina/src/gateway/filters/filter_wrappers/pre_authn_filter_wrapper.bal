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

// Pre Authentication filter

public type PreAuthnFilterWrapper object {

    PreAuthnFilter preAuthnFilter = new;

    public function filterRequest(http:Caller caller, http:Request request,@tainted http:FilterContext context) returns boolean {
        //Start a span attaching to the system span.
        int | error | () spanId_req = startSpan(PRE_AUTHN_FILTER_REQUEST);
        boolean result = self.preAuthnFilter.filterRequest(caller, request, context);
        //Finish span.
        finishSpan(PRE_AUTHN_FILTER_REQUEST, spanId_req);
        return result;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        boolean result = self.preAuthnFilter.filterResponse(response, context);
        return result;
    }
};
