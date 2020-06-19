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
import ballerina/io;

# Data holder for Application details available in Gateway Pilot node. 
# 
# + applications - Map of `Application` objects
type ApplicationDataStore object {
    //Tenant wise applications map
    map<map<Application>> applications = {};

    private string pilotUsername;
    private string pilotPassword;
    private string serviceContext;
    private string[]|error listOfTenants;

    public function __init(string username, string password, string context, string[]|error listOfTenants) {
        self.pilotUsername = username;
        self.pilotPassword = password;
        self.serviceContext = context + "/applications";
        self.listOfTenants = listOfTenants;
        self.fetchApplications();
    }

    # Retrieve a specific `Application` object from the Applicatio Data Store.
    # 
    # + appId - Application Id of the required `Application`
    # + return - `Application` with provided `appId`. If no match was found `()` is returned.
    function getApplication(string tenantDomain, string appId) returns (Application | ()) {
        if (self.applications.hasKey(tenantDomain) && self.applications.get(tenantDomain).hasKey(appId)) {
            return self.applications.get(tenantDomain).get(appId);
        }
        return ();
    }

    function addApplication(string tenantDomain, Application app) {
        map<Application> applicationMap;
        string appKey = app.id.toString();
        if (!self.applications.hasKey(tenantDomain)) {
            applicationMap = {};
            applicationMap[appKey] = app;
        } else {
            applicationMap = self.applications.get(tenantDomain);
            applicationMap[appKey] = app;
        }
        self.applications[tenantDomain] = applicationMap;
    }

    function removeApplication(string tenantDomain, Application app) {
        Application removedApp = self.applications.get(tenantDomain).remove(app.id.toString());
    }

    private function fetchApplications() {
        io:println("application fetch is called");
        string basicAuthHeader = buildBasicAuthHeader(self.pilotUsername, self.pilotPassword);
        http:Request appReq = new;
        appReq.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);
        var tenantList = self.listOfTenants;
        io:println("tenantList : " + tenantList.toString());
        if (tenantList is string[]) {
            io:println("inside tenant list");
            foreach string tenant in tenantList {
                io:println("tnenat : " + tenant);
                appReq.setHeader(EVENT_HUB_TENANT_HEADER, tenant);
                var response = gatewayPilotEndpoint->get(self.serviceContext, message = appReq);
                io:println(response);
                if (response is http:Response) {
                    map<Application> applicationMap = {};
                    var payload = response.getJsonPayload();
                    if (payload is json) {
                        io:println(KEY_APPLICATION_STORE, "Application list of tenant : " + tenant + " is : " + payload.toJsonString());
                        printDebug(KEY_APPLICATION_STORE, "Application list of tenant : " + tenant + " is : " + payload.toJsonString());
                        json[] list = <json[]>payload.list;
                        printDebug(KEY_APPLICATION_STORE, "Received valid application details");
                        foreach json jsonApp in list {
                            Application app = {
                                id: <int>jsonApp.id,
                                owner: jsonApp.subName.toString(),
                                name: jsonApp.name.toString(),
                                policyId: jsonApp.policy.toString(),
                                tokenType: jsonApp.tokenType.toString(),
                                groupIds: <json[]>jsonApp.groupIds,
                                attributes: <json[]>jsonApp.attributes
                            };
                            string appKey = app.id.toString();
                            applicationMap[appKey] = app;
                        }
                        self.applications[tenant] = applicationMap;
                    } else {
                        printError(KEY_APPLICATION_STORE, "Received invalid application data", payload);
                    }
                } else {
                    printError(KEY_APPLICATION_STORE, "Failed to retrieve application data", response);
                }
            }
            io:println("Tenant application map : " + self.applications.toString());
        } else {
            printError(KEY_APPLICATION_STORE, "Error while reading tenant list map from config.", tenantList);
        }
    }
};
