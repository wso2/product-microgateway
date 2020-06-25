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

# Data holder for Api details available in Gateway Pilot node.
# 
# + apis - Map of `Api` objects
type ApiDataStore object {
    map<map<Api>> apis = {};

    private string pilotUsername;
    private string pilotPassword;
    private string serviceContext;
    private string[]|error listOfTenants;

    public function __init(string username, string password, string context, string[]|error listOfTenants) {
        self.pilotUsername = username;
        self.pilotPassword = password;
        self.serviceContext = context + "/apis";
        self.listOfTenants = listOfTenants;

        if (apimEventHubEnabled) {
            future<()> apisFetch = start self.fetchApis();
        }
    }

    # Retrieve a specific `Api` object from the Api Data Store.
    #
    # + tenantDomain - Tenan t domaain to which the API belongs to
    # + apiKey - api key in the format of `provider:name:version`
    # + return - `Api` object mapping with provided attributes. If no match was found `()` is returned.
    function getApi(string tenantDomain, string apiKey) returns (Api | ()) {
        if (self.apis.hasKey(tenantDomain) && self.apis.get(tenantDomain).hasKey(apiKey)) {
            return self.apis.get(tenantDomain).get(apiKey);
        }
        return ();

    }

    function addApi(string tenantDomain, Api api) {
        map<Api> apiMap;
        string apiKey = api.name + ":" + api.apiVersion;
        if (!self.apis.hasKey(tenantDomain)) {
            apiMap = {};
            apiMap[apiKey] = api;
        } else {
            apiMap = self.apis.get(tenantDomain);
            apiMap[apiKey] = api;
        }
        lock {
            //Writing event should be locked, due to worker threads are reading the map during request validations
            self.apis[tenantDomain] = apiMap;
        }
    }

    function removeApi(string tenantDomain, Api api) {
        string apiKey = api.name + ":" + api.apiVersion;
        lock {
            //Remove event should be locked, due to worker threads are reading the map during request validations
            Api removedApi = self.apis.get(tenantDomain).remove(apiKey);
        }
    }

    private function fetchApis() {
        string basicAuthHeader = buildBasicAuthHeader(self.pilotUsername, self.pilotPassword);
        http:Request apiReq = new;
        apiReq.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);
        var tenantList = self.listOfTenants;
        if (tenantList is string[]) {
            foreach string tenant in tenantList {
                apiReq.setHeader(EVENT_HUB_TENANT_HEADER, tenant);
                var response = gatewayPilotEndpoint->get(self.serviceContext, message = apiReq);
                if (response is http:Response) {
                    map<Api> apiMap = {};
                    var payload = response.getJsonPayload();
                    if (payload is json) {
                        printDebug(KEY_API_STORE, "API list of tenant : " + tenant + " is : " + payload.toJsonString());
                        json[] list = <json[]>payload.list;
                        foreach json jsonApi in list {
                            // TODO: substore: Map URL mapping attribute
                            Api api = {
                                id: <int>jsonApi.apiId,
                                provider: jsonApi.provider.toString(),
                                name: jsonApi.name.toString(),
                                apiVersion: jsonApi.'version.toString(),
                                context: jsonApi.context.toString(),
                                policyId: jsonApi.policy.toString()
                            };
                            string apiKey = api.name + ":" + api.apiVersion;
                            apiMap[apiKey] = api;
                        }
                        self.apis[tenant] = apiMap;
                    } else {
                        printError(KEY_API_STORE, "Received invalid api data", payload);
                    }
                } else {
                    printError(KEY_API_STORE, "Failed to retrieve api data", response);
                }
            }
        } else {
            printError(KEY_APPLICATION_STORE, "Error while reading tenant list map from config.", tenantList);
        }
    }
};
