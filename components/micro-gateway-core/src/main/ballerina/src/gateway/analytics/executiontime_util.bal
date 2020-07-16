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
import ballerina/runtime;

public function generateExecutionTimeEvent(http:FilterContext context) returns ExecutionTimeDTO {
    ExecutionTimeDTO executionTimeDTO = {};
    boolean isSecured = <boolean>context.attributes[IS_SECURED];
    printDebug(KEY_ANALYTICS_FILTER, "Resource is secured : " + isSecured.toString());
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (isSecured && invocationContext.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        executionTimeDTO.provider = authContext.apiPublisher;
        executionTimeDTO.keyType = authContext.keyType;
    } else {
        APIConfiguration? apiConfig = apiConfigAnnotationMap[context.getServiceName()];
        if (apiConfig is APIConfiguration) {
            executionTimeDTO.provider = apiConfig.publisher;
        }
        executionTimeDTO.keyType = PRODUCTION_KEY_TYPE;
    }
    executionTimeDTO.apiName = getApiName(context);
    APIConfiguration? apiConfig = apiConfigAnnotationMap[context.getServiceName()];
    if (apiConfig is APIConfiguration) {
        executionTimeDTO.apiVersion = apiConfig.apiVersion;
    }
    executionTimeDTO.tenantDomain = getTenantDomain(context);
    executionTimeDTO.context = getContext(context);
    executionTimeDTO.correleationID = <string>context.attributes[MESSAGE_ID];

    executionTimeDTO.securityLatency = getSecurityLatency(context);
    executionTimeDTO.eventTime = getCurrentTime();
    if (context.attributes.hasKey(THROTTLE_LATENCY)) {
         executionTimeDTO.throttlingLatency = <int>context.attributes[THROTTLE_LATENCY];
    }
    executionTimeDTO.requestMediationLatency = 0;
    executionTimeDTO.otherLatency = 0;
    executionTimeDTO.responseMediationLatency = 0;
    if (invocationContext.attributes.hasKey(TS_REQUEST_OUT) && invocationContext.attributes.hasKey(TS_RESPONSE_IN)) {
        int timeRequestOut = <int>invocationContext.attributes[TS_REQUEST_OUT];
        int timeResponseIn = <int>invocationContext.attributes[TS_RESPONSE_IN];
        executionTimeDTO.backEndLatency = timeResponseIn - timeRequestOut;
    }
    printDebug(KEY_ANALYTICS_FILTER, "Execution time dto : " + executionTimeDTO.toString());
    return executionTimeDTO;
}

public function getSecurityLatency(http:FilterContext context) returns int {
    int latency = 0;
    if (context.attributes.hasKey(SECURITY_LATENCY_AUTHN)) {
        latency += <int>context.attributes[SECURITY_LATENCY_AUTHN];
    }
    if (context.attributes.hasKey(SECURITY_LATENCY_AUTHZ)) {
        latency += <int>context.attributes[SECURITY_LATENCY_AUTHZ];
    }
    if (context.attributes.hasKey(SECURITY_LATENCY_AUTHZ_RESPONSE)) {
        latency += <int>context.attributes[SECURITY_LATENCY_AUTHZ_RESPONSE];
    }
    if (context.attributes.hasKey(SECURITY_LATENCY_SUBS)) {
        latency += <int>context.attributes[SECURITY_LATENCY_SUBS];
    }
    if (context.attributes.hasKey(SECURITY_LATENCY_VALIDATION)) {
        latency += <int>context.attributes[SECURITY_LATENCY_VALIDATION];
    }
    return latency;
}
