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

import ballerina/log;
import ballerina/io;
import ballerina/http;
import ballerina/runtime;


function getReponseDataPayload(ResponseDTO responseDTO) returns string {
    string output = responseDTO.consumerKey + OBJ + responseDTO.context + OBJ + responseDTO.apiVersion +
        OBJ + responseDTO.api + OBJ + responseDTO.resourcePath + OBJ + responseDTO.resourceTemplate + OBJ
        + responseDTO.method + OBJ + responseDTO.versionOnly + OBJ + responseDTO.response + OBJ +
        responseDTO.responseTime + OBJ + responseDTO.serviceTime + OBJ + responseDTO.backendTime + OBJ + responseDTO.
        userName
        + OBJ + responseDTO.eventTime + OBJ + responseDTO.tenantDomain + OBJ + responseDTO.hostname + OBJ + responseDTO.
        apiPublisher
        + OBJ + responseDTO.appName + OBJ + responseDTO.appId + OBJ + responseDTO.cacheHit + OBJ + responseDTO.
        responseSize
        + OBJ + responseDTO.protocol + OBJ + responseDTO.responseCode + OBJ + responseDTO.destination;

    return output;
}

function getMetaDataForResponseData(ResponseDTO dto) returns string {
    json metaData = { "keyType": dto.keyType, "correlationID": dto.correlationID };
    return metaData.toString();
}

function generateEventFromResponseDTO(ResponseDTO responseDTO) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.response:1.1.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getMetaDataForResponseData(responseDTO);
    eventDTO.correlationData = "null";
    eventDTO.payloadData = getReponseDataPayload(responseDTO);
    return eventDTO;
}

function generateResponseDataEvent(http:Response response, http:FilterContext context) returns ResponseDTO {
    ResponseDTO responseDto;
    boolean isSecured = check <boolean>context.attributes[IS_SECURED];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        responseDto.apiPublisher = authContext.apiPublisher;
        responseDto.keyType = authContext.keyType;
        responseDto.consumerKey = authContext.consumerKey;
        responseDto.userName = authContext.username;
        responseDto.appId = authContext.applicationId;
        responseDto.appName = authContext.applicationName;
    } else {
        responseDto.apiPublisher = getAPIDetailsFromServiceAnnotation(
                                       reflect:getServiceAnnotations(context.serviceType)).publisher;
        responseDto.keyType = PRODUCTION_KEY_TYPE;
        responseDto.consumerKey = "-";
        responseDto.userName = END_USER_ANONYMOUS;
        responseDto.appId = ANONYMOUS_APP_ID;
        responseDto.appName = ANONYMOUS_APP_NAME;
    }
    responseDto.api = getApiName(context);
    string versionOfApi = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
    apiVersion;
    responseDto.versionOnly = versionOfApi;
    responseDto.apiVersion = responseDto.api + ":" + versionOfApi;
    responseDto.tenantDomain = getTenantDomain(context);
    responseDto.context = getContext(context);
    responseDto.correlationID = <string> context.attributes[MESSAGE_ID];

    responseDto.eventTime = getCurrentTime();
    responseDto.responseTime = 0;
    var res = response.cacheControl.noCache;
    match res   {
         boolean val => {
            responseDto.cacheHit = val;
        }
        () => {
            //todo: cacheHit does not gives boolean
        }
    }
    responseDto.hostname = <string>context.attributes[HOSTNAME_PROPERTY];
    responseDto.response = 1;
    //todo: Response size is yet to be decided
    responseDto.responseSize = 0;
    responseDto.responseCode = response.statusCode;
    responseDto.resourcePath = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName)).path;
    responseDto.resourceTemplate = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName)).path;
    //request method
    responseDto.method = <string> context.attributes[METHOD];
    int initTime = check <int> context.attributes[REQUEST_TIME];
    int timeRequestOut = check <int> runtime:getInvocationContext().attributes[TS_REQUEST_OUT];
    int timeResponseIn = check <int> runtime:getInvocationContext().attributes[TS_RESPONSE_IN];
    responseDto.serviceTime = timeRequestOut - initTime;
    responseDto.backendTime = timeResponseIn - timeRequestOut;
    responseDto.responseTime = timeResponseIn - initTime;
    //dummy values for protocol and destination for now
    responseDto.protocol = "http";
    responseDto.destination = "https://dummyDestination";

    return responseDto;
}
