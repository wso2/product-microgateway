// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License")=""; you may not use this file except
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

public type EligibilityStreamDTO record {
    string messageID = "";
    boolean isEligible = false;
    string throttleKey = "";
    int expiryTimeStamp = 0;
};

public type ThrottleAnalyticsEventDTO record {
    string metaClientType = "";
    string userName = "";
    string userTenantDomain = "";
    string apiName = "";
    string apiVersion = "";
    string apiContext = "";
    string apiCreator = "";
    string apiCreatorTenantDomain = "";
    string applicationId = "";
    string applicationName = "";
    string subscriber = "";
    string throttledOutReason = "";
    string gatewayType = "";
    int throttledTime = 0;
    string hostname = "";
};

public type IntermediateStream record {
    string throttleKey = "";
    int eventCount = 0;
    boolean stopOnQuota = false;
    int expiryTimeStamp = 0;
};

public type RequestStreamDTO record {
    int resetTimestamp = 0;
    int remainingQuota = 0;
    boolean isThrottled = false;
    string messageID = "";
    string apiKey = "";
    string appKey = "";
    boolean stopOnQuota = true;
    string subscriptionKey = "";
    string policyKey = "";
    string appTier = "";
    string apiTier = "";
    string subscriptionTier = "";
    string resourceKey = "";
    string resourceTier = "";
    string userId = "";
    string apiContext = "";
    string apiVersion = "";
    string appTenant = "";
    string apiTenant = "";
    string appId = "";
    string apiName = "";
    string properties = "";
    int resourceTierCount = -1;
    int resourceTierUnitTime = -1;
    string resourceTierTimeUnit = "";
    int appTierCount = -1;
    int appTierUnitTime = -1;
    string appTierTimeUnit = "";
    int apiTierCount = -1;
    int apiTierUnitTime = -1;
    string apiTierTimeUnit = "";
    int subscriptionTierCount = -1;
    int subscriptionTierUnitTime = -1;
    string subscriptionTierTimeUnit = "";
};

public type GlobalThrottleStreamDTO record {
    string policyKey = "";
    boolean stopOnQuota = true;
    int? resetTimestamp;
    int? remainingQuota;
    boolean isThrottled = false;
};

public type IPRangeDTO record {
    int id;
    string tenantDomain = "carbon.super";
    string 'type;
    string fixedIp;
    string startingIp;
    string endingIp;
    boolean invert = false;
};
