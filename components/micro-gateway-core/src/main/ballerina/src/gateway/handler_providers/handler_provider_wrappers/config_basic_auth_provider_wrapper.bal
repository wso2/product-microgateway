// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/auth;
import ballerina/observe;

public type BasicAuthProviderWrapper object {
    *auth:InboundAuthProvider;

    BasicAuthProvider basicAuthProvider;

    # Provides authentication based on the provided configuration.
    #
    # + basicAuthConfig - The Basic Auth provider configurations.
    public function __init(auth:BasicAuthConfig? basicAuthConfig = ()) {
        self.basicAuthProvider = new (basicAuthConfig);
    }

    # Attempts to authenticate with credentials.
    #
    # + credential - Credential
    # + return - `true` if authentication is successful, otherwise `false` or `Error` occurred while extracting credentials
    public function authenticate(string credential) returns (boolean | auth:Error) {
        //Start a span attaching to the system span.
        int | error | () spanId_req = startSpan(BASICAUTH_PROVIDER);
        //starting Gauge
        int startingTime = getCurrentTime();
        map<string> | () gaugeTags = gaugeTagDetails_basicAuth(FILTER_AUTHENTICATION);
        observe:Gauge | () localGauge = initializeGauge(PER_REQ_DURATION, REQ_FLTER_DURATION, gaugeTags);
        observe:Gauge | () localGauge_total = initializeGauge(REQ_DURATION_TOTAL, FILTER_TOTAL_DURATION, {"Category": FILTER_AUTHENTICATION});
        boolean | auth:Error result = self.basicAuthProvider.authenticate(credential);
        float | () latency = setGaugeDuration(startingTime);
        updateGauge(localGauge, latency);
        updateGauge(localGauge_total, latency);
        //Finish span.
        finishSpan(BASICAUTH_PROVIDER, spanId_req);
        return result;
    }

};
