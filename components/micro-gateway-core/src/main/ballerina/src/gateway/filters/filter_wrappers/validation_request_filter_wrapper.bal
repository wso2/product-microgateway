// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type ValidationRequestFilterWrapper object {

    *http:RequestFilter;
    ValidationRequestFilter validationRequestFilter;

    public function __init() {
        self.validationRequestFilter = new ValidationRequestFilter();
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context)
                         returns boolean {
        //Start a new root span attaching to the system span.
        int | error | () spanIdReq = startSpan(VALIDATION_FILTER_REQUEST);
        map<string> | () gaugeTags = gaugeTagDetails(request, context, FILTER_VALIDATION);
        setGaugeTagInvocationContext(VALIDATION_GAUGE_TAGS, gaugeTags);
        int startingTime = getCurrentTime();
        boolean result = self.validationRequestFilter.filterRequest(caller, request, context);
        float | () latency = setGaugeDuration(startingTime);
        setLatencyInvocationContext(VALIDATION_REQUEST_TIME, latency);
        //Finish span.
        finishSpan(VALIDATION_FILTER_REQUEST, spanIdReq);
        return result;
    }
};
