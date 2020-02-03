
// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerinax/java;

public function initThrottleDataPublisher() {
    int processPoolMaxIdle = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_POOL_MAX_IDLE, DEFAULT_PROCESS_POOL_MAX_IDLE);
    int processPoolInitIdleCapacity = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_POOL_INIT_IDLE_CAPACITY, DEFAULT_PROCESS_POOL_INIT_IDLE_CAPACITY);
    int processThreadPoolCoreSize = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_THREAD_POOL_CORE_SIZE, DEFAULT_PROCESS_THREAD_POOL_CORE_SIZE);
    int processThreadPoolMaximumSize = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_THREAD_POOL_MAXIMUM_SIZE, DEFAULT_PROCESS_THREAD_POOL_MAXIMUM_SIZE);
    int processThreadPoolKeepAliveTime = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_THREAD_POOL_KEEP_ALIVE_TIME, DEFAULT_PROCESS_THREAD_POOL_KEEP_ALIVE_TIME);
    int cleanUpFrequency = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, THROTTLE_CLEANUP_FREQUENCY, DEFAULT_THROTTLE_CLEANUP_FREQUENCY);
    jInitThrottleDataPublisher (processPoolMaxIdle, processPoolInitIdleCapacity, processThreadPoolCoreSize,
        processThreadPoolMaximumSize, processThreadPoolKeepAliveTime, cleanUpFrequency);
}

public function publishNonThrottledEvent(RequestStreamDTO throttleEvent) {
    handle apiKey = java:fromString(throttleEvent.apiKey);
    handle appKey = java:fromString(throttleEvent.appKey);
    handle stopOnQuota = java:fromString(throttleEvent.stopOnQuota.toString());
    handle subscriptionKey = java:fromString(throttleEvent.subscriptionKey);
    handle appTierCount = java:fromString(throttleEvent.appTierCount);
    handle appTierUnitTime = java:fromString(throttleEvent.appTierUnitTime);
    handle appTierTimeUnit = java:fromString(throttleEvent.appTierTimeUnit);
    handle subscriptionTierCount = java:fromString(throttleEvent.subscriptionTierCount);
    handle subscriptionTierUnitTime = java:fromString(throttleEvent.subscriptionTierUnitTime);
    handle subscriptionTierTimeUnit = java:fromString(throttleEvent.subscriptionTierTimeUnit);
    handle resourceKey = java:fromString(throttleEvent.resourceKey);
    handle resourceTierCount = java:fromString(throttleEvent.resourceTierCount);
    handle resourceTierUnitTime = java:fromString(throttleEvent.resourceTierUnitTime);
    handle resourceTierTimeUnit = java:fromString(throttleEvent.resourceTierTimeUnit);
    handle timestamp = java:fromString(throttleEvent.timestamp);
    handle appTier = java:fromString(throttleEvent.appTier);
    handle apiTier = java:fromString(throttleEvent.apiTier);
    handle resourceTier = java:fromString(throttleEvent.resourceTier);
    handle subscriptionTier = java:fromString(throttleEvent.subscriptionTier);

    jPublishNonThrottledEvent(apiKey, appKey, stopOnQuota, subscriptionKey, appTierCount, appTierUnitTime, appTierTimeUnit,
    subscriptionTierCount, subscriptionTierUnitTime, subscriptionTierTimeUnit, resourceKey, resourceTierCount, resourceTierUnitTime, resourceTierTimeUnit,
    timestamp, appTier, apiTier, resourceTier, subscriptionTier);
}

public function isResourceThrottled(string resourceKey) returns boolean {
    handle key = java:fromString(resourceKey);
    return jIsResourceThrottled(key);
}

public function isAppLevelThrottled(string appKey) returns boolean {
    handle key = java:fromString(appKey);
    return jIsAppLevelThrottled(key);
}

public function isSubLevelThrottled(string subscriptionKey) returns boolean {
    handle key = java:fromString(subscriptionKey);
    return jIsSubscriptionLevelThrottled(key);
}

public function jIsResourceThrottled(handle resourceKey) returns boolean = @java:Method  {
    name: "isResourceThrottled",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;

public function jIsSubscriptionLevelThrottled(handle subscriptionKey) returns boolean = @java:Method  {
    name: "isSubcriptionLevelThrottled",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;

public function jIsAppLevelThrottled(handle appKey) returns boolean = @java:Method  {
    name: "isAppLevelThrottled",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;


public function jInitThrottleDataPublisher(int processPoolMaxIdle, int processPoolInitIdleCapacity,
    int processThreadPoolCoreSize, int processThreadPoolMaximumSize, int processThreadPoolKeepAliveTime,
    int cleanUpFrequency) = @java:Method {
    name: "initThrottleDataReceiver",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;


public function jPublishNonThrottledEvent(handle apiKey, handle appKey, handle stopOnQuota, handle subscriptionKey,
 handle appTierCount, handle appTierUnitTime, handle appTierTimeUnit, handle subscriptionTierCount, handle subscriptionTierUnitTime,
 handle subscriptionTierTimeUnit, handle resourceKey, handle resourceTierCount, handle resourceTierUnitTime, handle resourceTierTimeUnit, handle timestamp, handle appTier,
 handle apiTier, handle resourceTier, handle subscriptionTier) = @java:Method {
    name: "processNonThrottledEvent",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;

