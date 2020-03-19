// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type ThrottleFilter object {
    public map<json> deployedPolicies = {};

    public function __init(map<json> deployedPolicies) {
        self.deployedPolicies = deployedPolicies;
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_THROTTLE_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        int startingTime = getCurrentTimeForAnalytics();
        boolean result = doThrottleFilterRequest(caller, request, context, self.deployedPolicies);
        setLatency(startingTime, context, THROTTLE_LATENCY);
        return result;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }
};

// TODO: need to refactor this function.
function doThrottleFilterRequest(http:Caller caller, http:Request request, http:FilterContext context, map<json>
deployedPolicies) returns boolean {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    printDebug(KEY_THROTTLE_FILTER, "Processing the request in ThrottleFilter");
    //Throttle Tiers
    string applicationLevelTier;
    string subscriptionLevelTier;
    //Throttled decisions
    boolean isThrottled = false;
    boolean stopOnQuota;
    string apiContext = getContext(context);
    boolean isSecured = <boolean>invocationContext.attributes[IS_SECURED];
    context.attributes[ALLOWED_ON_QUOTA_REACHED] = false;
    context.attributes[IS_THROTTLE_OUT] = false;

    AuthenticationContext keyValidationResult = {};
    string? apiVersion = getVersion(context);
    string? resourceLevelPolicyName = getResourceLevelPolicy(context);
    if (invocationContext.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        printDebug(KEY_THROTTLE_FILTER, "Context contains Authentication Context");
        keyValidationResult = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        if (isRequestBlocked(caller, request, context, keyValidationResult)) {
            setThrottleErrorMessageToContext(context, FORBIDDEN, BLOCKING_ERROR_CODE,
            BLOCKING_MESSAGE, BLOCKING_DESCRIPTION);
            sendErrorResponse(caller, request, context);
            return false;
        }
        string apiLevelPolicy = getAPITier(context.getServiceName(),keyValidationResult.apiTier);
        if(!checkAPILevelThrottled(caller, request, context, apiLevelPolicy, deployedPolicies, apiVersion)) {
            return false;
        }
        if(!checkResourceLevelThrottled(caller, request, context, resourceLevelPolicyName, deployedPolicies, apiVersion)) {
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking subscription level throttle policy '" + keyValidationResult.
                tier + "' exist.");
        if (keyValidationResult.tier != UNLIMITED_TIER && !isPolicyExist(deployedPolicies, keyValidationResult.tier, SUB_LEVEL_PREFIX)) {
            printDebug(KEY_THROTTLE_FILTER, "Subscription level throttle policy '" + keyValidationResult.tier
            + "' does not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
            INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
            INTERNAL_SERVER_ERROR_MESSAGE, POLICY_NOT_FOUND_DESCRIPTION);
            sendErrorResponse(caller, request, context);
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking subscription level throttling-out.");
        [isThrottled, stopOnQuota] = isSubscriptionLevelThrottled(context, keyValidationResult, deployedPolicies, apiVersion);
        printDebug(KEY_THROTTLE_FILTER, "Subscription level throttling result:: isThrottled:"
        + isThrottled.toString() + ", stopOnQuota:" + stopOnQuota.toString());
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
        + keyValidationResult.applicationTier + "' exist.");
        if (keyValidationResult.applicationTier != UNLIMITED_TIER &&
        !isPolicyExist(deployedPolicies, keyValidationResult.applicationTier, APP_LEVEL_PREFIX)) {
            printDebug(KEY_THROTTLE_FILTER, "Application level throttle policy '"
            + keyValidationResult.applicationTier + "' does not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
            INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
            INTERNAL_SERVER_ERROR_MESSAGE, POLICY_NOT_FOUND_DESCRIPTION);
            sendErrorResponse(caller, request, context);
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking application level throttling-out.");
        if (isApplicationLevelThrottled(keyValidationResult, deployedPolicies)) {
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
        string apiLevelPolicy = getAPITier(context.getServiceName(),"");
        if(!checkAPILevelThrottled(caller, request, context, apiLevelPolicy, deployedPolicies, apiVersion)) {
            return false;
        }
        if(!checkResourceLevelThrottled(caller, request, context, resourceLevelPolicyName, deployedPolicies, apiVersion)) {
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Not a secured resource. Proceeding with Unauthenticated tier.");
        // setting keytype to invocationContext
        invocationContext.attributes[KEY_TYPE_ATTR] = PRODUCTION_KEY_TYPE;

        printDebug(KEY_THROTTLE_FILTER, "Checking unauthenticated throttle policy '" + UNAUTHENTICATED_TIER
        + "' exist.");
        if (!isPolicyExist(deployedPolicies, UNAUTHENTICATED_TIER, SUB_LEVEL_PREFIX)) {
            printDebug(KEY_THROTTLE_FILTER, "Unauthenticated throttle policy '" + UNAUTHENTICATED_TIER
            + "' is not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
            INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
            INTERNAL_SERVER_ERROR_MESSAGE, POLICY_NOT_FOUND_DESCRIPTION);
            sendErrorResponse(caller, request, context);
            return false;
        }
        [isThrottled, stopOnQuota] = isUnauthenticateLevelThrottled(context);
        printDebug(KEY_THROTTLE_FILTER, "Unauthenticated tier throttled out result:: isThrottled:"
        + isThrottled.toString() + ", stopOnQuota:" + stopOnQuota.toString());
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
        keyValidationResult.authenticated = true;
        keyValidationResult.tier = UNAUTHENTICATED_TIER;
        keyValidationResult.stopOnQuotaReach = true;
        keyValidationResult.apiKey = clientIp;
        keyValidationResult.username = END_USER_ANONYMOUS;
        keyValidationResult.applicationId = clientIp;
        keyValidationResult.keyType = PRODUCTION_KEY_TYPE;
        // setting keytype to invocationContext
        invocationContext.attributes[KEY_TYPE_ATTR] = keyValidationResult.keyType;
    } else {
        printDebug(KEY_THROTTLE_FILTER, "Unknown error.");
        setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR, INTERNAL_ERROR_CODE,
        INTERNAL_SERVER_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_MESSAGE);
        sendErrorResponse(caller, request, context);
        return false;
    }

    //Publish throttle event to another worker flow to publish to internal policies or traffic manager
    RequestStreamDTO throttleEvent = generateThrottleEvent(request, context, keyValidationResult, deployedPolicies);
    publishEvent(throttleEvent);
    printDebug(KEY_THROTTLE_FILTER, "Request is not throttled");
    return true;
}

function publishEvent(RequestStreamDTO throttleEvent) {
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

function isSubscriptionLevelThrottled(http:FilterContext context, AuthenticationContext keyValidationDto,
        map<json> deployedPolicies, string? apiVersion) returns [
 boolean, boolean] {
    if (keyValidationDto.tier == UNLIMITED_TIER) {
        return [false, false];
    }

    string subscriptionLevelThrottleKey = keyValidationDto.applicationId + ":" + getContext(context);
    if (apiVersion is string) {
        subscriptionLevelThrottleKey += ":" + apiVersion;
    }
    printDebug(KEY_THROTTLE_FILTER, "Subscription level throttle key : " + subscriptionLevelThrottleKey);
    if (!enabledGlobalTMEventPublishing) {
        boolean stopOnQuota = <boolean>deployedPolicies.get(SUB_LEVEL_PREFIX + keyValidationDto.tier).stopOnQuota;
        boolean isThrottled = isSubLevelThrottled(subscriptionLevelThrottleKey);
        return [isThrottled, stopOnQuota];
    }
    return isRequestThrottled(subscriptionLevelThrottleKey);
}

function isApplicationLevelThrottled(AuthenticationContext keyValidationDto, map<json>
                                                                             deployedPolicies) returns (boolean) {
    if (keyValidationDto.applicationTier == UNLIMITED_TIER) {
        return false;
    }
    string applicationLevelThrottleKey = keyValidationDto.applicationId + ":" + keyValidationDto.username;
    printDebug(KEY_THROTTLE_FILTER, "Application level throttle key : " + applicationLevelThrottleKey);
    boolean throttled;
    boolean stopOnQuota;
    if (!enabledGlobalTMEventPublishing) {
        return isAppLevelThrottled(applicationLevelThrottleKey);
    }
    [throttled, stopOnQuota] = isRequestThrottled(applicationLevelThrottleKey);
    return throttled;
}

function isAPILevelThrottled(http:FilterContext context, string? apiVersion) returns boolean {
    boolean throttled;
    boolean stopOnQuota;
    string apiThrottleKey = getContext(context);
    if (apiVersion is string) {
        apiThrottleKey += ":" + apiVersion;
    }
    if (enabledGlobalTMEventPublishing) {
        apiThrottleKey += "_default";
    }
    if (!enabledGlobalTMEventPublishing) {
        return isApiThrottled(apiThrottleKey);
    }
    [throttled, stopOnQuota] = isRequestThrottled(apiThrottleKey);
    return throttled;
}


function isResourceLevelThrottled(http:FilterContext context, string? policy,
        map<json> deployedPolicies, string? apiVersion) returns (boolean) {
    if (policy is string) {
        if (policy == UNLIMITED_TIER) {
            return false;
        }

        string resourceLevelThrottleKey = context.getResourceName();
        if (apiVersion is string) {
            resourceLevelThrottleKey += ":" + apiVersion;
        }
        if (enabledGlobalTMEventPublishing) {
            resourceLevelThrottleKey += "_default";
        }
        printDebug(KEY_THROTTLE_FILTER, "Resource level throttle key : " + resourceLevelThrottleKey);
        boolean throttled;
        boolean stopOnQuota;
        if (!enabledGlobalTMEventPublishing) {
            return isResourceThrottled(resourceLevelThrottleKey);
        }
        [throttled, stopOnQuota] = isRequestThrottled(resourceLevelThrottleKey);
        return throttled;
    }
    return false;
}

function getResourceLevelPolicy(http:FilterContext context) returns string? {
    TierConfiguration? tier = resourceTierAnnotationMap[context.getResourceName()];
    return (tier is TierConfiguration) ? tier.policy : ();
}

function isUnauthenticateLevelThrottled(http:FilterContext context) returns [boolean, boolean] {
    string clientIp = <string>context.attributes[REMOTE_ADDRESS];
    string? apiVersion = getVersion(context);
    string throttleKey = clientIp + ":" + getContext(context);
    if (apiVersion is string) {
        throttleKey += ":" + apiVersion;
    }
    return isRequestThrottled(throttleKey);
}

function isRequestBlocked(http:Caller caller, http:Request request, http:FilterContext context, AuthenticationContext keyValidationResult) returns (boolean) {
    if (!enabledGlobalTMEventPublishing) {
        return false;
    }
    string apiLevelBlockingKey = getContext(context);
    string apiTenantDomain = getTenantDomain(context);
    string ipLevelBlockingKey = apiTenantDomain + ":" + getClientIp(request, caller);
    string appLevelBlockingKey = keyValidationResult.subscriber + ":" + keyValidationResult.applicationName;
    if (isAnyBlockConditionExist() && (isBlockConditionExist(apiLevelBlockingKey) ||
    isBlockConditionExist(ipLevelBlockingKey) || isBlockConditionExist(appLevelBlockingKey)) ||
    isBlockConditionExist(keyValidationResult.username)) {
        return true;
    } else {
        return false;
    }
}

function generateThrottleEvent(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto, map<json> deployedPolicies)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = {};
    if (!enabledGlobalTMEventPublishing) {
        requestStreamDTO = generateLocalThrottleEvent(req, context, keyValidationDto, deployedPolicies);
    } else {
        requestStreamDTO = generateGlobalThrottleEvent(req, context, keyValidationDto, deployedPolicies);
    }
    printDebug(KEY_THROTTLE_FILTER, "Resource key : " + requestStreamDTO.resourceKey +
    "\nSubscription key : " + requestStreamDTO.subscriptionKey +
    "\nApp key : " + requestStreamDTO.appKey +
    "\nAPI key : " + requestStreamDTO.apiKey +
    "\nResource Tier : " + requestStreamDTO.resourceTier +
    "\nSubscription Tier : " + requestStreamDTO.subscriptionTier +
    "\nApp Tier : " + requestStreamDTO.appTier +
    "\nAPI Tier : " + requestStreamDTO.apiTier);
    return requestStreamDTO;

}

function generateLocalThrottleEvent(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto, map<json> deployedPolicies)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = setCommonThrottleData(req, context, keyValidationDto, deployedPolicies);
    map<json> appPolicyDetails = getPolicyDetails(deployedPolicies, keyValidationDto.applicationTier, APP_LEVEL_PREFIX);
    requestStreamDTO.appTierCount = <int>appPolicyDetails.count;
    requestStreamDTO.appTierUnitTime = <int>appPolicyDetails.unitTime;
    requestStreamDTO.appTierTimeUnit = appPolicyDetails.timeUnit.toString();
    map<json> subPolicyDetails = getPolicyDetails(deployedPolicies, keyValidationDto.tier, SUB_LEVEL_PREFIX);
    requestStreamDTO.subscriptionTierCount = <int>subPolicyDetails.count;
    requestStreamDTO.subscriptionTierUnitTime = <int>subPolicyDetails.unitTime;
    requestStreamDTO.subscriptionTierTimeUnit = subPolicyDetails.timeUnit.toString();
    requestStreamDTO.stopOnQuota = <boolean>subPolicyDetails.stopOnQuota;
    map<json> resourcePolicyDetails = getPolicyDetails(deployedPolicies, requestStreamDTO.resourceTier, RESOURCE_LEVEL_PREFIX);
    requestStreamDTO.resourceTierCount = <int>resourcePolicyDetails.count;
    requestStreamDTO.resourceTierUnitTime = <int>resourcePolicyDetails.unitTime;
    requestStreamDTO.resourceTierTimeUnit = resourcePolicyDetails.timeUnit.toString();
    map<json> apiPolicyDetails = getPolicyDetails(deployedPolicies, requestStreamDTO.apiTier, RESOURCE_LEVEL_PREFIX);
    requestStreamDTO.apiTierCount = <int>apiPolicyDetails.count;
    requestStreamDTO.apiTierUnitTime = <int>apiPolicyDetails.unitTime;
    requestStreamDTO.apiTierTimeUnit = apiPolicyDetails.timeUnit.toString();
    setThrottleKeysWithVersion(requestStreamDTO, context);
    return requestStreamDTO;
}

function generateGlobalThrottleEvent(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto, map<json> deployedPolicies)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = setCommonThrottleData(req, context, keyValidationDto, deployedPolicies);
    requestStreamDTO.messageID = <string>context.attributes[MESSAGE_ID];
    requestStreamDTO.userId = keyValidationDto.username;
    requestStreamDTO.apiContext = getContext(context);
    requestStreamDTO.appTenant = keyValidationDto.subscriberTenantDomain;
    requestStreamDTO.apiTenant = getTenantDomain(context);
    requestStreamDTO.apiName = context.getServiceName();
    requestStreamDTO.appId = keyValidationDto.applicationId;
    setThrottleKeysWithVersion(requestStreamDTO, context);
    json properties = {};
    requestStreamDTO.properties = properties.toJsonString();
    return requestStreamDTO;
}

function setCommonThrottleData(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto, map<json> deployedPolicies)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = {};
    requestStreamDTO.appTier = keyValidationDto.applicationTier;
    requestStreamDTO.apiTier = getAPITier(context.getServiceName(), keyValidationDto.apiTier);
    requestStreamDTO.subscriptionTier = keyValidationDto.tier;
    requestStreamDTO.apiKey = getContext(context);

    if (requestStreamDTO.apiTier != UNLIMITED_TIER && requestStreamDTO.apiTier != "") {
        requestStreamDTO.resourceTier = requestStreamDTO.apiTier;
        requestStreamDTO.resourceKey = requestStreamDTO.apiKey;
    } else {
        string resourceKey = context.getResourceName();
        requestStreamDTO.resourceTier = getResourceTier(resourceKey);
        requestStreamDTO.resourceKey = resourceKey;
    }

    requestStreamDTO.appKey = keyValidationDto.applicationId + ":" + keyValidationDto.username;
    requestStreamDTO.subscriptionKey = keyValidationDto.applicationId + ":" + getContext(context);
    return requestStreamDTO;

}

function setThrottleKeysWithVersion(RequestStreamDTO requestStreamDTO, http:FilterContext context) {
    string? apiVersion = getVersion(context);
    if (apiVersion is string) {
        requestStreamDTO.apiVersion = apiVersion;
    }
    if (apiVersion is string) {
        requestStreamDTO.apiKey += ":" + apiVersion;
        requestStreamDTO.subscriptionKey += ":" + apiVersion;
        requestStreamDTO.resourceKey += ":" + apiVersion;
    }
}
function getVersion(http:FilterContext context) returns string | () {
    string? apiVersion = "";
    APIConfiguration? apiConfiguration = apiConfigAnnotationMap[context.getServiceName()];
    if (apiConfiguration is APIConfiguration) {
        apiVersion = apiConfiguration.apiVersion;
    }

    return apiVersion;
}

function checkAPILevelThrottled(http:Caller caller, http:Request request, http:FilterContext context,
                string apiLevelPolicy,  map<json> deployedPolicies, string? apiVersion) returns boolean {
    printDebug(KEY_THROTTLE_FILTER, "Checking api level throttle policy '" + apiLevelPolicy + "' exist.");
    if (apiLevelPolicy != UNLIMITED_TIER && !isPolicyExist(deployedPolicies, apiLevelPolicy, RESOURCE_LEVEL_PREFIX)) {
        printDebug(KEY_THROTTLE_FILTER, "API level throttle policy '" + apiLevelPolicy
        + "' does not exist.");
        setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
        INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
        INTERNAL_SERVER_ERROR_MESSAGE, POLICY_NOT_FOUND_DESCRIPTION);
        sendErrorResponse(caller, request, context);
        return false;
    }
    printDebug(KEY_THROTTLE_FILTER, "Checking API level throttling-out.");
    if (isAPILevelThrottled(context, apiVersion)) {
        printDebug(KEY_THROTTLE_FILTER, "API level throttled out. Sending throttled out response.");
        context.attributes[IS_THROTTLE_OUT] = true;
        context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED;
        setThrottleErrorMessageToContext(context, THROTTLED_OUT, API_THROTTLE_OUT_ERROR_CODE,
        THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
        sendErrorResponse(caller, request, context);
        return false;
    } else {
        printDebug(KEY_THROTTLE_FILTER, "API level throttled out: false");
    }
    return true;
}

function checkResourceLevelThrottled(http:Caller caller, http:Request request, http:FilterContext context,
                            string? resourceLevelPolicyName,  map<json> deployedPolicies, string? apiVersion) returns boolean {
    if (resourceLevelPolicyName is string) {
        printDebug(KEY_THROTTLE_FILTER, "Resource level throttle policy : " + resourceLevelPolicyName);
        if (resourceLevelPolicyName.length() > 0 && resourceLevelPolicyName != UNLIMITED_TIER &&
            !isPolicyExist(deployedPolicies, resourceLevelPolicyName, RESOURCE_LEVEL_PREFIX)) {
            printDebug(KEY_THROTTLE_FILTER, "Resource level throttle policy '" + resourceLevelPolicyName
            + "' does not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
            INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
            INTERNAL_SERVER_ERROR_MESSAGE, POLICY_NOT_FOUND_DESCRIPTION);
            sendErrorResponse(caller, request, context);
            return false;
        }
    }
    printDebug(KEY_THROTTLE_FILTER, "Checking resource level throttling-out.");
    if (isResourceLevelThrottled(context, resourceLevelPolicyName, deployedPolicies, apiVersion)) {
        printDebug(KEY_THROTTLE_FILTER, "Resource level throttled out. Sending throttled out response.");
        context.attributes[IS_THROTTLE_OUT] = true;
        context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED;
        setThrottleErrorMessageToContext(context, THROTTLED_OUT, RESOURCE_THROTTLE_OUT_ERROR_CODE,
        THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
        sendErrorResponse(caller, request, context);
        return false;
    } else {
        printDebug(KEY_THROTTLE_FILTER, "Resource level throttled out: false");
    }
    return true;
}
