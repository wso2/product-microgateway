// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Data holder for Subscription details available in Gateway Pilot node.
# 
# + subscriptions - Map of `Subscription` objects
type SubscriptionDataStore object {
    map<map<Subscription>> subscriptions = {};

    private string pilotUsername;
    private string pilotPassword;
    private string serviceContext;
    private string[]|error listOfTenants;

    public function __init(string username, string password, string context, string[]|error listOfTenants) {
        self.pilotUsername = username;
        self.pilotPassword = password;
        self.serviceContext = context + "/subscriptions";
        self.listOfTenants = listOfTenants;
        if (apimEventHubEnabled) {
            future<()> susbcriptionsFetch = start self.fetchSubscriptions();
        }
    }

    # Retrieve a specific `Subscription` object from the Subscription Data Store.
    # + tenantDomain - Tenant domain of the subscriber
    # + subKey - A subscription key in the format of `applicationId:apiId`
    # + return - `Subscription` object for `appId` and `apiId`. If no match was found `()` is returned
    function getSubscription(string tenantDomain, string subKey) returns (Subscription | ()) {
        if (self.subscriptions.hasKey(tenantDomain) && self.subscriptions.get(tenantDomain).hasKey(subKey)) {
            return self.subscriptions.get(tenantDomain).get(subKey);
        }
        return ();
    }

    function addSubscription(string tenantDomain, Subscription sub) {
        map<Subscription> subscriptionMap;
        string subKey = sub.appId.toString() + ":" + sub.apiId.toString();
        if (!self.subscriptions.hasKey(tenantDomain)) {
            subscriptionMap = {};
            subscriptionMap[subKey] = sub;
        } else {
            subscriptionMap = self.subscriptions.get(tenantDomain);
            subscriptionMap[subKey] = sub;
        }
        lock {
            self.subscriptions[tenantDomain] = subscriptionMap;
        }
    }

    function removeSubscription(string tenantDomain, Subscription sub) {
        string subKey = sub.appId.toString() + ":" + sub.apiId.toString();
        lock {
            Subscription removeSub = self.subscriptions.get(tenantDomain).remove(subKey.toString());
        }
    }

    private function fetchSubscriptions() {
        string basicAuthHeader = buildBasicAuthHeader(self.pilotUsername, self.pilotPassword);
        http:Request subReq = new;
        subReq.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);
        var tenantList = self.listOfTenants;
        if (tenantList is string[]) {
            foreach string tenant in tenantList {
                var response = gatewayPilotEndpoint->get(self.serviceContext, message = subReq);
                subReq.setHeader(EVENT_HUB_TENANT_HEADER, tenant);
                if (response is http:Response) {
                    map<Subscription> subscriptionnMap = {};
                    var payload = response.getJsonPayload();
                    if (payload is json) {
                        json[] list = <json[]>payload.list;
                        printDebug(KEY_SUBSCRIPTION_STORE, "Subscription list of tenant : " + tenant + " is : " + payload.toJsonString());
                        foreach json jsonSub in list {
                            Subscription sub = {
                                id: <int>jsonSub.subscriptionId,
                                apiId: <int>jsonSub.apiId,
                                appId: <int>jsonSub.appId,
                                policyId: jsonSub.policyId.toString(),
                                state: jsonSub.subscriptionState.toString()
                            };
                            string subKey = sub.appId.toString() + ":" + sub.apiId.toString();
                            subscriptionnMap[subKey] = sub;
                        }
                        self.subscriptions[tenant] = subscriptionnMap;
                    } else {
                        printError(KEY_SUBSCRIPTION_STORE, "Received invalid subscription data", payload);
                    }
                } else {
                    printError(KEY_SUBSCRIPTION_STORE, "Failed to retrieve subscription data", response);
                }
            }
        }
    }

};
