// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type RequestStreamDTO {
    string messageID;
    string apiKey;
    string appKey;
    string subscriptionKey;
    string appTier;
    string apiTier;
    string subscriptionTier;
    string resourceKey;
    string resourceTier;
    string userId;
    string apiContext;
    string apiVersion;
    string appTenant;
    string apiTenant;
    string appId;
    string apiName;
    string properties;
};

public type ThrottleConf {
    boolean enabledHeaderConditions = false;
    boolean enabledJWTClaimConditions = false;
    boolean enabledQueryParamConditions = false;
    boolean enabledGlobalTMEventPublishing = false;
};

public type GlobalThrottleStreamDTO {
    string throttleKey ;
    boolean isThrottled;
    int expiryTimeStamp;
    //TODO add the conditions once ballerina support comes
    //string conditions;
};

public type EligibilityStreamDTO {
    string messageID;
    boolean isEligible;
    string throttleKey;
};