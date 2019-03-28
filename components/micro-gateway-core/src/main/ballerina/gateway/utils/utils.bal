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
import ballerina/encoding;

public map<reflect:annotationData[]> resourceAnnotationMap = {};
public map<reflect:annotationData[]> serviceAnnotationMap = {};
public map<TierConfiguration?> resourceTierAnnotationMap = {};
public map<APIConfiguration?> apiConfigAnnotationMap = {};


public function populateAnnotationMaps(string serviceName, service s, string[] resourceArray) {
    foreach string resourceFunction in resourceArray {
        resourceAnnotationMap[resourceFunction] = reflect:getResourceAnnotations(s, resourceFunction);
        resourceTierAnnotationMap[resourceFunction] = getResourceLevelTier(reflect:getResourceAnnotations
            (s, resourceFunction));
    }
    serviceAnnotationMap[serviceName] = reflect:getServiceAnnotations(s);
    apiConfigAnnotationMap[serviceName] = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(s));
}

public function isResourceSecured(http:ListenerAuthConfig? resourceLevelAuthAnn, http:ListenerAuthConfig?
    serviceLevelAuthAnn) returns boolean {
    boolean isSecured = true;
    var authn =  resourceLevelAuthAnn.authentication;
        if(authn is http:Authentication)  {
            isSecured = authn.enabled;
        }
        else {
            // if not found at resource level, check in the service level
            var serviceAuthn =  serviceLevelAuthAnn.authentication;
            if(serviceAuthn is http:Authentication) {
                isSecured = serviceAuthn.enabled;
            }
            else {
                // by default if no value given, we think auth is enabled in gateway
                isSecured = true;
            }
        }
    return isSecured;
}


# Tries to retrieve the annotation value for authentication hierarchically - first from the resource
# level and then from the service level, if its not there in the resource level
#
# + annotationPackage - annotation package name
# + annotationName - annotation name
# + annData - array of annotationData instances
# + return - ListenerAuthConfig: ListenerAuthConfig instance if its defined, else nil
public function getAuthAnnotation(string annotationPackage, string annotationName, reflect:annotationData[] annData)
                    returns (http:ListenerAuthConfig?) {
    if (annData.length() == 0) {
        return ();
    }
    reflect:annotationData|() authAnn = ();
    foreach var ann in annData {
        if (ann.name == annotationName && ann.moduleName == annotationPackage) {
            authAnn = ann;
            break;
        }
    }
    if(authAnn is reflect:annotationData)  {
        if (annotationName == RESOURCE_ANN_NAME) {
            http:HttpResourceConfig resourceConfig =  <http:HttpResourceConfig>authAnn.value;
            return resourceConfig.authConfig;
        } else if (annotationName == SERVICE_ANN_NAME) {
            http:HttpServiceConfig serviceConfig =  <http:HttpServiceConfig>authAnn.value;
            return serviceConfig.authConfig;
        } else {
            return ();
        }
    } else {
        return ();
    }
}


# Retrieve the annotation related to resources
#
# + annData - array of annotationData instances
# + return - HttpResourceConfig: HttpResourceConfig instance if its defined, else nil
public function getResourceConfigAnnotation(reflect:annotationData[] annData)
                    returns (http:HttpResourceConfig?) {
    if (annData.length() == 0) {
        return ();
    }
    reflect:annotationData|() authAnn = ();
    foreach var ann in annData {
        if (ann.name == RESOURCE_ANN_NAME && ann.moduleName == ANN_PACKAGE) {
            authAnn = ann;
            break;
        }
    }
    if(authAnn is reflect:annotationData) {
        http:HttpResourceConfig resourceConfig =  <http:HttpResourceConfig>authAnn.value;
        return resourceConfig;
    }
    else {
        return ();
    }
}

# Retrieve the annotation related to resource level Tier
#
# + annData - array of annotationData instances
# + return - TierConfiguration: TierConfiguration instance if its defined, else nil
public function getResourceLevelTier(reflect:annotationData[] annData)
                    returns (TierConfiguration?) {
    if (annData.length() == 0) {
        return ();
    }
    reflect:annotationData|() tierAnn = ();
    foreach var ann in annData {
        if (ann.name == RESOURCE_TIER_ANN_NAME && ann.moduleName == RESOURCE_TIER_ANN_PACKAGE) {
            tierAnn = ann;
            break;
        }
    }
    if(tierAnn is reflect:annotationData) {
        TierConfiguration resourceLevelTier =  <TierConfiguration>tierAnn.value;
        return resourceLevelTier;
    }
    else {
        return ();
    }
}

# Retrieve the annotation related to service
#
# + annData - array of annotationData instances
# + return - HttpServiceConfig: HttpResourceConfig instance if its defined, else nil
public function getServiceConfigAnnotation(reflect:annotationData[] annData)
                    returns (http:HttpServiceConfig?) {
    if (annData.length() == 0) {
        return ();
    }
    reflect:annotationData|() authAnn = ();
    foreach var ann in annData {
        if (ann.name == SERVICE_ANN_NAME && ann.moduleName == ANN_PACKAGE) {
            authAnn = ann;
            break;
        }
    }
    if(authAnn is reflect:annotationData) {
        http:HttpServiceConfig serviceConfig =  <http:HttpServiceConfig>authAnn.value;
        return serviceConfig;
    }
    else {
        return ();
    }
}

# Retrieve the key validation request dto from filter context
#
# + return - api key validation request dto
public function getKeyValidationRequestObject(http:FilterContext context) returns APIRequestMetaDataDto {
    APIRequestMetaDataDto apiKeyValidationRequest = {};
    http:HttpServiceConfig? httpServiceConfig = getServiceConfigAnnotation(serviceAnnotationMap[getServiceName(context.serviceName)] ?: []);
    http:HttpResourceConfig? httpResourceConfig = getResourceConfigAnnotation(resourceAnnotationMap[context.resourceName] ?: []);
    string apiContext = <string>httpServiceConfig.basePath;
    APIConfiguration? apiConfig = apiConfigAnnotationMap[getServiceName(context.serviceName)];
    string apiVersion = <string>apiConfig.apiVersion;
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
    apiKeyValidationRequest.matchingResource = <string>httpResourceConfig.path;
    apiKeyValidationRequest.httpVerb = <string>httpResourceConfig.methods[0];
    apiKeyValidationRequest.accessToken = <string>runtime:getInvocationContext().attributes[ACCESS_TOKEN_ATTR];
    printDebug(KEY_UTILS, "Created request meta-data object with context: " + apiContext
            + ", resource: " + apiKeyValidationRequest.matchingResource
            + ", verb: " + apiKeyValidationRequest.httpVerb);
    return apiKeyValidationRequest;

}

# Retrieve the correct service name from service name that contains object reference(for ex; MyService$$service$0).
# This method is a work around due to ballerina filter context returns wrong service name
#
# + return - service name
public function getServiceName(string serviceObjectName) returns string {
    return serviceObjectName.split("\\$")[0];
}


public function getAPIDetailsFromServiceAnnotation(reflect:annotationData[] annData) returns APIConfiguration? {
    if (annData.length() == 0) {
        return ();
    }
    reflect:annotationData|() apiAnn = ();
    foreach var ann in annData {
        if (ann.name == API_ANN_NAME && ann.moduleName == GATEWAY_ANN_PACKAGE) {
            apiAnn = ann;
            break;
        }
    }
    if(apiAnn is reflect:annotationData) {
        APIConfiguration apiConfig =  <APIConfiguration>apiAnn.value;
        return apiConfig;
    } else {
        return ();
    }
}

public function getTenantFromBasePath(string basePath) returns string {
    string[] splittedArray = basePath.split("/");
    return splittedArray[splittedArray.length() - 1];
}


public function isAccessTokenExpired(APIKeyValidationDto apiKeyValidationDto) returns boolean {
    int|error validityPeriod =  int.convert(apiKeyValidationDto.validityPeriod);
    int|error issuedTime = int.convert(apiKeyValidationDto.issuedTime);
    int timestampSkew = getConfigIntValue(KM_CONF_INSTANCE_ID, TIMESTAMP_SKEW, 5000);
    int currentTime = time:currentTime().time;
    int intMaxValue = 9223372036854775807;
    if (!(validityPeriod is int) || !(issuedTime is int)) {
        error e = error("Error while converting time stamps to integer when retrieved from cache");
        panic e;
    }
    if(validityPeriod is int && issuedTime is int) {
        if( validityPeriod != intMaxValue &&
                // For cases where validityPeriod is closer to int.MAX_VALUE (then issuedTime + validityPeriod would spill
                // over and would produce a negative value)
                (currentTime - timestampSkew) > validityPeriod) {
            if ((currentTime - timestampSkew) > (issuedTime + validityPeriod)) {
                apiKeyValidationDto.validationStatus = API_AUTH_INVALID_CREDENTIALS_STRING;
                return true;
            }
        }
    }
    return false;
}
public function getContext(http:FilterContext context) returns (string) {
    http:HttpServiceConfig? httpServiceConfig = getServiceConfigAnnotation(serviceAnnotationMap[getServiceName(context.serviceName)] ?: []);
    return <string>httpServiceConfig.basePath;

}

public function getClientIp(http:Request request, http:Caller caller) returns (string) {
    string clientIp;
    if (request.hasHeader(X_FORWARD_FOR_HEADER)) {
        clientIp = request.getHeader(X_FORWARD_FOR_HEADER);
        int idx = clientIp.indexOf(",");
        if (idx > -1) {
            clientIp = clientIp.substring(0, idx);
        }
    } else {
        clientIp = caller.remoteAddress.host;
    }
    return clientIp;
}

public function extractAccessToken(http:Request req, string authHeaderName) returns (string|error) {
    string authHeader = req.getHeader(authHeaderName);
    string[] authHeaderComponents = authHeader.split(" ");
    if (authHeaderComponents.length() != 2){
        return handleError("Incorrect bearer authentication header format");
    }
    return authHeaderComponents[1];
}

public function handleError(string message) returns (error) {
    return error(message);
}
public function getTenantDomain(http:FilterContext context) returns (string) {
    // todo: need to implement to get tenantDomain
    string apiContext = getContext(context);
    string[] splittedContext = apiContext.split("/");
    if (splittedContext.length() > 3){
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

public function getConfigMapValue(string property) returns map<any> {
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

# Default error response sender with json error response
public function sendErrorResponse(http:Caller caller, http:Request request, http:FilterContext context) {
    string errorDescription = <string>context.attributes[ERROR_DESCRIPTION];
    string errorMesssage = <string>context.attributes[ERROR_MESSAGE];
    int errorCode = <int>context.attributes[ERROR_CODE];
    http:Response response = new;
    response.statusCode = <int>context.attributes[HTTP_STATUS_CODE];
    response.setContentType(APPLICATION_JSON);
    json payload = { fault: {
        code: errorCode,
        message: errorMesssage,
        description: errorDescription
    } };
    response.setJsonPayload(payload);
    var value = caller->respond(response);
    if(value is error) {
    log:printError("Error occurred while sending the error response", err = value);
    }
}

public function getAuthorizationHeader(reflect:annotationData[] annData) returns string {
    APIConfiguration? apiConfig = getAPIDetailsFromServiceAnnotation(annData);
    string authHeader = "";
    string? annotatedHeadeName = apiConfig["authorizationHeader"];
    if(annotatedHeadeName is string) {
        authHeader = annotatedHeadeName;
    }
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
    var compressResult = internal:compress(fileToZip, zipLocation);
    if(compressResult is error) {
        printFullError(KEY_UTILS, compressResult);
        return compressResult;
    } else {
        printInfo(KEY_UTILS, "File compressed successfully");
        var deleteResult = fileToZip.delete();
            if(deleteResult is ()) {
                printInfo(KEY_UTILS, "Existing file deleted successfully");
            }
            else {
                printFullError(KEY_UTILS, deleteResult);
            }
        return zipName;
    }
}

# Retrieve external configurations defined against a key
#
# + return - Returns the confif value as a string
public function retrieveConfig(string key, string default) returns string {
    return config:getAsString(key, default = default);
}

# mask all letters with given text except last 4 charactors.
#
# + return - Returns the masked string value
public function mask(string text) returns string {
    if (text.length() > 4) {
        string last = text.substring(text.length() - 4, text.length());
        string first = text.substring(0, text.length() - 4).replaceAll(".", "x");
        return first + last;
    } else {
        return "xxxx";
    }
}

# Returns the current message ID (uuid).
#
# + return - The UUID of current context
public function getMessageId() returns string {
    any messageId = runtime:getInvocationContext().attributes[MESSAGE_ID];
    if(messageId is string) {
        if (messageId == "") {
            return "-";
        } else {
            return messageId;
        }
    } else {
        return "-";
    }
}

# Add a error log with provided key (class) and message ID
public function printError(string key, string message) {
    log:printError(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

# Add a debug log with provided key (class) and message ID
public function printDebug(string key, string message) {
    log:printDebug(function() returns string {
            return io:sprintf("[%s] [%s] %s",  key, getMessageId(), message); });
}

# Add a trace log with provided key (class) and message ID
public function printTrace(string key, string message) {
    log:printTrace(function() returns string {
            return io:sprintf("[%s] [%s] %s",  key, getMessageId(), message); });
}

# Add a info log with provided key (class) and message ID
public function printInfo(string key, string message) {
    log:printInfo(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

# Add a full error log with provided key (class) and message ID
public function printFullError(string key, error message) {
    log:printError(io:sprintf("[%s] [%s] %s", key, getMessageId(), message.reason()), err = message);
}

public function setLatency(int starting, http:FilterContext context, string latencyType) {
    int ending = getCurrentTime();
    context.attributes[latencyType] = ending - starting;
    printDebug(KEY_THROTTLE_FILTER, "Throttling latency: " + (ending - starting) + "ms");
}

# Check MESSAGE_ID in context and set if it is not
public function checkOrSetMessageID(http:FilterContext context) {
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

# Encode a given value to base64 format
#
# + return - Returns a string in base64 format
public function encodeValueToBase64(string value) returns string {
    return encoding:encodeBase64(value.toByteArray("UTF-8"));
}

# Decode a given base64value to base10 format
#
# + return - Returns a string in base10 format
public function decodeValueToBase10(string value) returns string {
    string decodedValue = "";
    var result = encoding:decodeBase64(value);
    if(result is byte[]) {
        decodedValue = encoding:byteArrayToString(result);
    }
    else {
        printError(KEY_UTILS, result.reason());
    }
    return decodedValue;
}
