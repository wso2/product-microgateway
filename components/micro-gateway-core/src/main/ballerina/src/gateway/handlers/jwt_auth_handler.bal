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

import ballerina/auth;
import ballerina/http;
import ballerina/observe;
import ballerina/runtime;

# Representation of the jwt self validating handler
#
# + jwtAuthProvider - The reference to the jwt auth provider instance
public type JWTAuthHandler object {

    *http:InboundAuthHandler;

    public JwtAuthProvider jwtAuthProvider;

    public function __init(JwtAuthProvider jwtAuthProvider) {
        self.jwtAuthProvider = jwtAuthProvider;
    }

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString();
        if (req.hasHeader(authHeader)) {
            string headerValue = req.getHeader(authHeader);
            if (hasPrefix(headerValue, auth:AUTH_SCHEME_BEARER)) {
                string credential = headerValue.substring(6, headerValue.length()).trim();
                string[] splitContent = split(credential,"\\.");
                if (splitContent.length() == 3) {
                    printDebug(KEY_AUTHN_FILTER, "Request will authenticated via jwt handler");
                    return true;
                }
            }
        }
        return false;
    }

    # Authenticates the incoming request with the use of credentials passed as the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns @tainted boolean|http:AuthenticationError {
        //Start a span attaching to the system span.
        int|error|() spanId_Process = startingSpan(JWT_AUTHENHANDLER_PROCESS);
        int startingTime = getCurrentTime();
        map<string> gaugeTags = gageTagDetails_authn(req, FIL_AUTHENTICATION);
        observe:Gauge|() localGauge = gaugeInitializing(PER_REQ_DURATION, REQ_FLTER_DURATION, gaugeTags);
        observe:Gauge|() localGauge_total = gaugeInitializing(REQ_DURATION_TOTAL, FILTER_TOTAL_DURATION, {"Category":FIL_AUTHENTICATION});
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString();
        string headerValue = req.getHeader(authHeader);
        string credential = headerValue.substring(6, headerValue.length()).trim();
        var authenticationResult = self.jwtAuthProvider.authenticate(credential);
        float latency = setGaugeDuration(startingTime);
        UpdatingGauge(localGauge, latency);
        UpdatingGauge(localGauge_total, latency);
        if (authenticationResult is boolean) {
            //finishing span
            finishingSpan(JWT_AUTHENHANDLER_PROCESS, spanId_Process);
            return authenticationResult;
        } else {
            //finishing span
            finishingSpan(JWT_AUTHENHANDLER_PROCESS, spanId_Process);
            return prepareAuthenticationError("Failed to authenticate with jwt bearer auth handler.", authenticationResult);
        }
    }
};