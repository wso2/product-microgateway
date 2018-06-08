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
import ballerina/log;
import ballerina/cache;
import ballerina/config;
import ballerina/time;

@Description { value: "Representation of the Throttle filter" }
@Field { value: "filterRequest: request filter method which attempts to throttle the request" }
@Field { value: "filterRequest: response filter method (not used this scenario)" }
public type ThrottleFilter object {

    @Description { value: "Filter function implementation which tries to throttle the request" }
    @Param { value: "request: Request instance" }
    @Param { value: "context: FilterContext instance" }
    @Return { value: "FilterResult: Authorization result to indicate if the request can proceed or not" }
    public function filterRequest(http:Request request, http:FilterContext context) returns http:FilterResult {
        match <boolean> context.attributes[FILTER_FAILED]{
            boolean failed => {
                if(failed) {
                    return createFilterResult(true, 200, "Skipping filter due to parent filter has returned false");
                }
            } error err => {
            //Nothing to handle
            }
        }
        http:FilterResult requestFilterResult;
        boolean resourceLevelThrottled;
        boolean apiLevelThrottled;
        string resourceLevelThrottleKey;

        //Throttle Tiers
        string applicationLevelTier;
        string subscriptionLevelTier;
        TierConfiguration tier = getResourceLevelTier(reflect:getResourceAnnotations(context.serviceType,
                context.resourceName));
        string resourceLevelTier = tier.policy;
        string apiLevelTier;
        //Throttled decisions
        boolean isThrottled = false;
        boolean isResourceLevelThrottled = false;
        boolean apiLevelThrottledTriggered = false;
        boolean stopOnQuotaReach = true;
        string apiContext = getContext(context);
        string apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
        apiVersion;
        if (context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
            AuthenticationContext keyvalidationResult = check <AuthenticationContext>context.attributes[
            AUTHENTICATION_CONTEXT];
            requestFilterResult = {canProceed:true};
            if (isSubscriptionLevelThrottled(context, keyvalidationResult)){
                if (keyvalidationResult.stopOnQuotaReach) {
                    requestFilterResult = {canProceed:false, statusCode:429, message:
                    "You have exceeded your quota"};
                    publishThrottleAnalyticsEvent(request, context, keyvalidationResult,
                        THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED);
                    context.attributes[IS_THROTTLE_OUT] = false;
                    return requestFilterResult;
                } else {
                    // set properties in order to publish into analytics for billing
                    context.attributes[IS_THROTTLE_OUT] = true;
                }
            }
            if (isApplicationLevelThrottled(keyvalidationResult)){
                requestFilterResult = {canProceed:false, statusCode:429, message:
                "You have exceeded your quota"};
                publishThrottleAnalyticsEvent(request, context, keyvalidationResult,
                    THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED);
                return requestFilterResult;
            }

            //Publish throttle event to internal policies
            RequestStreamDTO throttleEvent = generateThrottleEvent(request, context,
                keyvalidationResult);
            publishNonThrottleEvent(throttleEvent);
        } else {
            requestFilterResult = {canProceed:false, statusCode:500, message:"Internal Error Occurred"};
        }
        return requestFilterResult;
    }
};

function isApiLevelThrottled(AuthenticationContext keyValidationDto) returns (boolean) {
    if (keyValidationDto.apiTier != "" && keyValidationDto.apiTier != UNLIMITED_TIER){
    }
    return false;
}

function isResourceLevelThrottled(AuthenticationContext keyValidationDto) returns (boolean) {
    return false;
}

function isHardlimitThrottled(string context, string apiVersion) returns (boolean) {

    return false;
}


function isSubscriptionLevelThrottled(http:FilterContext context, AuthenticationContext keyValidationDto) returns (
            boolean) {
    string subscriptionLevelThrottleKey = keyValidationDto.applicationId + ":" + getContext
        (context) + ":" + getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).apiVersion
    ;
    if (isThrottled(subscriptionLevelThrottleKey)){
        return true;
    } else {
        return false;
    }
}

function isApplicationLevelThrottled(AuthenticationContext keyValidationDto) returns (boolean) {
    string applicationLevelThrottleKey = keyValidationDto.applicationId + ":" + keyValidationDto.username;
    if (isThrottled(applicationLevelThrottleKey)){
        return true;
    } else {
        return false;
    }
}
function generateThrottleEvent(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto)
             returns (
                     RequestStreamDTO) {
    RequestStreamDTO requestStreamDto;
    string apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations
        (context.serviceType)).apiVersion;
    requestStreamDto.messageID = <string>context.attributes[MESSAGE_ID];
    requestStreamDto.apiKey = getContext(context) + ":" + apiVersion;
    requestStreamDto.appKey = keyValidationDto.applicationId + ":" + keyValidationDto.username;
    requestStreamDto.subscriptionKey = keyValidationDto.applicationId + ":" + getContext(context) + ":" +
        apiVersion;
    requestStreamDto.appTier = keyValidationDto.applicationTier;
    requestStreamDto.apiTier = keyValidationDto.apiTier;
    requestStreamDto.subscriptionTier = keyValidationDto.tier;
    requestStreamDto.resourceKey = getContext(context) + "/" + getAPIDetailsFromServiceAnnotation(reflect:
            getServiceAnnotations(context.serviceType)).apiVersion;
    TierConfiguration tier = getResourceLevelTier(reflect:getResourceAnnotations(context.serviceType,
            context.resourceName));
    requestStreamDto.resourceTier = tier.policy;
    requestStreamDto.userId = keyValidationDto.username;
    requestStreamDto.apiContext = getContext(context);
    requestStreamDto.apiVersion = apiVersion;
    requestStreamDto.appTenant = keyValidationDto.subscriberTenantDomain;
    requestStreamDto.apiTenant = getTenantDomain(context);
    requestStreamDto.apiName = getApiName(context);

    json properties = {};
    string remoteAddr = getClientIp(req);
    if(remoteAddr != "") {
        properties.ip = ipToLong(remoteAddr);
    }
    if (getGatewayConfInstance().getThrottleConf().enabledHeaderConditions){
        string[] headerNames = req.getHeaderNames();
        foreach headerName in headerNames {
            string headerValue = untaint req.getHeader(headerName);
            properties[headerName] = headerValue;
        }
    }
    if (getGatewayConfInstance().getThrottleConf().enabledQueryParamConditions){
        foreach k, v in req.getQueryParams() {
            properties[k] = v;
        }
    }
    if (getGatewayConfInstance().getThrottleConf().enabledJWTClaimConditions){
        foreach k, v in runtime:getInvocationContext().userPrincipal.claims {
            properties[k] = <string>v;
        }
    }

    requestStreamDto.properties = properties.toString();
    return requestStreamDto;
}

function ipToLong(string ipAddress) returns (int) {
    int result = 0;
    string[] ipAddressInArray = ipAddress.split("\\.");
    int i = 3;
    while (i >= 0 && (3-i) < lengthof ipAddressInArray) {
        match <int>ipAddressInArray[3 - i] {
            int ip => {
                result = result + shiftLeft(ip, i * 8);
                i = i - 1;
                //left shifting 24,16,8,0 and bitwise OR
                //1. 192 << 24
                //1. 168 << 16
                //1. 1   << 8
                //1. 2   << 0
            }
            error e => {
                log:printError("Error while converting Ip address to long");
                i = i - 1;
            }
        }
    }
    return result;
}

function shiftLeft(int a, int b) returns (int) {
    if (b == 0) {
        return a;
    }
    int i = b;
    int result = a;
    while (i > 0) {
        result = result * 2;
        i = i - 1;
    }
    return result;
}

function getMessageSize() returns (int) {
    return 0;
}

function publishThrottleAnalyticsEvent(http:Request req, http:FilterContext context, AuthenticationContext authConext,
    string reason) {
    ThrottleAnalyticsEventDTO eventDto = populateThrottleAnalyticdDTO(req, context, authConext, reason);
    //todo: publish eventDto to a stream
}

function populateThrottleAnalyticdDTO(http:Request req, http:FilterContext context, AuthenticationContext authConext,
    string reason) returns (ThrottleAnalyticsEventDTO) {
    ThrottleAnalyticsEventDTO eventDto;
    string apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).apiVersion;
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;

    json metaInfo = {};
    metaInfo.keyType = authConext.keyType;
    metaInfo.correlationID = <string>context.attributes[MESSAGE_ID];
    eventDto.clientType = metaInfo.toString();
    eventDto.accessToken = "-";
    eventDto.userId = authConext.username;
    eventDto.tenantDomain = getTenantDomain(context);
    eventDto.api = getApiName(context);
    eventDto.api_version = apiVersion;
    eventDto.context = getContext(context);
    eventDto.apiPublisher = authConext.apiPublisher;
    eventDto.throttledTime = currentTimeMills;
    eventDto.applicationName = authConext.applicationName;
    eventDto.applicationId = authConext.applicationId;
    eventDto.subscriber = authConext.subscriber;
    eventDto.throttledOutReason = reason;
    return eventDto;
}