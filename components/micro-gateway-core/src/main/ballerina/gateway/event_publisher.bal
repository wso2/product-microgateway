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

import ballerina/http;
import ballerina/log;

endpoint http:Client clientEndpoint {
    url: "https://localhost:9443/endpoints/"
};

public function startToPublish(RequestStreamDTO request) {
    if (!getGatewayConfInstance().getThrottleConf().enabledGlobalTMEventPublishing) {
        return;
    }
    json jsonMsg = {
        event:{
            metaData:{},
            correlationData:{},
            payloadData:{
                messageID: request.messageID,
                appKey: request.appKey,
                appTier: request.appTier,
                apiKey: request.apiKey,
                apiTier: request.apiTier,
                subscriptionKey: request.subscriptionKey,
                subscriptionTier: request.subscriptionTier,
                resourceKey: request.resourceKey,
                resourceTier: request.resourceTier,
                userId: request.userId,
                apiContext: request.apiContext,
                apiVersion: request.apiVersion,
                appTenant: request.appTenant,
                apiTenant: request.apiTenant,
                appId:request.appId,
                apiName: request.apiName,
                properties: request.properties
            }
        }
    };
    string base64Header = "admin:admin";
    string encodedBasicAuthHeader = check base64Header.base64Encode();
    http:Request clientRequest = new;
    clientRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE + encodedBasicAuthHeader);
    clientRequest.setPayload(jsonMsg);
    var response = clientEndpoint -> post("/throttleEventReceiver", request = clientRequest);
    match response {
        http:Response resp => {
            log:printInfo("\nStatus Code: " + resp.statusCode);
        }
        error err => { log:printError(err.message, err = err); }
    }
}