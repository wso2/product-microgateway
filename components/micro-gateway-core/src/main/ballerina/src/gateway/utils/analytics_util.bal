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
import ballerina/lang.'int;
import ballerina/runtime;
import ballerina/time;
import ballerina/stringutils;

boolean isAnalyticsEnabled = false;
boolean isOldAnalyticsEnabled = false;
boolean configsRead = false;

// ELK analytics configurations
boolean isELKAnalyticsEnabled = false;

// Choreo based analytics configurations
boolean isChoreoAnalyticsEnabled = false;

//gRPCConfigs
boolean isGrpcAnalyticsEnabled = false;
string endpointURL = "";
int gRPCReconnectTime = 3000;
int gRPCTimeout = DEFAULT_GRPC_TIMEOUT_IN_MILLIS;

function populateThrottleAnalyticsDTO(http:FilterContext context) returns (ThrottleAnalyticsEventDTO | error) {
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    ThrottleAnalyticsEventDTO eventDto = {};

    time:Time time = time:currentTime();
    int currentTimeMills = time.time;

    map<json> metaInfo = {};
    eventDto.userTenantDomain = getTenantDomain(context);
    if (context.attributes[API_METHOD_PROPERTY] is string) {
        eventDto.apiMethod = <string>context.attributes[API_METHOD_PROPERTY];
    } 
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
    string resourceName = context.getResourceName();
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
    if (httpResourceConfig is http:HttpResourceConfig) {
        eventDto.apiResourceTemplate = httpResourceConfig.path;
    }

    metaInfo["correlationID"] = <string>context.attributes[MESSAGE_ID];
    eventDto.metaClientType = metaInfo.toString();
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        if (invocationContext.attributes.hasKey(ADDITIONAL_ANALYTICS_PROPS) &&
        invocationContext.attributes[ADDITIONAL_ANALYTICS_PROPS] is string) {
        eventDto.properties = <string>invocationContext.attributes[ADDITIONAL_ANALYTICS_PROPS];
    }
    printDebug(KEY_ANALYTICS_FILTER, "Throttle Event DTO : " + eventDto.toString());
    return eventDto;
}

function populateFaultAnalyticsDTO(http:FilterContext context, string err) returns (FaultDTO | error) {
    printDebug(KEY_ANALYTICS_FILTER, "Populating fault analytics DTO context attributes : " + context.attributes.toString());
    printDebug(KEY_ANALYTICS_FILTER, "Populating fault analytics DTO Error message : " + err);
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    FaultDTO eventDto = {};
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    map<json> metaInfo = {};

    eventDto.apiContext = getContext(context);
    eventDto.apiName = getApiName(context);
    string resourceName = context.getResourceName();
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
    if (httpResourceConfig is http:HttpResourceConfig) {
        var resource_Path = httpResourceConfig.path;
        eventDto.resourcePath = resource_Path;
        eventDto.apiResourceTemplate = httpResourceConfig.path;
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
        eventDto.applicationOwner = authContext.subscriber;
    } else {
        metaInfo["keyType"] = PRODUCTION_KEY_TYPE;
        eventDto.consumerKey = ANONYMOUS_CONSUMER_KEY;
        eventDto.userName = END_USER_ANONYMOUS;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.userTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
        eventDto.applicationOwner = END_USER_ANONYMOUS;
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
    if (invocationContext.attributes.hasKey(ADDITIONAL_ANALYTICS_PROPS) &&
        invocationContext.attributes[ADDITIONAL_ANALYTICS_PROPS] is string) {
        eventDto.properties = <string>invocationContext.attributes[ADDITIONAL_ANALYTICS_PROPS];
    }
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
    gRPCTimeout = <int>getConfigIntValue(GRPC_ANALYTICS,GRPC_TIMEOUT_MILLISECONDS, DEFAULT_GRPC_TIMEOUT_IN_MILLIS);
    printDebug(KEY_GRPC_ANALYTICS, "gRPC endpoint URL : " + endpointURL);
    printDebug(KEY_GRPC_ANALYTICS, "gRPC keyStore file : " + <string>getConfigValue(LISTENER_CONF_INSTANCE_ID, KEY_STORE_PATH, DEFAULT_KEY_STORE_PATH));
    printDebug(KEY_GRPC_ANALYTICS, "gRPC trustStore file : " + <string>getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH));
    printDebug(KEY_GRPC_ANALYTICS, "gRPC retry time  : " + gRPCReconnectTime.toString());
    printDebug(KEY_GRPC_ANALYTICS, "gRPC timeout  : " + gRPCTimeout.toString());

    if (isGrpcAnalyticsEnabled) {
        initGRPCService();
    }
}

function initializeAnalytics() {
    if (!configsRead) {
        getAnalyticsEnableConfig();
        if (isAnalyticsEnabled) {
            printDebug(KEY_ANALYTICS_FILTER, "Analytics is enabled");
            future<()> uploadTask = start timerTask();            // file uploading task
            future<()> rotateTask = start rotatingTask();        // file rotating task
        } else {
            printDebug(KEY_ANALYTICS_FILTER, "Analytics is disabled");
        }
    }
}

function initializeELKAnalytics() {
    isELKAnalyticsEnabled = <boolean>getConfigBooleanValue(ELK_ANALYTICS, ELK_ANALYTICS_ENABLE, DEFAULT_ANALYTICS_ENABLED);
}

function initializeChoreoAnalytics() {
    isChoreoAnalyticsEnabled = <boolean>getConfigBooleanValue(CHOREO_ANALYTICS, CHOREO_ANALYTICS_ENABLE, DEFAULT_ANALYTICS_ENABLED);
}

public function retrieveHostname(string key, string defaultHost) returns string {
    return config:getAsString(key, defaultHost);
}

// Populates fault event data for APIM 4.x analytics
function populateFaultAnalytics4xDTO(http:Response response, http:FilterContext context) returns @tainted (FaultDTO | error) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();

    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    FaultDTO eventDto = {};
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    map<json> metaInfo = {};

    eventDto.apiContext = getContext(context);
    eventDto.apiName = getApiName(context);
    string resourceName = context.getResourceName();
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
    if (httpResourceConfig is http:HttpResourceConfig) {
        var resource_Path = httpResourceConfig.path;
        eventDto.resourcePath = resource_Path;
        eventDto.apiResourceTemplate = httpResourceConfig.path;
    }
    eventDto.method = <string> invocationContext.attributes[REQUEST_METHOD];
    if (context.attributes.hasKey(ERROR_CODE)) {
        eventDto.errorCode = <int> context.attributes[ERROR_CODE];
        eventDto.errorMessage = <string> context.attributes[ERROR_MESSAGE];
    } else if (invocationContext.attributes.hasKey(ERROR_CODE)) {
        eventDto.errorCode = <int> invocationContext.attributes[ERROR_CODE];
        eventDto.errorMessage = <string> invocationContext.attributes[ERROR_MESSAGE];
    } else {
        eventDto.errorCode = <int> invocationContext.attributes[ERROR_RESPONSE_CODE];
        eventDto.errorMessage = <string> invocationContext.attributes[ERROR_RESPONSE];
    }
    eventDto.faultTime = currentTimeMills;
    eventDto.apiCreatorTenantDomain = getTenantDomain(context);
    eventDto.hostName = retrieveHostname(DATACENTER_ID, <string>context.attributes[HOSTNAME_PROPERTY]);
    if (context.attributes[PROTOCOL_PROPERTY] is string) {
        eventDto.protocol = <string>context.attributes[PROTOCOL_PROPERTY];
    }

    // if response contains Content-Length header that value will be taken
    if (response.hasHeader(CONTENT_LENGHT_HEADER)) {
        var respSize = 'int:fromString(response.getHeader(CONTENT_LENGHT_HEADER));
        if (respSize is int) {
            eventDto.responseSize = respSize;
            printDebug(KEY_ANALYTICS_FILTER, "Response content lenght header : " + respSize.toString());
        } else {
            eventDto.responseSize = 0;
        }
    } else {
        eventDto.responseSize = 0;
    }
    // if response contains Content-Type header that value will be taken
    if (response.getContentType() != "") {
        eventDto.responseContentType = response.getContentType();
    } else {
        eventDto.responseContentType = UNKNOWN_VALUE;
    }

    if (isSecured && invocationContext.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        metaInfo["keyType"] = authContext.keyType;
        eventDto.keyType = authContext.keyType;
        eventDto.isAnonymous = false;
        eventDto.isAuthenticated = true;
        eventDto.consumerKey = authContext.consumerKey;
        eventDto.userName = authContext.username;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.applicationUUID = authContext.applicationUuid;
        eventDto.userTenantDomain = authContext.subscriberTenantDomain;
        eventDto.apiCreator = authContext.apiPublisher;
        eventDto.applicationOwner = authContext.subscriber;
    } else {
        metaInfo["keyType"] = PRODUCTION_KEY_TYPE;
        eventDto.keyType = PRODUCTION_KEY_TYPE;
        eventDto.isAnonymous = true;
        eventDto.isAuthenticated = false;
        eventDto.consumerKey = ANONYMOUS_CONSUMER_KEY;
        eventDto.userName = END_USER_ANONYMOUS;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.applicationUUID = ANONYMOUS_APP_ID;
        eventDto.userTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
        eventDto.applicationOwner = END_USER_ANONYMOUS;
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
    eventDto.correlationId = <string>context.attributes[MESSAGE_ID];
    eventDto.metaClientType = metaInfo.toString();
    if (invocationContext.attributes.hasKey(ADDITIONAL_ANALYTICS_PROPS) &&
        invocationContext.attributes[ADDITIONAL_ANALYTICS_PROPS] is string) {
        eventDto.properties = <string>invocationContext.attributes[ADDITIONAL_ANALYTICS_PROPS];
    }
    return eventDto;
}

function validateEvent(RequestResponseExecutionDTO requestResponseExecutionDTO) returns boolean {
    //considered as a malformed even when request timestamp is less than or equal to zero
    if(requestResponseExecutionDTO.requestTimestamp <= 0) {
        return false;
    }
    return true;
}
