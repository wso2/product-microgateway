// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/log;
import ballerina/auth;
import ballerina/config;
import ballerina/runtime;
import ballerina/system;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;

// Authentication filter

@Description { value: "Representation of the Authentication filter" }
@Field { value: "filterRequest: request filter method which attempts to authenticated the request" }
public type AuthnFilter object {

    public OAuthnAuthenticator oauthnHandler;//Handles the oauth2 authentication;
    public boolean isOauth2Enabled = false;

    @Description { value: "filterRequest: Request filter function" }
    public function filterRequest(http:Listener listener, http:Request request, http:FilterContext context)
                        returns boolean {

        string checkAuthentication = getConfigValue(MTSL_CONF_INSTANCE_ID, MTSL_CONF_SSLVERIFYCLIENT, "");
        //Setting UUID
        if (checkAuthentication != "require"){
            int startingTime = getCurrentTime();
            context.attributes[REQUEST_TIME] = startingTime;
            checkOrSetMessageID(context);
            boolean result = doFilterRequest(listener, request, context);
            setLatency(startingTime, context, SECURITY_LATENCY_AUTHN);
            return result;
        } else {
            // Skip this filter is mutualSSL is enabled.
            return true;
        }

    }

    @Description { value: "filterRequest: Request filter function" }
    public function doFilterRequest(http:Listener listener, http:Request request, http:FilterContext context)
                        returns boolean {
        runtime:getInvocationContext().attributes[MESSAGE_ID] = <string>context.attributes[MESSAGE_ID];
        printDebug(KEY_AUTHN_FILTER, "Processing request via Authentication filter.");

        context.attributes[REMOTE_ADDRESS] = getClientIp(request, listener);
        context.attributes[FILTER_FAILED] = false;
        runtime:getInvocationContext().attributes[SERVICE_TYPE_ATTR] = context.serviceType;
        runtime:getInvocationContext().attributes[RESOURCE_NAME_ATTR] = context.resourceName;
        // get auth config for this resource
        boolean authenticated;
        APIRequestMetaDataDto apiKeyValidationRequestDto = getKeyValidationRequestObject();
        var (isSecured, authProvidersIds) = getResourceAuthConfig(context);
        context.attributes[IS_SECURED] = isSecured;
        //Create auth handler chain with providerIds in service file
        http:AuthHandlerRegistry registry;
        http:AuthProvider[] authProviders = getAuthProviders();
        foreach i in authProvidersIds{
            if (i == "oauth2"){
                //check whether Oauth2 is enabled in service files.
                isOauth2Enabled = true;
            }
            foreach k in authProviders  {
                if (k.id == i){
                    registry.add(k.id, createAuthHandler(k));
                }
            }
        }
        http:AuthnHandlerChain authnHandlerChain = new(registry);
        //APIKeyValidationDto apiKeyValidationInfoDto;
        AuthenticationContext authenticationContext;
        boolean isAuthorized;
        printDebug(KEY_AUTHN_FILTER, "Resource secured: " + isSecured);
        if (isSecured) {
            string authHeader;
            string authHeaderName = getAuthorizationHeader(reflect:getServiceAnnotations(context.serviceType));
            if (request.hasHeader(authHeaderName)) {
                authHeader = request.getHeader(authHeaderName);
            } else {
                log:printError("No authorization header was provided");
                setErrorMessageToFilterContext(context, API_AUTH_MISSING_CREDENTIALS);
                sendErrorResponse(listener, request, untaint context);
                return false;
            }
            string providerId = getAuthenticationProviderType(authHeader);
            // if auth providers are there, use those to authenticate
            if (providerId == AUTH_SCHEME_JWT) {
                printDebug(KEY_AUTHN_FILTER, "Non-OAuth token found. Calling the auth scheme : " + providerId);
                // if authorization header is not default auth header we need to set it to the default header in
                // order for jwt to work. If there is an already default auth header we back up it to a temp auth
                // header and set the default authentication header.
                if (authHeaderName != AUTH_HEADER) {
                    if (request.hasHeader(AUTH_HEADER)) {
                        request.setHeader(TEMP_AUTH_HEADER, request.getHeader(AUTH_HEADER));
                        printDebug(KEY_AUTHN_FILTER,
                            "Authorization header found in the request. Backing up original value");
                    }
                    request.setHeader(AUTH_HEADER, authHeader);
                    printDebug(KEY_AUTHN_FILTER, "Replace the custom auth header : " + authHeaderName
                            + " with default the auth header:" + AUTH_HEADER);
                }

                try {
                    printDebug(KEY_AUTHN_FILTER, "Processing request with the Authentication handler chain");
                    isAuthorized = authnHandlerChain.handle(request);
                    printDebug(KEY_AUTHN_FILTER, "Authentication handler chain returned with value : " + isAuthorized);
                    checkAndRemoveAuthHeaders(request, authHeaderName);
                } catch (error err) {
                    // todo: need to properly handle this exception. Currently this is a generic exception catching.
                    // todo: need to check log:printError(errMsg, err = err);. Currently doesn't give any useful information.
                    printError(KEY_AUTHN_FILTER, "Error occurred while authenticating via JWT token.");
                    setErrorMessageToFilterContext(context, API_AUTH_INVALID_CREDENTIALS);
                    sendErrorResponse(listener, request, untaint context);
                    return false;
                }
            } else if (providerId == AUTH_SCHEME_OAUTH2){
                if (isOauth2Enabled){
                    match extractAccessToken(request, authHeaderName) {
                        string token => {
                            runtime:getInvocationContext().attributes[ACCESS_TOKEN_ATTR] = token;
                            printDebug(KEY_AUTHN_FILTER, "Successfully extracted the OAuth token from header : " +
                                    authHeaderName);
                            match self.oauthnHandler.handle(request) {
                                APIKeyValidationDto apiKeyValidationDto => {
                                    isAuthorized = <boolean>apiKeyValidationDto.authorized;
                                    printDebug(KEY_AUTHN_FILTER, "Authentication handler returned with value : " +
                                            isAuthorized);
                                    if (isAuthorized) {
                                        authenticationContext.authenticated = true;
                                        authenticationContext.tier = apiKeyValidationDto.tier;
                                        authenticationContext.apiKey = token;
                                        if (apiKeyValidationDto.endUserName != "") {
                                            authenticationContext.username = apiKeyValidationDto.endUserName;
                                        } else {
                                            authenticationContext.username = END_USER_ANONYMOUS;
                                        }
                                        authenticationContext.apiPublisher = apiKeyValidationDto.apiPublisher;
                                        authenticationContext.keyType = apiKeyValidationDto.keyType;
                                        authenticationContext.callerToken = apiKeyValidationDto.endUserToken;
                                        authenticationContext.applicationId = apiKeyValidationDto.applicationId;
                                        authenticationContext.applicationName = apiKeyValidationDto.applicationName;
                                        authenticationContext.applicationTier = apiKeyValidationDto.applicationTier;
                                        authenticationContext.subscriber = apiKeyValidationDto.subscriber;
                                        authenticationContext.consumerKey = apiKeyValidationDto.consumerKey;
                                        authenticationContext.apiTier = apiKeyValidationDto.apiTier;
                                        authenticationContext.subscriberTenantDomain = apiKeyValidationDto.
                                        subscriberTenantDomain;
                                        authenticationContext.spikeArrestLimit = check <int>apiKeyValidationDto.
                                        spikeArrestLimit;
                                        authenticationContext.spikeArrestUnit = apiKeyValidationDto.spikeArrestUnit;
                                        authenticationContext.stopOnQuotaReach = <boolean>apiKeyValidationDto.
                                        stopOnQuotaReach;
                                        authenticationContext.isContentAwareTierPresent = <boolean>apiKeyValidationDto
                                        .contentAware;
                                        printDebug(KEY_AUTHN_FILTER, "Caller token: " + authenticationContext.
                                                callerToken);
                                        if (authenticationContext.callerToken != "" && authenticationContext.callerToken
                                        != null) {
                                            string jwtheaderName = getConfigValue(JWT_CONFIG_INSTANCE_ID, JWT_HEADER,
                                                JWT_HEADER_NAME);
                                            request.setHeader(jwtheaderName, authenticationContext.callerToken);
                                        }
                                        checkAndRemoveAuthHeaders(request, authHeaderName);
                                        context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;

                                        // setting keytype to invocationContext
                                        runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = authenticationContext
                                        .keyType;
                                        runtime:AuthContext authContext = runtime:getInvocationContext().authContext;
                                        authContext.scheme = AUTH_SCHEME_OAUTH2;
                                        authContext.authToken = token;
                                    } else {
                                        int status = check <int>apiKeyValidationDto.validationStatus;
                                        printDebug(KEY_AUTHN_FILTER,
                                                "Authentication handler returned with validation status : " +
                                                status);
                                        setErrorMessageToFilterContext(context, status);
                                        sendErrorResponse(listener, request, untaint context);
                                        return false;
                                    }
                                }
                                error err => {
                                    log:printError(err.message, err = err);
                                    setErrorMessageToFilterContext(context, API_AUTH_GENERAL_ERROR);
                                    sendErrorResponse(listener, request, untaint context);
                                    return false;
                                }
                            }
                        }
                        error err => {
                            log:printError(err.message, err = err);
                            setErrorMessageToFilterContext(context, API_AUTH_MISSING_CREDENTIALS);
                            sendErrorResponse(listener, request, untaint context);
                            return false;
                        }
                    }
                } else {
                    setErrorMessageToFilterContext(context, API_AUTH_MISSING_CREDENTIALS);
                    sendErrorResponse(listener, request, untaint context);
                    return false;
                }
            } else if (providerId == AUTHN_SCHEME_BASIC)
            {
                //Basic auth valiadation
                BasicAuthUtils basicAuthentication = new BasicAuthUtils (authnHandlerChain);
                boolean isValidated = basicAuthentication.processRequest(listener, request, context);
                return isValidated;
            }

        } else {
            // not secured, no need to authenticate
            return true;
        }
        if (!isAuthorized) {
            setErrorMessageToFilterContext(context, API_AUTH_INVALID_CREDENTIALS);
            sendErrorResponse(listener, request, untaint context);
        }
        return isAuthorized;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }
};


@Description { value: "Checks if the resource is secured" }
@Param { value: "context: FilterContext object" }
@Return { value: "boolean, string[]: tuple of whether the resource is secured and the list of auth provider ids " }
function getResourceAuthConfig(http:FilterContext context) returns (boolean, string[]) {
    boolean resourceSecured;
    string[] authProviderIds = [];
    // get authn details from the resource level
    http:ListenerAuthConfig? resourceLevelAuthAnn = getAuthAnnotation(ANN_PACKAGE,
        RESOURCE_ANN_NAME,
        reflect:getResourceAnnotations(context.serviceType, context.resourceName));
    http:ListenerAuthConfig? serviceLevelAuthAnn = getAuthAnnotation(ANN_PACKAGE,
        SERVICE_ANN_NAME,
        reflect:getServiceAnnotations(context.serviceType));
    // check if authentication is enabled
    resourceSecured = isResourceSecured(resourceLevelAuthAnn, serviceLevelAuthAnn);
    // if resource is not secured, no need to check further
    if (!resourceSecured) {
        return (resourceSecured, authProviderIds);
    }
    // check if auth providers are given at resource level
    match resourceLevelAuthAnn.authProviders {
        string[] providers => {
            authProviderIds = providers;
        }
        () => {
            // no auth providers found in resource level, try in rest level
            match serviceLevelAuthAnn.authProviders {
                string[] providers => {
                    authProviderIds = providers;
                }
                () => {
                    // no auth providers found
                }
            }
        }
    }
    return (resourceSecured, authProviderIds);
}

function getAuthenticationProviderType(string authHeader) returns (string) {
    if (authHeader.contains(AUTH_SCHEME_BASIC)){
        return AUTHN_SCHEME_BASIC;
    } else if (authHeader.contains(AUTH_SCHEME_BEARER) && authHeader.contains(".")) {
        return AUTH_SCHEME_JWT;
    } else {
        return AUTH_SCHEME_OAUTH2;
    }
}

function checkAndRemoveAuthHeaders(http:Request request, string authHeaderName) {
    if (getConfigBooleanValue(AUTH_CONF_INSTANCE_ID, REMOVE_AUTH_HEADER_FROM_OUT_MESSAGE, true)) {
        request.removeHeader(authHeaderName);
        printDebug(KEY_AUTHN_FILTER, "Removed header : " + authHeaderName + " from the request");
    }
    if (request.hasHeader(TEMP_AUTH_HEADER)) {
        request.setHeader(AUTH_HEADER, request.getHeader(TEMP_AUTH_HEADER));
        printDebug(KEY_AUTHN_FILTER, "Setting the backed up auth header value to the header: " + AUTH_HEADER);
        request.removeHeader(TEMP_AUTH_HEADER);
        printDebug(KEY_AUTHN_FILTER, "Removed header : " + TEMP_AUTH_HEADER + " from the request");
    }
}

