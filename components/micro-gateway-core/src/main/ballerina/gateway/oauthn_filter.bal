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

@Description {value:"Representation of the Authentication filter"}
@Field {value:"filterRequest: request filter method which attempts to authenticated the request"}
public type OAuthnFilter object {

    public {
        OAuthnHandler oauthnHandler;// Handles the oauth2 authentication;
        http:AuthnHandlerChain authnHandlerChain;
    }

    public new (oauthnHandler, authnHandlerChain) {}

    @Description {value:"filterRequest: Request filter function"}
    public function filterRequest (http:Request request, http:FilterContext context) returns http:FilterResult {
        //Setting UUID
        context.attributes[MESSAGE_ID] = system:uuid();
        // get auth config for this resource
        boolean authenticated;
        APIKeyValidationRequestDto apiKeyValidationRequestDto = getKeyValidationRequestObject(context);
        var (isSecured, authProviders) = getResourceAuthConfig(context);
        APIKeyValidationDto apiKeyValidationDto;
        AuthenticationContext authenticationContext;
        boolean isAuthorized;
        if (isSecured) {
            string authHeader;
            if(request.hasHeader(AUTH_HEADER)) {
                authHeader = request.getHeader(AUTH_HEADER);
            } else {
                log:printError("No authorization header was provided");
                return createFilterResult(false, 401, "No authorization header was provided");
            }
            // if auth providers are there, use those to authenticate
            match getAuthenticationProviderType(request.getHeader(AUTH_HEADER)){
                string providerId => {
                    string[] providerIds = [providerId];
                    isAuthorized = self.authnHandlerChain.handleWithSpecificAuthnHandlers(providerIds, request);
                }
                () => {
                    match extractAccessToken(request) {
                        string token => {
                            apiKeyValidationRequestDto.accessToken = token;
                            match self.oauthnHandler.handle(request, apiKeyValidationRequestDto) {
                                APIKeyValidationDto apiKeyValidationDto => {
                                    isAuthorized = <boolean>apiKeyValidationDto.authorized;
                                    if(isAuthorized) {
                                        authenticationContext.authenticated = true;
                                        authenticationContext.tier = apiKeyValidationDto.tier;
                                        authenticationContext.apiKey = token;
                                        if (apiKeyValidationDto.endUserName != "") {
                                            authenticationContext.username = apiKeyValidationDto.endUserName;
                                        } else {
                                            authenticationContext.username = END_USER_ANONYMOUS;
                                        }
                                        authenticationContext.callerToken = apiKeyValidationDto.endUserToken;
                                        authenticationContext.applicationId = apiKeyValidationDto.applicationId;
                                        authenticationContext.applicationName = apiKeyValidationDto.applicationName;
                                        authenticationContext.applicationTier = apiKeyValidationDto.applicationTier;
                                        authenticationContext.subscriber = apiKeyValidationDto.subscriber;
                                        authenticationContext.consumerKey = apiKeyValidationDto.consumerKey;
                                        authenticationContext.apiTier = apiKeyValidationDto.apiTier;
                                        authenticationContext.subscriberTenantDomain = apiKeyValidationDto.subscriberTenantDomain;
                                        authenticationContext.spikeArrestLimit = check <int> apiKeyValidationDto.spikeArrestLimit;
                                        authenticationContext.spikeArrestUnit = apiKeyValidationDto.spikeArrestUnit;
                                        authenticationContext.stopOnQuotaReach = <boolean>apiKeyValidationDto.stopOnQuotaReach;
                                        authenticationContext.isContentAwareTierPresent = <boolean> apiKeyValidationDto
                                        .contentAware;
                                        context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                                        runtime:AuthContext authContext = runtime:getInvocationContext().authContext;
                                        authContext.scheme = AUTH_SCHEME_OAUTH2;
                                        authContext.authToken = token;
                                    } else {
                                        return createFilterResult(false, 401, "Authentication failed");
                                    }
                                }
                                error err => {
                                    log:printError(err.message);
                                    return createFilterResult(false, 500, "Error while authenticating the token " +
                                            token);
                                }
                            }
                        }
                        error err => {
                            log:printError(err.message);
                            return createFilterResult(false, 401, err.message);
                        }
                    }

                }
            }

        } else {
            // not secured, no need to authenticate
            string clientIp = getClientIp(request);
            authenticationContext.authenticated = true;
            authenticationContext.tier = UNAUTHENTICATED_TIER;
            authenticationContext.stopOnQuotaReach = true;
            authenticationContext.apiKey = clientIp ;
            authenticationContext.username = END_USER_ANONYMOUS;
            authenticationContext.applicationId = clientIp;
            context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
            return createFilterResult(true, 200 , "Successfully authenticated");
        }
        return createAuthnResult(isAuthorized);
    }
};


@Description {value:"Checks if the resource is secured"}
@Param {value:"context: FilterContext object"}
@Return {value:"boolean, string[]: tuple of whether the resource is secured and the list of auth provider ids "}
function getResourceAuthConfig (http:FilterContext context) returns (boolean, string[]) {
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
            // no auth providers found in resource level, try in service level
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

function getAuthenticationProviderType(string authHeader) returns (string|()) {
    if(authHeader.contains(AUTH_SCHEME_BASIC)){
        return AUTHN_SCHEME_BASIC;
    } else if (authHeader.contains(AUTH_SCHEME_BEARER) && authHeader.contains(".")) {
        return AUTH_SCHEME_JWT;
    } else {
        return ();
    }
}


