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

function populateThrottleAnalyticdDTO(http:FilterContext context) returns (ThrottleAnalyticsEventDTO) {
    boolean isSecured = check <boolean>context.attributes[IS_SECURED];
    ThrottleAnalyticsEventDTO eventDto;
    string apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).apiVersion;
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;

    json metaInfo = {};
    eventDto.tenantDomain = getTenantDomain(context);
    eventDto.api = getApiName(context);
    eventDto.context = getContext(context);
    eventDto.throttledTime = currentTimeMills;
    eventDto.throttledOutReason = <string>context.attributes[THROTTLE_OUT_REASON];
    if (isSecured) {
        AuthenticationContext authContext = check <AuthenticationContext>context
        .attributes[AUTHENTICATION_CONTEXT];
        metaInfo.keyType = authContext.keyType;
        eventDto.userId = authContext.username;
        eventDto.apiPublisher = authContext.apiPublisher;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
        eventDto.subscriber = authContext.subscriber;
        //consumer key is sent here. (not using for stat)
        eventDto.accessToken = authContext.consumerKey;
    } else {
        metaInfo.keyType = PRODUCTION_KEY_TYPE;
        eventDto.userId = END_USER_ANONYMOUS;
        eventDto.apiPublisher = getAPIDetailsFromServiceAnnotation(
                                    reflect:getServiceAnnotations(context.serviceType)).publisher;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.subscriber = END_USER_ANONYMOUS;
    }
    eventDto.api_version = eventDto.apiPublisher + ":" + apiVersion;
    metaInfo.correlationID = <string>context.attributes[MESSAGE_ID];
    eventDto.clientType = metaInfo.toString();
    return eventDto;
}

function populateFaultAnalyticsDTO(http:FilterContext context, error err) returns (FaultDTO) {
    boolean isSecured = check <boolean>context.attributes[IS_SECURED];
    FaultDTO eventDto;
    string apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).apiVersion;
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;
    json metaInfo = {};
    eventDto.context = getContext(context);
    eventDto.apiVersion = apiVersion;
    eventDto.apiName = getApiName(context);
    eventDto.resourcePath = getResourceConfigAnnotation(reflect:getResourceAnnotations(context.serviceType,
            context.resourceName)).path;
    eventDto.method = <string>context.attributes[METHOD];
    eventDto.versionOnly = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
    apiVersion;
    eventDto.errorCode = check <int>runtime:getInvocationContext().attributes[ERROR_RESPONSE_CODE];
    eventDto.errorMessage = err.message;
    eventDto.faultTime = currentTimeMills;
    eventDto.tenantDomain = getTenantDomain(context);
    eventDto.hostName = <string>context.attributes[HOSTNAME_PROPERTY];
    eventDto.protocol = <string>context.attributes[PROTOCOL_PROPERTY];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        metaInfo.keyType = authContext.keyType;
        eventDto.consumerKey = authContext.consumerKey;
        eventDto.apiPublisher = authContext.apiPublisher;
        eventDto.userName = authContext.username;
        eventDto.applicationName = authContext.applicationName;
        eventDto.applicationId = authContext.applicationId;
    } else {
        metaInfo.keyType = PRODUCTION_KEY_TYPE;
        eventDto.consumerKey = ANONYMOUS_CONSUMER_KEY;
        eventDto.apiPublisher = getAPIDetailsFromServiceAnnotation(
                                    reflect:getServiceAnnotations(context.serviceType)).publisher;
        eventDto.userName = END_USER_ANONYMOUS;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
    }
    metaInfo.correlationID = <string>context.attributes[MESSAGE_ID];
    eventDto.clientType = metaInfo.toString();
    return eventDto;
}


function getAnalyticsEnableConfig() {
    map vals = getConfigMapValue(ANALYTICS);
    isAnalyticsEnabled = check <boolean>vals[ENABLE];
    rotatingTime =  check <int> vals[ROTATING_TIME];
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
            future uploadTask = start timerTask();
            future rotateTask = start rotatingTask();
        } else {
            printDebug(KEY_ANALYTICS_FILTER, "Analytics is disabled");
        }
    }
}

function initStreamPublisher() {
    printDebug(KEY_UTILS, "Subscribing writing method to event stream");
    eventStream.subscribe(writeEventToFile);
}