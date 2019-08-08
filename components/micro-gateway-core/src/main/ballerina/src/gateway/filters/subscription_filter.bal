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
import ballerina/auth;
import ballerina/internal;
import ballerina/log;
import ballerina/io;
import ballerina/jwt;

// Subscription filter to validate the subscriptions which is available in the  jwt token
// This filter should only be engaged when jwt token is is used for authentication. For oauth2
// OAuthnFilter will handle the subscription validation as well.
public type SubscriptionFilter object {

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext)
                        returns boolean {
        int startingTime = getCurrentTime();
        checkOrSetMessageID(filterContext);
        boolean result = doSubscriptionFilterRequest(caller, request, filterContext);
        setLatency(startingTime, filterContext, SECURITY_LATENCY_SUBS);
        return result;
    }



    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }
};

function doSubscriptionFilterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext)
             returns boolean {
    runtime:AuthenticationContext? authContext= runtime:getInvocationContext()?.authenticationContext;
    if(authContext is runtime:AuthenticationContext) {
        string authScheme = authContext.scheme;
        printDebug(KEY_SUBSCRIPTION_FILTER, "Auth scheme: " + authScheme);
        if (authScheme != AUTH_SCHEME_JWT){
            printDebug(KEY_SUBSCRIPTION_FILTER, "Skipping since auth scheme != jwt.");
            return true;
        }
    
        string jwtToken = authContext.authToken;
        string currentAPIContext = getContext(filterContext);
        AuthenticationContext authenticationContext = {};

        json|error decodedPayload = {};
        var cachedJwt = trap <jwt:CachedJwt>jwtCache.get(jwtToken);
        if (cachedJwt is jwt:CachedJwt) {
            printDebug(KEY_SUBSCRIPTION_FILTER, "jwt found from the jwt cache");
            jwt:JwtPayload jwtPayload = cachedJwt.jwtPayload;
            json payload = {};
            if(payload is map<json>){
                map<json>? customClaims = jwtPayload?.customClaims;
                if(customClaims is map<json>) {
                    if(customClaims.hasKey(APPLICATION)) {
                        payload["application"] = customClaims[APPLICATION];
                    }
                    if(customClaims.hasKey(SUBSCRIBED_APIS)) {
                        payload["subscribedAPIs"] = customClaims[SUBSCRIBED_APIS];
                    }
                    if(customClaims.hasKey(CONSUMER_KEY)) {
                        payload["consumerKey"] = customClaims[CONSUMER_KEY];
                    }
                    if(customClaims.hasKey(KEY_TYPE)) {
                        payload["keytype"] = customClaims[KEY_TYPE];
                    }
                }
                if(jwtPayload?.sub is string) {
                    payload["sub"] = jwtPayload?.sub;
                }
                decodedPayload = payload;
            }
        } else {
            //If not found in cache decode jwt token and get the payload
            var jwtPayload = getEncodedJWTPayload(jwtToken);
            if (jwtPayload is error) {
                log:printError(jwtPayload.reason(), err = jwtPayload);
                setErrorMessageToFilterContext(filterContext, API_AUTH_GENERAL_ERROR);
                sendErrorResponse(caller, request, filterContext);
                return false;
            } else {
                printTrace(KEY_SUBSCRIPTION_FILTER, "Encoded JWT payload: " + jwtPayload);
                decodedPayload = getDecodedJWTPayload(jwtPayload);
            }
        }

        if(decodedPayload is json) {
            printTrace(KEY_SUBSCRIPTION_FILTER, "Decoded JWT payload: " + decodedPayload.toString());
            json[] subscribedAPIList = [];
            json|error jsonSubscribedApis = decodedPayload.subscribedAPIs;
            if (jsonSubscribedApis is json && subscribedAPIList is map<json>){
                printDebug(KEY_SUBSCRIPTION_FILTER, "subscribedAPIs claim found in the jwt");
                subscribedAPIList = jsonSubscribedApis;
                printDebug(KEY_SUBSCRIPTION_FILTER, "Subscribed APIs list : " + subscribedAPIList.toString());
                APIConfiguration? apiConfig = apiConfigAnnotationMap[getServiceName(filterContext.getServiceName())];
                int l = subscribedAPIList.length();
                if (l == 0){
                    authenticationContext.authenticated = true;
                    authenticationContext.apiKey = jwtToken;
                    authenticationContext.username = decodedPayload.sub.toString();
                    if (decodedPayload.application.id != null) {
                        authenticationContext.applicationId = decodedPayload.application.id.toString();
                    }
                    if (decodedPayload.application.name != null) {
                        authenticationContext.applicationName = decodedPayload.application.name.toString
                        ();
                    }
                    if (decodedPayload.application.tier != null) {
                        authenticationContext.applicationTier = decodedPayload.application.tier.toString
                        ();
                    }
                    authenticationContext.subscriber = decodedPayload.application.owner.toString();
                    authenticationContext.consumerKey = decodedPayload.consumerKey.toString();
                    authenticationContext.keyType = decodedPayload.keytype.toString();
                    runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = authenticationContext.
                    keyType;
                    filterContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                    return true;
                }
                int index = 0;
                while (index < l) {
                    var subscription = subscribedAPIList[index];
                    string apiName="";
                    string apiVersion="";
                    if(apiConfig is APIConfiguration){
                        apiName= apiConfig.name;
                        if(apiConfig?.apiVersion is string){
                            apiVersion = apiConfig?.apiVersion;
                        }
                    }
                    if (subscription.name.toString() == apiName &&
                        subscription.'version.toString() == apiVersion) {
                        printDebug(KEY_SUBSCRIPTION_FILTER, "Found a matching subscription with name:" +
                                subscription.name.toString() + " version:" + subscription.'version.
                                toString());
                        authenticationContext.authenticated = true;
                        authenticationContext.tier = subscription.subscriptionTier.toString();
                        authenticationContext.apiKey = jwtToken;
                        authenticationContext.username = decodedPayload.sub.toString();
                        authenticationContext.callerToken = jwtToken;
                        authenticationContext.applicationId = decodedPayload.application.id.toString();
                        authenticationContext.applicationName = decodedPayload.application.name.toString
                        ();
                        authenticationContext.applicationTier = decodedPayload.application.tier.toString
                        ();
                        authenticationContext.subscriber = decodedPayload.application.owner.toString();
                        authenticationContext.consumerKey = decodedPayload.consumerKey.toString();
                        authenticationContext.apiTier = subscription.subscriptionTier.toString();
                        authenticationContext.apiPublisher = subscription.publisher.toString();
                        authenticationContext.subscriberTenantDomain = subscription
                        .subscriberTenantDomain.toString();
                        authenticationContext.keyType = decodedPayload.keytype.toString();
                        // setting keytype to invocationContext
                        printDebug(KEY_SUBSCRIPTION_FILTER, "Setting key type as " +
                                authenticationContext.keyType);
                        runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = authenticationContext
                        .keyType;
                        filterContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                        printDebug(KEY_SUBSCRIPTION_FILTER, "Subscription validation success.");
                        return true;
                    }
                    index = index + 1;
                }
            } else {
                authenticationContext.authenticated = true;
                authenticationContext.apiKey = jwtToken;
                authenticationContext.username = decodedPayload.sub.toString();
                runtime:getInvocationContext().attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
                filterContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                return true;
            }
            setErrorMessageToFilterContext(filterContext, API_AUTH_FORBIDDEN);
            sendErrorResponse(caller, request, filterContext);
            return false;
        } else {
            log:printError("Error occurred while decoding the JWT token  : " +
                    jwtToken, err = decodedPayload);
            setErrorMessageToFilterContext(filterContext, API_AUTH_GENERAL_ERROR);
            sendErrorResponse(caller, request, filterContext);
            return false;
        }
    }

}
