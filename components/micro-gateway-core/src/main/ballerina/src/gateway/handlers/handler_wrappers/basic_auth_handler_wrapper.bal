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

import ballerina/http;
import ballerina/observe;

# Representation of the basic auth handler
#
# + basicAuthProvider - The reference to the basic auth provider instance
public type BasicAuthHandlerWrapper object {
    *http:InboundAuthHandler;
    public BasicAuthProvider basicAuthProvider;
    BasicAuthHandler basicAuthHandler;

    public function __init(BasicAuthProvider basicAuthProvider) {
        self.basicAuthProvider = basicAuthProvider;
        self.basicAuthHandler = new BasicAuthHandler(basicAuthProvider);
    }

    # Checks if the request can be authenticated with the Basic Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        boolean result = self.basicAuthHandler.canProcess(req);
        return result;
    }

    # Authenticates the incoming request with the use of credentials passed as the Basic Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns @tainted boolean | http:AuthenticationError {
        //Start a span attaching to the system span.
        int | error | () spanIdProcess = startSpan(BASIC_AUTH_HANDLER_PROCESS);
        //starting Gauge
        int startingTime = getCurrentTime();
        map<string> | () gaugeTags = gaugeTagDetails_authn(req, FILTER_AUTHENTICATION);
        observe:Gauge | () localGauge = initializeGauge(PER_REQ_DURATION, REQ_FLTER_DURATION, gaugeTags);
        observe:Gauge | () localGaugeTotal = initializeGauge(REQ_DURATION_TOTAL, FILTER_TOTAL_DURATION,
            {"Category": FILTER_AUTHENTICATION});
        boolean | http:AuthenticationError result = self.basicAuthHandler.process(req);
        float | () latency = setGaugeDuration(startingTime);
        updateGauge(localGauge, latency);
        updateGauge(localGaugeTotal, latency);
        //finishing span
        finishSpan(BASIC_AUTH_HANDLER_PROCESS, spanIdProcess);
        return result;
    }
};
