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
    map<Subscription> subscriptions = {};

    private string pilotUsername;
    private string pilotPassword;
    private string serviceContext;

    public function __init(string username, string password, string context) {
        self.pilotUsername = username;
        self.pilotPassword = password;
        self.serviceContext = context + "/subscriptions";
        if (apimEventHubEnabled) {
            future<()> susbcriptionsFetch = start self.fetchSubscriptions();
        }
    }

    # Retrieve a specific `Subscription` object from the Subscription Data Store.
    # + subKey - A subscription key in the format of `applicationId:apiId`
    # + return - `Subscription` object for `appId` and `apiId`. If no match was found `()` is returned
    function getSubscription(string subKey) returns (Subscription | ()) {
        if (self.subscriptions.hasKey(subKey)) {
            return self.subscriptions.get(subKey);
        }
        return ();
    }

    function addSubscription(Subscription sub) {
        string subKey = sub.appId.toString() + ":" + sub.apiId.toString();
        lock {
            self.subscriptions[subKey] = sub;
        }
    }

    function removeSubscription(Subscription sub) {
        string subKey = sub.appId.toString() + ":" + sub.apiId.toString();
        lock {
            Subscription removeSub = self.subscriptions.remove(subKey.toString());
        }
    }

    private function fetchSubscriptions() {
        string basicAuthHeader = buildBasicAuthHeader(self.pilotUsername, self.pilotPassword);
        http:Request subReq = new;
        subReq.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);
        var response = gatewayPilotEndpoint->get(self.serviceContext, message = subReq);
        if (response is http:Response) {
            map<Subscription> subscriptionnMap = {};
            var payload = response.getJsonPayload();
            if (payload is json) {
                json[] list = <json[]>payload.list;
                printDebug(KEY_SUBSCRIPTION_STORE, "Subscription list is : " + payload.toJsonString());
                foreach json jsonSub in list {
                    Subscription sub = {
                        id: <int>jsonSub.subscriptionId,
                        apiId: <int>jsonSub.apiId,
                        appId: <int>jsonSub.appId,
                        policyId: jsonSub.policyId.toString(),
                        state: jsonSub.subscriptionState.toString()
                    };
                    string subKey = sub.appId.toString() + ":" + sub.apiId.toString();
                    self.subscriptions[subKey] = sub;
                }
            } else {
                printError(KEY_SUBSCRIPTION_STORE, "Received invalid subscription data", payload);
            }
        } else {
            printError(KEY_SUBSCRIPTION_STORE, "Failed to retrieve subscription data", response);
        }
    }

};
