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

public type RequestResponseExecutionDTO record {
    string metaClientType;
    string applicationConsumerKey;
    string applicationName;
    string applicationId;
    string applicationOwner;
    string apiContext;
    string apiName;
    string apiVersion;
    string apiResourcePath;
    string apiResourceTemplate;
    string apiMethod;
    string apiCreator;
    string apiCreatorTenantDomain;
    string apiTier;
    string apiHostname;
    string userName;
    string userTenantDomain;
    string userIp;
    string userAgent;
    int requestTimestamp;
    boolean throttledOut;
    int responseTime;
    int serviceTime;
    int backendTime;
    boolean responseCacheHit;
    int responseSize;
    string protocol;
    int responseCode;
    string destination;
    ExecutionTimeDTO executionTime;
    string gatewayType;
    string label;
    string correlationId;
};

public type ExecutionTimeDTO record {
    int securityLatency;
    int throttlingLatency;
    int requestMediationLatency;
    int responseMediationLatency;
    int backEndLatency;
    int otherLatency;
};


public type FaultDTO record {
    string clientType;
    string consumerKey;
    string context;
    string apiVersion;
    string apiName;
    string resourcePath;
    string method;
    string versionOnly;
    int errorCode;
    string errorMessage;
    int faultTime;
    string userName;
    string tenantDomain;
    string hostName;
    string apiPublisher;
    string applicationName;
    string applicationId;
    string protocol;
};

public type EventDTO record {
    string streamId;
    int timeStamp;
    string metaData;
    string correlationData;
    string payloadData;
};