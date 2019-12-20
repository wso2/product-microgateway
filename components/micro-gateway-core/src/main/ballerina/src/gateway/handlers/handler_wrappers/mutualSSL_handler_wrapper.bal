// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Representation of the mutual ssl handler
#
public type MutualSSLHandlerWrapper object {
    *http:InboundAuthHandler;
    MutualSSLHandler mutualSSLHandler = new;

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        boolean result = self.mutualSSLHandler.canProcess(req);
        return result;
    }

    # Authenticates the incoming request knowing that mutual ssl has happened at the trasnport layer.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns boolean | http:AuthenticationError {
        //Start a span attaching to the system span.
        int | error | () spanIdReq = startSpan(MUTUALSSL_FILTER_PROCESS);
        boolean | http:AuthenticationError result = self.mutualSSLHandler.process(req);
        //Finish span.
        finishSpan(MUTUALSSL_FILTER_PROCESS, spanIdReq);
        return result;
    }
};
