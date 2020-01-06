// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/config;
import ballerina/http;
import ballerina/runtime;
import ballerina/log;
import ballerina/time;

boolean isAnalyticsEnabled = false;
boolean configsRead = false;

function populateThrottleAnalyticsDTO(http:FilterContext context) returns (ThrottleAnalyticsEventDTO | error) {
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    ThrottleAnalyticsEventDTO eventDto = {};

    APIConfiguration? apiConfiguration = apiConfigAnnotationMap[context.getServiceName()];
    if (apiConfiguration is APIConfiguration) {
        eventDto.apiVersion = apiConfiguration.apiVersion;
    }
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;

    map<json> metaInfo = {};
    eventDto.userTenantDomain = getTenantDomain(context);
    eventDto.apiName = getApiName(context);
    eventDto.apiContext = getContext(context);
    eventDto.throttledTime = currentTimeMills;
    eventDto.throttledOutReason = <string>context.attributes[THROTTLE_OUT_REASON];
    eventDto.apiCreatorTenantDomain = getTenantDomain(context);
    eventDto.gatewayType = GATEWAY_TYPE;
    eventDto.hostname = retrieveHostname(DATACENTER_ID, <string>context.attributes[
    HOSTNAME_PROPERTY]);
    if (isSecured) {
        AuthenticationContext authContext = <AuthenticationContext>runtime:getInvocationContext()
        .attributes[AUTHENTICATION_CONTEXT];
        metaInfo["keyType"] = authContext.keyType;
        eventDto.userName = authContext.username;
        eventDto.apiCreator = authContext.apiPublisher;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.subscriber = authContext.subscriber;
    } else {
        metaInfo["keyType"] = PRODUCTION_KEY_TYPE;
        eventDto.userName = END_USER_ANONYMOUS;
        APIConfiguration? apiConfig = apiConfigAnnotationMap[context.getServiceName()];
        if (apiConfig is APIConfiguration) {
            var api_Creator = apiConfig.publisher;
            eventDto.apiCreator = api_Creator;
        }
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.subscriber = END_USER_ANONYMOUS;
    }

    metaInfo["correlationID"] = <string>context.attributes[MESSAGE_ID];
    eventDto.metaClientType = metaInfo.toString();
    printDebug(KEY_ANALYTICS_FILTER, "Throttle Event DTO : " + eventDto.toString());
    return eventDto;
}

function populateFaultAnalyticsDTO(http:FilterContext context, string err) returns (FaultDTO | error) {
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    FaultDTO eventDto = {};
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    map<json> metaInfo = {};

    eventDto.apiContext = getContext(context);
    APIConfiguration? apiConfig = apiConfigAnnotationMap[context.getServiceName()];
    if (apiConfig is APIConfiguration) {
        var api_Version = apiConfig.apiVersion;
        eventDto.apiVersion = api_Version;
    }
    eventDto.apiName = getApiName(context);
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[context.attributes["ResourceName"].toString()];
    if (httpResourceConfig is http:HttpResourceConfig) {
        var resource_Path = httpResourceConfig.path;
        eventDto.resourcePath = resource_Path;
    }
    eventDto.method = <string>context.attributes[API_METHOD_PROPERTY];
    eventDto.errorCode = <int>runtime:getInvocationContext().attributes[ERROR_RESPONSE_CODE];
    eventDto.errorMessage = err;
    eventDto.faultTime = currentTimeMills;
    eventDto.apiCreatorTenantDomain = getTenantDomain(context);
    eventDto.hostName = retrieveHostname(DATACENTER_ID, <string>context.attributes[HOSTNAME_PROPERTY]);
    eventDto.protocol = <string>context.attributes[PROTOCOL_PROPERTY];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        metaInfo["keyType"] = authContext.keyType;
        eventDto.consumerKey = authContext.consumerKey;
        eventDto.apiCreator = authContext.apiPublisher;
        eventDto.userName = authContext.username;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.userTenantDomain = authContext.subscriberTenantDomain;
    } else {
        metaInfo["keyType"] = PRODUCTION_KEY_TYPE;
        eventDto.consumerKey = ANONYMOUS_CONSUMER_KEY;
        APIConfiguration? apiConfigs = apiConfigAnnotationMap[context.getServiceName()];
        if (apiConfigs is APIConfiguration) {
            var api_Creater = apiConfigs.publisher;
            eventDto.apiCreator = api_Creater;
        }
        eventDto.userName = END_USER_ANONYMOUS;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.userTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
    }
    metaInfo["correlationID"] = <string>context.attributes[MESSAGE_ID];
    eventDto.metaClientType = metaInfo.toString();
    return eventDto;
}


function getAnalyticsEnableConfig() {
    map<any> vals = getConfigMapValue(ANALYTICS);
    isAnalyticsEnabled = <boolean>vals[ENABLE];
    rotatingTime = <int>vals[ROTATING_TIME];
    uploadingUrl = <string>vals[UPLOADING_EP];
    configsRead = true;
    log:printDebug("Uploading url : "+ uploadingUrl);
    printDebug(KEY_UTILS, "Analytics configuration values read");
}

function getGRPCAnalyticsEnableConfig(){
    printDebug(KEY_UTILS, "gRPC Analytics configuration values read");
    map<any> gRPCConfigs = getConfigMapValue(GRPC_ANALYTICS);
    isgRPCAnalyticsEnabled = <boolean>gRPCConfigs[ENABLE];
    endpointURL = <string>gRPCConfigs[GRPC_ENDPOINT_URL];
    gRPCKeyStoreFile = <string>gRPCConfigs[KEYSTORE_FILE_PATH];
    gRPCKeyStorePassword = <string>gRPCConfigs[KEYSTORE_PASSWORD];
    gRPCTrustStoreFile = <string>gRPCConfigs[TURSTSTORE_FILE_PATH];
    gRPCTrustStorePassword = <string>gRPCConfigs[TRUSTSTORE_PASSWORD];
    gRPCReconnectTime = <int>gRPCConfigs[gRPC_RetryTimeMilliseconds];

    log:printDebug( "Endpint : " + endpointURL);
    log:printDebug( "K_Store : " + gRPCKeyStoreFile);
    log:printDebug( "K_Pass  : " + gRPCKeyStorePassword);
    log:printDebug( "T_Store : " + gRPCTrustStoreFile);
    log:printDebug( "T_Pass  : " + gRPCTrustStorePassword);
    log:printDebug( "Retry_time  : " + gRPCReconnectTime.toString());

    if(isgRPCAnalyticsEnabled == true){
        initGRPCService();
    }
}

function initializegRPCAnalytics(){
    log:printDebug("getgRPCAnalyticsEnableConfig method in analytics util bal called");
    getGRPCAnalyticsEnableConfig();
}

function initializeAnalytics() {
    if (!configsRead) {
        getAnalyticsEnableConfig();
        if (isAnalyticsEnabled) {
            initStreamPublisher();
            printDebug(KEY_ANALYTICS_FILTER, "Analytics is enabled");
            future<()> uploadTask = start timerTask();            // file uploading task
            future<()> rotateTask = start rotatingTask();        // file rotating task
        } else {
            printDebug(KEY_ANALYTICS_FILTER, "Analytics is disabled");
        }
    }
}

function initStreamPublisher() {
    printDebug(KEY_UTILS, "Subscribing writing method to event stream");
    eventStream.subscribe(writeEventToFile);
}

public function retrieveHostname(string key, string defaultHost) returns string {
    return config:getAsString(key, defaultHost);
}
