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
public string KVT = "--KVS--";
public string EVS = "--EVT--";
public string OBJ = "--OBJ--";

int initializingTime = 0;
int rotatingTime = 0;
//streams associated with DTOs
stream<EventDTO> eventStream;

function getPayload(AnalyticsRequestStream requestStreamForPayload) returns (string) {
    return requestStreamForPayload.consumerKey + OBJ + requestStreamForPayload.context + OBJ + requestStreamForPayload.api + ":" + requestStreamForPayload
        .apiVersion + OBJ +
        requestStreamForPayload.api + OBJ + requestStreamForPayload.resourcePath + OBJ + requestStreamForPayload.resourceTemplate + OBJ +
        requestStreamForPayload.method + OBJ + requestStreamForPayload.apiVersion + OBJ + requestStreamForPayload.requestCount + OBJ +
        requestStreamForPayload.requestTime + OBJ + requestStreamForPayload.username + OBJ + requestStreamForPayload.tenantDomain + OBJ +
        requestStreamForPayload.hostName + OBJ + requestStreamForPayload.apiPublisher + OBJ + requestStreamForPayload.applicationName + OBJ
        + requestStreamForPayload.applicationId + OBJ + requestStreamForPayload.userAgent + OBJ + requestStreamForPayload
        .tier +
        OBJ + requestStreamForPayload.continuedOnThrottleOut + OBJ + requestStreamForPayload.clientIp + OBJ + requestStreamForPayload
        .applicationOwner;
}

function getMetaData(AnalyticsRequestStream requestStreamForMetaData) returns (string) {
    return "{\\\"keyType\\\":\"" + requestStreamForMetaData.keyType + "\",\\\"correlationID\\\":\"" + requestStreamForMetaData
        .correlationID + "\"}";
}

function getCorrelationData(AnalyticsRequestStream request) returns (string) {
    return request.correlationID;
}

function generateRequestEvent(http:Request request, http:FilterContext context) returns (AnalyticsRequestStream){
    //ready authentication context to get values
    AnalyticsRequestStream analyticsRequestStream;
    boolean isSecured =check <boolean>context.attributes[IS_SECURED];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        analyticsRequestStream.consumerKey = authContext.consumerKey;
        analyticsRequestStream.username = authContext.username;
        analyticsRequestStream.applicationId = authContext.applicationId;
        analyticsRequestStream.applicationName = authContext.applicationName;
        analyticsRequestStream.applicationOwner = authContext.subscriber;
        analyticsRequestStream.tier = authContext.tier;
        analyticsRequestStream.continuedOnThrottleOut = !authContext.stopOnQuotaReach;
        analyticsRequestStream.apiPublisher = authContext.apiPublisher;
        analyticsRequestStream.keyType = authContext.keyType;
    } else {
        analyticsRequestStream.consumerKey = "-";
        analyticsRequestStream.username = END_USER_ANONYMOUS;
        analyticsRequestStream.applicationId = ANONYMOUS_APP_ID;
        analyticsRequestStream.applicationName = ANONYMOUS_APP_NAME;
        analyticsRequestStream.applicationOwner = ANONYMOUS_APP_OWNER;
        analyticsRequestStream.tier = UNAUTHENTICATED_TIER;
        analyticsRequestStream.continuedOnThrottleOut = check <boolean>context.attributes[ALLOWED_ON_QUOTA_REACHED];
        analyticsRequestStream.apiPublisher = getAPIDetailsFromServiceAnnotation(
                                                  reflect:getServiceAnnotations(context.serviceType)).publisher;
        analyticsRequestStream.keyType = PRODUCTION_KEY_TYPE;
    }
    analyticsRequestStream.userAgent = request.userAgent;
    analyticsRequestStream.clientIp = <string>context.attributes[REMOTE_ADDRESS];
    analyticsRequestStream.context = getContext(context);
    analyticsRequestStream.tenantDomain = getTenantDomain(context);
    analyticsRequestStream.api = getApiName(context);
    analyticsRequestStream.apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
    apiVersion;

    //todo: hostname verify
    analyticsRequestStream.hostName = "localhost";
    analyticsRequestStream.method = request.method;
    context.attributes[METHOD] = request.method;
    analyticsRequestStream.resourceTemplate = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName)).path;
    analyticsRequestStream.resourcePath = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName)).path;
    analyticsRequestStream.correlationID = <string>context.attributes[MESSAGE_ID];
    analyticsRequestStream.requestCount = 1;
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    analyticsRequestStream.requestTime = currentTimeMills;
    return analyticsRequestStream;

}

function generateEventFromRequest(AnalyticsRequestStream requestStreamForEvent) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.request:1.1.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getMetaData(requestStreamForEvent);
    eventDTO.correlationData = getCorrelationData(requestStreamForEvent);
    eventDTO.payloadData = getPayload(requestStreamForEvent);
    return eventDTO;
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
                log:printInfo("Event is being written");
            }
            error err => {
                throw err;
            }
        }
    } finally {
        match charChannel.close() {
            error sourceCloseError => {
                log:printError("Error occurred while closing the channel: ", err = sourceCloseError);
            }
            () => {
                log:printDebug("Source channel closed successfully.");
            }
        }
    }
}
