// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/runtime;

function convertApplicationEventToApplicationDTO(json appEvent) returns Application {
    Application application = {
        id : <int>appEvent.applicationId,
        name: appEvent.applicationName.toString(),
        tenantDomain : appEvent.tenantDomain.toString(),
        policyId : appEvent.applicationPolicy.toString(),
        tokenType : appEvent.tokenType.toString()
    };
    return application;
}

function convertSubscriptionEventToSubscriptionDTO(json subEvent) returns Subscription {
    Subscription subscription = {
        id : <int>subEvent.subscriptionId,
        apiId: <int>subEvent.apiId,
        appId: <int>subEvent.applicationId,
        tenantDomain : subEvent.tenantDomain.toString(),
        policyId : (subEvent.policyId is error) ? "" : subEvent.policyId.toString(),
        state : subEvent.subscriptionState.toString(),
        timestamp : <int>subEvent.timeStamp
    };
    return subscription;
}

function convertKeyGenerationEventToKeyMapDTO(json keyEvent) returns KeyMap {
    KeyMap keyMapping = {
       appId :  <int>keyEvent.applicationId,
       consumerKey : keyEvent.consumerKey.toString(),
       keyType : keyEvent.keyType.toString(),
       tenantDomain : keyEvent.tenantDomain.toString()
    };
    return keyMapping;
}

function convertApiEventToApiDTO(json apiEvent) returns Api {
    Api api = {
        id : <int>apiEvent.apiId,
        provider: apiEvent.apiProvider.toString(),
        name: apiEvent.apiName.toString(),
        tenantDomain : apiEvent.tenantDomain.toString(),
        apiVersion : apiEvent.apiVersion.toString(),
        context : apiEvent.apiContext.toString()
    };
    return api;
}

function validateSubscriptionFromDataStores(string token, string consumerKey, string apiName, string apiVersion,
                    boolean isValidateSubscription) returns ([AuthenticationContext, boolean]) {
    boolean isAllowed = !isValidateSubscription;
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    invocationContext.attributes[KEY_TYPE_ATTR] = PRODUCTION_KEY_TYPE;
    AuthenticationContext authenticationContext = {
        apiKey: token,
        authenticated: !isValidateSubscription
    };
    authenticationContext.consumerKey = consumerKey;
    string subscriptionKey = consumerKey + ":" + apiName + ":"  + apiVersion;
    string apiContext = invocationContext.attributes[API_CONTEXT].toString();
    string apiTenantDomain = getTenantFromBasePath(apiContext);
    if (apimEventHubEnabled) {
        var keyMap = pilotDataProvider.getKeyMapping(consumerKey);
        var api = pilotDataProvider.getApi(apiName, apiVersion);

        if (keyMap is ()) {
            printDebug(KEY_PILOT_UTIL, "Key mapping for consumer key : " + consumerKey + " is missing in the data store.");
            keyMap = pilotDataProvider.loadKeyMappingFromService(<@untainted>consumerKey);
        }
        if (keyMap is KeyMap) {
            authenticationContext.keyType = keyMap.keyType;
            invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
            var app = pilotDataProvider.getApplication(keyMap.appId);
            if (app is ()) {
                printDebug(KEY_PILOT_UTIL, "Application with id : " + keyMap.appId.toString() + " is missing in the data store.");
                app = pilotDataProvider.loadAppplicationFromService(<@untainted>keyMap.appId);
            }
            if (app is Application) {
                authenticationContext.applicationId = app.id.toString();
                authenticationContext.applicationName = app.name;
                authenticationContext.applicationTier = app.policyId;
                authenticationContext.subscriber = app.owner;

                if (api is ()) {
                    printDebug(KEY_PILOT_UTIL, "API : " + apiName + ":" + apiVersion + " is missing in the data store.");
                    api = pilotDataProvider.loadApiFromService(apiContext, apiVersion);
                }
                if (api is Api) {
                    var sub = pilotDataProvider.getSubscription(keyMap.appId, api.id);
                    if (sub is ()) {
                        printDebug(KEY_PILOT_UTIL, "Subscription for API : " + apiName + ":" + apiVersion
                            + " for the application : " + app.name + " is missing in the data store.");
                        sub = pilotDataProvider.loadSubscriptionFromService(api.id, <@untainted>keyMap.appId);
                    }

                    // if subscription in "UNBLOCKED" state is found in the pilot data, key is allowed
                    if (sub is Subscription) {
                        if (sub.state == "UNBLOCKED" || sub.state == "TIER_UPDATE_PENDING") {
                            printDebug(KEY_PILOT_UTIL, "Found a subscription for api: " + apiName + "__" + apiVersion
                            + " from the application : " + app.name);
                            isAllowed = true;
                            authenticationContext.authenticated = true;
                            authenticationContext.apiPublisher = api.provider;
                            authenticationContext.tier = sub.policyId;
                            setSubsciberTenantDomain(authenticationContext);
                        } else if (sub.state == "ON_HOLD") {
                            printError(KEY_PILOT_UTIL,"Subscription is in inactive state for API : " + apiName + "__" + apiVersion +
                                                " for the application : " +  authenticationContext.applicationName);
                            setErrorMessageToInvocationContext(SUBSCRIPTION_INACTIVE);
                        } else if (sub.state == "BLOCKED") {
                            printError(KEY_PILOT_UTIL,"Subscription is blocked for API : " + apiName + "__" + apiVersion +
                                                " for the application : " +  authenticationContext.applicationName);
                            setErrorMessageToInvocationContext(API_BLOCKED);
                        } else if(sub.state == "PROD_ONLY_BLOCKED" && invocationContext.attributes[KEY_TYPE_ATTR].toString() == PRODUCTION_KEY_TYPE) {
                            printError(KEY_PILOT_UTIL,"Subscription is blocked for production only for API : " + apiName + "__" + apiVersion +
                                                                        " for the application : " +  authenticationContext.applicationName);
                            setErrorMessageToInvocationContext(API_BLOCKED);
                        } else {
                            printError(KEY_PILOT_UTIL,"Subscription  found for API : " + apiName + "__" + apiVersion +
                            " for the application : " +  authenticationContext.applicationName + ". But the subscription state is : " + sub.state);
                            setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
                        }
                    } else {
                        printError(KEY_PILOT_UTIL,"Subscription not found for API : " + apiName + "__" + apiVersion +
                        " for the application : " +  authenticationContext.applicationName);
                        setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
                    }
                } else {
                    printError(KEY_PILOT_UTIL, "API not found for name : " + apiName + " and version : " + apiVersion);
                    setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
                }
            } else {
                printError(KEY_PILOT_UTIL, "Application not found for consumer key : " + consumerKey + " and app Id : " + keyMap.appId.toString());
                setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
            }
        } else {
            printError(KEY_PILOT_UTIL, "Key mapping not found for consumer key : " + consumerKey);
            setErrorMessageToInvocationContext(API_AUTH_FORBIDDEN);
        }
    } else {
        printDebug(KEY_PILOT_UTIL, "API envent hub is disabled. Can not fetch subscription data.");
    }
    return [authenticationContext,isAllowed];
}