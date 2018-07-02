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

function populateThrottleAnalyticdDTO(http:FilterContext context) returns (ThrottleAnalyticsEventDTO) {
    boolean isSecured =check <boolean>context.attributes[IS_SECURED];
    ThrottleAnalyticsEventDTO eventDto;
    string apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).apiVersion;
    time:Time time = time:currentTime();
    int currentTimeMills = time.time;

    json metaInfo = {};
    metaInfo.correlationID = <string>context.attributes[MESSAGE_ID];
    eventDto.clientType = metaInfo.toString();
    eventDto.accessToken = "-";
    eventDto.tenantDomain = getTenantDomain(context);
    eventDto.api = getApiName(context);
    eventDto.api_version = apiVersion;
    eventDto.context = getContext(context);
    eventDto.throttledTime = currentTimeMills;
    eventDto.throttledOutReason = <string> context.attributes[THROTTLE_OUT_REASON];
    if(isSecured){
        AuthenticationContext authConext = check <AuthenticationContext>context
            .attributes[AUTHENTICATION_CONTEXT];
        metaInfo.keyType = authConext.keyType;
        eventDto.userId = authConext.username;
        eventDto.apiPublisher = authConext.apiPublisher;
        eventDto.applicationName = authConext.applicationName;
        eventDto.applicationId = authConext.applicationId;
        eventDto.subscriber = authConext.subscriber;
    } else {
        metaInfo.keyType = PRODUCTION_KEY_TYPE;
        eventDto.userId = END_USER_ANONYMOUS;
        eventDto.apiPublisher = getAPIDetailsFromServiceAnnotation(
                                    reflect:getServiceAnnotations(context.serviceType)).publisher;
        eventDto.applicationName = ANONYMOUS_APP_NAME;
        eventDto.applicationId = ANONYMOUS_APP_ID;
        eventDto.subscriber = END_USER_ANONYMOUS;
    }
    return eventDto;
}