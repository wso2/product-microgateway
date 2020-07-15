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

import ballerina/time;
import ballerina/stringutils;
import ballerina/runtime;

map<any> throttleDataMap = {};
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
    printDebug(KEY_THROTTLE_UTIL, "Successfully subscribed to global throttle stream.");
    if(enabledGlobalTMEventPublishing) {
        registerkeyTemplateRetrievalTask();
        registerBlockingConditionRetrievalTask();
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
    }
    else {
        if (throttleEvent.policyKey.length() > 0) {
            _ = throttleDataMap.remove(throttleEvent.policyKey);
        }
    }
}

public function getThrottleMetaData(ThrottleAnalyticsEventDTO dto) returns string {
    return dto.metaClientType;
}

public function getThrottlePayloadData(ThrottleAnalyticsEventDTO dto, string apimAnalyticsVertion) returns string {
    string additionalProps = OBJ;
    string properties = "";
    if (apimAnalyticsVertion == DEFAULT_AM_ANALYTICS_VERSION) {
        additionalProps = additionalProps + dto.apiResourceTemplate + OBJ + dto.apiMethod + OBJ;
    }
    if (apimAnalyticsVertion !== DEFAULT_AM_ANALYTICS_VERSION_300) {
            properties = OBJ + dto.properties;
    }
    return dto.userName + OBJ + dto.userTenantDomain + OBJ + dto.apiName + OBJ +
    dto.apiVersion + OBJ + dto.apiContext + OBJ + dto.apiCreator + OBJ + dto.apiCreatorTenantDomain + additionalProps +
    dto.applicationId + OBJ + dto.applicationName + OBJ + dto.subscriber + OBJ + dto.throttledOutReason + OBJ + dto.
    gatewayType + OBJ + dto.throttledTime.toString() + OBJ + dto.hostname + properties;
}

public function getEventFromThrottleData(ThrottleAnalyticsEventDTO dto, string apimAnalyticsVertion) returns EventDTO | error {
    EventDTO eventDTO = {};
    eventDTO.streamId = "org.wso2.apimgt.statistics.throttle:" + apimAnalyticsVertion;
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getThrottleMetaData(dto);
    eventDTO.correlationData = "null";
    eventDTO.payloadData = getThrottlePayloadData(dto, apimAnalyticsVertion);
    return eventDTO;
}

public function putThrottleData(GlobalThrottleStreamDTO throttleEvent, string throttleKey) {
    throttleDataMap[throttleKey] = <@untainted>throttleEvent;
}
public function removeThrottleData(string key) {
    _ = throttleDataMap.remove(key);
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

