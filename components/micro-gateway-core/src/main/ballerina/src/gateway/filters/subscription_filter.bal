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
import ballerina/jwt;
import ballerina/runtime;

// Subscription filter to validate the subscriptions which is available in the  jwt token
// This filter should only be engaged when jwt token is is used for authentication. For oauth2
// OAuthnFilter will handle the subscription validation as well.
public type SubscriptionFilter object {

    public boolean subsciptionEnabled = getConfigBooleanValue(JWT_INSTANCE_ID, VALIDATE_SUBSCRIPTION, false);

    public function filterRequest(http:Caller caller, http:Request request, @tainted http:FilterContext filterContext)
                        returns boolean {
        int startingTime = getCurrentTime();
        checkOrSetMessageID(filterContext);
        boolean result = doSubscriptionFilterRequest(caller, request, filterContext, self.subsciptionEnabled);
        setLatency(startingTime, filterContext, SECURITY_LATENCY_SUBS);
        return result;
    }



    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        return true;
    }
};

function doSubscriptionFilterRequest(http:Caller caller, http:Request request,
        @tainted http:FilterContext filterContext, boolean subsciptionEnabled) returns boolean {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    runtime:AuthenticationContext? authContext= runtime:getInvocationContext()?.authenticationContext;
    if (authContext is runtime:AuthenticationContext) {
        string? authScheme = authContext?.scheme;
        if (authScheme is string) {
            printDebug(KEY_SUBSCRIPTION_FILTER, "Auth scheme: " + authScheme);
            if (authScheme != AUTH_SCHEME_JWT) {
                printDebug(KEY_SUBSCRIPTION_FILTER, "Skipping since auth scheme != jwt.");
                return true;
            }
        } else {
            printWarn(KEY_SUBSCRIPTION_FILTER, "Auth schema was not defined in the authentication context");
            return true;
        }


        string? jwtToken = authContext?.authToken;
        string currentAPIContext = getContext(filterContext);
        boolean subscriptionValidated = false;
        AuthenticationContext authenticationContext = {};
        json|error decodedPayload = {};
        if (jwtToken is string) {
            var cachedJwt = trap <jwt:CachedJwt>jwtCache.get(jwtToken);
            if (cachedJwt is jwt:CachedJwt) {
                printDebug(KEY_SUBSCRIPTION_FILTER, "jwt found from the jwt cache");
                jwt:JwtPayload jwtPayload = cachedJwt.jwtPayload;
                json payload = {};
                if (payload is map<json>) {
                    map<json>? customClaims = jwtPayload?.customClaims;
                    if (customClaims is map<json>) {
                        if (customClaims.hasKey(APPLICATION)) {
                            payload["application"] = customClaims[APPLICATION];
                        }
                        if (customClaims.hasKey(SUBSCRIBED_APIS)) {
                            payload["subscribedAPIs"] = customClaims[SUBSCRIBED_APIS];
                        }
                        if (customClaims.hasKey(CONSUMER_KEY)) {
                            payload["consumerKey"] = customClaims[CONSUMER_KEY];
                        }
                        if (customClaims.hasKey(KEY_TYPE)) {
                            payload["keytype"] = customClaims[KEY_TYPE];
                        }
                    }
                    if (jwtPayload?.sub is string) {
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


            if (decodedPayload is json) {
                printTrace(KEY_SUBSCRIPTION_FILTER, "Decoded JWT payload: " + decodedPayload.toString());
                json[] subscribedAPIList = [];
                authenticationContext.apiKey = jwtToken;
                authenticationContext.username = decodedPayload.sub.toString();
                authenticationContext.callerToken = jwtToken;
                json|error application = decodedPayload.application;
                if (application is map<json>) {
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
                    if (decodedPayload.application.owner != null) {
                        authenticationContext.subscriber = decodedPayload.application.owner.toString();
                    }
                }
                if (decodedPayload.consumerKey != null) {
                    authenticationContext.consumerKey = decodedPayload.consumerKey.toString();
                }
                if (decodedPayload.keytype != null) {
                    authenticationContext.keyType = decodedPayload.keytype.toString();
                    invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
                    printDebug(KEY_SUBSCRIPTION_FILTER, "Setting key type as " +
                                                        authenticationContext.keyType);
                }
                json|error jsonSubscribedApis = decodedPayload.subscribedAPIs;
                if (jsonSubscribedApis is json) {
                    printDebug(KEY_SUBSCRIPTION_FILTER, "subscribedAPIs claim found in the jwt");
                    if (jsonSubscribedApis is json[]) {
                    subscribedAPIList = jsonSubscribedApis;
                    }
                    printDebug(KEY_SUBSCRIPTION_FILTER, "Subscribed APIs list : " + subscribedAPIList.toString());
                    APIConfiguration? apiConfig = apiConfigAnnotationMap[filterContext.getServiceName()];
                    int l = subscribedAPIList.length();
                    int index = 0;
                    while (index < l) {
                        var subscription = subscribedAPIList[index];
                        string apiName="";
                        string apiVersion="";
                        if (apiConfig is APIConfiguration) {
                            apiName= apiConfig.name;
                            apiVersion = apiConfig?.apiVersion;
                        }
                        if (subscription.name.toString() == apiName &&
                                            subscription.'version.toString() == apiVersion) {
                            printDebug(KEY_SUBSCRIPTION_FILTER, "Found a matching subscription with name:" +
                                    subscription.name.toString() + " version:" + subscription.'version.
                                    toString());
                            subscriptionValidated = true;
                            authenticationContext.authenticated = true;
                            authenticationContext.tier = subscription.subscriptionTier.toString();
                            authenticationContext.apiTier = subscription.subscriptionTier.toString();
                            authenticationContext.apiPublisher = subscription.publisher.toString();
                            authenticationContext.subscriberTenantDomain = subscription
                            .subscriberTenantDomain.toString();
                            invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                            printDebug(KEY_SUBSCRIPTION_FILTER, "Subscription validation success.");
                            return true;
                        }
                        index+=1;
                    }
                    invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                    if (subsciptionEnabled && !subscriptionValidated) {
                        setErrorMessageToFilterContext(filterContext, API_AUTH_FORBIDDEN);
                        sendErrorResponse(caller, request, <@untainted>filterContext);
                        return false;
                    }
                } else {
                    printDebug(KEY_SUBSCRIPTION_FILTER, "subscribedAPIs claim not found in the jwt");
                    if (subsciptionEnabled) {
                        setErrorMessageToFilterContext(filterContext, API_AUTH_FORBIDDEN);
                        sendErrorResponse(caller, request, <@untainted>filterContext);
                        return false;
                    }
                    authenticationContext.authenticated = true;
                    invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                    return true;
                }

            } else {
                log:printError("Error occurred while decoding the JWT token  : " +
                        jwtToken, err = decodedPayload);
                setErrorMessageToFilterContext(filterContext, API_AUTH_GENERAL_ERROR);
                sendErrorResponse(caller, request, <@untainted>filterContext);
                return false;
            }
        }
    }
    return true;

}

