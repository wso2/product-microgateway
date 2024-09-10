// Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/java;

function jinitELKAnalyticsDataPublisher() = @java:Method {
    name: "initELKDataPublisher",
    class: "org.wso2.micro.gateway.core.analytics.DefaultAnalyticsEventPublisher"
} external;

function jinitChoreoAnalyticsDataPublisher(handle configEndpoint, handle authToken) = @java:Method {
    name: "initChoreoDataPublisher",
    class: "org.wso2.micro.gateway.core.analytics.DefaultAnalyticsEventPublisher"
} external;

function invokeJinitChoreoAnalyticsDataPublisher() {
    handle configEndpoint = java:fromString(getConfigValue(CHOREO_ANALYTICS, CHOREO_ANALYTICS_CONFIG_ENDPOINT, DEFAULT_CHOREO_ANALYTICS_CONFIG_ENDPOINT));
    handle authToken = java:fromString(getConfigValue(CHOREO_ANALYTICS, CHOREO_ANALYTICS_AUTH_TOKEN, DEFAULT_CHOREO_ANALYTICS_AUTH_TOKEN));
    jinitChoreoAnalyticsDataPublisher(configEndpoint, authToken);
}

function jpublishAnalyticsEvent(Analytics4xEventData analyticsEvent) = @java:Method {
    name: "publishEventData",
    class: "org.wso2.micro.gateway.core.analytics.DefaultAnalyticsEventPublisher"
} external;
