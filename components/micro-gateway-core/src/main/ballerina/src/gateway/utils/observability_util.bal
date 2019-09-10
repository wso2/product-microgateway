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

import ballerina/log;
import ballerina/observe;
import ballerina/http;

boolean isTracingEnabled = getConfigBooleanValue("b7a.observability.tracing", "enabled", false);
boolean isMetricsEnabled = getConfigBooleanValue("b7a.observability.metrics", "enabled", false);


//metrics
// public function setCounterDuration(int starting) returns int{
//     int ending = getCurrentTime();
//     int latency = (ending - starting);
//     return (latency);
// }

// public function CounterInitializing(string counterName) returns observe:Counter|() {
// 	if (isMetricsEnabled){
// 		observe:Counter localCounter = new(counterName);
// 		registeringCounter(localCounter);
// 		return localCounter;
// 	}
// 	else{
// 		return () ;
// 	}
// }

// public function UpdatingCounter(observe:Counter|() localCounter, int latency){
// 	if (localCounter is  observe:Counter){
// 		localCounter.increment(amount = latency);
// 	}
// }

// public function registeringCounter(observe:Counter Counter){
//     error? result = Counter.register();
//         if (result is error) {
//             log:printError("Error in registering Counter", err = result);
//         }
// }

public function setGaugeDuration(int starting) returns float{
    int ending = getCurrentTime();
    float latency = (ending - starting)*1.0;
    return (latency);
}

public function gaugeInitializing(string name, string description, map<string> gaugeTags) returns observe:Gauge|(){
	if (isMetricsEnabled){
		observe:Gauge localGauge = new(name, description,gaugeTags);
        registeringGauge(localGauge);
		return localGauge;
	}
	else{
		return ();
	}
}

public function UpdatingGauge(observe:Gauge|() localGauge, float latency){
	if (localGauge is  observe:Gauge){
		localGauge.setValue(latency);
	}
}

public function registeringGauge(observe:Gauge gauge){
    error? result = gauge.register();
        if (result is error) {
            log:printError("Error in registering Counter", err = result);
        }
}

public function gageTagDetails(http:Request request, http:FilterContext context, string category) returns map<string> {
    map<string> gaugeTags = { "Category":category , "Method":request.method, "ServicePath":request.rawPath, "Service": context.getServiceName()};
    return gaugeTags;
}

//tracing
public function startingSpan(string spanName) returns int|error|(){
    if (isTracingEnabled){
	    return observe:startSpan(spanName);
    }
    else {
        return ();
    }
}

public function finishingSpan(string spanName, int|error|() spanId){
    if (spanId is int) {
        error? result = observe:finishSpan(spanId);
        checkFinishSpanError(result, spanName);
    }
}

public function checkFinishSpanError(error? result, string spanName){
    if (result is error){
        log:printError(spanName, err=result);
    }
}