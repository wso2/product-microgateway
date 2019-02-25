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

public type BasicAuthUtils object {

    http:AuthHandlerRegistry registry = new;
    http:AuthnHandlerChain authnHandlerChain = new(registry);

    public function processRequest(http:Caller caller, http:Request request, http:FilterContext context)
                        returns boolean {

        boolean isAuthenticated;
        //API authentication info
        AuthenticationContext authenticationContext = new;
        boolean isAuthorized;
        string[] providerIds = [AUTHN_SCHEME_BASIC];
        //set Username from the request
        string authHead = request.getHeader(AUTHORIZATION_HEADER);
        string[] headers = authHead.trim().split("\\s* \\s*");
        string encodedCredentials = headers[1];
        var decodedCredentials = encoding:decodeBase64(encodedCredentials.toByteArray("UTF-8"));
        //Extract username and password from the request
        string userName;
        string passWord;
        if(decodedCredentials is string){
            if (!decodedCredentials.contains(":")) {
                setErrorMessageToFilterContext(context, API_AUTH_BASICAUTH_INVALID_FORMAT);
                sendErrorResponse(caller, request, untaint context);
                return false;
            }
            string[] decodedCred = decodedCredentials.trim().split(":");
            userName = decodedCred[0];
            if (decodedCred.length() < 2) {
                int status;
                if (<int>context.attributes[HTTP_STATUS_CODE] == INTERNAL_SERVER_ERROR) {
                    status = UNAUTHORIZED;
                    context.attributes[HTTP_STATUS_CODE] = status;
                }
                setErrorMessageToFilterContext(context, API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                sendErrorResponse(caller, request, untaint context);
                return false;
            }
            passWord = decodedCred[1];
        } else {
            int status;
            if (<int>context.attributes[HTTP_STATUS_CODE] == INTERNAL_SERVER_ERROR) {
                status = UNAUTHORIZED;
                context.attributes[HTTP_STATUS_CODE] = status;
            }
            setErrorMessageToFilterContext(context, API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
            sendErrorResponse(caller, request, untaint context);
            return false;
        }

        //Hashing mechanism
        string hashedPass = crypto:hashSha1(passWord.toByteArray("UTF-8"));
        string credentials = userName + ":" + hashedPass;
        string hashedRequest;
        var encodedVal = encoding:encodeBase64(credentials.toByteArray("UTF-8"));
        if(encodedVal is string) {
            hashedRequest = "Basic " + encodedVal;
        } else {
            throw err;
        }
        request.setHeader(AUTHORIZATION_HEADER, hashedRequest);

        try {
            printDebug(KEY_AUTHN_FILTER, "Processing request with the Authentication handler chain");
            isAuthorized = self.authnHandlerChain.handleWithSpecificAuthnHandlers(providerIds, request);
            printDebug(KEY_AUTHN_FILTER, "Authentication handler chain returned with value : " + isAuthorized);
            if (isAuthorized == false) {
                setErrorMessageToFilterContext(context, API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                sendErrorResponse(caller, request, untaint context);
                return false;
            }
        } catch (error err) {
            // todo: need to properly handle this exception. Currently this is a generic exception catching.
            // todo: need to check log:printError(errMsg, err = err);. Currently doesn't give any useful information.
            printError(KEY_AUTHN_FILTER,
                "Error occurred while authenticating via Basic authentication credentials");
            setErrorMessageToFilterContext(context, API_AUTH_INVALID_CREDENTIALS);
            sendErrorResponse(caller, request, untaint context);
            return false;
        }

        int startingTime = getCurrentTime();
        context.attributes[REQUEST_TIME] = startingTime;
        context.attributes[FILTER_FAILED] = false;
        //Set authenticationContext data
        authenticationContext.authenticated = true;
        //Authentication context data is set to default value bacuase in basic authentication we cannot have informtaion on subscription and applications
        authenticationContext.tier = UNAUTHENTICATED_TIER;
        authenticationContext.applicationTier = UNLIMITED_TIER;
        authenticationContext.apiKey = ANONYMOUS_APP_ID;
        //Username is extracted from the request
        authenticationContext.username = userName;
        authenticationContext.applicationId = ANONYMOUS_APP_ID;
        authenticationContext.applicationName = ANONYMOUS_APP_NAME;
        authenticationContext.subscriber = ANONYMOUS_APP_OWNER;
        authenticationContext.consumerKey = ANONYMOUS_CONSUMER_KEY;
        authenticationContext.apiTier = UNAUTHENTICATED_TIER;
        authenticationContext.apiPublisher = USER_NAME_UNKNOWN;
        authenticationContext.subscriberTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
        authenticationContext.keyType = ANONYMOUS_CONSUMER_KEY;
        runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
        context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
        isAuthenticated = true;
        return isAuthenticated;
    }
};