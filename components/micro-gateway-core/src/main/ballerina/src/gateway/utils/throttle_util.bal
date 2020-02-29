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
import wso2/jms;
import ballerina/stringutils;

map<string> blockConditions = {};
map<any> throttleDataMap = {};
stream<RequestStreamDTO> requestStream = new;
stream<GlobalThrottleStreamDTO> globalThrottleStream = new;
boolean isStreamsInitialized = false;
future<()> ftr = start initializeThrottleSubscription();

boolean blockConditionExist = false;
boolean enabledGlobalTMEventPublishing = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID,
GLOBAL_TM_EVENT_PUBLISH_ENABLED, false);

public function isBlockConditionExist(string key) returns (boolean) {
    return blockConditions.hasKey(key);
}
public function isAnyBlockConditionExist() returns (boolean) {
    return blockConditionExist;
}
public function putBlockCondition(jms:MapMessage m) {
    string? | error condition = m.getString(BLOCKING_CONDITION_KEY);
    string? | error conditionValue = m.getString(BLOCKING_CONDITION_VALUE);
    string? | error conditionState = m.getString(BLOCKING_CONDITION_STATE);
    if (conditionState == TRUE && conditionState is string && conditionValue is string) {
        blockConditionExist = true;
        blockConditions[conditionValue] = <@untainted>conditionValue;
    } else {
        if (conditionValue is string) {
            _ = blockConditions.remove(conditionValue);
            if (blockConditions.keys().length() == 0) {
                blockConditionExist = false;
            }
        }
    }
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
        printDebug(KEY_THROTTLE_UTIL, "Request stream : " + requestStream.toString());
        printDebug(KEY_THROTTLE_UTIL, "Throttle out event is sent to the queue.");
    }
}

public function initializeThrottleSubscription() {
    globalThrottleStream.subscribe(onReceiveThrottleEvent);
    isStreamsInitialized = true;
    printDebug(KEY_THROTTLE_UTIL, "Successfully subscribed global throttle stream.");
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

public function getThrottlePayloadData(ThrottleAnalyticsEventDTO dto) returns string {
    return dto.userName + OBJ + dto.userTenantDomain + OBJ + dto.apiName + OBJ +
    dto.apiVersion + OBJ + dto.apiContext + OBJ + dto.apiCreator + OBJ + dto.apiCreatorTenantDomain + OBJ +
    dto.applicationId + OBJ + dto.applicationName + OBJ + dto.subscriber + OBJ + dto.throttledOutReason + OBJ + dto.
    gatewayType + OBJ + dto.throttledTime.toString() + OBJ + dto.hostname;

}

public function getEventFromThrottleData(ThrottleAnalyticsEventDTO dto) returns EventDTO | error {
    EventDTO eventDTO = {};
    eventDTO.streamId = "org.wso2.apimgt.statistics.throttle:3.0.0";
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

public function getRequestStream() returns stream<RequestStreamDTO> {
    return requestStream;
}

public function getGlobalThrottleStream() returns stream<GlobalThrottleStreamDTO> {
    return globalThrottleStream;
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
