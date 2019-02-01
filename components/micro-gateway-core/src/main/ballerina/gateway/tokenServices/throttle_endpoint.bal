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
import ballerina/log;
import ballerina/http;

string throttleEndpointUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, THROTTLE_ENDPOINT_URL,
    "https://localhost:9443/endpoints");
string throttleEndpointbase64Header = getConfigValue(THROTTLE_CONF_INSTANCE_ID, THROTTLE_ENDPOINT_BASE64_HEADER,
    "admin:admin");

endpoint http:Client throttleEndpoint {
    url: throttleEndpointUrl
};

public function publishThrottleEventToTrafficManager(RequestStreamDTO throttleEvent) {

    json sendEvent = {
        event: {
            metaData: {},
            correlationData: {},
            payloadData: {
                messageID: throttleEvent.messageID,
                appKey: throttleEvent.appKey,
                appTier: throttleEvent.appTier,
                apiKey: throttleEvent.apiKey,
                apiTier: throttleEvent.apiTier,
                subscriptionKey: throttleEvent.subscriptionKey,
                subscriptionTier: throttleEvent.subscriptionTier,
                resourceKey: throttleEvent.resourceKey,
                resourceTier: throttleEvent.resourceTier,
                userId: throttleEvent.userId,
                apiContext: throttleEvent.apiContext,
                apiVersion: throttleEvent.apiVersion,
                appTenant: throttleEvent.appTenant,
                apiTenant: throttleEvent.apiTenant,
                appId: throttleEvent.appId,
                apiName: throttleEvent.apiName,
                properties: throttleEvent.properties
            }
        }
    };

    http:Request clientRequest = new;
    string encodedBasicAuthHeader = check throttleEndpointbase64Header.base64Encode();
    clientRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE + encodedBasicAuthHeader);
    clientRequest.setPayload(sendEvent);

    log:printDebug("ThrottleMessage is sent to traffic manager");

    var response = throttleEndpoint->post("/throttleEventReceiver", clientRequest);

    match response {
        http:Response resp => {
            log:printDebug("\nStatus Code: " + resp.statusCode);
        }
        error err => {
            log:printError(err.message, err = err);
        }
    }
}



