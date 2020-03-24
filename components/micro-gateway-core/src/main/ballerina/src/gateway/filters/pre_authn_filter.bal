// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file   except
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
import ballerina/stringutils;

// Pre Authentication filter

public type PreAuthnFilter object {

    public function filterRequest(http:Caller caller, http:Request request,@tainted http:FilterContext context) returns boolean {
        setFilterSkipToFilterContext(context);
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_PRE_AUTHN_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        //Setting UUID
        int startingTime = getCurrentTimeForAnalytics();
        context.attributes[REQUEST_TIME] = startingTime;
        checkOrSetMessageID(context);
        setHostHeaderToFilterContext(request, context);
        //To set the properties which are required for the validation filter
        setReqPathAndMethodToFilterContext(request, context);
        boolean result = doAuthnFilterRequest(caller, request, <@untainted>context);
        setLatency(startingTime, context, SECURITY_LATENCY_AUTHN);
        return result;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_PRE_AUTHN_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        if (response.statusCode == 401) {
            runtime:InvocationContext invocationContext = runtime:getInvocationContext();
            //This handles the case where the empty Bearer/Basic value provided for authorization header. If all the
            //auth handlers are invoked and returning 401 without proper error message in the context means, invalid
            //credentials are provided. Hence we seth invalid credentials message to context
            if (!invocationContext.attributes.hasKey(ERROR_CODE)) {
                setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
            }
            sendErrorResponseFromInvocationContext(response);
        }
        return true;
    }
};

function doAuthnFilterRequest(http:Caller caller, http:Request request, http:FilterContext context)
returns boolean {
    boolean isOauth2Enabled = false;
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    invocationContext.attributes[MESSAGE_ID] = <string>context.attributes[MESSAGE_ID];
    printDebug(KEY_PRE_AUTHN_FILTER, "Processing request via Pre Authentication filter.");

    context.attributes[REMOTE_ADDRESS] = getClientIp(request, caller);
    context.attributes[FILTER_FAILED] = false;
    invocationContext.attributes[FILTER_FAILED] = false;
    string serviceName = context.getServiceName();
    string resourceName = context.getResourceName();
    invocationContext.attributes[SERVICE_TYPE_ATTR] = context.getService();
    invocationContext.attributes[RESOURCE_NAME_ATTR] = resourceName;
    boolean isSecuredResource = isSecured(serviceName, resourceName);
    printDebug(KEY_PRE_AUTHN_FILTER, "Resource secured : " + isSecuredResource.toString());
    invocationContext.attributes[IS_SECURED] = isSecuredResource;
    context.attributes[IS_SECURED] = isSecuredResource;
    invocationContext.attributes[REQUEST_METHOD] = request.method;
    invocationContext.attributes[REQUEST_RAWPATH] = request.rawPath;

    string authHeader = "";
    string | error extractedToken = "";
    string authHeaderName = getAuthHeaderFromFilterContext(context);
    printDebug(KEY_PRE_AUTHN_FILTER, "Authentication header name : " + authHeaderName);
    invocationContext.attributes[AUTH_HEADER] = authHeaderName;
    string[] authProvidersIds = getAuthProviders(context.getServiceName(), context.getResourceName());
    printDebug(KEY_PRE_AUTHN_FILTER, "Auth providers array  : " + authProvidersIds.toString());

    boolean isAPIKeyAuth = false;
    if (request.hasHeader(authHeaderName)) {
        authHeader = request.getHeader(authHeaderName);
    } else {
        //process apikey authentication
        if (authProvidersIds.indexOf(AUTH_SCHEME_API_KEY) != ()) {
            json[] apiKeys = getAPIKeysforResource(context.getServiceName(), context.getResourceName());
            printDebug(KEY_PRE_AUTHN_FILTER, apiKeys.toString());
            if (apiKeys.length() > 0) {
                foreach json apiKey in apiKeys {
                    if (apiKey is  map<json>) {
                        string inName = apiKey[API_KEY_IN].toString();
                        string name = apiKey[API_KEY_NAME].toString();
                        printDebug(KEY_PRE_AUTHN_FILTER, "Detected apikey security in : " + inName + " name: " + name);
                        if (stringutils:equalsIgnoreCase(HEADER, inName) && request.hasHeader(name)) {
                            printDebug(KEY_PRE_AUTHN_FILTER, "Request has apikey header : " + name);
                            isAPIKeyAuth = true;
                            setAPIKeyAuth(inName, name);
                            authHeader = AUTH_SCHEME_API_KEY;
                            break;
                        } else if (stringutils:equalsIgnoreCase(QUERY, inName)
                                && request.getQueryParamValue(name) is string) {
                            printDebug(KEY_PRE_AUTHN_FILTER, "Request has apikey query : " + name);
                            isAPIKeyAuth = true;
                            setAPIKeyAuth(inName, name);
                            authHeader = AUTH_SCHEME_API_KEY;
                            break;
                        }
                    }
                }
            }
        }
    }
    // set api's mutual ssl client verify configuration
    setMutualSSL(context.getServiceName());

    string providerId = getAuthenticationProviderType(authHeader);
    printDebug(KEY_PRE_AUTHN_FILTER, "Provider Id for authentication handler : " + providerId);
    boolean canHandleAuthentication = isAPIKeyAuth;
    foreach string provider in authProvidersIds {
        if (provider == providerId) {
            canHandleAuthentication = true;
        }
    }

    boolean isOptional = isAppSecurityOptionalforResource(context.getServiceName(), context.getResourceName());
    if (isSecuredResource && !isOptional) {
        if ((!request.hasHeader(authHeaderName) || request.getHeader(authHeaderName).length() == 0) && !isAPIKeyAuth) {
            printDebug(KEY_PRE_AUTHN_FILTER, "Authentication header is missing for secured resource");
            setErrorMessageToInvocationContext(API_AUTH_MISSING_CREDENTIALS);
            setErrorMessageToFilterContext(context, API_AUTH_MISSING_CREDENTIALS);
            sendErrorResponse(caller, request, context);
            return false;
        }
        if (!canHandleAuthentication) {
            printDebug(KEY_PRE_AUTHN_FILTER, "Request does not have a valid authentication scheme");
            setErrorMessageToInvocationContext(API_AUTH_PROVIDER_INVALID);
            setErrorMessageToFilterContext(context, API_AUTH_PROVIDER_INVALID);
            sendErrorResponse(caller, request, context);
            return false;
        }
    }
    return true;
}

function getAuthenticationProviderType(string authHeader) returns (string) {
    printDebug(KEY_PRE_AUTHN_FILTER, "authHeader: " + authHeader);
    string authHdr = authHeader.toLowerAscii();
    if (contains(authHdr, AUTH_SCHEME_BASIC_LOWERCASE)) {
        return AUTHN_SCHEME_BASIC;
    } else if (contains(authHdr, AUTH_SCHEME_BEARER_LOWERCASE) && contains(authHeader, ".")) {
        return AUTH_SCHEME_JWT;
    } else {
        return AUTH_SCHEME_OAUTH2;
    }
}
