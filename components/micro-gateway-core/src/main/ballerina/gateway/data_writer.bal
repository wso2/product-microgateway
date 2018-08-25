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

import ballerina/io;
import ballerina/http;

@final
public string KVT = "-KS-";
public string EVS = "-ES-";
public string OBJ = "-OS-";

int initializingTime = 0;
int rotatingTime = 0;
//streams associated with DTOs
stream<EventDTO> eventStream;


function setRequestAttributesToContext(http:Request request, http:FilterContext context) {
    //ready authentication context to get values
    boolean isSecured =check <boolean>context.attributes[IS_SECURED];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        context.attributes[APPLICATION_OWNER_PROPERTY] = authContext.subscriber;
        context.attributes[API_TIER_PROPERTY] = authContext.apiTier;
        context.attributes[CONTINUE_ON_TROTTLE_PROPERTY] = !authContext.stopOnQuotaReach;
    } else {
        context.attributes[APPLICATION_OWNER_PROPERTY] = ANONYMOUS_APP_OWNER;
        context.attributes[API_TIER_PROPERTY] = UNAUTHENTICATED_TIER;
        context.attributes[CONTINUE_ON_TROTTLE_PROPERTY] = check <boolean>context.attributes[ALLOWED_ON_QUOTA_REACHED];
    }
    context.attributes[USER_AGENT_PROPERTY] = request.userAgent;
    context.attributes[USER_IP_PROPERTY] = <string>context.attributes[REMOTE_ADDRESS];
    context.attributes[API_CREATOR_TENANT_DOMAIN_PROPERTY] = getTenantDomain(context);
    context.attributes[API_METHOD_PROPERTY] = request.method;

    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    context.attributes[REQUEST_TIME_PROPERTY] = currentTimeMills;
}

function getEventData(EventDTO dto) returns string {
    string output = "streamId" + KVT + dto.streamId + EVS + "timestamp" + KVT + dto.timeStamp + EVS +
        "metadata" + KVT + dto.metaData + EVS + "correlationData" + KVT + "null" + EVS +
        "payLoadData" + KVT + dto.payloadData + "\n";
    return output;
}

function writeEventToFile(EventDTO eventDTO) {
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR) + PATH_SEPERATOR;
    io:ByteChannel channel = io:openFile(fileLocation + API_USAGE_FILE, io:APPEND);
    io:CharacterChannel charChannel = new(channel, "UTF-8");
    try {
        match charChannel.write(getEventData(eventDTO), 0) {
            int numberOfCharsWritten => {
                printDebug(KEY_ANALYTICS_FILTER, "Event is being written");
            }
            error err => {
                throw err;
            }
        }
    } finally {
        match charChannel.close() {
            error sourceCloseError => {
                printError(KEY_ANALYTICS_FILTER, "Error occurred while closing the channel: "
                        + sourceCloseError.message);
            }
            () => {
                printDebug(KEY_ANALYTICS_FILTER, "Source channel closed successfully.");
            }
        }
    }
}
