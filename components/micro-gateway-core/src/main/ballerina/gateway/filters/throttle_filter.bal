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

public type ThrottleFilter object {
    public map<boolean> deployedPolicies = {};

    public function __init(map<boolean> deployedPolicies) {
        self.deployedPolicies = deployedPolicies;
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        int startingTime = getCurrentTime();
        checkOrSetMessageID(context);
        boolean result = doThrottleFilterRequest(caller, request, context, self.deployedPolicies);
        setLatency(startingTime, context, THROTTLE_LATENCY);
        return result;
    }



    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }
};

// TODO: need to refactor this function.
function doThrottleFilterRequest(http:Caller caller, http:Request request, http:FilterContext context,map<boolean>
    deployedPolicies) returns boolean {
    printDebug(KEY_THROTTLE_FILTER, "Processing the request in ThrottleFilter");
    //Throttle Tiers
    string applicationLevelTier;
    string subscriptionLevelTier;
    //Throttled decisions
    boolean isThrottled = false;
    boolean stopOnQuota;
    string apiContext = getContext(context);
    string? apiVersion = apiConfigAnnotationMap[getServiceName(context.serviceName)].apiVersion;
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    context.attributes[ALLOWED_ON_QUOTA_REACHED] = false;
    context.attributes[IS_THROTTLE_OUT] = false;

    AuthenticationContext keyvalidationResult = {};
    if (context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        if (isRequestBlocked(caller, request, context)) {
            setThrottleErrorMessageToContext(context, FORBIDDEN, BLOCKING_ERROR_CODE,
                BLOCKING_MESSAGE, BLOCKING_DESCRIPTION);
            sendErrorResponse(caller, request, context);
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Context contains Authentication Context");
        keyvalidationResult = <AuthenticationContext>context.attributes[
        AUTHENTICATION_CONTEXT];
        printDebug(KEY_THROTTLE_FILTER, "Checking subscription level throttle policy '" + keyvalidationResult.
                tier
                + "' exist.");
        if (keyvalidationResult.tier != UNLIMITED_TIER && !deployedPolicies.hasKey(keyvalidationResult.tier)) {
            printDebug(KEY_THROTTLE_FILTER, "Subscription level throttle policy '" + keyvalidationResult.tier
                    + "' is not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
                INTERNAL_SERVER_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_MESSAGE);
            sendErrorResponse(caller, request, context);
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking subscription level throttling-out.");
        (isThrottled, stopOnQuota) =         isSubscriptionLevelThrottled(context, keyvalidationResult);
        printDebug(KEY_THROTTLE_FILTER, "Subscription level throttling result:: isThrottled:"
                + isThrottled + ", stopOnQuota:" + stopOnQuota);
        if (isThrottled) {
            if (stopOnQuota) {
                printDebug(KEY_THROTTLE_FILTER, "Sending throttled out responses.");
                context.attributes[IS_THROTTLE_OUT] = true;
                context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED;
                setThrottleErrorMessageToContext(context, THROTTLED_OUT, SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE,
                    THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
                sendErrorResponse(caller, request, context);
                return false;
            } else {
                // set properties in order to publish into analytics for billing
                context.attributes[ALLOWED_ON_QUOTA_REACHED] = true;
                printDebug(KEY_THROTTLE_FILTER, "Proceeding(1st) since stopOnQuota is set to false.");
            }
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking application level throttle policy '"
                + keyvalidationResult.applicationTier + "' exist.");
        if (keyvalidationResult.applicationTier != UNLIMITED_TIER &&
            !deployedPolicies.hasKey(keyvalidationResult.applicationTier)) {
            printDebug(KEY_THROTTLE_FILTER, "Application level throttle policy '"
                    + keyvalidationResult.applicationTier + "' is not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
                INTERNAL_SERVER_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_MESSAGE);
            sendErrorResponse(caller, request, context);
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking application level throttling-out.");
        if (isApplicationLevelThrottled(keyvalidationResult)) {
            printDebug(KEY_THROTTLE_FILTER, "Application level throttled out. Sending throttled out response.");
            context.attributes[IS_THROTTLE_OUT] = true;
            context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED;
            setThrottleErrorMessageToContext(context, THROTTLED_OUT, APPLICATION_THROTTLE_OUT_ERROR_CODE,
                THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
            sendErrorResponse(caller, request, context);
            return false;
        } else {
            printDebug(KEY_THROTTLE_FILTER, "Application level throttled out: false");
        }
    } else if (!isSecured) {
        printDebug(KEY_THROTTLE_FILTER, "Not a secured resource. Proceeding with Unauthenticated tier.");
        // setting keytype to invocationContext
        runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = PRODUCTION_KEY_TYPE;

        printDebug(KEY_THROTTLE_FILTER, "Checking unauthenticate throttle policy '" + UNAUTHENTICATED_TIER
                + "' exist.");
        if (!deployedPolicies.hasKey(UNAUTHENTICATED_TIER)) {
            printDebug(KEY_THROTTLE_FILTER, "Unauthenticate throttle policy '" + UNAUTHENTICATED_TIER
                    + "' is not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
                INTERNAL_SERVER_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_MESSAGE);
            sendErrorResponse(caller, request, context);
            return false;
        }
        (isThrottled, stopOnQuota) =         isUnauthenticateLevelThrottled(context);
        printDebug(KEY_THROTTLE_FILTER, "Unauthenticated tier throttled out result:: isThrottled:"
                + isThrottled + ", stopOnQuota:" + stopOnQuota);
        if (isThrottled) {
            if (stopOnQuota) {
                printDebug(KEY_THROTTLE_FILTER, "Sending throttled out response.");
                context.attributes[IS_THROTTLE_OUT] = true;
                context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED;
                setThrottleErrorMessageToContext(context, THROTTLED_OUT, SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE,
                    THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
                sendErrorResponse(caller, request, context);
                return false;
            } else {
                // set properties in order to publish into analytics for billing
                context.attributes[ALLOWED_ON_QUOTA_REACHED] = true;
                printDebug(KEY_THROTTLE_FILTER, "Proceeding(2nd) since stopOnQuota is set to false.");
            }
        }
        string clientIp = <string>context.attributes[REMOTE_ADDRESS];
        keyvalidationResult.authenticated = true;
        keyvalidationResult.tier = UNAUTHENTICATED_TIER;
        keyvalidationResult.stopOnQuotaReach = true;
        keyvalidationResult.apiKey = clientIp;
        keyvalidationResult.username = END_USER_ANONYMOUS;
        keyvalidationResult.applicationId = clientIp;
        keyvalidationResult.keyType = PRODUCTION_KEY_TYPE;
        // setting keytype to invocationContext
        runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = keyvalidationResult.keyType;
    } else {
        printDebug(KEY_THROTTLE_FILTER, "Unknown error.");
        setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR, INTERNAL_ERROR_CODE,
            INTERNAL_SERVER_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_MESSAGE);
        sendErrorResponse(caller, request, context);
        return false;
    }

    //Publish throttle event to another worker flow to publish to internal policies or traffic manager
    RequestStreamDTO throttleEvent = generateThrottleEvent(request, context, keyvalidationResult);
    future<()> publishedEvent = start asyncPublishEvent(throttleEvent);
    printDebug(KEY_THROTTLE_FILTER, "Request is not throttled");
    return true;
}

function asyncPublishEvent(RequestStreamDTO throttleEvent) {
    printDebug(KEY_THROTTLE_FILTER, "Checking application sending throttle event to another worker.");
    publishNonThrottleEvent(throttleEvent);
}

function setThrottleErrorMessageToContext(http:FilterContext context, int statusCode, int errorCode, string
    errorMessage, string errorDescription) {
    context.attributes[HTTP_STATUS_CODE] = statusCode;
    context.attributes[FILTER_FAILED] = true;
    context.attributes[ERROR_CODE] = errorCode;
    context.attributes[ERROR_MESSAGE] = errorMessage;
    context.attributes[ERROR_DESCRIPTION] = errorDescription;
}

function isSubscriptionLevelThrottled(http:FilterContext context, AuthenticationContext keyValidationDto) returns (
    boolean, boolean) {
    if (keyValidationDto.tier == UNLIMITED_TIER) {
        return (false, false);
    }

    string? apiVersion = apiConfigAnnotationMap[getServiceName(context.serviceName)].apiVersion;
    string subscriptionLevelThrottleKey = keyValidationDto.applicationId + ":" + getContext(context);
    if (apiVersion is string) {
        subscriptionLevelThrottleKey += ":" + apiVersion;
    }
    return isRequestThrottled(subscriptionLevelThrottleKey);
}

function isApplicationLevelThrottled(AuthenticationContext keyValidationDto) returns (boolean) {
    if (keyValidationDto.applicationTier == UNLIMITED_TIER) {
        return false;
    }
    string applicationLevelThrottleKey = keyValidationDto.applicationId + ":" + keyValidationDto.username;
    boolean throttled;
    boolean stopOnQuota;
    (throttled, stopOnQuota) = isRequestThrottled(applicationLevelThrottleKey);
    return throttled;
}

function isUnauthenticateLevelThrottled(http:FilterContext context) returns (boolean, boolean) {
    string clientIp = <string>context.attributes[REMOTE_ADDRESS];
    string? apiVersion = apiConfigAnnotationMap[getServiceName(context.serviceName)].apiVersion;
    string throttleKey = clientIp + ":" + getContext(context);
    if (apiVersion is string) {
        throttleKey += ":" + apiVersion;
    }
    return isRequestThrottled(throttleKey);
}
function isRequestBlocked(http:Caller caller, http:Request request, http:FilterContext context) returns (boolean) {
    AuthenticationContext keyvalidationResult = <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
    string apiLevelBlockingKey = getContext(context);
    string apiTenantDomain = getTenantDomain(context);
    string ipLevelBlockingKey = apiTenantDomain + ":" + getClientIp(request, caller);
    string appLevelBlockingKey = keyvalidationResult.subscriber + ":" + keyvalidationResult.applicationName;
    if (isAnyBlockConditionExist() && (isBlockConditionExist(apiLevelBlockingKey) ||
    isBlockConditionExist(ipLevelBlockingKey) || isBlockConditionExist(appLevelBlockingKey)) ||
    isBlockConditionExist(keyvalidationResult.username)) {
        return true;
    } else {
        return false;
    }
}

function generateThrottleEvent(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDto = {};
    string? apiVersion = apiConfigAnnotationMap[getServiceName(context.serviceName)].apiVersion;
    requestStreamDto.messageID = <string>context.attributes[MESSAGE_ID];
    requestStreamDto.apiKey = getContext(context);
    requestStreamDto.appKey = keyValidationDto.applicationId + ":" + keyValidationDto.username;
    requestStreamDto.subscriptionKey = keyValidationDto.applicationId + ":" + getContext(context);
    requestStreamDto.appTier = keyValidationDto.applicationTier;
    requestStreamDto.apiTier = keyValidationDto.apiTier;
    requestStreamDto.subscriptionTier = keyValidationDto.tier;
    requestStreamDto.resourceKey = getContext(context);
    TierConfiguration? tier = resourceTierAnnotationMap[context.resourceName];
    string? policy = tier.policy;
    if (policy is string) {
       requestStreamDto.resourceTier = policy;
    }
    requestStreamDto.userId = keyValidationDto.username;
    requestStreamDto.apiContext = getContext(context);
    if (apiVersion is string) {
        requestStreamDto.apiVersion = apiVersion;
    }
    requestStreamDto.appTenant = keyValidationDto.subscriberTenantDomain;
    requestStreamDto.apiTenant = getTenantDomain(context);
    requestStreamDto.apiName = getApiName(context);
    requestStreamDto.appId = keyValidationDto.applicationId;

    if (apiVersion is string) {
        requestStreamDto.apiKey += ":" + apiVersion;
        requestStreamDto.subscriptionKey +=":" + apiVersion;
        requestStreamDto.resourceKey += "/" + apiVersion;
    }

    json properties = {};
    requestStreamDto.properties = properties.toString();
    return requestStreamDto;
}
