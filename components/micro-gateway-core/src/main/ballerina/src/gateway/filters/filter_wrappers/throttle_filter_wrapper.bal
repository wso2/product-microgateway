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

import ballerina/http;
import ballerina/observe;

public type ThrottleFilterWrapper object {
    ThrottleFilter throttleFilter;

    public function __init(map<boolean> deployedPolicies) {
        self.throttleFilter = new ThrottleFilter(deployedPolicies);
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        //Gauge metric initialization
        map<string> gaugeTags = gaugeTagDetails(request, context, FILTER_THROTTLING);
        observe:Gauge localGauge = InitializeGauge(PER_REQ_DURATION, REQ_FLTER_DURATION, gaugeTags);
        observe:Gauge localGauge_total = InitializeGauge(REQ_DURATION_TOTAL, FILTER_TOTAL_DURATION, {"Category": FILTER_THROTTLING});
        int startingTime = getCurrentTime();
        boolean result = self.throttleFilter.filterRequest(caller, request, context);
        float latency = setGaugeDuration(startingTime);
        updateGauge(localGauge, latency);
        updateGauge(localGauge_total, latency);
        return result;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        boolean result = self.throttleFilter.filterResponse(response, context);
        return result;
    }
};
