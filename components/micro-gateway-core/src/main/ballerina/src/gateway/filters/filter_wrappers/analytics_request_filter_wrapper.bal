// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/runtime;

public type AnalyticsRequestFilterWrapper object {
    AnalyticsRequestFilter analyticsRequestFilter = new;

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        map<string> gaugeTags = gaugeTagDetails(request, context, FILTER_ANALYTICS);
        setGaugeTagInvocationContext(ANALYTIC_GAUGE_TAGS, gaugeTags);
        int startingTime = getCurrentTime();
        boolean result = self.analyticsRequestFilter.filterRequest(caller, request, context);
        float latency = setGaugeDuration(startingTime);
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        invocationContext.attributes[ANALYTIC_REQUEST_TIME] = latency;
        return result;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        //starting a Gauge metric
        map<string> gaugeTags = getGaugeTagInvocationContext(ANALYTIC_GAUGE_TAGS);
        observe:Gauge localGauge = InitializeGauge(PER_REQ_DURATION, REQ_FLTER_DURATION, gaugeTags);
        observe:Gauge localGauge_total = InitializeGauge(REQ_DURATION_TOTAL, FILTER_TOTAL_DURATION, {"Category": FILTER_ANALYTICS});
        int startingTime = getCurrentTime();
        boolean result = self.analyticsRequestFilter.filterResponse(response, context);
        float latency = setGaugeDuration(startingTime);
        float req_latency = <float>runtime:getInvocationContext().attributes[ANALYTIC_REQUEST_TIME];
        float total_latency = req_latency + latency;
        updateGauge(localGauge, total_latency);
        updateGauge(localGauge_total, total_latency);
        return result;
    }

};
