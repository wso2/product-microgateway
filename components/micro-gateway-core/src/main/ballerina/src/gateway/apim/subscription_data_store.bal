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
        if (self.subscriptions.hasKey(subKey)) {
            int earlierTimestamp = self.subscriptions.get(subKey).timestamp ;
            // When subscription added two events are received for ON_HOLD and UNBLOCKED. If these jms event are
            //received in mixed up order then subscription validation will fail. Hence we need to ignore the events
            //that are coming in wrong order based on the timestamp.
            if (earlierTimestamp > 0 && earlierTimestamp > sub.timestamp) {
                return;
            }
        }
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

    function loadSubscriptionFromService(int apiId, int appId) returns Subscription? {
        string basicAuthHeader = buildBasicAuthHeader(self.pilotUsername, self.pilotPassword);
        http:Request apiReq = new;
        apiReq.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);
        string serviceContext = self.serviceContext + "?apiId=" + apiId.toString() + "&appId=" + appId.toString();
        var response = gatewayPilotEndpoint->get(serviceContext, message = apiReq);
        if (response is http:Response) {
            var payload = response.getJsonPayload();
            if (payload is json) {
                printDebug(KEY_SUBSCRIPTION_STORE, "Subscription list for api Id : " + apiId.toString()
                                    + " and application Id : " + appId.toString() + " is : " + payload.toJsonString());
                json[] list = <json[]>payload.list;
                if (list.length() > 0 ) {
                    Subscription sub = {
                        id: <int>list[0].subscriptionId,
                        apiId: <int>list[0].apiId,
                        appId: <int>list[0].appId,
                        policyId: list[0].policyId.toString(),
                        state: list[0].subscriptionState.toString()
                    };
                    string subKey = appId.toString() + ":" + apiId.toString();
                    self.subscriptions[subKey] = <@untainted>sub;
                    printDebug(KEY_SUBSCRIPTION_STORE, "Returned subscription from service is : " + sub.toString());
                    return <@untainted>sub;
                }
            } else {
              printError(KEY_SUBSCRIPTION_STORE, "Received invalid subscription data for api id : " + apiId.toString()
                                        + " and app id : " + appId.toString(), payload);
            }
        } else {
          printError(KEY_SUBSCRIPTION_STORE, "Failed to retrieve subscription data for api id : " + apiId.toString()
                                     + " and app id : " + appId.toString(), response);
        }
        return ();
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
