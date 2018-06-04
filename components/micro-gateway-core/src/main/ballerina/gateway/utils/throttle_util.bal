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

map blockConditions;
map throttleDataMap;
public stream<RequestStreamDTO> requestStream;
public stream<GlobalThrottleStreamDTO> globalThrottleStream;
future ftr = start initializeThrottleSubscription();
boolean blockConditionExist;
public function isBlockConditionExist(string key) returns (boolean) {
    return blockConditions.hasKey(key);
}
public function isAnyBlockConditionExist() returns (boolean) {
    return blockConditionExist;
}
public function putBlockCondition(map m) {
    string condition = <string>m[BLOCKING_CONDITION_KEY];
    string conditionValue = <string>m[BLOCKING_CONDITION_VALUE];
    string conditionState = <string>m[BLOCKING_CONDITION_STATE];
    if (conditionState == TRUE){
        blockConditionExist = true;
        blockConditions[conditionValue] = conditionValue;
    } else {
        _ = blockConditions.remove(conditionValue);
        if (lengthof blockConditions.keys() == 0){
            blockConditionExist = false;
        }
    }
}

public function isThrottled(string key) returns (boolean) {
    boolean isThrottled = throttleDataMap.hasKey(key);
    if (isThrottled){
        int currentTime = time:currentTime().time;
        int timeStamp = check <int>throttleDataMap[key];
        if (timeStamp >= currentTime) {
            return isThrottled;
        } else {
            boolean status = throttleDataMap.remove(key);
            return false;
        }
    }
    return isThrottled;
}

public function publishNonThrottleEvent(RequestStreamDTO request) {
    requestStream.publish(request);
}
function initializeThrottleSubscription() {
    globalThrottleStream.subscribe(onReceiveThrottleEvent);
    requestStream.subscribe(startToPublish);
}
public function onReceiveThrottleEvent(GlobalThrottleStreamDTO throttleEvent) {
    io:println("Event GlobalThrottleStream: ", throttleEvent);
    if (throttleEvent.isThrottled){
        throttleDataMap[throttleEvent.throttleKey] = throttleEvent.expiryTimeStamp;
    }
}
public function initializeBlockConditions() {
    string base64Header = "admin:admin";
    string encodedBasicAuthHeader = check base64Header.base64Encode();
    http:Request clientRequest = new;
    clientRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE +
            encodedBasicAuthHeader);
    var response = conditionRetrievalEndpoint->get("/throttle/data/v1/block", request = clientRequest);
    match response {
        http:Response httpResponse => {
            if (httpResponse.statusCode == http:OK_200){
                json payload = check httpResponse.getJsonPayload();
                log:printDebug("Payload Retrieved From Block Condition Rest API :" + payload.toString());
                foreach blockingValue in payload {
                    match blockingValue {
                        json value => {
                            foreach condition in value {
                                blockConditions[condition.toString()] = condition.toString();
                            }
                        }
                    }
                }
                blockConditionExist = (lengthof blockConditions.keys() > 0);
            } else if (httpResponse.statusCode == http:UNAUTHORIZED_401){
                error err = { message: "Couldn't Retrieve Block Condition due to invalid credentials" };
                log:printError("Couldn't Retrieve Block Conditions", err = err);
            } else {
                error err = { message: httpResponse.reasonPhrase };
                log:printError("Couldn't Retrieve Block Conditions", err = err);

            }
        }
        error err => {
            log:printError("Couldn't Retrieve Block Conditions", err = err);
        }
    }
}
public function putThrottleData(string key, int expiryTimeStamp) {
    throttleDataMap[key] = expiryTimeStamp;
}
public function removeThrottleData(string key){
    _ = throttleDataMap.remove(key);
}