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

# Provides a common interface to interact with Gateway Pilot data stores.
# Initializing an instance of this object will initialize all pilot data stores
# resulting multiple network calls to Gateway pilot note. Therefore its recommended
# to use a shared single instance of this object within the module.
#
# + apiStore - Instance of `ApiDataStore` used to fetch `Api` Data
# + appStore - Instance of `ApplicationDataStore` used to fetch `Application` Data
# + subStore - Instance of `SubscriptionDataStore` used to fetch `Subscription` Data
# + keyMapStore - Instance of `KeyMappingDataStore` used to fetch `KeyMap` Data
public type PilotDataProvider object {
    ApiDataStore apiStore;
    ApplicationDataStore appStore;
    SubscriptionDataStore subStore;
    KeyMappingDataStore keyMapStore;

    private string username;
    private string password;
    private string serviceContext;
    private http:Client gatewayPilotEndpoint;

    public function __init(http:Client gatewayPilotEndpoint) {
        self.username = getConfigValue(EVENT_HUB_INSTANCE_ID, EVENT_HUB_USERNAME, DEFAULT_PILOT_USERNAME);
        self.password = getConfigValue(EVENT_HUB_INSTANCE_ID, EVENT_HUB_PASSWORD, DEFAULT_PILOT_PASSWORD);
        self.serviceContext = getConfigValue(EVENT_HUB_INSTANCE_ID, EVENT_HUB_INT_CONTEXT, DEFAULT_PILOT_INT_CONTEXT);
        self.gatewayPilotEndpoint = gatewayPilotEndpoint;

        self.subStore = new(self.username, self.password, self.serviceContext);
        self.apiStore = new(self.username, self.password, self.serviceContext);
        self.keyMapStore = new(self.username, self.password, self.serviceContext);
        self.appStore = new(self.username, self.password, self.serviceContext);

    }

    # Get Api details from `ApiDataStore`.
    #
    # + name - Api name
    # + apiVersion - Api version
    # + return - `Api` object if requested Api is found. If not `()`
    public function getApi(string name, string apiVersion) returns Api | () {
        string apiKey = name + ":" + apiVersion;
        return self.apiStore.getApi(apiKey);
    }

    public function addApi(Api api) {
        self.apiStore.addApi(api);
    }

    public function removeApi(Api api) {
        self.apiStore.removeApi(api);
    }

    # Get Key Mapping details from `KeyMappingDataStore`.
    #
    # + tenantDomain - Tenant domain to which the application belongs to.
    # + consumerKey - Consumer key of the application
    # + return - `KeyMap` object if requested Key Mapping is found. If not `()`
    public function getKeyMapping(string consumerKey) returns KeyMap | () {
        return self.keyMapStore.getMapping(consumerKey);
    }

    public function addKeyMapping(KeyMap keyMap) {
        self.keyMapStore.addKeyMapping(keyMap);
    }

    public function removeKeyMapping(KeyMap keyMap) {
        self.keyMapStore.removeKeyMapping(keyMap);
    }

    # Get Subscription details from `SubscriptionDataStore`.
    #
    # + tenantDomain - Tenant domain of the subscriber
    # + appId - Application Id of the subscription
    # + apiId - Api Id of the subscription
    # + return - `Subscription` object if requested Subscription is found. If not `()`
    public function getSubscription(int appId, int apiId) returns Subscription | () {
        string subKey = appId.toString() + ":" + apiId.toString();

        return self.subStore.getSubscription(subKey);
    }

    public function addSubscription(Subscription sub) {
        self.subStore.addSubscription(sub);
    }

    public function removeSubscription(Subscription sub) {
        self.subStore.removeSubscription(sub);
    }

    # Get Application details from `ApplicationDataStore`.
    #
    # + tenantDomain - Tenant domain to which the application belongs
    # + appId - Application Id of the application
    # + return - `Application` object if requested Application is found. If not `()`
    public function getApplication(int appId) returns Application | () {
        string appKey = appId.toString();

        return self.appStore.getApplication(appKey);
    }

    public function addApplication(Application app) {
        self.appStore.addApplication(app);
    }

    public function removeApplication(Application app) {
        self.appStore.removeApplication(app);
    }
};
