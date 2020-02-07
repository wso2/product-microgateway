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

import ballerina/http;
import ballerina/jwt;
import ballerina/runtime;

// Subscription filter to validate the subscriptions which is available in the  jwt token
// This filter should only be engaged when jwt token is is used for authentication. For oauth2
// OAuthnFilter will handle the subscription validation as well.
public type SubscriptionFilter object {
    public function filterRequest(http:Caller caller, http:Request request,@tainted http:FilterContext filterContext)
    returns boolean {
        if (filterContext.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>filterContext.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_SUBSCRIPTION_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
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

function doSubscriptionFilterRequest(http:Caller caller, http:Request request,
@tainted http:FilterContext filterContext) returns boolean {
    boolean subscriptionValidated = false;
    boolean subscriptionValEnabled = getConfigBooleanValue(JWT_INSTANCE_ID, VALIDATE_SUBSCRIPTION, false);

    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    runtime:AuthenticationContext? authContext = runtime:getInvocationContext()?.authenticationContext;
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
        //get payload
        string? jwtToken = authContext?.authToken;
        if (jwtToken is string) {
            (jwt:JwtPayload | error) payload = getDecodedJWTPayload(jwtToken);
            if (payload is jwt:JwtPayload) {
                json subscribedAPIList = [];
                map<json>? customClaims = payload?.customClaims;
                //get allowed apis
                if (customClaims is map<json> && customClaims.hasKey(SUBSCRIBED_APIS)) {
                    printDebug(KEY_SUBSCRIPTION_FILTER, "subscribedAPIs claim found in the jwt");
                    subscribedAPIList = customClaims.get(SUBSCRIBED_APIS);
                }
                if (subscribedAPIList is json[]) {
                    if (subscriptionValEnabled && subscribedAPIList.length() < 1) {
                        printError(KEY_SUBSCRIPTION_FILTER, "subscribedAPI list is empty");
                        setErrorMessageToFilterContext(filterContext, API_AUTH_FORBIDDEN);
                        sendErrorResponse(caller, request, <@untainted>filterContext);
                        return false;
                    }
                    subscriptionValidated = handleSubscribedAPIs(jwtToken, payload, subscribedAPIList,
                        subscriptionValEnabled);
                    if (subscriptionValidated || !subscriptionValEnabled) {
                        printDebug(KEY_SUBSCRIPTION_FILTER, "Subscriptions validated.");
                        return true;
                    } else {
                        printError(KEY_SUBSCRIPTION_FILTER, "Subscriptions validation fails.");
                        setErrorMessageToFilterContext(filterContext, API_AUTH_FORBIDDEN);
                        sendErrorResponse(caller, request, <@untainted>filterContext);
                        return false;
                    }
                }
            }
            printError(KEY_SUBSCRIPTION_FILTER, "Failed to decode the JWT");
            setErrorMessageToFilterContext(filterContext, API_AUTH_GENERAL_ERROR);
            sendErrorResponse(caller, request, <@untainted>filterContext);
            return false;
        }
    }
    return true;
}
