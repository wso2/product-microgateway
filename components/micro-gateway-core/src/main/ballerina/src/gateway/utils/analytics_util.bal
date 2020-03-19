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
import ballerina/time;
import ballerina/stringutils;

boolean isAnalyticsEnabled = false;
boolean isOldAnalyticsEnabled = false;
boolean configsRead = false;

//gRPCConfigs
boolean isGrpcAnalyticsEnabled = false;
string endpointURL = "";
int gRPCReconnectTime = 3000;

function populateThrottleAnalyticsDTO(http:FilterContext context) returns (ThrottleAnalyticsEventDTO | error) {
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    ThrottleAnalyticsEventDTO eventDto = {};

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
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.subscriber = authContext.subscriber;
        eventDto.apiCreator = authContext.apiPublisher;
    } else {
        metaInfo["keyType"] = PRODUCTION_KEY_TYPE;
        eventDto.userName = END_USER_ANONYMOUS;
        APIConfiguration? apiConfig = apiConfigAnnotationMap[context.getServiceName()];
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.subscriber = END_USER_ANONYMOUS;
    }

    APIConfiguration? apiConfiguration = apiConfigAnnotationMap[context.getServiceName()];
    if (apiConfiguration is APIConfiguration) {
        eventDto.apiVersion = apiConfiguration.apiVersion;
        if (!stringutils:equalsIgnoreCase("", <string>apiConfiguration.publisher)
                && stringutils:equalsIgnoreCase("", eventDto.apiCreator)) {
            eventDto.apiCreator = <string>apiConfiguration.publisher;
        } else if (stringutils:equalsIgnoreCase("", eventDto.apiCreator)) {
            //sets API creator if x-wso2-owner extension not specified.
            eventDto.apiCreator = UNKNOWN_VALUE;
        }
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

    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (isSecured && invocationContext.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        metaInfo["keyType"] = authContext.keyType;
        eventDto.consumerKey = authContext.consumerKey;
        eventDto.userName = authContext.username;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.userTenantDomain = authContext.subscriberTenantDomain;
        eventDto.apiCreator = authContext.apiPublisher;
    } else {
        metaInfo["keyType"] = PRODUCTION_KEY_TYPE;
        eventDto.consumerKey = ANONYMOUS_CONSUMER_KEY;
        eventDto.userName = END_USER_ANONYMOUS;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.userTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
    }

    APIConfiguration? apiConfig = apiConfigAnnotationMap[context.getServiceName()];
    if (apiConfig is APIConfiguration) {
        var api_Version = apiConfig.apiVersion;
        eventDto.apiVersion = api_Version;
        if (!stringutils:equalsIgnoreCase("", <string>apiConfig.publisher)
                && stringutils:equalsIgnoreCase("", eventDto.apiCreator)) {
            eventDto.apiCreator = <string>apiConfig.publisher;
        } else if (stringutils:equalsIgnoreCase("", eventDto.apiCreator)) {
            //sets API creator if x-wso2-owner extension not specified.
            eventDto.apiCreator = UNKNOWN_VALUE;
        }
    }
metaInfo["correlationID"] = <string>context.attributes[MESSAGE_ID];
    eventDto.metaClientType = metaInfo.toString();
    return eventDto;
}


function getAnalyticsEnableConfig() {
    isAnalyticsEnabled = <boolean>getConfigBooleanValue(FILE_UPLOAD_ANALYTICS,FILE_UPLOAD_ENABLE, DEFAULT_ANALYTICS_ENABLED);
    isOldAnalyticsEnabled =  <boolean>getConfigBooleanValue(OLD_FILE_UPLOAD_ANALYTICS,FILE_UPLOAD_ENABLE, false);
    if (isOldAnalyticsEnabled) {
        //enables config reads for older versions
        rotatingTime = <int>getConfigIntValue(OLD_FILE_UPLOAD_ANALYTICS,ROTATING_TIME, DEFAULT_ROTATING_PERIOD_IN_MILLIS); 
        uploadingUrl = <string>getConfigValue(OLD_FILE_UPLOAD_ANALYTICS,UPLOADING_EP, DEFAULT_UPLOADING_EP);
        configsRead = true;
    } else {
        rotatingTime = <int>getConfigIntValue(FILE_UPLOAD_ANALYTICS,ROTATING_TIME, DEFAULT_ROTATING_PERIOD_IN_MILLIS); 
        uploadingUrl = <string>getConfigValue(FILE_UPLOAD_ANALYTICS,UPLOADING_EP, DEFAULT_UPLOADING_EP);
        configsRead = true;
    }
    printDebug(KEY_ANALYTICS_FILTER, "File upload analytics uploading URL : "+ uploadingUrl);
    printDebug(KEY_UTILS, "Analytics configuration values read"); 
}

function initializegRPCAnalytics() {
    printDebug(KEY_UTILS, "gRPC Analytics configuration values read");
    isGrpcAnalyticsEnabled = <boolean>getConfigBooleanValue(GRPC_ANALYTICS, GRPC_ANALYTICS_ENABLE, DEFAULT_ANALYTICS_ENABLED);
    endpointURL = <string>getConfigValue(GRPC_ANALYTICS, GRPC_ENDPOINT_URL, DEFAULT_GRPC_ENDPOINT_URL);
    gRPCReconnectTime = <int>getConfigIntValue(GRPC_ANALYTICS, GRPC_RETRY_TIME_MILLISECONDS, DEFAULT_GRPC_RECONNECT_TIME_IN_MILLES);
    printDebug(KEY_GRPC_ANALYTICS, "gRPC endpoint URL : " + endpointURL);
    printDebug(KEY_GRPC_ANALYTICS, "gRPC keyStore file : " + <string>getConfigValue(LISTENER_CONF_INSTANCE_ID, KEY_STORE_PATH, DEFAULT_KEY_STORE_PATH));
    printDebug(KEY_GRPC_ANALYTICS, "gRPC keyStore password  : " + <string>getConfigValue(LISTENER_CONF_INSTANCE_ID, KEY_STORE_PASSWORD, DEFAULT_KEY_STORE_PASSWORD));
    printDebug(KEY_GRPC_ANALYTICS, "gRPC trustStore file : " + <string>getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH));
    printDebug(KEY_GRPC_ANALYTICS, "gRPC tustStore password  : " + <string>getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD));
    printDebug(KEY_GRPC_ANALYTICS, "gRPC retry time  : " + gRPCReconnectTime.toString());

    if (isGrpcAnalyticsEnabled) {
        initGRPCService();
    }
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
