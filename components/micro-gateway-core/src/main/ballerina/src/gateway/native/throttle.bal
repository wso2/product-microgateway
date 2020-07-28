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

import ballerina/java;

public function initThrottleDataPublisher() {
    int processThreadPoolCoreSize = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_THREAD_POOL_CORE_SIZE, DEFAULT_PROCESS_THREAD_POOL_CORE_SIZE);
    int processThreadPoolMaximumSize = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_THREAD_POOL_MAXIMUM_SIZE, DEFAULT_PROCESS_THREAD_POOL_MAXIMUM_SIZE);
    int processThreadPoolKeepAliveTime = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, PROCESS_THREAD_POOL_KEEP_ALIVE_TIME, DEFAULT_PROCESS_THREAD_POOL_KEEP_ALIVE_TIME);
    int cleanUpFrequency = getConfigIntValue(LOCAL_THROTTLE_CONF_INSTANCE_ID, THROTTLE_CLEANUP_FREQUENCY, DEFAULT_THROTTLE_CLEANUP_FREQUENCY);
    jInitThrottleDataPublisher (processThreadPoolCoreSize,
        processThreadPoolMaximumSize, processThreadPoolKeepAliveTime, cleanUpFrequency);
}

public function publishNonThrottledEvent(RequestStreamDTO throttleEvent) {
    jPublishNonThrottledEvent(throttleEvent);
}

public function isResourceThrottled(string resourceKey) returns boolean {
    handle key = java:fromString(resourceKey);
    return jIsResourceThrottled(key);
}

public function isAppLevelThrottled(string appKey) returns boolean {
    handle key = java:fromString(appKey);
    return jIsAppLevelThrottled(key);
}

public function isApiThrottled(string appKey) returns boolean {
    handle key = java:fromString(appKey);
    return jIsApiLevelThrottled(key);
}

public function isSubLevelThrottled(string subscriptionKey) returns boolean {
    handle key = java:fromString(subscriptionKey);
    return jIsSubscriptionLevelThrottled(key);
}

public function ipToBigInteger(string ipAddress) returns string {
    handle ip = java:fromString(ipAddress);
    return jIpToBigInteger(ip).toString();
}

public function isIpWithinRange(string ip, string startingIp, string endingIp) returns boolean {
    handle ipHandle = java:fromString(ip);
    handle startingIpHandle = java:fromString(startingIp);
    handle endingIpHandle = java:fromString(endingIp);
    return jIsIpWithinRange(ipHandle, startingIpHandle, endingIpHandle);
}

function extractAPIorResourceKey(string throttleKey) returns APICondition | error {
    handle tKey = java:fromString(throttleKey);
    string res =  jExtractAPIorResourceKey(tKey).toString();

    return APICondition.constructFrom(<json>res.fromJsonString());
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

public function jIsApiLevelThrottled(handle apiKey) returns boolean = @java:Method  {
    name: "isApiLevelThrottled",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;

public function jInitThrottleDataPublisher(int processThreadPoolCoreSize, int processThreadPoolMaximumSize,
    int processThreadPoolKeepAliveTime, int cleanUpFrequency) = @java:Method {
    name: "initThrottleDataReceiver",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;


public function jPublishNonThrottledEvent(RequestStreamDTO throttleEvent) = @java:Method {
    name: "processNonThrottledEvent",
    class: "org.wso2.micro.gateway.core.throttle.ThrottleDataReceiver"
} external;

public function jIpToBigInteger(handle ip) returns handle = @java:Method {
    name: "ipToBigInteger",
    class: "org.wso2.micro.gateway.core.throttle.global.ThrottleUtils"
} external;

public function jIsIpWithinRange(handle ip, handle startingIp, handle endingIp) returns boolean = @java:Method  {
    name: "isIpWithinRange",
    class: "org.wso2.micro.gateway.core.throttle.global.ThrottleUtils"
} external;

public function jExtractAPIorResourceKey(handle throttleKey) returns handle = @java:Method {
    name: "extractAPIorResourceKey",
    class: "org.wso2.micro.gateway.core.throttle.global.ThrottleUtils"
} external;
