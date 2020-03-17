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
import ballerina/io;
import ballerina/runtime;
import ballerina/time;


public const string KVT = "-KS-";
public const string EVS = "-ES-";
public const string OBJ = "-OS-";

int initializingTime = 0;
int rotatingTime = 0;
//streams associated with DTOs
stream<EventDTO> eventStream = new;


function setRequestAttributesToContext(http:Request request, http:FilterContext context) returns error? {
    //ready authentication context to get values
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    printDebug(KEY_THROTTLE_FILTER, "Resource level throttled out: false");
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (isSecured && invocationContext.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        context.attributes[APPLICATION_OWNER_PROPERTY] = authContext.subscriber;
        context.attributes[API_TIER_PROPERTY] = authContext.apiTier;
        context.attributes[CONTINUE_ON_TROTTLE_PROPERTY] = !authContext.stopOnQuotaReach;
    } else {
        context.attributes[APPLICATION_OWNER_PROPERTY] = ANONYMOUS_APP_OWNER;
        context.attributes[API_TIER_PROPERTY] = UNAUTHENTICATED_TIER;
        context.attributes[CONTINUE_ON_TROTTLE_PROPERTY] = <boolean>context.attributes[ALLOWED_ON_QUOTA_REACHED];
    }
    context.attributes[USER_AGENT_PROPERTY] = request.userAgent;
    context.attributes[USER_IP_PROPERTY] = <string>context.attributes[REMOTE_ADDRESS];
    context.attributes[API_CREATOR_TENANT_DOMAIN_PROPERTY] = getTenantDomain(context);
    context.attributes[API_METHOD_PROPERTY] = request.method;

    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    context.attributes[REQUEST_TIME_PROPERTY] = currentTimeMills;
}

public function getEventData(EventDTO dto) returns string {
    string output = "streamId" + KVT + dto.streamId + EVS + "timestamp" + KVT + dto.timeStamp.toString() + EVS +
    "metadata" + KVT + dto.metaData + EVS + "correlationData" + KVT + "null" + EVS +
    "payLoadData" + KVT + dto.payloadData + "\n";
    return output;
}

function writeEventToFile(EventDTO eventDTO) {
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR) + PATH_SEPERATOR;
    // errors from 'openWritableFile' will be 'panicked'
    var writableChannel = <io:WritableByteChannel>io:openWritableFile(fileLocation + TEMP_API_USAGE_FILE, true);
    io:WritableCharacterChannel charChannel = new (writableChannel, "UTF-8");
    var result = charChannel.write(getEventData(eventDTO), 0);
    if (result is io:GenericError) {
        closeWC(charChannel);
        panic result;
    } else if (result is io:ConnectionTimedOutError) {
        closeWC(charChannel);
        panic result;
    } else {
        closeWC(charChannel);
        printDebug(KEY_ANALYTICS_FILTER, "Event is being written");
    }

}

public function closeWC(io:WritableCharacterChannel charChannel) {
    var result = charChannel.close();
    if (result is error) {
        printError(KEY_ANALYTICS_FILTER, "Error occurred while closing the channel: "
        + result.reason());
    } else {
        printDebug(KEY_ANALYTICS_FILTER, "Source channel closed successfully.");
    }
}
