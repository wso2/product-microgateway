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


import ballerina/io;

public type AnalyticsRequestStream  {
    string consumerKey;
    string username;
    string tenantDomain;
    string context;
    string apiVersion;
    string api;
    string resourcePath;
    string resourceTemplate;
    string method;
    string hostName;
    string apiPublisher;
    string applicationName;
    string applicationId;
    string protocol;
    string clientIp;
    string applicationOwner;
    string keyType;
    string correlationID;
    int requestTime;
    string userAgent;
    string tier;
    boolean continuedOnThrottleOut;
    int requestCount;

};

public type ExecutionTimeDTO {
    string apiName;
    string apiVersion;
    string tenantDomain;
    string provider;
    string context;
    string keyType;
    string correleationID;
    int apiResponseTime;
    int tenantId;
    int eventTime;
    int securityLatency;
    int throttlingLatency;
    int requestMediationLatency;
    int responseMediationLatency;
    int backEndLatency;
    int otherLatency;
};


public type ResponseDTO {
    string consumerKey;
    string context;
    string apiVersion;
    string api;
    string resourcePath;
    string resourceTemplate;
    string method;
    string versionOnly;
    string response;
    int responseTime;
    int serviceTime;
    int backendTime;
    string userName;
    int eventTime;
    string tenantDomain;
    string userAgent;
    string hostname;
    string apiPublisher;
    string appName;
    string appId;
    string cacheHit;
    string responseSize;
    string protoco;
    string responseCode;
    string destination;
};


public type faultDTO {
    string consumerKey;
    string context;
    string apiVersion;
    string apiName;
    string resourcePath;
    string method;
    string versionOnly;
    string errorCode;
    string errorMessage;
    string requestTime;
    string userName;
    string tenantDomain;
    string hostName;
    string apiPublisher;
    string appName;
    string appId;
    string protoco;
};

public type EventDTO {
    string streamId;
    int timeStamp;
    string metaData;
    string correlationData;
    string payloadData;
};