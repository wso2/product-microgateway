// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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



public type RequestResponseExecutionDTO record {
    string metaClientType = "";
    string applicationConsumerKey = "";
    string applicationName = "";
    string applicationId = "";
    string applicationOwner = "";
    string apiContext = "";
    string apiName = "";
    string apiVersion = "";
    string apiResourcePath = "";
    string apiResourceTemplate = "";
    string apiMethod = "";
    string apiCreator = "";
    string apiCreatorTenantDomain = "";
    string apiTier = "";
    string apiHostname = "";
    string userName = "";
    string userTenantDomain = "";
    string userIp = "";
    string userAgent = "";
    int requestTimestamp = 0;
    boolean throttledOut = false;
    int responseTime = 0;
    int serviceTime = 0;
    int backendTime = 0;
    boolean responseCacheHit = false;
    int responseSize = 0;
    string protocol = "";
    int responseCode = 0;
    string destination = "";
    ExecutionTimeDTO executionTime = {};
    string gatewayType = "";
    string label = "";
    string correlationId = "";
    boolean cacheHit = false;
};

public type ExecutionTimeDTO record {
    int securityLatency = 0;
    int throttlingLatency = 0;
    int requestMediationLatency = 0;
    int responseMediationLatency = 0;
    int backEndLatency = 0;
    int otherLatency = 0;
    int eventTime?;
    string provider?;
    string keyType?;
    string apiName?;
    string apiVersion?;
    string tenantDomain?;
    string context?;
    string correleationID?;
};


public type FaultDTO record {
    string metaClientType = "";
    string consumerKey = "";
    string apiVersion = "";
    string apiName = "";
    string apiContext = "";
    string resourcePath = "";
    string method = "";
    string apiCreator = "";
    string userName = "";
    string userTenantDomain = "";
    string apiCreatorTenantDomain = "";
    string hostName = "";
    string applicationId = "";
    string applicationName = "";
    string protocol = "";
    int errorCode = 0;
    string errorMessage = "";
    int faultTime = 0;
};

public type EventDTO record {
    string streamId = "";
    int timeStamp = 0;
    string metaData = "";
    string correlationData = "";
    string payloadData = "";
};
