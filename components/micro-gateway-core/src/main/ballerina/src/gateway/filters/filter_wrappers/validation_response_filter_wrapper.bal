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
import ballerina/observe;

public type ValidationResponseFilterWrapper object {

    *http:ResponseFilter;

    ValidationResponseFilter validationResponseFilter;

    public function __init() {
        self.validationResponseFilter = new ValidationResponseFilter();
    }

    public function filterResponse(@tainted http:Response response, http:FilterContext context) returns boolean {
        //Start a new root span attaching to the system span.
        int | error | () spanIdRes = startSpan(VALIDATION_FILTER_RESPONSE);
        //starting a Gauge metric
        map<string> | () gaugeTags = getGaugeTagInvocationContext(VALIDATION_GAUGE_TAGS);
        if (gaugeTags is ()) {
            gaugeTags = gaugeTagDetailsFromContext(context, FILTER_VALIDATION);
        }
        observe:Gauge | () localGauge = initializeGauge(PER_REQ_DURATION, REQ_FLTER_DURATION, gaugeTags);
        observe:Gauge | () localGaugeTotal = initializeGauge(REQ_DURATION_TOTAL, FILTER_TOTAL_DURATION,
                {"Category": FILTER_VALIDATION});
        int startingTime = getCurrentTime();
        boolean result = self.validationResponseFilter.filterResponse(response, context);
        float | () latency = setGaugeDuration(startingTime);
        float | () reqLatency = getLatencyInvocationContext(VALIDATION_REQUEST_TIME);
        float | () totalLatency = calculateLatency(reqLatency, latency);
        updateGauge(localGauge, totalLatency);
        updateGauge(localGaugeTotal, totalLatency);
        //Finish span.
        finishSpan(VALIDATION_FILTER_RESPONSE, spanIdRes);
        return result;
    }
};
