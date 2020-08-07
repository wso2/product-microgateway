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

import ballerina/encoding;
import ballerina/time;
import ballerina/stringutils;
import ballerina/lang.'string as strings;
import ballerina/runtime;
import ballerina/http;
import ballerina/jwt;

map<any> throttleDataMap = {};
map<map<ConditionDto[]>> conditionDataMap = {};
boolean isStreamsInitialized = false;

boolean blockConditionExist = false;
boolean enabledGlobalTMEventPublishing = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID,
GLOBAL_TM_EVENT_PUBLISH_ENABLED, false);

public function isBlockConditionExist(string key) returns (boolean) {
    return blockConditionsMap.hasKey(key);
}
public function isAnyBlockConditionExist() returns (boolean) {
    return blockConditionExist;
}

//check whether throttle event is in the local map(request is throttled or not)
public function isRequestThrottled(string key) returns [boolean, boolean] {
    printDebug(KEY_THROTTLE_UTIL, "throttle data map : " + throttleDataMap.toString());
    printDebug(KEY_THROTTLE_UTIL, "throttle data key : " + key);
    boolean isThrottled = throttleDataMap.hasKey(key);

    if (isThrottled) {
        GlobalThrottleStreamDTO dto = <GlobalThrottleStreamDTO>throttleDataMap[key];
        boolean stopOnQuota = dto.stopOnQuota;
        if (enabledGlobalTMEventPublishing == true) {
            int currentTime = time:currentTime().time;
            int? resetTimestamp = dto.resetTimestamp;
            stopOnQuota = true;
            if (resetTimestamp is int) {
                if (resetTimestamp < currentTime) {
                    var value = throttleDataMap.remove(key);
                    return [false, stopOnQuota];
                }
            } else {
                //if the resetTimestamp is not included, throttling is disabled
                printDebug(KEY_THROTTLE_UTIL, "throttle event for the throttle key:" + key +
                    "does not contain expiry timestamp.");
                return [false, stopOnQuota];
            }
        }
        return [isThrottled, stopOnQuota];
    }
    return [isThrottled, false];
}

# Decide whether request details provided in arguments is throttled or not by the global traffic manager.
# This function is deinfed to evalute only API and Resource level throttling decisions recieved from the
# traffic manager.
#
# + key - throttle key of the request
# + info - request details required to make conditional throttle decisions
# + return - [is request throttled, should stop on quota]
public function isApiThrottledByTM(string key, ConditionalThrottleInfo? info) returns [boolean, boolean] {
    printDebug(KEY_THROTTLE_UTIL, "throttle data map : " + throttleDataMap.toString());
    printDebug(KEY_THROTTLE_UTIL, "throttle data key : " + key);
    boolean isThrottled = false;
    boolean stopOnQuota = false;

    if (enabledGlobalTMEventPublishing == false) {
        return [false, false];
    }
    boolean hasThrottledCondition = conditionDataMap.hasKey(key);
    printDebug(KEY_THROTTLE_UTIL, "hasThrottledCondition : " + hasThrottledCondition.toString());

    if (hasThrottledCondition && (info is ConditionalThrottleInfo)) {
        // get the condition groups for provided throttleKey
        map<ConditionDto[]> conditionGrps = conditionDataMap.get(key);
        string? conditionKey = ();

        // iterate through all available conditions and find if the current request
        // attributes are eligible to be throttled by the available throttled conditions
        foreach var [name, dto] in conditionGrps.entries() {
            if (DEFAULT_THROTTLE_CONDITION != name) {
                boolean isPipelineThrottled = isThrottledByCondition(dto, info);
                if (isPipelineThrottled) {
                    conditionKey = name;
                    break;
                }
            }
        }

        if (conditionKey is () && conditionGrps.hasKey(DEFAULT_THROTTLE_CONDITION)) {
            ConditionDto[] dto = conditionGrps.get(DEFAULT_THROTTLE_CONDITION);
            boolean isPipelineThrottled = isThrottledByCondition(dto, info);
            if (!isPipelineThrottled) {
                conditionKey = DEFAULT_THROTTLE_CONDITION;
            }
        }

        // if we detect the request is throttled by a condition. Then check the validity of throttle
        // decision from the throttle event data available in the throttleDataMap
        if (conditionKey is string) {
            printDebug(KEY_THROTTLE_UTIL, "throttled with condition: " + conditionKey);
            string combinedThrottleKey = key + "_" + conditionKey;

            // if throttle data is not available for the combined key, conditional throttle decision
            // is no longer valid
            if (!throttleDataMap.hasKey(combinedThrottleKey)) {
                return [false, false];
            }
            var dto = throttleDataMap.get(combinedThrottleKey);
            if (dto is GlobalThrottleStreamDTO) {
                int currentTime = time:currentTime().time;
                int? resetTimestamp = dto.resetTimestamp;
                stopOnQuota = true;
                if (resetTimestamp is int) {
                    if (resetTimestamp < currentTime) {
                        _ = throttleDataMap.remove(combinedThrottleKey);
                        _ = conditionDataMap.remove(key);
                        return [false, stopOnQuota];
                    }
                    return [true, stopOnQuota];
                } else {
                    // if the resetTimestamp is not included, throttling is disabled
                    printDebug(KEY_THROTTLE_UTIL, "throttle event for the throttle key:" + key +
                        "does not contain expiry timestamp.");
                    return [false, stopOnQuota];
                }
            }
        }
    }
    return [false, false];
}

public function publishNonThrottleEvent(RequestStreamDTO throttleEvent) {
    //Publish throttle event to traffic manager
    if (enabledGlobalTMEventPublishing == true) {
        future<()> publishedEvent = start publishThrottleEventToTrafficManager(throttleEvent);
        printDebug(KEY_THROTTLE_UTIL, "Throttle out event is sent to the traffic manager.");
    }
    //Publish throttle event to internal policies
    else {
        publishNonThrottledEvent(throttleEvent);
        printDebug(KEY_THROTTLE_UTIL, "Throttle out event is sent to the queue.");
    }
}

public function initializeThrottleSubscription() {
    isStreamsInitialized = true;
    if(enabledGlobalTMEventPublishing) {
        registerkeyTemplateRetrievalTask();
        registerBlockingConditionRetrievalTask();
        printDebug(KEY_THROTTLE_UTIL, "Successfully subscribed to global throttle stream.");
    }
}

// insert throttleevent into the map if it is throttled other wise remove the throttle key it from the throttledata map
public function onReceiveThrottleEvent(GlobalThrottleStreamDTO throttleEvent) {
    printDebug(KEY_THROTTLE_UTIL, "Event globalThrottleStream: throttleKey: " + throttleEvent.policyKey +
    " ,isThrottled:" + throttleEvent.isThrottled.toString());
    if (throttleEvent.isThrottled) {
        if (throttleEvent.policyKey.length() > 0) {
            throttleDataMap[throttleEvent.policyKey] = throttleEvent;
        }
    } else {
        if (throttleEvent.policyKey.length() > 0) {
            _ = throttleDataMap.remove(throttleEvent.policyKey);
        }
    }
}

public function getThrottleMetaData(ThrottleAnalyticsEventDTO dto) returns string {
    return dto.metaClientType;
}

public function getThrottlePayloadData(ThrottleAnalyticsEventDTO dto) returns string {
    string additionalProps = OBJ;
    string properties = "";
    if (amAnalyticsVersion == DEFAULT_AM_ANALYTICS_VERSION) {
        additionalProps = additionalProps + dto.apiResourceTemplate + OBJ + dto.apiMethod + OBJ;
    }
    if (amAnalyticsVersion != DEFAULT_AM_ANALYTICS_VERSION_300) {
            properties = OBJ + dto.properties;
    }
    return dto.userName + OBJ + dto.userTenantDomain + OBJ + dto.apiName + OBJ +
    dto.apiVersion + OBJ + dto.apiContext + OBJ + dto.apiCreator + OBJ + dto.apiCreatorTenantDomain + additionalProps +
    dto.applicationId + OBJ + dto.applicationName + OBJ + dto.subscriber + OBJ + dto.throttledOutReason + OBJ + dto.
    gatewayType + OBJ + dto.throttledTime.toString() + OBJ + dto.hostname + properties;
}

public function getEventFromThrottleData(ThrottleAnalyticsEventDTO dto) returns EventDTO | error {
    EventDTO eventDTO = {};
    eventDTO.streamId = "org.wso2.apimgt.statistics.throttle:" + amAnalyticsVersion;
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getThrottleMetaData(dto);
    eventDTO.correlationData = "null";
    eventDTO.payloadData = getThrottlePayloadData(dto);
    return eventDTO;
}

public function putThrottleData(GlobalThrottleStreamDTO throttleEvent, string throttleKey) {
    throttleDataMap[throttleKey] = <@untainted>throttleEvent;
}

public function removeThrottleData(string key) {
    _ = throttleDataMap.remove(key);
}

public function putThrottledConditions(ConditionDto[] conditions, string resourceKey, string conditionKey) {
    map<ConditionDto[]> conditionMapping = {};

    if (conditionDataMap.hasKey(resourceKey)) {
        conditionMapping = conditionDataMap.get(resourceKey);
    } else {
        conditionDataMap[resourceKey] = conditionMapping;
    }

    if (!conditionMapping.hasKey(conditionKey)) {
        conditionMapping[conditionKey] = conditions;
    }
}

public function removeThrottledConditions(string resourceKey, string conditionKey) {
    if (conditionDataMap.hasKey(resourceKey)) {
        map<ConditionDto[]> conditionMapping = conditionDataMap.get(resourceKey);
        _ = conditionMapping.removeIfHasKey(conditionKey);
        if (conditionMapping.length() == 0) {
            _ = conditionDataMap.remove(resourceKey);
        }
    }
}

//check whether the throttle policy is available if in built throttling is used
public function isPolicyExist(map<json> deployedPolicies, string policyName, string prefix) returns boolean {
    if (!enabledGlobalTMEventPublishing) {
        return deployedPolicies.hasKey(prefix + policyName);
    }
    return true;
}

public function getPolicyDetails(map<json> deployedPolicies, string policyName, string prefix) returns (map<json>) {
    if (stringutils:equalsIgnoreCase(policyName, UNLIMITED_TIER) || policyName.length() == 0) {
        return { count : -1, unitTime :-1, timeUnit : "min", stopOnQuota : true };
    }
    return <map<json>>deployedPolicies.get(prefix + policyName);
}

public function getIsStreamsInitialized() returns boolean {
    return isStreamsInitialized;
}

public function getResourceTier(string resourceName) returns string {
    TierConfiguration? tier = resourceTierAnnotationMap[resourceName];
    string? policy = (tier is TierConfiguration) ? tier.policy : ();
    if (policy is string) {
        if (policy.length() == 0) {
            return UNLIMITED_TIER;
        }
        return policy;
    }
    return UNLIMITED_TIER;
}

public function getAPITier(string serviceName, string tierFromKeyValidation) returns string {
    string apiTier = "";
    APIConfiguration? apiConfig = apiConfigAnnotationMap[serviceName];
    if (apiConfig is APIConfiguration) {
        apiTier = apiConfig.apiTier;
    }
    if(apiTier == "") {
        apiTier = tierFromKeyValidation;
    }
    return apiTier;
}

public function getResourceThrottleKey(runtime:InvocationContext invocationContext, string apiContext, string? apiVersion) returns string {
    string resourceLevelThrottleKey = apiContext;
    if (apiVersion is string) {
        resourceLevelThrottleKey += "/" + apiVersion;
    }
    resourceLevelThrottleKey += invocationContext.attributes[MATCHING_RESOURCE].toString() + ":" +
        invocationContext.attributes[REQUEST_METHOD].toString();
    return resourceLevelThrottleKey;
}

public function convertJsonToIpRange(map<json> ip) returns IPRangeDTO {
    IPRangeDTO ipRange = {
        id : <int>ip.id,
        tenantDomain : ip.tenantDomain.toString(),
        fixedIp : (ip[BLOCKING_CONDITION_FIXED_IP] != ())? ip.fixedIp.toString() : "",
        startingIp : (ip[BLOCKING_CONDITION_START_IP] != ())? ip.startingIp.toString() : "",
        startingIpNumber: "",
        endingIp : (ip[BLOCKING_CONDITION_END_IP] != ())? ip.endingIp.toString() : "",
        endingIpNumber: "",
        invert : <boolean>ip.invert,
        'type : ip.'type.toString()
    };
    return ipRange;
}

function addIpDataToBlockConditionTable(map<json> ip) {
    printDebug(KEY_THROTTLE_UTIL, "Retrived IP Blocking condition : " + ip.toJsonString());
    IPRangeDTO|error ipRange = trap convertJsonToIpRange(ip);
    if(ipRange is IPRangeDTO) {
        modifyIpWithNumericRanges(ipRange);
        var ret = IpBlockConditionsMap.add(ipRange);
        if(ret is error) {
            printError(KEY_THROTTLE_UTIL, "Error while adding IP or IP range blocking condition to the table.", ret);
        }
        blockConditionExist = true;
    } else {
        printError(KEY_THROTTLE_UTIL, "Error while parsing IP or IP range blocking condition", ipRange);
    }
}

function removeIpDataFromBlockConditionTable(int id) {
    int|error count = IpBlockConditionsMap.remove(function(IPRangeDTO ipRange) returns boolean {
        return (ipRange.id == id);
    });
    if(count is int) {
        printDebug(KEY_THROTTLE_UTIL, "Removed the IP blocking condition with id : " + id.toString() + " from the map");
        printDebug(KEY_THROTTLE_UTIL, "Number of items removed from the map : " + count.toString());
    } else {
        printError(KEY_THROTTLE_UTIL, "Error while removing blocking IP condition with id : " + id.toString(), count);
    }

}

function modifyIpWithNumericRanges(IPRangeDTO ipRange) {
    if(stringutils:equalsIgnoreCase(ipRange.'type, BLOCKING_CONDITION_IP_RANGE)) {
        ipRange.startingIpNumber = ipToBigInteger(ipRange.startingIp);
        ipRange.endingIpNumber = ipToBigInteger(ipRange.endingIp);
    } else {
        ipRange.startingIpNumber = "";
        ipRange.endingIpNumber  = "";
    }
}


# Build a list of `ConditionDto`s from the provided base64 encoded condition list.
#
# + base64Conditions - A base64 encoded json string containing the list of conditions
# evaluated by the throttle engine to take the throttle decision.
# + return - A list of conditions evaluated during making the throttle decision. Incase of an
# error during converting the conditions, empty `ConditionDto[]` will be returned
function extractConditionDto(string base64Conditions) returns ConditionDto[] {
    ConditionDto[] conditions = [];
    byte[]|error base64Decoded = encoding:decodeBase64Url(base64Conditions);
    if (base64Decoded is byte[]) {
        string|error result = strings:fromBytes(base64Decoded);
        if (result is error) {
            printError(KEY_THROTTLE_UTIL, result.reason(), result);
        }

        string conditionsPayload = <string>result;
        json[] | error jsonPayload = <json[]>conditionsPayload.fromJsonString();

        if (jsonPayload is json[]) {
            printDebug(KEY_THROTTLE_UTIL, "Decoded throttle conditions json :" + jsonPayload.toJsonString());

            foreach json condition in jsonPayload {
                ConditionDto conditionDto = {};
                var jIpSpecific = condition.ipspecific;
                var jIpRange = condition.iprange;
                var jHeader = condition.header;
                var jQuery = condition.queryparametertype;
                var jJwt = condition.jwtclaims;

                // Build IP condition DTOs
                if (jIpSpecific is json) {
                    IPCondition ip = {
                        specificIp: jIpSpecific.specificIp.toString(),
                        invert: <boolean>jIpSpecific.invert
                    };
                    conditionDto.ipCondition = ip;
                } else if (jIpRange is json) {
                    IPCondition ip = {
                        startingIp: jIpRange.startingIp.toString(),
                        endingIp: jIpRange.endingIp.toString(),
                        invert: <boolean>jIpRange.invert
                    };
                    conditionDto.ipRangeCondition = ip;
                }

                // Build header condition DTOs
                if (jHeader is json) {
                    var header = HeaderConditions.constructFrom(jHeader);
                    if (header is HeaderConditions) {
                        conditionDto.headerConditions = header;
                    } else {
                        printError(KEY_THROTTLE_UTIL, "HeaderCondition is not in the expected format", header);
                    }
                }

                // Build query param condition DTOs
                if (jQuery is json) {
                    var query = QueryParamConditions.constructFrom(jQuery);
                    if (query is QueryParamConditions) {
                        conditionDto.queryParamConditions = query;
                    } else {
                        printError(KEY_THROTTLE_UTIL, "QueryParamCondition is not in the expected format", query);
                    }
                }

                // Build jwt condition DTOs
                if (jJwt is json) {
                    var jwt = JwtConditions.constructFrom(jJwt);
                    if (jwt is JwtConditions) {
                        conditionDto.jwtClaimConditions = jwt;
                    } else {
                        printError(KEY_THROTTLE_UTIL, "JwtConditions is not in the expected format", jwt);
                    }
                }

                conditions.push(conditionDto);
            }
        } else {
            printError(KEY_THROTTLE_UTIL, "Couldn't build a valid json from the throttle conditions", jsonPayload);
        }
    } else {
        printError(KEY_THROTTLE_UTIL, "Couldn't decode throttle conditions", base64Decoded);
    }

    return conditions;
}

# Check if the request is throttled by an advanced throttle condition.
# Such as IP, header, query param based conditions.
#
# + conditions - throttled conditions recieved from global throttle engine
# + info - information required to derive conditional throttle status
# + return - `true` if throttled by a condition, `false` otherwise
function isThrottledByCondition(ConditionDto[] conditions, ConditionalThrottleInfo info) returns boolean {
    boolean isThrottled = false;

    foreach ConditionDto condition in conditions {
        // We initially set throttled flag to true. Then we move onto evaluating all conditions and
        // set the flag to false accordingly. This is done in this way to implement the `AND` logic
        // between each condition inside a condition group.
        isThrottled = true;
        HeaderConditions? headerConditions = condition?.headerConditions;
        IPCondition? ipCondition = condition?.ipCondition;
        IPCondition? ipRangeCondition = condition?.ipRangeCondition;
        QueryParamConditions? queryConditions = condition?.queryParamConditions;
        JwtConditions? claimConditions = condition?.jwtClaimConditions;

        if (ipCondition is IPCondition) {
            if (!isMatchingIp(info.clientIp, ipCondition)) {
                isThrottled = false;
            }
        } else if (ipRangeCondition is IPCondition) {
            if (!isWithinIpRange(info.clientIp, ipRangeCondition)) {
                isThrottled = false;
            }
        }
        if (info.isHeaderConditionsEnabled && (headerConditions is HeaderConditions)) {
            if (!isHeaderPresent(info.request, headerConditions)) {
                isThrottled = false;
            }
        }
        if (info.isQueryConditionsEnabled && (queryConditions is QueryParamConditions)) {
            if (!isQueryParamPresent(info.request, queryConditions)) {
                isThrottled = false;
            }
        }
        if (info.isJwtConditionsEnabled && (claimConditions is JwtConditions)) {
            if (!isClaimPresent(info.request, claimConditions)) {
                isThrottled = false;
            }
        }

        if (isThrottled) {
            break;
        }
    }

    return isThrottled;
}

function isMatchingIp(string clientIp, IPCondition ipCondition) returns boolean {
    string longIp = ipToBigInteger(clientIp);
    boolean isMatched = (longIp == ipCondition.specificIp);

    if (ipCondition.invert) {
        return !isMatched;
    }

    return isMatched;
}

function isWithinIpRange(string clientIp, IPCondition ipCondition) returns boolean {
    boolean isMatched = isIpWithinRange(clientIp, ipCondition.startingIp, ipCondition.endingIp);

    if (ipCondition.invert) {
        return !isMatched;
    }

    return isMatched;
}

function isHeaderPresent(http:Request req, HeaderConditions conditions) returns boolean {
    boolean status = true;

    foreach var [name, value] in conditions.values.entries() {
        if (req.hasHeader(name)) {
            string headerVal = req.getHeader(name);
            if (headerVal != "") {
                status = status && isPatternMatched(value, headerVal);
            } else {
                status = false;
                break;
            }
        }
    }

    status = conditions.invert ? !status : status;
    return status;
}

function isQueryParamPresent(http:Request req, QueryParamConditions conditions) returns boolean {
    boolean status = true;

    foreach var [name, value] in conditions.values.entries() {
        string? paramValue = req.getQueryParamValue(name);
        if (paramValue is string) {
            if (paramValue != "") {
                status = status && isPatternMatched(value, paramValue);
            } else {
                status = false;
                break;
            }
        }
    }

    status = conditions.invert ? !status : status;
    return status;
}

function isClaimPresent(http:Request req, JwtConditions conditions) returns boolean {
    boolean status = true;
    string? assertion = req.hasHeader(jwtheaderName) ? req.getHeader(jwtheaderName) : ();

    if (assertion is string) {
        jwt:JwtPayload | error decoded = decodeJWTPayload(assertion);
        if (decoded is jwt:JwtPayload) {
            foreach var [name, value] in conditions.values.entries() {
                map<json>? customClaims = decoded["customClaims"];
                if (decoded.hasKey(name)) {
                    string claim = decoded.get(name).toString();
                    status = status && isPatternMatched(value, claim);
                } else if (customClaims is map<json> && customClaims.hasKey(name)) {
                    string claim = customClaims.get(name).toString();
                    status = status && isPatternMatched(value, claim);
                } else {
                    status = false;
                    break;
                }
            }
        }
    }

    status = conditions.invert ? !status : status;
    return status;
}
