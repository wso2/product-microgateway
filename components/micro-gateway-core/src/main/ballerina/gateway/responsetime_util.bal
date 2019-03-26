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


public function getRequestReponseExecutionDataPayload(RequestResponseExecutionDTO requestResponseExecutionDTO) returns string {
    string output =
        requestResponseExecutionDTO.applicationConsumerKey + OBJ +
        requestResponseExecutionDTO.applicationName + OBJ + requestResponseExecutionDTO.applicationId + OBJ +
        requestResponseExecutionDTO.applicationOwner + OBJ + requestResponseExecutionDTO.apiContext + OBJ +
        requestResponseExecutionDTO.apiName + OBJ + requestResponseExecutionDTO.apiVersion + OBJ +
        requestResponseExecutionDTO.apiResourcePath + OBJ + requestResponseExecutionDTO.apiResourceTemplate + OBJ +
        requestResponseExecutionDTO.apiMethod + OBJ + requestResponseExecutionDTO.apiCreator + OBJ +
        requestResponseExecutionDTO.apiCreatorTenantDomain + OBJ + requestResponseExecutionDTO.apiTier + OBJ +
        requestResponseExecutionDTO.apiHostname + OBJ + requestResponseExecutionDTO.userName + OBJ +
        requestResponseExecutionDTO.userTenantDomain + OBJ +
        requestResponseExecutionDTO.userIp + OBJ +
        requestResponseExecutionDTO.userAgent + OBJ +
        requestResponseExecutionDTO.requestTimestamp + OBJ +
        requestResponseExecutionDTO.throttledOut + OBJ +
        requestResponseExecutionDTO.responseTime + OBJ +
        requestResponseExecutionDTO.serviceTime + OBJ +
        requestResponseExecutionDTO.backendTime + OBJ +
        requestResponseExecutionDTO.responseCacheHit + OBJ +
        requestResponseExecutionDTO.responseSize + OBJ +
        requestResponseExecutionDTO.protocol + OBJ +
        requestResponseExecutionDTO.responseCode + OBJ +
        requestResponseExecutionDTO.destination + OBJ +
        requestResponseExecutionDTO.executionTime.securityLatency + OBJ +
        requestResponseExecutionDTO.executionTime.throttlingLatency + OBJ +
        requestResponseExecutionDTO.executionTime.requestMediationLatency + OBJ +
        requestResponseExecutionDTO.executionTime.responseMediationLatency + OBJ +
        requestResponseExecutionDTO.executionTime.backEndLatency + OBJ +
        requestResponseExecutionDTO.executionTime.otherLatency + OBJ +
        requestResponseExecutionDTO.gatewayType + OBJ +
        requestResponseExecutionDTO.label;
    return output;
}

public function getMetaDataForRequestResponseExecutionData(RequestResponseExecutionDTO dto) returns string {
    json metaData = { "clientType": dto.metaClientType, "correlationID": dto.correlationId };
    return metaData.toString();
}

function generateEventFromRequestResponseExecutionDTO(RequestResponseExecutionDTO requestResponseExecutionDTO) returns
                                                                                                                   EventDTO
{
    EventDTO eventDTO = {};
    eventDTO.streamId = "org.wso2.apimgt.statistics.request:3.0.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getMetaDataForRequestResponseExecutionData(requestResponseExecutionDTO);
    eventDTO.correlationData = "null";
    eventDTO.payloadData = getRequestReponseExecutionDataPayload(requestResponseExecutionDTO);
    return eventDTO;
}

public function generateRequestResponseExecutionDataEvent(http:Response response, http:FilterContext context) returns
                                                                                                           RequestResponseExecutionDTO
{
    RequestResponseExecutionDTO requestResponseExecutionDTO = {};
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        requestResponseExecutionDTO.apiCreator = authContext.apiPublisher;
        requestResponseExecutionDTO.metaClientType = authContext.keyType;
        requestResponseExecutionDTO.applicationConsumerKey = authContext.consumerKey;
        requestResponseExecutionDTO.userName = authContext.username;
        requestResponseExecutionDTO.applicationId = authContext.applicationId;
        requestResponseExecutionDTO.applicationName = authContext.applicationName;
        requestResponseExecutionDTO.userTenantDomain = authContext.subscriberTenantDomain;
    } else {
        requestResponseExecutionDTO.apiCreator = <string>apiConfigAnnotationMap[getServiceName(context.serviceName)].publisher;
        requestResponseExecutionDTO.metaClientType = PRODUCTION_KEY_TYPE;
        requestResponseExecutionDTO.applicationConsumerKey = ANONYMOUS_CONSUMER_KEY;
        requestResponseExecutionDTO.userName = END_USER_ANONYMOUS;
        requestResponseExecutionDTO.applicationId = ANONYMOUS_APP_ID;
        requestResponseExecutionDTO.applicationName = ANONYMOUS_APP_NAME;
        requestResponseExecutionDTO.userTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
    }
    requestResponseExecutionDTO.apiName = getApiName(context);
    requestResponseExecutionDTO.apiVersion = <string>apiConfigAnnotationMap[getServiceName(context.serviceName)].apiVersion;
    requestResponseExecutionDTO.apiContext = getContext(context);
    requestResponseExecutionDTO.correlationId = <string>context.attributes[MESSAGE_ID];

    var res = response.cacheControl.noCache;
    if(res is boolean) {
        requestResponseExecutionDTO.cacheHit = res;
    } else {
        //todo: cacheHit does not gives boolean
    }

    requestResponseExecutionDTO.apiHostname = retrieveHostname(DATACENTER_ID, <string>context.attributes[
        HOSTNAME_PROPERTY]);
    //todo: Response size is yet to be decided
    requestResponseExecutionDTO.responseSize = 0;
    requestResponseExecutionDTO.responseCode = response.statusCode;
    requestResponseExecutionDTO.apiResourcePath = <string>getResourceConfigAnnotation
    (resourceAnnotationMap[context.resourceName] ?: []).path;
    requestResponseExecutionDTO.apiResourceTemplate = <string>getResourceConfigAnnotation
    (resourceAnnotationMap[context.resourceName] ?: []).path;
    //request method
    requestResponseExecutionDTO.apiMethod = <string>context.attributes[API_METHOD_PROPERTY];
    int initTime = <int>context.attributes[REQUEST_TIME];
    int timeRequestOut = <int>runtime:getInvocationContext().attributes[TS_REQUEST_OUT];
    int timeResponseIn = <int>runtime:getInvocationContext().attributes[TS_RESPONSE_IN];
    requestResponseExecutionDTO.serviceTime = timeRequestOut - initTime;
    requestResponseExecutionDTO.backendTime = timeResponseIn - timeRequestOut;
    requestResponseExecutionDTO.responseTime = timeResponseIn - initTime;
    //dummy values for protocol and destination for now
    requestResponseExecutionDTO.protocol = <string>context.attributes[PROTOCOL_PROPERTY];
    requestResponseExecutionDTO.destination = <string>runtime:getInvocationContext().attributes[DESTINATION];

    //Set data which were set to context in the Request path
    requestResponseExecutionDTO.applicationOwner = <string>context.attributes[APPLICATION_OWNER_PROPERTY];
    requestResponseExecutionDTO.apiCreatorTenantDomain = <string>context.attributes[API_CREATOR_TENANT_DOMAIN_PROPERTY];
    requestResponseExecutionDTO.apiTier = <string>context.attributes[API_TIER_PROPERTY];

    requestResponseExecutionDTO.throttledOut = <boolean>context.attributes[CONTINUE_ON_TROTTLE_PROPERTY];

    requestResponseExecutionDTO.userAgent = <string>context.attributes[USER_AGENT_PROPERTY];
    requestResponseExecutionDTO.userIp = <string>context.attributes[USER_IP_PROPERTY];
    requestResponseExecutionDTO.requestTimestamp = <int>context.attributes[REQUEST_TIME_PROPERTY];
    requestResponseExecutionDTO.gatewayType = GATEWAY_TYPE;
    requestResponseExecutionDTO.label = GATEWAY_TYPE;

    requestResponseExecutionDTO.executionTime = generateExecutionTimeEvent(context);

    return requestResponseExecutionDTO;
}
