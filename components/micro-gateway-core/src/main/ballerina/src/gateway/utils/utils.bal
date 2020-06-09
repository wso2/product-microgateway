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
import ballerina/jwt;
import ballerina/lang.'int;
import ballerina/lang.'array as arrays;
import ballerina/lang.'string as strings;
import ballerina/log;
import ballerina/oauth2;
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
map<http:InboundAuthHandler> authHandlersMap = {}; //all handlers except for jwt handlers
http:InboundAuthHandler[] jwtHandlers = [];//all jwt issuer handlers

// values read from configuration
string authHeaderFromConfig = getConfigValue(AUTH_CONF_INSTANCE_ID, AUTH_HEADER_NAME, DEFAULT_AUTH_HEADER_NAME);
string jwtheaderName = getConfigValue(JWT_CONFIG_INSTANCE_ID, JWT_HEADER, DEFAULT_JWT_HEADER_NAME);
map<anydata>[] | error apiCertificateList = map<anydata>[].constructFrom(config:getAsArray(MUTUAL_SSL_API_CERTIFICATE));
string trustStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH);
string trustStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD);
string headerName = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_CERT_HEADER_NAME, "");
boolean isClientCertificateValidationEnabled = getConfigBooleanValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_IS_CLIENT_CER_VALIDATION_ENABLED, true);

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
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    string serviceName = invocationContext.attributes[http:SERVICE_NAME].toString();
    string resourceName = invocationContext.attributes[http:RESOURCE_NAME].toString();
    printDebug(KEY_UTILS, "Service Name : " + serviceName);
    printDebug(KEY_UTILS, "Resource Name : " + resourceName);
    http:HttpServiceConfig httpServiceConfig = <http:HttpServiceConfig>serviceAnnotationMap[serviceName];
    http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
    if (httpResourceConfig is http:HttpResourceConfig) {
        apiKeyValidationRequest.matchingResource = <string>httpResourceConfig.path;
        apiKeyValidationRequest.httpVerb = <string>httpResourceConfig.methods[0];
        // we are explicitly setting the scopes to principal, because scope validation happens at key validation
        //service. Doing scope validation again in ballerina authz filter causes authorization failure.
        //So we trust the scope validation done by key validation service and forcefully make the ballerina
        //authz scope validation successful.
        setResourceScopesToPrincipal(httpResourceConfig, invocationContext, accessToken);
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

    // provide backward compatibility for skew time
    int timestampSkew = getConfigIntValue(SERVER_CONF_ID, SERVER_TIMESTAMP_SKEW, DEFAULT_SERVER_TIMESTAMP_SKEW);
    if (timestampSkew == DEFAULT_SERVER_TIMESTAMP_SKEW) {
        timestampSkew = getConfigIntValue(KM_CONF_INSTANCE_ID, TIMESTAMP_SKEW, DEFAULT_TIMESTAMP_SKEW);
    }
   
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
        if (clientIp == "") {
            clientIp = caller.localAddress.host;
        }
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
    if (splittedContext.length() > 3 && apiContext.startsWith(TENANT_DOMAIN_PREFIX)) {
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

public function getConfigArrayValue(string instanceId, string property) returns any[] {
    return config:getAsArray(instanceId + "." + property);
}

function getDefaultStringValue(anydata val, string defaultVal) returns string {
    if (val is string) {
        return <string>val;
    }
    return defaultVal;
}

function getDefaultBooleanValue(anydata val, boolean defaultVal) returns boolean {
    if (val is boolean) {
        return <boolean>val;
    }
    return defaultVal;
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
# + filterName - filter from which the method is invoked (for logging purposes).
public function sendErrorResponse(http:Caller caller, http:Request request, http:FilterContext context, string filterName) {
    string errorDescription = <string>context.attributes[ERROR_DESCRIPTION];
    string errorMessage = <string>context.attributes[ERROR_MESSAGE];
    int errorCode = <int>context.attributes[ERROR_CODE];
    http:Response response = new;
    response.statusCode = <int>context.attributes[HTTP_STATUS_CODE];
    response.setContentType(APPLICATION_JSON);
    if (!isGrpcRequest(context)) {
        json payload = {
            fault: {
                code: errorCode,
                message: errorMessage,
                description: errorDescription
            }
        };
        response.setJsonPayload(payload);
    } else {
        attachGrpcErrorHeaders (response, errorMessage);
    }
    var value = caller->respond(response);
    if (value is error) {
        printError(filterName, "Error occurred while sending the error response", value);
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
    if (! context.attributes.hasKey(IS_GRPC)) {
        json payload = {
            fault: {
                code: errorCode,
                message: errorMessage,
                description: errorDescription
            }
        };
        response.setJsonPayload(payload);
    } else {
        attachGrpcErrorHeaders (response, errorMessage);
    }
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
        authHeader = authHeaderFromConfig;
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
        authHeader = authHeaderFromConfig;
    }
    return authHeader;
}


function checkAndRemoveAuthHeaders(http:Request request, string authHeaderName) {
    if (getConfigBooleanValue(AUTH_CONF_INSTANCE_ID, REMOVE_AUTH_HEADER_FROM_OUT_MESSAGE, DEFAULT_REMOVE_AUTH_HEADER_FROM_OUT_MESSAGE)) {
        request.removeHeader(authHeaderName);
        printDebug(KEY_PRE_AUTHN_FILTER, "Removed header : " + authHeaderName + " from the request");
    }
}

public function getCurrentTime() returns int {

    time:Time currentTime = time:currentTime();
    int time = currentTime.time;
    return time;

}

public function getCurrentTimeForAnalytics() returns int {
    if (!isAnalyticsEnabled && !isGrpcAnalyticsEnabled) {
        return 0;
    }
    return getCurrentTime();
}

public function rotateFile(string filePath) returns string | error {
    string uuid = system:uuid();
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR) + PATH_SEPERATOR;
    int filePathLength = filePath.length();
    //to remove the .tmp extension from the filePath ('api-usage-data.dat.tmp'). This is the filename required by the
    //analytics node "api-usage-data.dat". It is required to rename the file before compressing in order to avoid
    //the data loss.
    string eventFilePath = filePath.substring(0, filePathLength - 4) ;
    int rotatingTimeStamp = getCurrentTime();
    var renameFileResult = file:rename(filePath, eventFilePath);
    if (renameFileResult is error) {
        printError(KEY_UTILS, "Failed to rename file", renameFileResult);
    }
    //Until the compression happens, the file will have the name <fileNameWithoutExtension> with '.tmp' extension.
    //After the compression is completed successfully, the file will be renamed to <zipName>. Only the files
    //with .zip extension will be uploaded. Hence the partially compressed files will not be uploaded to the analytics
    //node.
    string fileNameWithoutExtension = fileLocation + API_USAGE_FILE + "." + rotatingTimeStamp.toString() + "." +
        uuid;
    string tempZipName = fileNameWithoutExtension + TMP_EXTENSION;
    string zipName = fileNameWithoutExtension + ZIP_EXTENSION;
    var compressResult = compress(eventFilePath, tempZipName);
    if (compressResult is error) {
        printError(KEY_UTILS, "Failed to compress the file", compressResult);
        return compressResult;
    } else {
        printInfo(KEY_UTILS, "File compressed successfully");
        var renameZipResult = file:rename(tempZipName, zipName);
        var deleteFileResult = file:remove(eventFilePath);
        if (renameZipResult is error) {
            printError(KEY_UTILS, "Failed to rename file", renameZipResult);
        }
        if (deleteFileResult is ()) {
            printInfo(KEY_UTILS, "Existing file deleted successfully");
        } else {
            printError(KEY_UTILS, "Failed to delete file", deleteFileResult);
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
# + errorMessage - The error message to be logged.
public function printError(string key, string message, error? errorMessage = ()) {
    log:printError(io:sprintf("[%s] [%s] %s", key, getMessageId(), message), err = errorMessage);
}

# Add a debug log with provided key (class) and message ID.
# + key - The name of the bal file from which the log is printed.
# + message - The message to be logged.
public function printDebug(string key, string message) {
    if(isDebugEnabled) {
        log:printDebug(function() returns string {
            return io:sprintf("[%s] [%s] %s", key, getMessageId(), message);
        });
    }
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

public function setLatency(int starting, http:FilterContext context, string latencyType) {
    if (!isAnalyticsEnabled && !isGrpcAnalyticsEnabled) {
            return;
    }
    int ending = getCurrentTime();
    context.attributes[latencyType] = ending - starting;
    int latency = ending - starting;
    printDebug(KEY_THROTTLE_FILTER, latencyType + " latency: " + latency.toString() + "ms");
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
    if (!isAnalyticsEnabled && !isGrpcAnalyticsEnabled) {
        return;
    }
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
    setRequestDataToInvocationContext(httpServiceConfig, httpResourceConfig);
    if (httpResourceConfig is http:HttpResourceConfig) {
        resourceLevelAuthAnn = httpResourceConfig?.auth;
        boolean resourceSecured = isServiceResourceSecured(resourceLevelAuthAnn);
        // if resource is not secured, no need to check further
        if (!resourceSecured) {
            printDebug(KEY_UTILS, "Resource is not secured. `enabled: false`.");
            return false;
        }
    }
    serviceLevelAuthAnn = httpResourceConfig?.auth;
    boolean serviceSecured = isServiceResourceSecured(serviceLevelAuthAnn);
    if (!serviceSecured) {
        printWarn(KEY_UTILS, "Service is not secured. `enabled: false`.");
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

public function getSecurityForService(string serviceName, string security) returns json | error {
    APIConfiguration? apiConfig = apiConfigAnnotationMap[serviceName];
    if (apiConfig is APIConfiguration) {
        map<json> securityMap = <map<json>>apiConfig.security;
        return securityMap[security];
    }
    printError(KEY_UTILS, "Error while reading the api configs");
    return error("Error while reading the api configs");
}

public function getAPIKeysforResource(string serviceName, string resourceName) returns json[] {
    printDebug(KEY_UTILS, "Service name provided to retrieve apikey configuration  : " + serviceName);
    json[] apiKeys = [];
    ResourceConfiguration? resourceConfig = resourceConfigAnnotationMap[resourceName];
    if (resourceConfig is ResourceConfiguration) {
        map<json> securityMap = <map<json>>resourceConfig.security;
        json apiKeysJson = securityMap[AUTH_SCHEME_API_KEY];
        apiKeys = <json[]>apiKeysJson;
        if (apiKeys.length() > 0) {
            return apiKeys;
        }
    }

    json | error securityAPIKey = getSecurityForService(serviceName, AUTH_SCHEME_API_KEY);
    if (securityAPIKey is json) {
        apiKeys = <json[]> securityAPIKey;
    }
    return apiKeys;
}

public function setMutualSSL(string serviceName) {
    string mutualSSL = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, DEFAULT_SSL_VERIFY_CLIENT);
    json | error securityMutualSSL = getSecurityForService(serviceName, MTSL);
    if (securityMutualSSL is json) {
        mutualSSL = <string> securityMutualSSL;
    }
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    invocationContext.attributes[MTSL] = mutualSSL;
}

public function getMutualSSL() returns string | error {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (invocationContext.attributes.hasKey(MTSL)) {
        return <string>invocationContext.attributes[MTSL];
    } else {
        printError(KEY_UTILS, "MutualSSL is missing in authentication context");
        return error("MutualSSL is missing in authentication context");
    }
}

public function isAppSecurityOptionalforResource(string serviceName, string resourceName) returns boolean {
    boolean appSecurityOptional = false;
    ResourceConfiguration? resourceConfig = resourceConfigAnnotationMap[resourceName];
    if (resourceConfig is ResourceConfiguration) {
        map<json> securityMap = <map<json>>resourceConfig.security;
        json appSecurityOptionalJson = securityMap[APP_SECURITY_OPTIONAL];
        appSecurityOptional = <boolean> appSecurityOptionalJson;
        return appSecurityOptional;
    }

    json | error securityOptional= getSecurityForService(serviceName, APP_SECURITY_OPTIONAL);
    if (securityOptional is json) {
        appSecurityOptional = <boolean> securityOptional;
    }
    return appSecurityOptional;
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
    printDebug(KEY_UTILS, message);
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

function isGrpcRequest(http:FilterContext context) returns boolean {
    //if the key is there, the value is always set to true.
    return context.attributes.hasKey(IS_GRPC);
}

public function initAuthHandlers() {
    //Initializes jwt handlers
    readMultipleJWTIssuers();
    int timestampSkew = getConfigIntValue(SERVER_CONF_ID, SERVER_TIMESTAMP_SKEW, DEFAULT_SERVER_TIMESTAMP_SKEW);
    if (timestampSkew == DEFAULT_SERVER_TIMESTAMP_SKEW) {
        timestampSkew = getConfigIntValue(KM_CONF_INSTANCE_ID, TIMESTAMP_SKEW, DEFAULT_TIMESTAMP_SKEW);
    }
    //Initializes apikey handler
    jwt:JwtValidatorConfig apiKeyValidatorConfig = {
        issuer: getConfigValue(API_KEY_INSTANCE_ID, ISSUER, DEFAULT_API_KEY_ISSUER),
        clockSkewInSeconds: timestampSkew/1000,
        trustStoreConfig: {
            trustStore: {
                path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
                password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
            },
            certificateAlias: getConfigValue(API_KEY_INSTANCE_ID, CERTIFICATE_ALIAS, DEFAULT_API_KEY_ALIAS)
        },
        jwtCache: jwtCache
    };
    APIKeyProvider apiKeyProvider = new (apiKeyValidatorConfig);
    APIKeyHandler | APIKeyHandlerWrapper apiKeyHandler;
    if (isMetricsEnabled || isTracingEnabled) {
        apiKeyHandler = new APIKeyHandlerWrapper(apiKeyProvider);
    } else {
        apiKeyHandler = new APIKeyHandler(apiKeyProvider);
    }

    // Initializes the key validation handler
    http:ClientSecureSocket secureSocket = {
        trustStore: {
            path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH),
            password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD)
        },
        verifyHostname: getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
    };

    http:OutboundAuthConfig? auth = ();
    // support backward compatibility in reading the basic auth credentials when connecting with KM.
    string username = getConfigValue(KM_CONF_INSTANCE_ID, USERNAME, "");
    string password = getConfigValue(KM_CONF_INSTANCE_ID, PASSWORD, "");
    if (username.length() == 0 && password.length() == 0) {
        username = getConfigValue(KM_CONF_SECURITY_BASIC_INSTANCE_ID, USERNAME, DEFAULT_USERNAME);
        password = getConfigValue(KM_CONF_SECURITY_BASIC_INSTANCE_ID, PASSWORD, DEFAULT_PASSWORD);
    }
    if (getConfigBooleanValue(KM_CONF_SECURITY_BASIC_INSTANCE_ID, ENABLED, true)) {
        auth:OutboundBasicAuthProvider basicAuthOutboundProvider = new ({
            username: username,
            password: password
        });
        http:BasicAuthHandler basicAuthOutboundHandler = new (basicAuthOutboundProvider);
        auth = {authHandler: basicAuthOutboundHandler};
    } else if (getConfigBooleanValue(KM_CONF_SECURITY_OAUTH2_INSTANCE_ID, ENABLED, DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED)) {
        oauth2:OutboundOAuth2Provider | error oauth2Provider = getOauth2OutboundProvider();
        if (oauth2Provider is oauth2:OutboundOAuth2Provider) {
            http:BearerAuthHandler bearerAuthOutboundHandler = new (oauth2Provider);
            auth = {authHandler: bearerAuthOutboundHandler};
        } else {
            printError(KEY_UTILS, "Failed to get oauth2 outbound provider", oauth2Provider);
        }
    } else {
        printWarn(KEY_UTILS, "Key validation service security confogurations not enabled.");
    }
    http:ClientConfiguration clientConfig = {
        auth: auth,
        cache: {enabled: false},
        secureSocket: secureSocket,
        http1Settings : {
            proxy: getClientProxyForInternalServices()
        }
    };
    oauth2:IntrospectionServerConfig keyValidationConfig = {
        url: getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, DEFAULT_KM_SERVER_URL),
        clientConfig: clientConfig
    };
    string introspectURL = getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, DEFAULT_KM_SERVER_URL);
    string keymanagerContext = getConfigValue(KM_CONF_INSTANCE_ID, KM_TOKEN_CONTEXT, DEFAULT_KM_TOKEN_CONTEXT);
    introspectURL = (introspectURL.endsWith(PATH_SEPERATOR)) ? introspectURL + keymanagerContext : introspectURL + PATH_SEPERATOR + keymanagerContext;
    introspectURL = (introspectURL.endsWith(PATH_SEPERATOR)) ? introspectURL + INTROSPECT_CONTEXT : introspectURL + PATH_SEPERATOR + INTROSPECT_CONTEXT;
    oauth2:IntrospectionServerConfig introspectionServerConfig = {
        url: introspectURL,
        oauth2Cache: introspectCache,
        clientConfig: clientConfig
    };
    OAuth2KeyValidationProvider oauth2KeyValidationProvider = new (keyValidationConfig);
    oauth2:InboundOAuth2Provider introspectionProvider = new (introspectionServerConfig);
    KeyValidationHandler | KeyValidationHandlerWrapper keyValidationHandler;
    if (isMetricsEnabled || isTracingEnabled) {
        keyValidationHandler = new KeyValidationHandlerWrapper(oauth2KeyValidationProvider, introspectionProvider);
    } else {
        keyValidationHandler = new KeyValidationHandler(oauth2KeyValidationProvider, introspectionProvider);
    }


    // Initializes the basic auth handler
    auth:BasicAuthConfig basicAuthConfig = {tableName: CONFIG_USER_SECTION};
    BasicAuthProvider configBasicAuthProvider;
    configBasicAuthProvider = new (basicAuthConfig);
    BasicAuthHandler | BasicAuthHandlerWrapper basicAuthHandler;
    if (isMetricsEnabled || isTracingEnabled) {
        basicAuthHandler = new BasicAuthHandlerWrapper(configBasicAuthProvider);
    } else {
        basicAuthHandler = new BasicAuthHandler(configBasicAuthProvider);
    }

    //load the Keystore
    loadKeyStore(trustStorePath,trustStorePassword);

    //Initializes the mutual ssl handler
    MutualSSLHandler | MutualSSLHandlerWrapper mutualSSLHandler;
    if (isMetricsEnabled || isTracingEnabled) {
        mutualSSLHandler = new MutualSSLHandlerWrapper();
    } else {
        mutualSSLHandler = new MutualSSLHandler(apiCertificateList, headerName, isClientCertificateValidationEnabled);
    }

    //Initializes the cookie based handler
    // CookieAuthHandler cookieBasedHandler = new;

    //set to the map
    authHandlersMap[MUTUAL_SSL_HANDLER] = mutualSSLHandler;
    authHandlersMap[KEY_VALIDATION_HANDLER] = keyValidationHandler;
    authHandlersMap[BASIC_AUTH_HANDLER] = basicAuthHandler;
    // authHandlersMap[COOKIE_BASED_HANDLER] = cookieBasedHandler;
    authHandlersMap[API_KEY_HANDLER] = apiKeyHandler;
}

function readMultipleJWTIssuers() {
    map<anydata>[] | error jwtIssuers = map<anydata>[].constructFrom(config:getAsArray(JWT_INSTANCE_ID));
    if (jwtIssuers is map<anydata>[] && jwtIssuers.length() > 0) {
        initiateJwtMap();
        printDebug(KEY_UTILS, "Found new multiple JWT issuer configs");
        int timestampSkew = getConfigIntValue(SERVER_CONF_ID, SERVER_TIMESTAMP_SKEW, DEFAULT_SERVER_TIMESTAMP_SKEW);
        if (timestampSkew == DEFAULT_SERVER_TIMESTAMP_SKEW) {
            timestampSkew = getConfigIntValue(KM_CONF_INSTANCE_ID, TIMESTAMP_SKEW, DEFAULT_TIMESTAMP_SKEW);
        }
        foreach map<anydata> jwtIssuer in jwtIssuers {
            jwt:JwtValidatorConfig jwtValidatorConfig = {
                issuer: getDefaultStringValue(jwtIssuer[ISSUER], DEFAULT_JWT_ISSUER),
                audience: getDefaultStringValue(jwtIssuer[AUDIENCE], DEFAULT_AUDIENCE),
                clockSkewInSeconds: timestampSkew/1000,
                trustStoreConfig: {
                    trustStore: {
                        path: trustStorePath,
                        password: trustStorePassword
                    },
                    certificateAlias: getDefaultStringValue(jwtIssuer[CERTIFICATE_ALIAS], DEFAULT_CERTIFICATE_ALIAS)
                },
                jwtCache: jwtCache
            };
            boolean classLoaded = false;
            string className = "";
            if(jwtIssuer.hasKey(ISSUER_CLASSNAME)) {
               className = getDefaultStringValue(jwtIssuer[ISSUER_CLASSNAME], DEFAULT_ISSUER_CLASSNAME);
               classLoaded = loadMappingClass(className);
            }
            map<anydata>[] | error claims = [];
            if(jwtIssuer.hasKey(ISSUER_CLAIMS)) {
                  claims = map<anydata>[].constructFrom((jwtIssuer[ISSUER_CLAIMS]));
            }
            JwtAuthProvider jwtAuthProvider
                = new (jwtValidatorConfig, getDefaultBooleanValue(jwtIssuer[VALIDATE_SUBSCRIPTION],
                    DEFAULT_VALIDATE_SUBSCRIPTION), claims, className, classLoaded);
            JWTAuthHandler | JWTAuthHandlerWrapper jwtAuthHandler;
            if (isMetricsEnabled || isTracingEnabled) {
                jwtAuthHandler = new JWTAuthHandlerWrapper(jwtAuthProvider);
            } else {
                jwtAuthHandler = new JWTAuthHandler(jwtAuthProvider);
            }
            jwtHandlers.push(jwtAuthHandler);
        }
    }

    if (jwtHandlers.length() < 1) {
        //Support old config model
        printDebug(KEY_UTILS, "Find old jwt configurations or set default JWT configurations.");
        jwt:JwtValidatorConfig jwtValidatorConfig = {
            issuer: getConfigValue(JWT_INSTANCE_ID, ISSUER, DEFAULT_JWT_ISSUER),
            audience: getConfigValue(JWT_INSTANCE_ID, AUDIENCE, DEFAULT_AUDIENCE),
            clockSkewInSeconds: 60,
            trustStoreConfig: {
                trustStore: {
                    path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH),
                    password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD)
                },
                certificateAlias: getConfigValue(JWT_INSTANCE_ID, CERTIFICATE_ALIAS, DEFAULT_CERTIFICATE_ALIAS)
            },
            jwtCache: jwtCache
        };
        JwtAuthProvider jwtAuthProvider
            = new (jwtValidatorConfig, getConfigBooleanValue(JWT_INSTANCE_ID, VALIDATE_SUBSCRIPTION,
                DEFAULT_VALIDATE_SUBSCRIPTION), [] , "", false);
        JWTAuthHandler | JWTAuthHandlerWrapper jwtAuthHandler;
        if (isMetricsEnabled || isTracingEnabled) {
            jwtAuthHandler = new JWTAuthHandlerWrapper(jwtAuthProvider);
        } else {
            jwtAuthHandler = new JWTAuthHandler(jwtAuthProvider);
        }
        jwtHandlers.push(jwtAuthHandler);
    }
}

function appendMultipleJWTIssuers(http:InboundAuthHandler[] handlers) {
    foreach http:InboundAuthHandler jwtHandler in jwtHandlers {
        handlers.push(jwtHandler);
    }
}

# Get application security handlers.
#
# + appSecurity - appsecurity array
# + return - auth handlers
public function getHandlers(string[] appSecurity) returns http:InboundAuthHandler[] {
    http:InboundAuthHandler[] handlers = [];
    //enforce handler order mutualssl, jwts, opaque, basic, apikey
    if (appSecurity.indexOf(AUTH_SCHEME_MUTUAL_SSL) != ()) {
        handlers.push(authHandlersMap.get(MUTUAL_SSL_HANDLER));
    }
    if (appSecurity.indexOf(AUTH_SCHEME_JWT) != ()) {
        appendMultipleJWTIssuers(handlers);
    }
    if (appSecurity.indexOf(AUTH_SCHEME_OAUTH2) != ()) {
        handlers.push(authHandlersMap.get(KEY_VALIDATION_HANDLER));
    }
    if (appSecurity.indexOf(AUTHN_SCHEME_BASIC) != ()) {
        handlers.push(authHandlersMap.get(BASIC_AUTH_HANDLER));
    }
    if (appSecurity.indexOf(AUTH_SCHEME_API_KEY) != ()) {
        handlers.push(authHandlersMap.get(API_KEY_HANDLER));
    }
    return handlers;
}

# This method is used to skip the scope validation done by the ballerina authorization filter. This is done because
# at the moment there is no way to skip this filter. So we have to set the scopes define in the resource annotation and
# set it to the principal, so ballerina authorization filter will pass. This is used in security mechanisms like api key
# where concept of scope is not applicable.
#
# + httpResourceConfig - resource level annotation config.
# + invocationContext - invocation context object.
# + user - fuser name to be set to the principal.
public function setResourceScopesToPrincipal(http:HttpResourceConfig httpResourceConfig,
        runtime:InvocationContext invocationContext, string user) {
    http:ResourceAuth? resourceLevelAuthAnn = httpResourceConfig?.auth;
    if (resourceLevelAuthAnn is http:ResourceAuth) {
        var resourceScopes = resourceLevelAuthAnn?.scopes;
        if (resourceScopes is string[]) {
            runtime:Principal principal = {username: user, scopes: resourceScopes};
            invocationContext.principal = principal;
        }
    }
}

function setRequestDataToInvocationContext(http:HttpServiceConfig httpServiceConfig, http:HttpResourceConfig? httpResourceConfig) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    string serviceName = invocationContext.attributes[http:SERVICE_NAME].toString();
    string apiContext = <string>httpServiceConfig.basePath;
    invocationContext.attributes[API_CONTEXT] = apiContext;
    if (httpResourceConfig is http:HttpResourceConfig) {
        string requestPath = httpResourceConfig.path;
        invocationContext.attributes[MATCHING_RESOURCE] =  requestPath;
    }
    APIConfiguration? apiConfig = apiConfigAnnotationMap[serviceName];
    if (apiConfig is APIConfiguration) {
        invocationContext.attributes[API_VERSION_PROPERTY] = <string>apiConfig.apiVersion;
        invocationContext.attributes[API_PUBLISHER] = <string>apiConfig.publisher;
        invocationContext.attributes[API_NAME] = <string>apiConfig.name;
    }
}

# Format context to remove preceding and trailing "/".
#
# + context - context to be formatted
# + return - context
public function removePrePostSlash(string context) returns string {
    string formattedContext = context;
    if (formattedContext.startsWith(PATH_SEPERATOR)) {
        formattedContext = formattedContext.substring(1, formattedContext.length());
    }
    if (formattedContext.endsWith(PATH_SEPERATOR)) {
        formattedContext = formattedContext.substring(0, formattedContext.length() - 1);
    }
    return formattedContext;
}
