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

import ballerina/io;
import ballerina/http;
import ballerina/time;

public type AnalyticsRequestFilter object {

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        //Filter only if analytics is enabled.
        if (isAnalyticsEnabled) {
            checkOrSetMessageID(context);
            context.attributes[PROTOCOL_PROPERTY] = caller.protocol;
            error? err = trap doFilterRequest(request, context);
            if(err is error) {
                printError(KEY_ANALYTICS_FILTER, "Error while setting analytics data in request path");
                printFullError(KEY_ANALYTICS_FILTER, err);
            }
        }
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {

        if (isAnalyticsEnabled) {
            boolean filterFailed = <boolean>context.attributes[FILTER_FAILED];
            if (context.attributes.hasKey(IS_THROTTLE_OUT)) {
                boolean isThrottleOut = <boolean>context.attributes[IS_THROTTLE_OUT];
                if (isThrottleOut) {
                    ThrottleAnalyticsEventDTO|error  throttleAnalyticsEventDTO = trap populateThrottleAnalyticsDTO(context);
                    if(throttleAnalyticsEventDTO is ThrottleAnalyticsEventDTO) {
                        EventDTO|error eventDTO  = trap getEventFromThrottleData(throttleAnalyticsEventDTO);
                        if(eventDTO is EventDTO) {
                            eventStream.publish(eventDTO);
                        } else {
                            printError(KEY_ANALYTICS_FILTER, "Error while creating throttle analytics event");
                            printFullError(KEY_ANALYTICS_FILTER, eventDTO);
                        }
                    } else {
                        printError(KEY_ANALYTICS_FILTER, "Error while populating throttle analytics event data");
                        printFullError(KEY_ANALYTICS_FILTER, throttleAnalyticsEventDTO);
                    }
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

function doFilterFault(http:FilterContext context, string errorMessage) {
    FaultDTO|error faultDTO = trap populateFaultAnalyticsDTO(context, errorMessage);
    if(faultDTO is FaultDTO) {
        EventDTO|error eventDTO = trap getEventFromFaultData(faultDTO);
        if(eventDTO is EventDTO) {
            eventStream.publish(eventDTO);
        } else {
            printError(KEY_ANALYTICS_FILTER, "Error while genaratting analytics data for fault event");
            printFullError(KEY_ANALYTICS_FILTER, eventDTO);
        }
    } else {
        printError(KEY_ANALYTICS_FILTER, "Error while populating analytics fault event data");
        printFullError(KEY_ANALYTICS_FILTER, faultDTO);
    }
}

function doFilterResponseData(http:Response response, http:FilterContext context) {
    //Response data publishing
    RequestResponseExecutionDTO|error requestResponseExecutionDTO = trap generateRequestResponseExecutionDataEvent(response,
            context);
    if(requestResponseExecutionDTO is RequestResponseExecutionDTO) {
        EventDTO|error event = trap generateEventFromRequestResponseExecutionDTO(requestResponseExecutionDTO);
        if(event is EventDTO) {
            eventStream.publish(event);
        } else {
            printError(KEY_ANALYTICS_FILTER, "Error while genarating analytics data event");
            printFullError(KEY_ANALYTICS_FILTER, event);
        }
    } else {
        printError(KEY_ANALYTICS_FILTER, "Error while publishing analytics data");
        printFullError(KEY_ANALYTICS_FILTER, requestResponseExecutionDTO);
    }
}

function doFilterAll(http:Response response, http:FilterContext context) {
    var resp = runtime:getInvocationContext().attributes[ERROR_RESPONSE];
    if (resp is ()) {
        printDebug(KEY_ANALYTICS_FILTER, "No any faulty analytics events to handle.");
        doFilterResponseData(response, context);
    } else if(resp is string) {
        printDebug(KEY_ANALYTICS_FILTER, "Error response value present and handling faulty analytics events");
        doFilterFault(context, resp);
    }
}
