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
import ballerina/crypto;
import ballerina/encoding;

import constants;
import dtos;
import utils;

public type BasicAuthUtils object {

    http:AuthHandlerRegistry registry = new;
    http:AuthnHandlerChain authnHandlerChain = new(registry);

    public function processRequest(http:Caller caller, http:Request request, http:FilterContext context)
                        returns boolean {

        boolean isAuthenticated;
        //API authentication info
        dtos:AuthenticationContext authenticationContext = {};
        boolean isAuthorized;
        string[] providerIds = [constants:AUTHN_SCHEME_BASIC];
        //set Username from the request
        string authHead = request.getHeader(constants:AUTHORIZATION_HEADER);
        string[] headers = authHead.trim().split("\\s* \\s*");
        string encodedCredentials = headers[1];
        byte[]|error decodedCredentials = encoding:decodeBase64(encodedCredentials);
        //Extract username and password from the request
        string userName;
        string passWord;
        if (decodedCredentials is byte[]) {
            string decodedCredentialsString = encoding:byteArrayToString(decodedCredentials);
            if (!decodedCredentialsString.contains(":")) {
                utils:setErrorMessageToFilterContext(context, constants:API_AUTH_BASICAUTH_INVALID_FORMAT);
                utils:sendErrorResponse(caller, request, untaint context);
                return false;
            }
            string[] decodedCred = decodedCredentialsString.trim().split(":");
            userName = decodedCred[0];
            if (decodedCred.length() < 2) {
                int status;
                if (<int>context.attributes[constants:HTTP_STATUS_CODE] == constants:INTERNAL_SERVER_ERROR) {
                    status = constants:UNAUTHORIZED;
                    context.attributes[constants:HTTP_STATUS_CODE] = status;
                }
                utils:setErrorMessageToFilterContext(context, constants:API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                utils:sendErrorResponse(caller, request, untaint context);
                return false;
            }
            passWord = decodedCred[1];
        } else {
            int status;
            if (<int>context.attributes[constants:HTTP_STATUS_CODE] == constants:INTERNAL_SERVER_ERROR) {
                status = constants:UNAUTHORIZED;
                context.attributes[constants:HTTP_STATUS_CODE] = status;
            }
            utils:setErrorMessageToFilterContext(context, constants:API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
            utils:sendErrorResponse(caller, request, untaint context);
            return false;
        }

        //Hashing mechanism
        string hashedPass = encoding:byteArrayToString(crypto:hashSha1(passWord.toByteArray("UTF-8")));
        string credentials = userName + ":" + hashedPass;
        string hashedRequest;
        string encodedVal = encoding:encodeBase64(credentials.toByteArray("UTF-8"));
        hashedRequest = "Basic " + encodedVal;
        request.setHeader(constants:AUTHORIZATION_HEADER, hashedRequest);

        log:printDebug(constants:KEY_AUTHN_FILTER + ": Processing request with the Authentication handler chain");
        isAuthorized = self.authnHandlerChain.handleWithSpecificAuthnHandlers(providerIds, request);
        log:printDebug(constants:KEY_AUTHN_FILTER + ": Authentication handler chain returned with value: " +
                isAuthorized);
        if (!isAuthorized) {
            utils:setErrorMessageToFilterContext(context, constants:API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
            utils:sendErrorResponse(caller, request, untaint context);
            return false;
        }

        int startingTime = utils:getCurrentTime();
        context.attributes[constants:REQUEST_TIME] = startingTime;
        context.attributes[constants:FILTER_FAILED] = false;
        //Set authenticationContext data
        authenticationContext.authenticated = true;
        //Authentication context data is set to default value bacuase in basic authentication we cannot have informtaion on subscription and applications
        authenticationContext.tier = constants:UNAUTHENTICATED_TIER;
        authenticationContext.applicationTier = constants:UNLIMITED_TIER;
        authenticationContext.apiKey = constants:ANONYMOUS_APP_ID;
        //Username is extracted from the request
        authenticationContext.username = userName;
        authenticationContext.applicationId = constants:ANONYMOUS_APP_ID;
        authenticationContext.applicationName = constants:ANONYMOUS_APP_NAME;
        authenticationContext.subscriber = constants:ANONYMOUS_APP_OWNER;
        authenticationContext.consumerKey = constants:ANONYMOUS_CONSUMER_KEY;
        authenticationContext.apiTier = constants:UNAUTHENTICATED_TIER;
        authenticationContext.apiPublisher = constants:USER_NAME_UNKNOWN;
        authenticationContext.subscriberTenantDomain = constants:ANONYMOUS_USER_TENANT_DOMAIN;
        authenticationContext.keyType = constants:ANONYMOUS_CONSUMER_KEY;
        runtime:getInvocationContext().attributes[constants:KEY_TYPE_ATTR] = authenticationContext.keyType;
        context.attributes[constants:AUTHENTICATION_CONTEXT] = authenticationContext;
        isAuthenticated = true;
        return isAuthenticated;
    }
};
