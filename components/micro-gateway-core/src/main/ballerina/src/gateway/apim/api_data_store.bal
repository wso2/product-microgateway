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
    map<Api> apis = {};

    private string pilotUsername;
    private string pilotPassword;
    private string serviceContext;

    public function __init(string username, string password, string context) {
        self.pilotUsername = username;
        self.pilotPassword = password;
        self.serviceContext = context + "/apis";

        self.fetchApis();
    }

    # Retrieve a specific `Api` object from the Api Data Store.
    # 
    # + apiKey - api key in the format of `provider:name:version`
    # + return - `Api` object mapping with provided attributes. If no match was found `()` is returned.
    function getApi(string apiKey) returns (Api | ()) {
        if (!self.apis.hasKey(apiKey)) {
            return ();
        }

        return self.apis.get(apiKey);
    }

    private function fetchApis() {
        string basicAuthHeader = buildBasicAuthHeader(self.pilotUsername, self.pilotPassword);
        http:Request apiReq = new;
        apiReq.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);
        var response = gatewayPilotEndpoint->get(self.serviceContext, message = apiReq);

        if (response is http:Response) {
            var payload = response.getJsonPayload();

            if (payload is json) {
                json[] list = <json[]>payload.list;
                printDebug(KEY_API_STORE, "Received valid api details");

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
                    string apiKey = api.provider + ":" + api.name + ":" + api.apiVersion;
                    self.apis[apiKey] = api;
                }
            } else {
                printError(KEY_API_STORE, "Received invalid api data", payload);
            }
        } else {
            printError(KEY_API_STORE, "Failed to retrieve api data", response);
        }
    }
};
