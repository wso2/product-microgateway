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
APIGatewayCache gatewayCacheObject = new;

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
        policyId : subEvent.policyId.toString(),
        state : subEvent.subscriptionState.toString()
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
    string subscriptionKey = consumerKey + ":" + apiName + ":"  + apiVersion;
    string apiTenantDomain = getTenantFromBasePath(invocationContext.attributes[API_CONTEXT].toString());
    var keyMap = pilotDataProvider.getKeyMapping(consumerKey);
    var api = pilotDataProvider.getApi(apiName, apiVersion);
    AuthenticationContext authenticationContext = {
        apiKey: token,
        authenticated: !isValidateSubscription
    };
    authenticationContext.consumerKey = consumerKey.toString();
    invocationContext.attributes[KEY_TYPE_ATTR] = PRODUCTION_KEY_TYPE;
    //check from subscription cache
    var authContext = gatewayCacheObject.retrieveFromSubcriptionCache(subscriptionKey);
    if (authContext is AuthenticationContext) {
        printDebug(JWT_UTIL, "Subcription key found from the cache : " + subscriptionKey);
        authenticationContext = authContext;
        invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
        isAllowed = true;
    } else {
        if (keyMap is KeyMap) {
            authenticationContext.keyType = keyMap.keyType;
            invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
            var app = pilotDataProvider.getApplication(keyMap.appId);
            if (app is Application) {
                authenticationContext.applicationId = app.id.toString();
                authenticationContext.applicationName = app.name;
                authenticationContext.applicationTier = app.policyId;
                authenticationContext.subscriber = app.owner;
            } else {
                printError(JWT_UTIL, "Application not found for consumer key : " + consumerKey + " and app Id : " + keyMap.appId.toString());
            }

            if (api is Api) {
                var sub = pilotDataProvider.getSubscription(keyMap.appId, api.id);

                // if subscription in "UNBLOCKED" state is found in the pilot data, key is allowed
                if (sub is Subscription && sub.state == "UNBLOCKED") {
                    printDebug(JWT_UTIL, "Found a subscription for api: " + apiName + "__" + apiVersion);
                    isAllowed = true;
                    authenticationContext.authenticated = true;
                    authenticationContext.apiPublisher = api.provider;
                    authenticationContext.tier = sub.policyId;
                    setSubsciberTenantDomain(authenticationContext);

                    gatewayCacheObject.addToSubcriptionCache(subscriptionKey, authenticationContext);
                } else {
                    printError(JWT_UTIL,"Subscription not found for API : " + apiName + "__" + apiVersion +
                    " for the application : " +  authenticationContext.applicationName);
                }
            } else {
                printError(JWT_UTIL, "API not found for name : " + apiName + " and version : " + apiVersion);
            }
        } else {
            printError(JWT_UTIL, "Key mapping not found for consumer key : " + consumerKey);
        }
    }

    return [authenticationContext,isAllowed];
}