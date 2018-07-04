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
import ballerina/time;
import ballerina/io;
import ballerina/log;

map throttleDataMap;
public stream<RequestStreamDTO> requestStream;
public stream<GlobalThrottleStreamDTO> globalThrottleStream;
public boolean isStreamsInitialized;
future ftr = start initializeThrottleSubscription();

public function isRequestThrottled(string key) returns (boolean, boolean) {
    boolean isThrottled = throttleDataMap.hasKey(key);
    if (isThrottled) {
        int currentTime = time:currentTime().time;
        GlobalThrottleStreamDTO dto = check <GlobalThrottleStreamDTO>throttleDataMap[key];
        int timeStamp = dto.expiryTimeStamp;
        boolean stopOnQuota = dto.stopOnQuota;
        if (timeStamp >= currentTime) {
            return (isThrottled, stopOnQuota);
        } else {
            boolean status = throttleDataMap.remove(key);
            return (false, stopOnQuota);
        }
    }
    return (isThrottled, false);
}

public function publishNonThrottleEvent(RequestStreamDTO request) {
    requestStream.publish(request);
    printDebug(KEY_THROTTLE_UTIL, "Throttle out event is sent to the queue.");
}
function initializeThrottleSubscription() {
    globalThrottleStream.subscribe(onReceiveThrottleEvent);
    isStreamsInitialized = true;
}
public function onReceiveThrottleEvent(GlobalThrottleStreamDTO throttleEvent) {
    printDebug(KEY_THROTTLE_UTIL, "Event GlobalThrottleStream: throttleKey:" + throttleEvent.throttleKey + ",isThrottled:"
        + throttleEvent.isThrottled + ",expiryTimeStamp:" + throttleEvent.expiryTimeStamp);
    if (throttleEvent.isThrottled){
        throttleDataMap[throttleEvent.throttleKey] = throttleEvent;
    }
}

public function getThrottleMetaData(ThrottleAnalyticsEventDTO dto) returns string {
    return dto.clientType;
}

public function getThrottlePayloadData(ThrottleAnalyticsEventDTO dto) returns string {
    return dto.accessToken + OBJ + dto.userId + OBJ + dto.tenantDomain + OBJ + dto.api + OBJ +
        dto.api_version + OBJ + dto.context + OBJ + dto.apiPublisher + OBJ + dto.throttledTime + OBJ +
        dto.applicationName + OBJ + dto.applicationId + OBJ + dto.subscriber + OBJ + dto.throttledOutReason;

}

public function getEventFromThrottleData(ThrottleAnalyticsEventDTO dto) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.throttle:1.0.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getThrottleMetaData(dto);
    eventDTO.correlationData = "null";
    eventDTO.payloadData = getThrottlePayloadData(dto);
    return eventDTO;
}