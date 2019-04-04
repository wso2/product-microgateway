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

boolean isAnalyticsEnabled = false;
boolean configsRead = false;

function populateThrottleAnalyticsDTO(http:FilterContext context) returns (ThrottleAnalyticsEventDTO) {
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    ThrottleAnalyticsEventDTO eventDto = {};
    var api_Version = apiConfigAnnotationMap[getServiceName(context.serviceName)].apiVersion;
    string apiVersion = (api_Version is string) ? api_Version : "";
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;

    json metaInfo = {};
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
        AuthenticationContext authContext = <AuthenticationContext>context
        .attributes[AUTHENTICATION_CONTEXT];
        metaInfo.keyType = authContext.keyType;
        eventDto.userName = authContext.username;
        eventDto.apiCreator = authContext.apiPublisher;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.subscriber = authContext.subscriber;
    } else {
        metaInfo.keyType = PRODUCTION_KEY_TYPE;
        eventDto.userName = END_USER_ANONYMOUS;
        var api_Creator = apiConfigAnnotationMap[getServiceName(context.serviceName)].publisher;
        eventDto.apiCreator = (api_Creator is string) ? api_Creator : "";
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.subscriber = END_USER_ANONYMOUS;
    }
    eventDto.apiVersion =  apiVersion;
    metaInfo.correlationID = <string>context.attributes[MESSAGE_ID];
    eventDto.metaClientType = metaInfo.toString();
    return eventDto;
}

function populateFaultAnalyticsDTO(http:FilterContext context, error err) returns (FaultDTO) {
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    FaultDTO eventDto = {};
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    json metaInfo = {};
    eventDto.apiContext = getContext(context);
    var api_Version = apiConfigAnnotationMap[getServiceName(context.serviceName)].apiVersion;
    eventDto.apiVersion = (api_Version is string) ? api_Version : "";
    eventDto.apiName = getApiName(context);
    var resource_Path = getResourceConfigAnnotation(resourceAnnotationMap[context.resourceName] ?: []).path;
    eventDto.resourcePath = (resource_Path is string) ? resource_Path : "";
    eventDto.method = <string>context.attributes[API_METHOD_PROPERTY];
    eventDto.errorCode = <int>runtime:getInvocationContext().attributes[ERROR_RESPONSE_CODE];
    eventDto.errorMessage = err.reason();
    eventDto.faultTime = currentTimeMills;
    eventDto.apiCreatorTenantDomain = getTenantDomain(context);
    eventDto.hostName = retrieveHostname(DATACENTER_ID, <string>context.attributes[HOSTNAME_PROPERTY]);
    eventDto.protocol = <string>context.attributes[PROTOCOL_PROPERTY];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        metaInfo.keyType = authContext.keyType;
        eventDto.consumerKey = authContext.consumerKey;
        eventDto.apiCreator = authContext.apiPublisher;
        eventDto.userName = authContext.username;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.userTenantDomain = authContext.subscriberTenantDomain;
    } else {
        metaInfo.keyType = PRODUCTION_KEY_TYPE;
        eventDto.consumerKey = ANONYMOUS_CONSUMER_KEY;
        var api_Creater = apiConfigAnnotationMap[getServiceName(context.serviceName)].publisher;
        eventDto.apiCreator = (api_Creater is string) ? api_Creater : "";
        eventDto.userName = END_USER_ANONYMOUS;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.userTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
    }
    metaInfo.correlationID = <string>context.attributes[MESSAGE_ID];
    eventDto.metaClientType = metaInfo.toString();
    return eventDto;
}


function getAnalyticsEnableConfig() {
    map<any> vals = getConfigMapValue(ANALYTICS);
    isAnalyticsEnabled = <boolean>vals[ENABLE];
    rotatingTime =  <int> vals[ROTATING_TIME];
    uploadingUrl = <string> vals[UPLOADING_EP];
    configsRead = true;
    printDebug(KEY_UTILS, "Analytics configuration values read");
}


function initializeAnalytics() {
    if (!configsRead) {
        getAnalyticsEnableConfig();
        if (isAnalyticsEnabled) {
            initStreamPublisher();
            printDebug(KEY_ANALYTICS_FILTER, "Analytics is enabled");
            future<()> uploadTask = start timerTask();
            future<()> rotateTask = start rotatingTask();
        } else {
            printDebug(KEY_ANALYTICS_FILTER, "Analytics is disabled");
        }
    }
}

function initStreamPublisher() {
    printDebug(KEY_UTILS, "Subscribing writing method to event stream");
    eventStream.subscribe(writeEventToFile);
}

public function retrieveHostname(string key, string default) returns string {
    return config:getAsString(key, default = default);
}