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
import ballerina/stringutils;
import ballerina/jwt;

boolean isHeaderConditionsEnabled = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID, HEADER_CONDITIONS_ENABLED,
    DEFAULT_HEADER_CONDITIONS_ENABLED);
boolean isQueryConditionsEnabled = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID, QUERY_CONDITIONS_ENABLED,
    DEFAULT_QUERY_CONDITIONS_ENABLED);
boolean isJwtConditionsEnabled = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID, JWT_CONDITIONS_ENABLED,
    DEFAULT_JWT_CONDITIONS_ENABLED);

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
    context.attributes[API_METHOD_PROPERTY] = request.method;

    AuthenticationContext keyValidationResult = {};
    string? apiVersion = getVersion(context);
    string resourceLevelThrottleKey = getResourceThrottleKey(invocationContext, apiContext, apiVersion);
    string? resourceLevelPolicyName = getResourceLevelPolicy(context);
    string clientIP = (enabledGlobalTMEventPublishing) ? getClientIp(request, caller): "";
    string tenantDomain = (enabledGlobalTMEventPublishing) ? getTenantDomain(context) : "";
    if (invocationContext.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        printDebug(KEY_THROTTLE_FILTER, "Context contains Authentication Context");
        keyValidationResult = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        if (isRequestBlocked(caller, request, context, keyValidationResult, apiContext, tenantDomain, clientIP)) {
            setThrottleErrorMessageToContext(context, FORBIDDEN, BLOCKING_ERROR_CODE,
            BLOCKING_MESSAGE, BLOCKING_DESCRIPTION);
            sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
            return false;
        }
        string apiLevelPolicy = getAPITier(context.getServiceName(),keyValidationResult.apiTier);
        if(!checkAPILevelThrottled(caller, request, context, apiLevelPolicy, deployedPolicies, apiContext, apiVersion)) {
            return false;
        }
        if(!checkResourceLevelThrottled(caller, request, context, resourceLevelPolicyName, deployedPolicies, resourceLevelThrottleKey)) {
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
            sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking subscription level throttling-out.");
        [isThrottled, stopOnQuota] = isSubscriptionLevelThrottled(keyValidationResult, deployedPolicies,
            apiContext, apiVersion);
        printDebug(KEY_THROTTLE_FILTER, "Subscription level throttling result:: isThrottled:"
        + isThrottled.toString() + ", stopOnQuota:" + stopOnQuota.toString());
        if (isThrottled) {
            if (stopOnQuota) {
                printDebug(KEY_THROTTLE_FILTER, "Sending throttled out responses.");
                context.attributes[IS_THROTTLE_OUT] = true;
                context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED;
                setThrottleErrorMessageToContext(context, THROTTLED_OUT, SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE,
                THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
                sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
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
            sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
            return false;
        }
        printDebug(KEY_THROTTLE_FILTER, "Checking application level throttling-out.");
        if (isApplicationLevelThrottled(keyValidationResult, deployedPolicies)) {
            printDebug(KEY_THROTTLE_FILTER, "Application level throttled out. Sending throttled out response.");
            context.attributes[IS_THROTTLE_OUT] = true;
            context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED;
            setThrottleErrorMessageToContext(context, THROTTLED_OUT, APPLICATION_THROTTLE_OUT_ERROR_CODE,
            THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
            sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
            return false;
        } else {
            printDebug(KEY_THROTTLE_FILTER, "Application level throttled out: false");
        }
        if(enabledGlobalTMEventPublishing && keyTemplateMap.length() > 0 &&
                !checkCustomThrottlePolicies(caller, request, context, keyValidationResult, apiContext,
                apiVersion, resourceLevelThrottleKey, tenantDomain, clientIP)) {
            return false;
        }

    } else if (!isSecured) {
        string apiLevelPolicy = getAPITier(context.getServiceName(),"");
        if (!checkAPILevelThrottled(caller, request, context, apiLevelPolicy, deployedPolicies, apiContext, apiVersion)) {
            return false;
        }
        if (!checkResourceLevelThrottled(caller, request, context, resourceLevelPolicyName, deployedPolicies, resourceLevelThrottleKey)) {
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
            sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
            return false;
        }
        [isThrottled, stopOnQuota] = isUnauthenticateLevelThrottled(context, apiContext, apiVersion);
        printDebug(KEY_THROTTLE_FILTER, "Unauthenticated tier throttled out result:: isThrottled:"
        + isThrottled.toString() + ", stopOnQuota:" + stopOnQuota.toString());
        if (isThrottled) {
            if (stopOnQuota) {
                printDebug(KEY_THROTTLE_FILTER, "Sending throttled out response.");
                context.attributes[IS_THROTTLE_OUT] = true;
                context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED;
                setThrottleErrorMessageToContext(context, THROTTLED_OUT, SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE,
                THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
                sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
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
        sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
        return false;
    }

    //Publish throttle event to another worker flow to publish to internal policies or traffic manager
    RequestStreamDTO throttleEvent = generateThrottleEvent(request, context, keyValidationResult, deployedPolicies,
        tenantDomain, apiContext, apiVersion);
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

function isSubscriptionLevelThrottled(AuthenticationContext keyValidationDto,
        map<json> deployedPolicies, string apiContext, string? apiVersion) returns [
 boolean, boolean] {
    if (keyValidationDto.tier == UNLIMITED_TIER) {
        return [false, false];
    }

    string subscriptionLevelThrottleKey = keyValidationDto.applicationId + ":" + apiContext;
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

function isAPILevelThrottled(string apiContext, string? apiVersion, ConditionalThrottleInfo? info)
        returns boolean {
    boolean throttled;
    boolean stopOnQuota;
    string apiThrottleKey = apiContext;
    if (apiVersion is string) {
        apiThrottleKey += ":" + apiVersion;
    }

    if (!enabledGlobalTMEventPublishing) {
        return isApiThrottled(apiThrottleKey);
    }
    [throttled, stopOnQuota] = isApiThrottledByTM(apiThrottleKey, info);
    return throttled;
}


function isResourceLevelThrottled(string? policy,
        map<json> deployedPolicies, string resourceKey, ConditionalThrottleInfo? info) returns (boolean) {
    if (policy is string) {
        string resourceLevelThrottleKey = resourceKey;
        if (policy == UNLIMITED_TIER) {
            return false;
        }
        boolean throttled;
        boolean stopOnQuota;
        if (!enabledGlobalTMEventPublishing) {
            return isResourceThrottled(resourceLevelThrottleKey);
        }
        [throttled, stopOnQuota] = isApiThrottledByTM(resourceLevelThrottleKey, info);
        return throttled;
    }
    return false;
}

function getResourceLevelPolicy(http:FilterContext context) returns string? {
    TierConfiguration? tier = resourceTierAnnotationMap[context.getResourceName()];
    return (tier is TierConfiguration) ? tier.policy : ();
}

function isUnauthenticateLevelThrottled(http:FilterContext context, string apiContext, string? apiVersion) returns [boolean, boolean] {
    string clientIp = <string>context.attributes[REMOTE_ADDRESS];
    string throttleKey = clientIp + ":" + apiContext;
    if (apiVersion is string) {
        throttleKey += ":" + apiVersion;
    }
    return isRequestThrottled(throttleKey);
}

function isRequestBlocked(http:Caller caller, http:Request request, http:FilterContext context,
        AuthenticationContext keyValidationResult, string apiContext, string tenantDomain, string clientIP) returns (boolean) {
    if (!enabledGlobalTMEventPublishing) {
        return false;
    }
    string apiTenantDomain = tenantDomain;
    string userBlockingKey = keyValidationResult.username;
    string appLevelBlockingKey = keyValidationResult.subscriber + ":" + keyValidationResult.applicationName;
    if (isAnyBlockConditionExist() && (isBlockConditionExist(apiContext) || isBlockConditionExist(appLevelBlockingKey))
    || isBlockConditionExist(userBlockingKey) || isIpLevelBlocked(clientIP, apiTenantDomain)) {
        return true;
    } else {
        return false;
    }
}

function isIpLevelBlocked(string clientIp, string tenanatDomain) returns boolean {
    foreach IPRangeDTO ipRange in IpBlockConditionsMap {
        if(ipRange.tenantDomain == tenanatDomain) {
            if(ipRange.'type == BLOCKING_CONDITION_IP) {
                if(ipRange.fixedIp == clientIp) {
                    if(!ipRange.invert) {
                        printDebug(KEY_THROTTLE_FILTER, "Blocked IP selected for blocking : " + clientIp);
                        return true;
                    }
                } else {
                    if(ipRange.invert) {
                        printDebug(KEY_THROTTLE_FILTER, "Blocked IP invert condition selected for IP : " + clientIp);
                        return true;
                    }
                }
            } else if(ipRange.'type == BLOCKING_CONDITION_IP_RANGE) {
                boolean isIpInRange = isIpWithinRange(clientIp, ipRange.startingIpNumber, ipRange.endingIpNumber);
                if(isIpInRange) {
                    if(!ipRange.invert) {
                        printDebug(KEY_THROTTLE_FILTER, "The client IP : " + clientIp + " is within the blocking " +
                        "IP range : [" + ipRange.startingIp + ", " + ipRange.endingIp + "].");
                        return true;
                    }
                } else {
                    if(ipRange.invert) {
                        printDebug(KEY_THROTTLE_FILTER, "The client IP : " + clientIp + " is within the inverted  " +
                        "blocking IP range : [" + ipRange.startingIp + ", " + ipRange.endingIp + "].");
                        return true;
                    }
                }
            }
        }
    }
    return false;
}

function generateThrottleEvent(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto,
                        map<json> deployedPolicies, string tenantDomain, string apiContext, string? apiVersion)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = {};
    if (!enabledGlobalTMEventPublishing) {
        requestStreamDTO = generateLocalThrottleEvent(req, context, keyValidationDto, deployedPolicies,
            apiContext, apiVersion);
    } else {
        requestStreamDTO = generateGlobalThrottleEvent(req, context, keyValidationDto, deployedPolicies, tenantDomain,
            apiContext, apiVersion);
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

function generateLocalThrottleEvent(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto,
    map<json> deployedPolicies, string apiContext, string? apiVersion) returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = setCommonThrottleData(req, context, keyValidationDto, deployedPolicies,
        apiContext, apiVersion);
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
    return requestStreamDTO;
}

function generateGlobalThrottleEvent(http:Request req, http:FilterContext context,
        AuthenticationContext keyValidationDto, map<json> deployedPolicies, string tenantDomain, string apiContext, string? apiVersion)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = setCommonThrottleData(req, context, keyValidationDto, deployedPolicies,
        apiContext, apiVersion);
    requestStreamDTO.messageID = <string>context.attributes[MESSAGE_ID];
    requestStreamDTO.userId = keyValidationDto.username;
    requestStreamDTO.apiContext = apiContext;
    requestStreamDTO.appTenant = keyValidationDto.subscriberTenantDomain;
    requestStreamDTO.apiTenant = tenantDomain;
    requestStreamDTO.apiName = context.getServiceName();
    requestStreamDTO.appId = keyValidationDto.applicationId;

    map<json> properties = getAdditionalProperties(context, req);
    requestStreamDTO.properties = properties.toJsonString();
    return requestStreamDTO;
}

function setCommonThrottleData(http:Request req, http:FilterContext context, AuthenticationContext keyValidationDto,
    map<json> deployedPolicies, string apiContext, string? apiVersion)
    returns (RequestStreamDTO) {
    RequestStreamDTO requestStreamDTO = {};
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    requestStreamDTO.appTier = keyValidationDto.applicationTier;
    requestStreamDTO.apiTier = getAPITier(context.getServiceName(), keyValidationDto.apiTier);
    requestStreamDTO.subscriptionTier = keyValidationDto.tier;
    requestStreamDTO.apiKey = apiContext;
    requestStreamDTO.subscriptionKey = keyValidationDto.applicationId + ":" + apiContext;
    requestStreamDTO.appKey = keyValidationDto.applicationId + ":" + keyValidationDto.username;
    setThrottleKeysWithVersion(requestStreamDTO, context);
    if (requestStreamDTO.apiTier != UNLIMITED_TIER && requestStreamDTO.apiTier != "") {
        requestStreamDTO.resourceTier = requestStreamDTO.apiTier;
        requestStreamDTO.resourceKey = requestStreamDTO.apiKey;
    } else {
        string resourceKey = getResourceThrottleKey(invocationContext, apiContext, apiVersion);
        requestStreamDTO.resourceTier = getResourceTier(context.getResourceName());
        requestStreamDTO.resourceKey = resourceKey;
    }
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
                string apiLevelPolicy,  map<json> deployedPolicies, string apiContext, string? apiVersion) returns boolean {
    printDebug(KEY_THROTTLE_FILTER, "Checking api level throttle policy '" + apiLevelPolicy + "' exist.");
    if (apiLevelPolicy != "" && apiLevelPolicy != UNLIMITED_TIER && !isPolicyExist(deployedPolicies, apiLevelPolicy, RESOURCE_LEVEL_PREFIX)) {
        printDebug(KEY_THROTTLE_FILTER, "API level throttle policy '" + apiLevelPolicy
        + "' does not exist.");
        setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
        INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
        INTERNAL_SERVER_ERROR_MESSAGE, POLICY_NOT_FOUND_DESCRIPTION);
        sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
        return false;
    }
    printDebug(KEY_THROTTLE_FILTER, "Checking API level throttling-out.");
    ConditionalThrottleInfo info = buildConditionalThrottleInfo(caller, request);
    if (isAPILevelThrottled(apiContext, apiVersion, info)) {
        printDebug(KEY_THROTTLE_FILTER, "API level throttled out. Sending throttled out response.");
        context.attributes[IS_THROTTLE_OUT] = true;
        context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED;
        setThrottleErrorMessageToContext(context, THROTTLED_OUT, API_THROTTLE_OUT_ERROR_CODE,
        THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
        sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
        return false;
    } else {
        printDebug(KEY_THROTTLE_FILTER, "API level throttled out: false");
    }
    return true;
}

function checkResourceLevelThrottled(http:Caller caller, http:Request request, http:FilterContext context,
                            string? resourceLevelPolicyName,  map<json> deployedPolicies, string resourceKey) returns boolean {
    if (resourceLevelPolicyName is string) {
        printDebug(KEY_THROTTLE_FILTER, "Resource level throttle policy : " + resourceLevelPolicyName);
        if (resourceLevelPolicyName.length() > 0 && resourceLevelPolicyName != UNLIMITED_TIER &&
            !isPolicyExist(deployedPolicies, resourceLevelPolicyName, RESOURCE_LEVEL_PREFIX)) {
            printDebug(KEY_THROTTLE_FILTER, "Resource level throttle policy '" + resourceLevelPolicyName
            + "' does not exist.");
            setThrottleErrorMessageToContext(context, INTERNAL_SERVER_ERROR,
            INTERNAL_ERROR_CODE_POLICY_NOT_FOUND,
            INTERNAL_SERVER_ERROR_MESSAGE, POLICY_NOT_FOUND_DESCRIPTION);
            sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
            return false;
        }
    }
    printDebug(KEY_THROTTLE_FILTER, "Checking resource level throttling-out.");
    ConditionalThrottleInfo info = buildConditionalThrottleInfo(caller, request);
    if (isResourceLevelThrottled(resourceLevelPolicyName, deployedPolicies, resourceKey, info)) {
        printDebug(KEY_THROTTLE_FILTER, "Resource level throttled out. Sending throttled out response.");
        context.attributes[IS_THROTTLE_OUT] = true;
        context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED;
        setThrottleErrorMessageToContext(context, THROTTLED_OUT, RESOURCE_THROTTLE_OUT_ERROR_CODE,
        THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
        sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
        return false;
    } else {
        printDebug(KEY_THROTTLE_FILTER, "Resource level throttled out: false");
    }
    return true;
}

function checkCustomThrottlePolicies(http:Caller caller, http:Request request, http:FilterContext context,
                        AuthenticationContext keyValidationDto, string apiContext, string? apiVersion,
                        string resourceLevelThrottleKey, string tenantDomain, string clientIp)  returns boolean {

    printDebug(KEY_THROTTLE_FILTER, "Checking custom throttlle policies");
    string userId = keyValidationDto.username;
    string appTenant = keyValidationDto.subscriberTenantDomain;
    string apiTenant = tenantDomain;
    string appId = keyValidationDto.applicationId;

    foreach KeyTemplate key in keyTemplateMap {
        string modifiedKey = replaceAll(key.value, "\\$resourceKey", resourceLevelThrottleKey);
        modifiedKey = replaceAll(modifiedKey, "\\$userId", userId);
        modifiedKey = replaceAll(modifiedKey, "\\$apiContext", apiContext);
        if(apiVersion is string) {
            modifiedKey = replaceAll(modifiedKey, "\\$apiVersion", apiVersion);
        }
        modifiedKey = replaceAll(modifiedKey, "\\$appTenant", appTenant);
        modifiedKey = replaceAll(modifiedKey, "\\$apiTenant", apiTenant);
        modifiedKey = replaceAll(modifiedKey, "\\$appId", appId);
        modifiedKey = replaceAll(modifiedKey, "\\$clientIp", clientIp);
        printDebug(KEY_THROTTLE_FILTER, "Custom policy throttle key : " + modifiedKey);
        boolean isThrottled;
        boolean stopOnQuota;
        [isThrottled, stopOnQuota] = isRequestThrottled(modifiedKey);
        if(isThrottled) {
            printDebug(KEY_THROTTLE_FILTER, "Custom policy throttle out for key : " + modifiedKey + ". Sending throttled out response.");
            context.attributes[IS_THROTTLE_OUT] = true;
            context.attributes[THROTTLE_OUT_REASON] = THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED;
            setThrottleErrorMessageToContext(context, THROTTLED_OUT, CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE,
            THROTTLE_OUT_MESSAGE, THROTTLE_OUT_DESCRIPTION);
            sendErrorResponse(caller, request, context, KEY_THROTTLE_FILTER);
            return false;
        }

    }
    return true;
}

# Retrive additional property list required for throttling event published from MGW.
#
# + context - Used to retrieve filter level properties
# + req - Used to retrieve request details such as headers and parameters
# + return - `json` map containing all identified and valid throttling properties
function getAdditionalProperties(http:FilterContext context, http:Request req) returns map<json> {
    // Set IP address properties
    map<json> propMap = {};
    string clientIp = <string>context.attributes[REMOTE_ADDRESS];
    string[] ipParts = stringutils:split(clientIp, ":");
    boolean jwtGeneratorEnabled = gatewayConf.jwtGeneratorConfig.jwtGeneratorEnabled;

    if (ipParts.length() > 1) {
        // This means the IP is a ipv6
        propMap["ipv6"] = ipToBigInteger(clientIp);
        propMap["ip"] = 0;
    } else {
        propMap["ip"] = ipToInt(clientIp);
        propMap["ipv6"] = 0;
    }

    if (isHeaderConditionsEnabled) {
        // Set request headers as properties
        printDebug(KEY_THROTTLE_FILTER, "setting request headers as condition properties");
        foreach string header in req.getHeaderNames() {
            propMap[header] = req.getHeader(<@untained>  header);
        }
    }

    if (isQueryConditionsEnabled) {
        // Set query params as properties
        printDebug(KEY_THROTTLE_FILTER, "setting request query params as condition properties");
        map<string[]> params = req.getQueryParams();
        foreach string param in params.keys() {
            string[] paramValues = <string[]>params.get(param);
            // Get only the last value of the list. This is to make the behavior similar to APIM
            propMap[param] = paramValues[paramValues.length() - 1];
        }
    }

    if (isJwtConditionsEnabled && req.hasHeader(jwtheaderName)) {
        // Set jwt claims as condition properties. This is set only if jwt header conditions are
        // enabled and backend jwt header is available in the request headers.
        printDebug(KEY_THROTTLE_FILTER, "setting jwt claims as condition properties");
        string jwt = req.getHeader(jwtheaderName);
        jwt:JwtPayload|error decoded = decodeJWTPayload(jwt);
        if (decoded is jwt:JwtPayload) {
            map<json>? customClaims = decoded["customClaims"];
            foreach var [key, value] in decoded.entries() {
                if (key == "customClaims" && customClaims is map<json>) {
                    foreach var [claimName, claimValue] in customClaims.entries() {
                        propMap[claimName] = <@untainted>claimValue;
                    }
                    continue;
                }
                propMap[key] = <@untainted>value;
            }
        }
    }

    return propMap;
}

function buildConditionalThrottleInfo(http:Caller caller, http:Request request) returns ConditionalThrottleInfo {
    string clientIp = getClientIp(request, caller);

    ConditionalThrottleInfo info = {
        clientIp: clientIp,
        request: request,
        isHeaderConditionsEnabled: isHeaderConditionsEnabled,
        isQueryConditionsEnabled: isQueryConditionsEnabled,
        isJwtConditionsEnabled: isJwtConditionsEnabled
    };

    return info;
}
