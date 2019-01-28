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
import ballerina/log;
import ballerina/auth;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;
import ballerina/internal;
import ballerina/system;

public function isResourceSecured(http:ListenerAuthConfig? resourceLevelAuthAnn, http:ListenerAuthConfig?
    serviceLevelAuthAnn) returns boolean {
    boolean isSecured = true;
    match resourceLevelAuthAnn.authentication {
        http:Authentication authn => {
            isSecured = authn.enabled;
        }
        () => {
            // if not found at resource level, check in the service level
            match serviceLevelAuthAnn.authentication {
                http:Authentication authn => {
                    isSecured = authn.enabled;
                }
                () => {
                    // by default if no value given, we think auth is enabled in gateway
                    isSecured = true;
                }
            }
        }
    }
    return isSecured;
}

@Description { value: "Tries to retrieve the annotation value for authentication hierarchically - first from the resource
level
and then from the service level, if its not there in the resource level" }
@Param { value: "annotationPackage: annotation package name" }
@Param { value: "annotationName: annotation name" }
@Param { value: "annData: array of annotationData instances" }
@Return { value: "ListenerAuthConfig: ListenerAuthConfig instance if its defined, else nil" }
public function getAuthAnnotation(string annotationPackage, string annotationName, reflect:annotationData[] annData)
                    returns (http:ListenerAuthConfig?) {
    if (lengthof annData == 0) {
        return ();
    }
    reflect:annotationData|() authAnn;
    foreach ann in annData {
        if (ann.name == annotationName && ann.pkgName == annotationPackage) {
            authAnn = ann;
            break;
        }
    }
    match authAnn {
        reflect:annotationData annData1 => {
            if (annotationName == RESOURCE_ANN_NAME) {
                http:HttpResourceConfig resourceConfig = check <http:HttpResourceConfig>annData1.value;
                return resourceConfig.authConfig;
            } else if (annotationName == SERVICE_ANN_NAME) {
                http:HttpServiceConfig serviceConfig = check <http:HttpServiceConfig>annData1.value;
                return serviceConfig.authConfig;
            } else {
                return ();
            }
        }
        () => {
            return ();
        }
    }
}


@Description { value: "Retrieve the annotation related to resources" }
@Return { value: "HttpResourceConfig: HttpResourceConfig instance if its defined, else nil" }
public function getResourceConfigAnnotation(reflect:annotationData[] annData)
                    returns (http:HttpResourceConfig) {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() authAnn;
    foreach ann in annData {
        if (ann.name == RESOURCE_ANN_NAME && ann.pkgName == ANN_PACKAGE) {
            authAnn = ann;
            break;
        }
    }
    match authAnn {
        reflect:annotationData annData1 => {
            http:HttpResourceConfig resourceConfig = check <http:HttpResourceConfig>annData1.value;
            return resourceConfig;
        }
        () => {
            return {};
        }
    }
}

@Description { value: "Retrieve the annotation related to resource level Tier" }
@Return { value: "TierConfiguration: TierConfiguration instance if its defined, else nil" }
public function getResourceLevelTier(reflect:annotationData[] annData)
                    returns (TierConfiguration) {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() tierAnn;
    foreach ann in annData {
        if (ann.name == RESOURCE_TIER_ANN_NAME && ann.pkgName == RESOURCE_TIER_ANN_PACKAGE) {
            tierAnn = ann;
            break;
        }
    }
    match tierAnn {
        reflect:annotationData annData1 => {
            TierConfiguration resourceLevelTier = check <TierConfiguration>annData1.value;
            return resourceLevelTier;
        }
        () => {
            return {};
        }
    }
}

@Description { value: "Retrieve the annotation related to service" }
@Return { value: "HttpServiceConfig: HttpResourceConfig instance if its defined, else nil" }
public function getServiceConfigAnnotation(reflect:annotationData[] annData)
                    returns (http:HttpServiceConfig) {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() authAnn;
    foreach ann in annData {
        if (ann.name == SERVICE_ANN_NAME && ann.pkgName == ANN_PACKAGE) {
            authAnn = ann;
            break;
        }
    }
    match authAnn {
        reflect:annotationData annData1 => {
            http:HttpServiceConfig serviceConfig = check <http:HttpServiceConfig>annData1.value;
            return serviceConfig;
        }
        () => {
            return {};
        }
    }
}

@Description { value: "Retrieve the key validation request dto from filter context" }
@Return { value: "api key validation request dto" }
public function getKeyValidationRequestObject() returns APIRequestMetaDataDto {
    APIRequestMetaDataDto apiKeyValidationRequest = {};
    typedesc serviceType = check <typedesc>runtime:getInvocationContext().attributes[SERVICE_TYPE_ATTR];
    http:HttpServiceConfig httpServiceConfig = getServiceConfigAnnotation(reflect:getServiceAnnotations(serviceType));
    http:HttpResourceConfig httpResourceConfig = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(serviceType, <string>runtime:
            getInvocationContext().attributes[RESOURCE_NAME_ATTR]));
    string apiContext = httpServiceConfig.basePath;
    APIConfiguration apiConfig = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(serviceType));
    string apiVersion = apiConfig.apiVersion;
    apiKeyValidationRequest.apiVersion = apiVersion;
    if (!apiContext.contains(apiVersion)){
        if (apiContext.hasSuffix(PATH_SEPERATOR)) {
            apiContext = apiContext + apiVersion;
        } else {
            apiContext = apiContext + PATH_SEPERATOR + apiVersion;
        }
    }
    apiKeyValidationRequest.context = apiContext;
    apiKeyValidationRequest.requiredAuthenticationLevel = ANY_AUTHENTICATION_LEVEL;
    apiKeyValidationRequest.clientDomain = "*";
    apiKeyValidationRequest.matchingResource = httpResourceConfig.path;
    apiKeyValidationRequest.httpVerb = httpResourceConfig.methods[0];
    apiKeyValidationRequest.accessToken = <string>runtime:getInvocationContext().attributes[ACCESS_TOKEN_ATTR];
    printDebug(KEY_UTILS, "Created request meta-data object with context: " + apiContext
            + ", resource: " + apiKeyValidationRequest.matchingResource
            + ", verb: " + apiKeyValidationRequest.httpVerb);
    return apiKeyValidationRequest;

}


public function getAPIDetailsFromServiceAnnotation(reflect:annotationData[] annData) returns APIConfiguration {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() apiAnn;
    foreach ann in annData {
        if (ann.name == API_ANN_NAME && ann.pkgName == GATEWAY_ANN_PACKAGE) {
            apiAnn = ann;
            break;
        }
    }
    match apiAnn {
        reflect:annotationData annData1 => {
            APIConfiguration apiConfig = check <APIConfiguration>annData1.value;
            return apiConfig;
        }
        () => {
            return {};
        }
    }
}

public function getTenantFromBasePath(string basePath) returns string {
    string[] splittedArray = basePath.split("/");
    return splittedArray[lengthof splittedArray - 1];
}


public function isAccessTokenExpired(APIKeyValidationDto apiKeyValidationDto) returns boolean {
    int validityPeriod = check <int>apiKeyValidationDto.validityPeriod;
    int issuedTime = check <int>apiKeyValidationDto.issuedTime;
    int timestampSkew = getConfigIntValue(KM_CONF_INSTANCE_ID, TIMESTAMP_SKEW, 5000);
    int currentTime = time:currentTime().time;
    int intMaxValue = 9223372036854775807;
    if (validityPeriod != intMaxValue &&
            // For cases where validityPeriod is closer to int.MAX_VALUE (then issuedTime + validityPeriod would spill
            // over and would produce a negative value)
            (currentTime - timestampSkew) > validityPeriod) {
        if ((currentTime - timestampSkew) > (issuedTime + validityPeriod)) {
            apiKeyValidationDto.validationStatus = API_AUTH_INVALID_CREDENTIALS_STRING;
            return true;
        }
    }
    return false;
}
public function getContext(http:FilterContext context) returns (string) {
    http:HttpServiceConfig httpServiceConfig = getServiceConfigAnnotation(reflect:getServiceAnnotations
        (context.serviceType));
    return httpServiceConfig.basePath;

}

public function getClientIp(http:Request request, http:Listener listener) returns (string) {
    string clientIp;
    if (request.hasHeader(X_FORWARD_FOR_HEADER)) {
        clientIp = request.getHeader(X_FORWARD_FOR_HEADER);
        int idx = clientIp.indexOf(",");
        if (idx > -1) {
            clientIp = clientIp.substring(0, idx);
        }
    } else {
        clientIp = listener.remote.host;
    }
    return clientIp;
}

public function extractAccessToken(http:Request req, string authHeaderName) returns (string|error) {
    string authHeader = req.getHeader(authHeaderName);
    string[] authHeaderComponents = authHeader.split(" ");
    if (lengthof authHeaderComponents != 2){
        return handleError("Incorrect bearer authentication header format");
    }
    return authHeaderComponents[1];
}

public function handleError(string message) returns (error) {
    error e = { message: message };
    return e;
}
public function getTenantDomain(http:FilterContext context) returns (string) {
    // todo: need to implement to get tenantDomain
    string apiContext = getContext(context);
    string[] splittedContext = apiContext.split("/");
    if (lengthof splittedContext > 3){
        // this check if basepath have /t/domain in
        return splittedContext[2];
    } else {
        return SUPER_TENANT_DOMAIN_NAME;
    }
}
public function getApiName(http:FilterContext context) returns (string) {
    string serviceName = context.serviceName;
    return serviceName.split("_")[0];
}

public function getConfigValue(string instanceId, string property, string defaultValue) returns string {
    return config:getAsString(instanceId + "." + property, default = defaultValue);
}

public function getConfigIntValue(string instanceId, string property, int defaultValue) returns int {
    return config:getAsInt(instanceId + "." + property, default = defaultValue);
}

public function getConfigBooleanValue(string instanceId, string property, boolean defaultValue) returns boolean {
    return config:getAsBoolean(instanceId + "." + property, default = defaultValue);
}

public function getConfigFloatValue(string instanceId, string property, float defaultValue) returns float {
    return config:getAsFloat(instanceId + "." + property, default = defaultValue);
}

public function getConfigMapValue(string property) returns map {
    return config:getAsMap(property);
}

public function setErrorMessageToFilterContext(http:FilterContext context, int errorCode) {
    int status;
    if (errorCode == API_AUTH_GENERAL_ERROR) {
        status = INTERNAL_SERVER_ERROR;
    } else if (errorCode == API_AUTH_INCORRECT_API_RESOURCE ||
        errorCode == API_AUTH_FORBIDDEN ||
        errorCode == INVALID_SCOPE) {
        status = FORBIDDEN;
    } else if(errorCode == INVALID_ENTITY) {
        status = UNPROCESSABLE_ENTITY;
    } else if(errorCode == INVALID_RESPONSE) {
        status = INTERNAL_SERVER_ERROR;
    } else {
        status = UNAUTHORIZED;

    }
    context.attributes[HTTP_STATUS_CODE] = status;
    context.attributes[FILTER_FAILED] = true;
    context.attributes[ERROR_CODE] = errorCode;
    string errorMessage = getAuthenticationFailureMessage(errorCode);
    context.attributes[ERROR_MESSAGE] = errorMessage;
    context.attributes[ERROR_DESCRIPTION] = getFailureMessageDetailDescription(errorCode, errorMessage);
}

@Description { value: "Default error response sender with json error response" }
public function sendErrorResponse(http:Listener listener, http:Request request, http:FilterContext context) {
    endpoint http:Listener caller = listener;
    int statusCode = check <int>context.attributes[HTTP_STATUS_CODE];
    string errorDescription = <string>context.attributes[ERROR_DESCRIPTION];
    string errorMesssage = <string>context.attributes[ERROR_MESSAGE];
    int errorCode = check <int>context.attributes[ERROR_CODE];
    http:Response response;
    response.statusCode = statusCode;
    response.setContentType(APPLICATION_JSON);
    json payload = { fault: {
        code: errorCode,
        message: errorMesssage,
        description: errorDescription
    } };
    response.setJsonPayload(payload);
    var value = caller->respond(response);
    match value {
        error err => log:printError("Error occurred while sending the error response", err = err);
        () => {}
    }
}

public function getAuthorizationHeader(reflect:annotationData[] annData) returns string {
    APIConfiguration apiConfig = getAPIDetailsFromServiceAnnotation(annData);
    string authHeader = apiConfig.authorizationHeader;
    if (authHeader == "") {
        authHeader = getConfigValue(AUTH_CONF_INSTANCE_ID, AUTH_HEADER_NAME, AUTHORIZATION_HEADER);
    }
    return authHeader;

}

public function getCurrentTime() returns int {
    time:Time currentTime = time:currentTime();
    int time = currentTime.time;
    return time;

}

public function rotateFile(string fileName) returns string|error {
    string uuid = system:uuid();
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR) + PATH_SEPERATOR;
    int rotatingTimeStamp = getCurrentTime();
    string zipName = fileName + "." + rotatingTimeStamp + "." + uuid + ZIP_EXTENSION;
    internal:Path zipLocation = new(fileLocation + zipName);
    internal:Path fileToZip = new(fileLocation + fileName);
    match internal:compress(fileToZip, zipLocation) {
        error compressError => {
            printFullError(KEY_UTILS, compressError);
            return compressError;
        }
        () => {
            printInfo(KEY_UTILS, "File compressed successfully");
            match fileToZip.delete() {
                () => {
                    printInfo(KEY_UTILS, "Existing file deleted successfully");
                }
                error err => {
                    printFullError(KEY_UTILS, err);
                }
            }
            return zipName;
        }
    }
}

@Description { value: "Retrieve external configurations defined against a key" }
public function retrieveConfig(string key, string default) returns string {
    return config:getAsString(key, default = default);
}

@Description { value: "mask all letters with given text except last 4 charactors." }
public function mask(string text) returns string {
    if (text.length() > 4) {
        string last = text.substring(text.length() - 4, text.length());
        string first = text.substring(0, text.length() - 4).replaceAll(".", "x");
        return first + last;
    } else {
        return "xxxx";
    }
}

@Description { value: "Returns the current message ID (uuid)" }
public function getMessageId() returns string {
    string messageId = <string>runtime:getInvocationContext().attributes[MESSAGE_ID];
    if (messageId == null) {
        return "-";
    } else {
        return messageId;
    }
}

@Description { value: "Add a error log with provided key (class) and message ID" }
public function printError(string key, string message) {
    log:printError(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

@Description { value: "Add a debug log with provided key (class) and message ID" }
public function printDebug(string key, string message) {
    log:printDebug(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

@Description { value: "Add a trace log with provided key (class) and message ID" }
public function printTrace(string key, string message) {
    log:printTrace(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

@Description { value: "Add a info log with provided key (class) and message ID" }
public function printInfo(string key, string message) {
    log:printInfo(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

@Description { value: "Add a full error log with provided key (class) and message ID" }
public function printFullError(string key, error message) {
    log:printError(io:sprintf("[%s] [%s] %s", key, getMessageId(), message.message), err = message);
}

function setLatency(int starting, http:FilterContext context, string latencyType) {
    int ending = getCurrentTime();
    context.attributes[latencyType] = ending - starting;
    printDebug(KEY_THROTTLE_FILTER, "Throttling latency: " + (ending - starting) + "ms");
}

@Description { value: "Check MESSAGE_ID in context and set if it is not" }
function checkOrSetMessageID(http:FilterContext context) {
    if (!context.attributes.hasKey(MESSAGE_ID)) {
        context.attributes[MESSAGE_ID] = system:uuid();
    }
}

public function checkExpectHeaderPresent(http:Request request) {
    if (request.expects100Continue()) {
        request.removeHeader(EXPECT_HEADER);
        printDebug(KEY_UTILS, "Expect header is removed from the request");

    }
}
