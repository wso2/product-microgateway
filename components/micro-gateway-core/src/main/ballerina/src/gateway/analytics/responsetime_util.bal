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
import ballerina/lang.'int;
import ballerina/runtime;
import ballerina/stringutils;


public function getRequestReponseExecutionDataPayload(RequestResponseExecutionDTO requestResponseExecutionDTO) returns string {
    printDebug(KEY_ANALYTICS_FILTER, "Request response execution DTO : " + requestResponseExecutionDTO.toString());
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
    requestResponseExecutionDTO.requestTimestamp.toString() + OBJ +
    requestResponseExecutionDTO.throttledOut.toString() + OBJ +
    requestResponseExecutionDTO.responseTime.toString() + OBJ +
    requestResponseExecutionDTO.serviceTime.toString() + OBJ +
    requestResponseExecutionDTO.backendTime.toString() + OBJ +
    requestResponseExecutionDTO.responseCacheHit.toString() + OBJ +
    requestResponseExecutionDTO.responseSize.toString() + OBJ +
    requestResponseExecutionDTO.protocol + OBJ +
    requestResponseExecutionDTO.responseCode.toString() + OBJ +
    requestResponseExecutionDTO.destination + OBJ +
    requestResponseExecutionDTO.executionTime.securityLatency.toString() + OBJ +
    requestResponseExecutionDTO.executionTime.throttlingLatency.toString() + OBJ +
    requestResponseExecutionDTO.executionTime.requestMediationLatency.toString() + OBJ +
    requestResponseExecutionDTO.executionTime.responseMediationLatency.toString() + OBJ +
    requestResponseExecutionDTO.executionTime.backEndLatency.toString() + OBJ +
    requestResponseExecutionDTO.executionTime.otherLatency.toString() + OBJ +
    requestResponseExecutionDTO.gatewayType + OBJ +
    requestResponseExecutionDTO.label;
    printDebug(KEY_ANALYTICS_FILTER, "Request response execution DTO string : " + output);
    return output;
}

public function getMetaDataForRequestResponseExecutionData(RequestResponseExecutionDTO dto) returns string {
    json metaData = {"clientType": dto.metaClientType, "correlationID": dto.correlationId};
    return metaData.toString();
}

function generateEventFromRequestResponseExecutionDTO(RequestResponseExecutionDTO requestResponseExecutionDTO) returns
EventDTO | error
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
@tainted RequestResponseExecutionDTO | error
{
    RequestResponseExecutionDTO requestResponseExecutionDTO = {};
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (isSecured && invocationContext.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        requestResponseExecutionDTO.apiCreator = authContext.apiPublisher;
        requestResponseExecutionDTO.metaClientType = authContext.keyType;
        requestResponseExecutionDTO.applicationConsumerKey = authContext.consumerKey;
        requestResponseExecutionDTO.userName = authContext.username;
        requestResponseExecutionDTO.applicationId = authContext.applicationId;
        requestResponseExecutionDTO.applicationName = authContext.applicationName;
        requestResponseExecutionDTO.userTenantDomain = authContext.subscriberTenantDomain;
    } else {
        requestResponseExecutionDTO.metaClientType = PRODUCTION_KEY_TYPE;
        requestResponseExecutionDTO.applicationConsumerKey = ANONYMOUS_CONSUMER_KEY;
        requestResponseExecutionDTO.userName = END_USER_ANONYMOUS;
        requestResponseExecutionDTO.applicationId = ANONYMOUS_APP_ID;
        requestResponseExecutionDTO.applicationName = ANONYMOUS_APP_NAME;
        requestResponseExecutionDTO.userTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
    }
    APIConfiguration? apiConfiguration = apiConfigAnnotationMap[context.getServiceName()];
    if (apiConfiguration is APIConfiguration) {
        if (!stringutils:equalsIgnoreCase("", <string>apiConfiguration.publisher) 
            && stringutils:equalsIgnoreCase("", requestResponseExecutionDTO.apiCreator)) {
            requestResponseExecutionDTO.apiCreator = <string>apiConfiguration.publisher;
        } else if (stringutils:equalsIgnoreCase("", requestResponseExecutionDTO.apiCreator)) {
            requestResponseExecutionDTO.apiCreator = UNKNOWN_VALUE;
        }
        requestResponseExecutionDTO.apiVersion = <string>apiConfiguration.apiVersion;
    }
    requestResponseExecutionDTO.apiName = getApiName(context);

    // apim analytics requires context to be '<basePath>/<version>'
    string mgContext = getContext(context);
    mgContext = split(mgContext, "/(?=$)")[0];    //split from last '/'
    string analyticsContext = mgContext;

    if (!hasSuffix(mgContext, requestResponseExecutionDTO.apiVersion)) {
        analyticsContext = mgContext + "/" + requestResponseExecutionDTO.apiVersion;
    }

    requestResponseExecutionDTO.apiContext = analyticsContext;
    requestResponseExecutionDTO.correlationId = <string>context.attributes[MESSAGE_ID];
    http:ResponseCacheControl? responseCacheControl = response.cacheControl;
    if (responseCacheControl is http:ResponseCacheControl) {
        var res = responseCacheControl.noCache;
        requestResponseExecutionDTO.cacheHit = res;
    }

    requestResponseExecutionDTO.apiHostname = retrieveHostname(DATACENTER_ID, <string>context.attributes[
    HOSTNAME_PROPERTY]);
    // if response contains Content-Length header that value will be taken
    if (response.hasHeader(CONTENT_LENGHT_HEADER)) {
        var respSize = 'int:fromString(response.getHeader(CONTENT_LENGHT_HEADER));
        if (respSize is int) {
            requestResponseExecutionDTO.responseSize = respSize;
            printDebug(KEY_ANALYTICS_FILTER, "Response content lenght header : " + respSize.toString());
        } else {
            requestResponseExecutionDTO.responseSize = 0;
        }
    } else {        //TODO: we are not building message in order to get the response size if the message is chunk
        requestResponseExecutionDTO.responseSize = 0;
    }
    requestResponseExecutionDTO.responseCode = response.statusCode;
    string resourceName = context.getResourceName();
    string serviceName = context.getServiceName();
    http:HttpServiceConfig httpServiceConfig = <http:HttpServiceConfig>serviceAnnotationMap[serviceName];
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
    if (httpResourceConfig is http:HttpResourceConfig) {
        requestResponseExecutionDTO.apiResourcePath = httpResourceConfig.path;
        requestResponseExecutionDTO.apiResourceTemplate = httpResourceConfig.path;
    }
    //request method
    requestResponseExecutionDTO.apiMethod = <string>context.attributes[API_METHOD_PROPERTY];
    int initTime = <int>context.attributes[REQUEST_TIME];
    int timeRequestOut = <int>invocationContext.attributes[TS_REQUEST_OUT];
    int timeResponseIn = <int>invocationContext.attributes[TS_RESPONSE_IN];
    requestResponseExecutionDTO.serviceTime = timeRequestOut - initTime;
    requestResponseExecutionDTO.backendTime = timeResponseIn - timeRequestOut;
    requestResponseExecutionDTO.responseTime = timeResponseIn - initTime;
    //dummy values for protocol and destination for now
    requestResponseExecutionDTO.protocol = <string>context.attributes[PROTOCOL_PROPERTY];
    requestResponseExecutionDTO.destination = <string>invocationContext.attributes[DESTINATION];

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
