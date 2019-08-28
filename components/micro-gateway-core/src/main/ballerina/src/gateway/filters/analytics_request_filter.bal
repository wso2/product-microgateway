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
import ballerina/runtime;

public type AnalyticsRequestFilter object {

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        //Filter only if analytics is enabled.
        if (isAnalyticsEnabled) {
            checkOrSetMessageID(context);
            context.attributes[PROTOCOL_PROPERTY] = caller.protocol;
            doFilterRequest(request, context);
        }
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {

        if (isAnalyticsEnabled) {
            boolean filterFailed = <boolean>context.attributes[FILTER_FAILED];
            if (context.attributes.hasKey(IS_THROTTLE_OUT)) {
                boolean isThrottleOut = <boolean>context.attributes[IS_THROTTLE_OUT];
                if (isThrottleOut) {
                    ThrottleAnalyticsEventDTO eventDto = populateThrottleAnalyticsDTO(context);
                    eventStream.publish(getEventFromThrottleData(eventDto));
                } else {
                    if (!filterFailed) {
                        doFilterAll(response, context);
                    }
                }
            } else {
                if (!filterFailed) {
                    context.attributes[THROTTLE_LATENCY] = 0;
                    doFilterAll(response, context);
                }
            }
        }
        return true;
    }

};


function doFilterRequest(http:Request request, http:FilterContext context) {
    setRequestAttributesToContext(request, context);
}

function doFilterFault(http:FilterContext context, error err) {
    FaultDTO faultDTO = populateFaultAnalyticsDTO(context, err);
    eventStream.publish(getEventFromFaultData(faultDTO));
}

function doFilterResponseData(http:Response response, http:FilterContext context) {
    //Response data publishing
    RequestResponseExecutionDTO requestResponseExecutionDTO = generateRequestResponseExecutionDataEvent(response,
        context);
    EventDTO event = generateEventFromRequestResponseExecutionDTO(requestResponseExecutionDTO);
    eventStream.publish(event);
}

function doFilterAll(http:Response response, http:FilterContext context) {
    // TODO: refactor the logic. error does not belong to type any
    var code = runtime:getInvocationContext().attributes[ERROR_RESPONSE];
    if (code is ()) {
        printDebug(KEY_ANALYTICS_FILTER, "No any faulty analytics events to handle.");
        doFilterResponseData(response, context);
    }
    // } else if(code is error) {
    //     printDebug(KEY_ANALYTICS_FILTER, "Error response value present and handling faulty analytics events");
    //     doFilterFault(context, code);
    // }
}
