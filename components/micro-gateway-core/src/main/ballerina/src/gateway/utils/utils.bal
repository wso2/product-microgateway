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

import ballerina/auth;
import ballerina/config;
import ballerina/file;
import ballerina/http;
import ballerina/io;
import ballerina/lang.'int;
import ballerina/lang.'array as arrays;
import ballerina/lang.'string as strings;
import ballerina/log;
import ballerina/reflect;
import ballerina/runtime;
import ballerina/stringutils;
import ballerina/system;
import ballerina/time;

map<http:HttpResourceConfig?> resourceAnnotationMap = {};
map<http:HttpServiceConfig?> serviceAnnotationMap = {};
map<TierConfiguration?> resourceTierAnnotationMap = {};
map<APIConfiguration?> apiConfigAnnotationMap = {};
map<ResourceConfiguration?> resourceConfigAnnotationMap = {};
map<FilterConfiguration?> filterConfigAnnotationMap = {};

public function populateAnnotationMaps(string serviceName, service s, string[] resourceArray) {
    foreach string resourceFunction in resourceArray {
        resourceAnnotationMap[resourceFunction] = <http:HttpResourceConfig?>reflect:getResourceAnnotations(s, resourceFunction, RESOURCE_ANN_NAME, ANN_PACKAGE);
        resourceTierAnnotationMap[resourceFunction] = <TierConfiguration?>reflect:getResourceAnnotations(s, resourceFunction, RESOURCE_TIER_ANN_NAME, GATEWAY_ANN_PACKAGE);
        resourceConfigAnnotationMap[resourceFunction] = <ResourceConfiguration?>reflect:getResourceAnnotations(s, resourceFunction, RESOURCE_CONFIGURATION_ANN_NAME, GATEWAY_ANN_PACKAGE);
    }
    serviceAnnotationMap[serviceName] = <http:HttpServiceConfig?>reflect:getServiceAnnotations(s, SERVICE_ANN_NAME, ANN_PACKAGE);
    apiConfigAnnotationMap[serviceName] = <APIConfiguration?>reflect:getServiceAnnotations(s, API_ANN_NAME, GATEWAY_ANN_PACKAGE);
    filterConfigAnnotationMap[serviceName] = <FilterConfiguration?>reflect:getServiceAnnotations(s, FILTER_ANN_NAME, GATEWAY_ANN_PACKAGE);
    printDebug(KEY_UTILS, "Service annotation map: " + serviceAnnotationMap.toString());
    printDebug(KEY_UTILS, "Resource annotation map: " + resourceAnnotationMap.toString());
    printDebug(KEY_UTILS, "API config annotation map: " + apiConfigAnnotationMap.toString());
    printDebug(KEY_UTILS, "Resource tier annotation map: " + resourceTierAnnotationMap.toString());
    printDebug(KEY_UTILS, "Resource Configuration annotation map: " + resourceConfigAnnotationMap.toString());
    printDebug(KEY_UTILS, "Filter Configuration annotation map: " + filterConfigAnnotationMap.toString());
}

# Retrieve the key validation request dto from filter context.
# + context - invocation context.
# + accessToken - access token sent in the authorization header.
# + return - api key validation request dto.
public function getKeyValidationRequestObject(runtime:InvocationContext context, string accessToken) returns APIRequestMetaDataDto {
    APIRequestMetaDataDto apiKeyValidationRequest = {};
    string serviceName = runtime:getInvocationContext().attributes[http:SERVICE_NAME].toString();
    string resourceName = runtime:getInvocationContext().attributes[http:RESOURCE_NAME].toString();
    printDebug(KEY_UTILS, "Service Name : " + serviceName);
    printDebug(KEY_UTILS, "Resource Name : " + resourceName);
    http:HttpServiceConfig httpServiceConfig = <http:HttpServiceConfig>serviceAnnotationMap[serviceName];
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
    io:println(httpServiceConfig);
    if (httpResourceConfig is http:HttpResourceConfig) {
        apiKeyValidationRequest.matchingResource = <string>httpResourceConfig.path;
        apiKeyValidationRequest.httpVerb = <string>httpResourceConfig.methods[0];
    }
    string apiContext = <string>httpServiceConfig.basePath;
    APIConfiguration? apiConfig = apiConfigAnnotationMap[serviceName];
    string apiVersion = "";
    if (apiConfig is APIConfiguration) {
        apiVersion = <string>apiConfig.apiVersion;
    }
    apiKeyValidationRequest.apiVersion = apiVersion;
    if (!contains(apiContext, apiVersion)) {
        if (hasSuffix(apiContext, PATH_SEPERATOR)) {
            apiContext = apiContext + apiVersion;
        } else {
            apiContext = apiContext + PATH_SEPERATOR + apiVersion;
        }
    }
    apiKeyValidationRequest.context = apiContext;
    apiKeyValidationRequest.requiredAuthenticationLevel = ANY_AUTHENTICATION_LEVEL;
    apiKeyValidationRequest.clientDomain = "*";

    apiKeyValidationRequest.accessToken = accessToken;
    printDebug(KEY_UTILS, "Created request meta-data object with context: " + apiContext
    + ", resource: " + apiKeyValidationRequest.matchingResource
    + ", verb: " + apiKeyValidationRequest.httpVerb);
    return apiKeyValidationRequest;

}


public function getTenantFromBasePath(string basePath) returns string {
    string[] splittedArray = split(basePath, "/");
    return splittedArray[splittedArray.length() - 1];
}

public function isAccessTokenExpired(APIKeyValidationDto apiKeyValidationDto) returns boolean {
    int | error validityPeriod = 0;
    int | error issuedTime = 0;
    string? validPeriod = apiKeyValidationDto?.validityPeriod;
    string? issueTime = apiKeyValidationDto?.issuedTime;
    if (validPeriod is string) {
        validityPeriod = 'int:fromString(validPeriod);
    }
    if (issueTime is string) {
        issuedTime = 'int:fromString(issueTime);
    }
    int timestampSkew = getConfigIntValue(KM_CONF_INSTANCE_ID, TIMESTAMP_SKEW, 5000);
    int currentTime = time:currentTime().time;
    int intMaxValue = 9223372036854775807;
    if (!(validityPeriod is int) || !(issuedTime is int)) {
        error e = error("Error while converting time stamps to integer when retrieved from cache");
        panic e;
    }
    if (validityPeriod is int && issuedTime is int) {
        if (validityPeriod != intMaxValue &&
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
    http:HttpServiceConfig httpServiceConfig = <http:HttpServiceConfig>serviceAnnotationMap[context.getServiceName()];
    return <string>httpServiceConfig.basePath;

}

public function getClientIp(http:Request request, http:Caller caller) returns (string) {
    string clientIp;
    if (request.hasHeader(X_FORWARD_FOR_HEADER)) {
        clientIp = request.getHeader(X_FORWARD_FOR_HEADER);
        int? idx = clientIp.indexOf(",");
        if (idx is int) {
            clientIp = clientIp.substring(0, idx);
        }
    } else {
        clientIp = caller.remoteAddress.host;
    }
    return clientIp;
}

public function extractAccessToken(http:Request req, string authHeaderName) returns (string | error) {
    string authHeader = req.getHeader(authHeaderName);
    string[] authHeaderComponents = split(authHeader, " ");
    if (authHeaderComponents.length() != 2) {
        return handleError("Incorrect bearer authentication header format");
    }
    return authHeaderComponents[1];
}

public function handleError(string message) returns ( error) {
    return error(message);
}

public function getTenantDomain(http:FilterContext context) returns (string) {
    // todo: need to implement to get tenantDomain
    string apiContext = getContext(context);
    string[] splittedContext = split(apiContext, "/");
    if (splittedContext.length() > 3) {
        // this check if basepath have /t/domain in
        return splittedContext[2];
    } else {
        return SUPER_TENANT_DOMAIN_NAME;
    }
}

public function getApiName(http:FilterContext context) returns (string) {
    string serviceName = context.getServiceName();
    string apiName = split(serviceName, "__")[0];

    if (contains(apiName, "_")) {
        apiName = replaceAll(apiName, "_", "-");
    }

    return apiName;
}

public function getConfigValue(string instanceId, string property, string defaultValue) returns string {
    if (stringutils:equalsIgnoreCase("", instanceId)) {
        return config:getAsString(property, defaultValue);
    }
    return config:getAsString(instanceId + "." + property, defaultValue);
}

public function getConfigIntValue(string instanceId, string property, int defaultValue) returns int {
    return config:getAsInt(instanceId + "." + property, defaultValue);
}

public function getConfigBooleanValue(string instanceId, string property, boolean defaultValue) returns boolean {
    return config:getAsBoolean(instanceId + "." + property, defaultValue);
}

public function getConfigFloatValue(string instanceId, string property, float defaultValue) returns float {
    return config:getAsFloat(instanceId + "." + property, defaultValue);
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
    } else if (errorCode == INVALID_ENTITY) {
        status = UNPROCESSABLE_ENTITY;
    } else if (errorCode == INVALID_RESPONSE) {
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

public function setErrorMessageToInvocationContext(int errorCode) {
    runtime:InvocationContext context = runtime:getInvocationContext();
    int status;
    if (errorCode == API_AUTH_GENERAL_ERROR) {
        status = INTERNAL_SERVER_ERROR;
    } else if (errorCode == API_AUTH_INCORRECT_API_RESOURCE || errorCode == API_AUTH_FORBIDDEN ||
    errorCode == INVALID_SCOPE) {
        status = FORBIDDEN;
    } else if (errorCode == INVALID_ENTITY) {
        status = UNPROCESSABLE_ENTITY;
    } else if (errorCode == INVALID_RESPONSE) {
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

# Default error response sender with json error response.
# + caller - http caller object.
# + request - http request object.
# + context - filter context object.
public function sendErrorResponse(http:Caller caller, http:Request request, http:FilterContext context) {
    string errorDescription = <string>context.attributes[ERROR_DESCRIPTION];
    string errorMesssage = <string>context.attributes[ERROR_MESSAGE];
    int errorCode = <int>context.attributes[ERROR_CODE];
    http:Response response = new;
    response.statusCode = <int>context.attributes[HTTP_STATUS_CODE];
    response.setContentType(APPLICATION_JSON);
    json payload = {
        fault: {
            code: errorCode,
            message: errorMesssage,
            description: errorDescription
        }
    };
    response.setJsonPayload(payload);
    var value = caller->respond(response);
    if (value is error) {
        log:printError("Error occurred while sending the error response", err = value);
    }
}

# Default error response sender with json error response.
# + response - http response object.
public function sendErrorResponseFromInvocationContext(http:Response response) {
    runtime:InvocationContext context = runtime:getInvocationContext();
    string errorDescription = <string>context.attributes[ERROR_DESCRIPTION];
    string errorMessage = <string>context.attributes[ERROR_MESSAGE];
    int errorCode = <int>context.attributes[ERROR_CODE];
    response.statusCode = <int>context.attributes[HTTP_STATUS_CODE];
    response.setContentType(APPLICATION_JSON);
    json payload = {
        fault: {
            code: errorCode,
            message: errorMessage,
            description: errorDescription
        }
    };
    response.setJsonPayload(payload);
}

public function getAuthorizationHeader(runtime:InvocationContext context) returns string {
    string serviceName = context.attributes[http:SERVICE_NAME].toString();
    APIConfiguration? apiConfig = apiConfigAnnotationMap[serviceName];
    string authHeader = "";
    string? annotatedHeadeName = apiConfig["authorizationHeader"];
    if (annotatedHeadeName is string) {
        authHeader = annotatedHeadeName;
    }
    if (authHeader == "") {
        authHeader = getConfigValue(AUTH_CONF_INSTANCE_ID, AUTH_HEADER_NAME, AUTHORIZATION_HEADER);
    }
    return authHeader;

}

public function getAuthHeaderFromFilterContext(http:FilterContext context) returns string {
    string serviceName = context.getServiceName();
    APIConfiguration? apiConfig = apiConfigAnnotationMap[serviceName];
    string authHeader = "";
    string? annotatedHeadeName = apiConfig["authorizationHeader"];
    if (annotatedHeadeName is string) {
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

public function rotateFile(string filePath) returns string | error {
    string uuid = system:uuid();
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR) + PATH_SEPERATOR;
    int rotatingTimeStamp = getCurrentTime();
    string zipName = fileLocation + API_USAGE_FILE + "." + rotatingTimeStamp.toString() + "." + uuid + ZIP_EXTENSION;
    var compressResult = compress(filePath, zipName);
    if (compressResult is error) {
        printFullError(KEY_UTILS, compressResult);
        return compressResult;
    } else {
        printInfo(KEY_UTILS, "File compressed successfully");
        var deleteResult = file:remove(filePath);
        if (deleteResult is ()) {
            printInfo(KEY_UTILS, "Existing file deleted successfully");
        } else {
            printFullError(KEY_UTILS, deleteResult);
        }
        return zipName;
    }
}

# Retrieve external configurations defined against a key.
# + key - The key of the configurations to be read.
# + defaultConfig - Default value of the configuration.
# + return - Returns the confif value as a string.
public function retrieveConfig(string key, string defaultConfig) returns string {
    return config:getAsString(key, defaultConfig);
}

# mask all letters with given text except last 4 charactors.
# + text - The string to be masked.
# + return - Returns the masked string value.
public function mask(string text) returns string {
    if (text.length() > 4) {
        string last = text.substring(text.length() - 4, text.length());
        string first = replaceAll(text.substring(0, text.length() - 4), ".", "x");
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
    if (messageId is string) {
        if (messageId == "") {
            return "-";
        } else {
            return messageId;
        }
    } else {
        return "-";
    }
}

# Add a error log with provided key (class) and message ID.
# + key - The name of the bal file from which the log is printed.
# + message - The message to be logged.
public function printError(string key, string message) {
    log:printError(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

# Add a debug log with provided key (class) and message ID.
# + key - The name of the bal file from which the log is printed.
# + message - The message to be logged.
public function printDebug(string key, string message) {
    log:printDebug(function() returns string {
        return io:sprintf("[%s] [%s] %s", key, getMessageId(), message);
    });
}

# Add a warn log with provided key (class) and message ID.
# + key - The name of the bal file from which the log is printed.
# + message - The message to be logged.
public function printWarn(string key, string message) {
    log:printWarn(function() returns string {
        return io:sprintf("[%s] [%s] %s", key, getMessageId(), message);
    });
}

# Add a trace log with provided key (class) and message ID.
# + key - The name of the bal file from which the log is printed.
# + message - The message to be logged.
public function printTrace(string key, string message) {
    log:printTrace(function() returns string {
        return io:sprintf("[%s] [%s] %s", key, getMessageId(), message);
    });
}

# Add a info log with provided key (class) and message ID.
# + key - The name of the bal file from which the log is printed.
# + message - The message to be logged.
public function printInfo(string key, string message) {
    log:printInfo(io:sprintf("[%s] [%s] %s", key, getMessageId(), message));
}

# Add a full error log with provided key (class) and message ID.
# + key - The name of the bal file from which the log is printed.
# + message - The message to be logged.
public function printFullError(string key, error message) {
    log:printError(io:sprintf("[%s] [%s] %s", key, getMessageId(), message.reason()), err = message);
}

public function setLatency(int starting, http:FilterContext context, string latencyType) {
    int ending = getCurrentTime();
    context.attributes[latencyType] = ending - starting;
    int latency = ending - starting;
    printDebug(KEY_THROTTLE_FILTER, "Throttling latency: " + latency.toString() + "ms");
}

# Check MESSAGE_ID in context and set if it is not.
# + context - http filter context object.
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
# + value - String value to be encoded.
# + return - Returns a string in base64 format.
public function encodeValueToBase64(string value) returns string {
    return value.toBytes().toBase64();
}

# Decode a given base64value to base10 format
# + value - String value to be decoded.
# + return - Returns a string in base10 format
public function decodeValueToBase10(string value) returns string {
    string decodedValue = "";
    var result = arrays:fromBase64(value);
    if (result is byte[]) {
        string | error decodedValueFromBytes = strings:fromBytes(result);
        if (decodedValueFromBytes is string) {
            decodedValue = decodedValueFromBytes;
        } else {
            printError(KEY_UTILS, decodedValueFromBytes.reason());
        }
    }
    else {
        printError(KEY_UTILS, result.reason());
    }
    return decodedValue;
}

# Extracts host header from request and set it to the filter context
# + request - http request object.
# + context - http filter context object.
public function setHostHeaderToFilterContext(http:Request request,@tainted http:FilterContext context) {
    if (context.attributes[HOSTNAME_PROPERTY] == ()) {
        printDebug(KEY_AUTHN_FILTER, "Setting hostname to filter context");
        if (request.hasHeader(HOST_HEADER_NAME)) {
            context.attributes[HOSTNAME_PROPERTY] = request.getHeader(HOST_HEADER_NAME);

        } else {
            context.attributes[HOSTNAME_PROPERTY] = "localhost";
        }
        printDebug(KEY_UTILS, "Hostname attribute of the filter context is set to : " +
        <string>context.attributes[HOSTNAME_PROPERTY]);
    } else {
        printDebug(KEY_UTILS, "Hostname attribute of the filter context is already set to : " +
        <string>context.attributes[HOSTNAME_PROPERTY]);
    }
}

public function isSecured(string serviceName, string resourceName) returns boolean {
    http:ResourceAuth? resourceLevelAuthAnn = ();
    http:ResourceAuth? serviceLevelAuthAnn = ();
    http:HttpServiceConfig httpServiceConfig = <http:HttpServiceConfig>serviceAnnotationMap[serviceName];
    http:HttpResourceConfig? httpResourceConfig = <http:HttpResourceConfig?>resourceAnnotationMap[resourceName];
    if (httpResourceConfig is http:HttpResourceConfig) {
        resourceLevelAuthAnn = httpResourceConfig?.auth;
        boolean resourceSecured = isServiceResourceSecured(resourceLevelAuthAnn);
        // if resource is not secured, no need to check further
        if (!resourceSecured) {
            log:printWarn("Resource is not secured. `enabled: false`.");
            return false;
        }
    }
    serviceLevelAuthAnn = httpResourceConfig?.auth;
    boolean serviceSecured = isServiceResourceSecured(serviceLevelAuthAnn);
    if (!serviceSecured) {
        log:printWarn("Service is not secured. `enabled: false`.");
        return true;
    }
    return true;
}

# Check for the service or the resource is secured by evaluating the enabled flag configured by the user.
#
# + resourceAuth - Service or resource auth annotation
# + return - Whether the service or resource secured or not
function isServiceResourceSecured(http:ResourceAuth? resourceAuth) returns boolean {
    boolean secured = true;
    if (resourceAuth is http:ResourceAuth) {
        secured = resourceAuth["enabled"] ?: true;
    }
    return secured;
}

public function getAuthProviders(string serviceName, string resourceName) returns string[] {
    printDebug(KEY_UTILS, "Service name provided to retrieve auth configuration  : " + serviceName);
    string[] authProviders = [];
    ResourceConfiguration? resourceConfig = resourceConfigAnnotationMap[resourceName];
    if (resourceConfig is ResourceConfiguration) {
        authProviders = resourceConfig.authProviders;
        if (authProviders.length() > 0) {
            return authProviders;
        }
    }
    APIConfiguration? apiConfig = apiConfigAnnotationMap[serviceName];
    if (apiConfig is APIConfiguration) {
        authProviders = apiConfig.authProviders;
    }
    return authProviders;
}

# Log and prepare `error` as a `Error`.
#
# + message - Error message
# + err - `error` instance
# + return - Prepared `Error` instance
public function prepareError(string message, error? err = ()) returns auth:Error {
    log:printError(message, err);
    auth:Error authError;
    if (err is error) {
        authError = error(auth:AUTH_ERROR, message = message, cause = err);
    } else {
        authError = error(auth:AUTH_ERROR, message = message);
    }
    return authError;
}

# Logs, prepares, and returns the `AuthenticationError`.
#
# + message -The error message.
# + err - The `error` instance.
# + return - Returns the prepared `AuthenticationError` instance.
function prepareAuthenticationError(string message, error? err = ()) returns http:AuthenticationError {
    log:printDebug(function () returns string {
        return message;
    });
    if (err is error) {
        http:AuthenticationError preparedError = error(http:AUTHN_FAILED, message = message, cause = err);
        return preparedError;
    }
    http:AuthenticationError preparedError = error(http:AUTHN_FAILED, message = message);
    return preparedError;
}

# Read the filter skip annotation from service and set to the filter context
#
# + context - Filter Context object.
public function setFilterSkipToFilterContext(http:FilterContext context) {
    if (context.attributes.hasKey(SKIP_ALL_FILTERS)) {
        return;
    }
    string serviceName = context.getServiceName();
    boolean skipFilter = false;
    FilterConfiguration? filterConfigAnn = filterConfigAnnotationMap[serviceName];
    if (filterConfigAnn is FilterConfiguration) {
        skipFilter = filterConfigAnn.skipAll;
    }
    context.attributes[SKIP_ALL_FILTERS] = skipFilter;
}

public function getFilterConfigAnnotationMap() returns map<FilterConfiguration?> {
    return filterConfigAnnotationMap;
}


