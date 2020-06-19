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

function convertApplicationEventToApplicationDTO(json appEvent) returns Application {
    Application application = {
        id : <int>appEvent.applicationId,
        name: appEvent.applicationName.toString(),
        tenantId : <int>appEvent.tenantId,
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
        tenantId : <int>subEvent.tenantId,
        tenantDomain : subEvent.tenantDomain.toString(),
        policyId : subEvent.policyId.toString(),
        state : subEvent.subscriptionState.toString()
    };
    return subscription;
}