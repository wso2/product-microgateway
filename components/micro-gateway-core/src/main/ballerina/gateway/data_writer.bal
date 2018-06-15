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
//streams associated with DTOs
stream<EventDTO> eventStream;

function getPayload(AnalyticsRequestStream requestStream) returns (string) {
    return requestStream.consumerKey + OBJ + requestStream.context + OBJ + requestStream.apiVersion + OBJ +
        requestStream.api + OBJ + requestStream.resourcePath + OBJ +requestStream.resourceTemplate + OBJ +
        requestStream.method + OBJ + requestStream.apiVersion + OBJ + requestStream.requestCount + OBJ +
        requestStream.requestTime + OBJ + requestStream.username + OBJ + requestStream.tenantDomain + OBJ +
        requestStream.hostName + OBJ + requestStream.apiPublisher + OBJ + requestStream.applicationName + OBJ
        + requestStream.applicationId + OBJ + requestStream.userAgent + OBJ + requestStream.tier +
        OBJ + requestStream.continuedOnThrottleOut + OBJ + requestStream.clientIp + requestStream.applicationOwner;
}

function getMetaData(AnalyticsRequestStream requestStream) returns (string) {
    return "{\"keyType\":\"" + requestStream.keyType + "\",\"correlationID\":\"" + requestStream.correlationID + "\"}";
}

function getCorrelationData(AnalyticsRequestStream request) returns (string) {
    return request.correlationID;
}

function generateRequestEvent(http:Request request, http:FilterContext context) returns (AnalyticsRequestStream){
    //ready authentication context to get values
    AnalyticsRequestStream requestStream;
    AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
    if ( authContext != null) {
        requestStream.consumerKey = authContext.consumerKey;
        requestStream.username = authContext.username;
        requestStream.applicationId = authContext.applicationId;
        requestStream.applicationName = authContext.applicationName;
        requestStream.applicationOwner = authContext.subscriber;
        requestStream.tier = authContext.tier;
        requestStream.continuedOnThrottleOut = !authContext.stopOnQuotaReach;
    }
    requestStream.userAgent = request.userAgent;
    requestStream.clientIp = getClientIp(request);
    requestStream.context = getContext(context);
    requestStream.tenantDomain = getTenantDomain(context);
    requestStream.api = getApiName(context);
    requestStream.apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
    apiVersion;

    //todo: hostname verify
    requestStream.hostName = "localhost";   //todo:get the host properl
    //todo:check if apiPublisher comes in keyValidation context
    requestStream.apiPublisher = "admin@carbon.super";   //todo:get publisher properly
    requestStream.method = request.method;
    //todo:verify resourcepath and resourceTemplate
    requestStream.resourceTemplate = "resourcePath";
    requestStream.resourcePath = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName)).path;
    requestStream.keyType = "PRODUCTION";
    //todo:random uuid taken from throttle filter
    requestStream.correlationID = "71c60dbd-b2be-408d-9e2e-4fd11f60cfbc";
    requestStream.requestCount = 1;
    //todo:get request time from authentication filter
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    requestStream.requestTime = currentTimeMills;
    return requestStream;

}

function generateEventFromRequest(AnalyticsRequestStream requestStream) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.request:1.1.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getMetaData(requestStream);
    eventDTO.correlationData = getCorrelationData(requestStream);
    eventDTO.payloadData = getPayload(requestStream);
    return eventDTO;
}

function getEventData(EventDTO dto) returns string {
    string output = "streamId" + KVT + dto.streamId + EVS + "timestamp" + KVT + dto.timeStamp + EVS +
        "metadata" + KVT + dto.metaData + EVS + "correlationData" + KVT + dto.correlationData + EVS +
        "payLoadData" + KVT + dto.payloadData + "\n";
    return output;
}

function writeEventToFile(EventDTO eventDTO) {
    //todo:batch events to reduce IO cost
    int currentTime = getCurrentTime();
    if (initializingTime == 0 ) {
        initializingTime = getCurrentTime();
        io:println("initialTime: " + initializingTime);
    }
    if ( currentTime - initializingTime > 60*1000) {
        var result = rotateFile("api-usage-data.dat");
        initializingTime = getCurrentTime();
        match result {
            string name => {
                io:println("File rotated successfully.");
            }
            error err => {
                io:println("Error occurred while rotating the file.");
            }
        }
    }
    io:ByteChannel channel = io:openFile("api-usage-data.dat", io:APPEND);
    io:CharacterChannel  charChannel = new(channel,  "UTF-8");
    try {
        io:println("writing to events to a file");
        match charChannel.write(getEventData(eventDTO),0) {
            int numberOfCharsWritten => {
                io:println(" No of characters written : " + numberOfCharsWritten);
            }
            error err => {
                throw err;
            }
        }

    } finally {
        match charChannel.close() {
            error sourceCloseError => {
                io:println("Error occured while closing the channel: " +
                        sourceCloseError.message);
            }
            () => {
                io:println("Source channel closed successfully.");
            }
        }
    }
}