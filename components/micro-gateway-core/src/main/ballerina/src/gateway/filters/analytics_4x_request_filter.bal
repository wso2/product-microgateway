// Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type Analytics4xRequestFilter object {

    public function __init() {
        jinitAnalyticsDataPublisher();
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        printDebug(KEY_ANALYTICS_FILTER, "Analytics Version: 4.x");
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_ANALYTICS_FILTER, "Skip all filter annotation set in the service. Skipping the filter");
            return true;
        }
        //Filter only if analytics is enabled.
        if (isELKAnalyticsEnabled) {
            context.attributes[PROTOCOL_PROPERTY] = caller.protocol;
            doFilterRequest4x(request, context);
        }
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        printDebug(KEY_ANALYTICS_FILTER, "Analytics Version: 4.x");
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_ANALYTICS_FILTER, "Skip all filter annotation set in the service. Skipping the filter");
            return true;
        }
        if (isELKAnalyticsEnabled) {
            runtime:InvocationContext invocationContext = runtime:getInvocationContext();
            boolean filterFailed = <boolean>invocationContext.attributes[FILTER_FAILED];
            printDebug(KEY_ANALYTICS_FILTER, "Filter failed filter response : " + filterFailed.toString());
            printDebug(KEY_ANALYTICS_FILTER, "Response code filter response : " + response.statusCode.toString());
            printDebug(KEY_ANALYTICS_FILTER, "Context attributes filter response : " + context.attributes.toString());
            printDebug(KEY_ANALYTICS_FILTER, "Invocation context attributes filter response : " + invocationContext.attributes.toString());
            if (!context.attributes.hasKey(IS_THROTTLE_OUT)) {
                context.attributes[THROTTLE_LATENCY] = 0;
            }
            doFilterResponse4x(response, context);
        }
        return true;
    }
};


function doFilterRequest4x(http:Request request, http:FilterContext context) {
    printDebug(KEY_ANALYTICS_FILTER, "doFilterRequest4x Mehtod called");
    error? result = trap setRequestAttributesToContext(request, context);
    if (result is error) {
        printError(KEY_ANALYTICS_FILTER, "Error while setting analytics data in request path", result);
    }
}

function doFilterFault4x(http:Response response, http:FilterContext context) {
    printDebug(KEY_ANALYTICS_FILTER, "doFilterFault4x method called");
}

function doFilterResponse4x(http:Response response, http:FilterContext context) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (invocationContext.attributes.hasKey(ERROR_CODE) || invocationContext.attributes.hasKey(ERROR_RESPONSE_CODE) 
        || context.attributes.hasKey(ERROR_CODE)) {
        printDebug(KEY_ANALYTICS_FILTER, "doFilterResponse4x INVOCATION CONTEXT: " + invocationContext.attributes.toString());
        printDebug(KEY_ANALYTICS_FILTER, "doFilterResponse4x FILTER CONTEXT: " + context.attributes.toString());
        printDebug(KEY_ANALYTICS_FILTER, "doFilterResponse4x RESPONSE CODE: " + response.statusCode.toString());
        Analytics4xEventData | error analyticsEvent = generateFalut4xEventData(response, context);
        if (analyticsEvent is error) {
            printError(KEY_ANALYTICS_FILTER, "Error while generating analytics event", analyticsEvent);
            return;
        } else {
            jpublishAnalyticsEvent(analyticsEvent);
        }
    } else {
        Analytics4xEventData | error analyticsEvent = generateAnalytics4xEventData(response, context);
        if (analyticsEvent is error) {
            printError(KEY_ANALYTICS_FILTER, "Error while generating analytics event", analyticsEvent);
            return;
        } else {
            jpublishAnalyticsEvent(analyticsEvent);
        }
    }  
}

function doFilterThrottleResponse4x(http:Response response, http:FilterContext context) {
    printDebug(KEY_ANALYTICS_FILTER, "doFilterThrottleResponse4x method called");
}

function generateAnalytics4xEventData(http:Response response, http:FilterContext context) returns @tainted Analytics4xEventData | error {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    printDebug(KEY_ANALYTICS_FILTER, "Invocation context: " + invocationContext.attributes.toString());
    printDebug(KEY_ANALYTICS_FILTER, "Context: " + context.attributes.toString());
    printDebug(KEY_ANALYTICS_FILTER, "Response: " + response.toString());
    RequestResponseExecutionDTO|error requestResponseExecutionDTO = trap generateRequestResponseExecutionDataEvent(response, context);
    if ((requestResponseExecutionDTO is RequestResponseExecutionDTO && !validateEvent(requestResponseExecutionDTO)) || requestResponseExecutionDTO is error) {
        return error("Event dropped without publishing due to malformation of attributes");
    } else {
        //TODO: api uuid is currently set to the API service name since there could be occasions that the API UUID is not available
        Analytics4xEventData analyticsEvent = {
            isFault: false,
            isAnonymous: requestResponseExecutionDTO.isAnonymous,
            isAuthenticated: requestResponseExecutionDTO.isAuthenticated,
            responseCode: response.statusCode,
            //TODO: check API UUID
            apiUUID: <string> invocationContext.attributes[SERVICE_NAME_ATTR],
            apiType: "HTTP",
            apiName: <string> requestResponseExecutionDTO.apiName,
            apiVersion: <string> requestResponseExecutionDTO.apiVersion,
            apiContext: <string> requestResponseExecutionDTO.apiContext,
            apiCreator: <string> requestResponseExecutionDTO.apiCreator,
            apiCreatorTenantDomain: <string> requestResponseExecutionDTO.apiCreatorTenantDomain,
            organizationId: <string> requestResponseExecutionDTO.userTenantDomain,
            applicationUUID: <string> requestResponseExecutionDTO.applicationUUID,
            applicationName: <string> requestResponseExecutionDTO.applicationName,
            applicationOwner: <string> requestResponseExecutionDTO.applicationOwner,
            applicationKeyType: <string> requestResponseExecutionDTO.metaClientType,
            httpMethod: <string> requestResponseExecutionDTO.apiMethod,
            apiResourceTemplate: <string> requestResponseExecutionDTO.apiResourceTemplate,
            // TODO: check target response code
            targetResponseCode: response.statusCode,
            responseCacheHit: requestResponseExecutionDTO.responseCacheHit,
            destination: <string> requestResponseExecutionDTO.destination,
            requestTime: requestResponseExecutionDTO.requestTimestamp,
            correlationId: <string> requestResponseExecutionDTO.correlationId,
            // TODO: check region ID
            regionId: "region ID",
            userAgentHeader: <string> requestResponseExecutionDTO.userAgent,
            userName: <string> requestResponseExecutionDTO.userName,
            endUserIP: <string> requestResponseExecutionDTO.userIp,
            backendLatency: requestResponseExecutionDTO.backendTime,
            responseLatency: requestResponseExecutionDTO.responseTime,
            responseSize: requestResponseExecutionDTO.responseSize,
            responseContentType: requestResponseExecutionDTO.responseContentType
        };
        return analyticsEvent;
    }
}

function generateFalut4xEventData(http:Response response, http:FilterContext context) returns (Analytics4xEventData | error) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    printDebug(KEY_ANALYTICS_FILTER, "Invocation context: " + invocationContext.attributes.toString());
    printDebug(KEY_ANALYTICS_FILTER, "Context: " + context.attributes.toString());
    printDebug(KEY_ANALYTICS_FILTER, "Response: " + response.toString());
    FaultDTO|error faultDTO = trap populateFaultAnalytics4xDTO(response, context);
    if (faultDTO is error) {
        return error("Event dropped without publishing due to malformation of attributes");
    } else {
        //TODO: api uuid is currently set to the API service name since there could be occasions that the API UUID is not available
        Analytics4xEventData analyticsEvent = {
            isFault: true,
            isAnonymous: faultDTO.isAnonymous,
            isAuthenticated: faultDTO.isAuthenticated,
            responseCode: response.statusCode,
            apiUUID: <string> invocationContext.attributes[SERVICE_NAME_ATTR],
            apiType: "HTTP",
            apiName: <string> faultDTO.apiName,
            apiVersion: <string> faultDTO.apiVersion,
            apiContext: <string> faultDTO.apiContext,
            apiCreator: <string> faultDTO.apiCreator,
            apiCreatorTenantDomain: <string> faultDTO.apiCreatorTenantDomain,
            userName: <string> faultDTO.userName,
            organizationId: <string> faultDTO.userTenantDomain,
            applicationUUID: <string> faultDTO.applicationUUID,
            applicationName: <string> faultDTO.applicationName,
            applicationOwner: <string> faultDTO.applicationOwner,
            applicationKeyType: <string> faultDTO.keyType,
            httpMethod: <string> faultDTO.method,
            apiResourceTemplate: <string> faultDTO.apiResourceTemplate,
            requestTime: faultDTO.faultTime,
            errorCode: faultDTO.errorCode,
            correlationId: <string> faultDTO.correlationId
        };
        return analyticsEvent;
    }
}
