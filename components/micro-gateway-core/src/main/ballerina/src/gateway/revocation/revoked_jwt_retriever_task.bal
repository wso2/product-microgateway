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
import ballerina/task;

boolean enabledPersistentMessage = getConfigBooleanValue(PERSISTENT_MESSAGE_INSTANCE_ID,
        PERSISTENT_MESSAGE_ENABLED, DEFAULT_TOKEN_REVOCATION_ENABLED);
const int revokedJWTRetrievalRetries = 15;
string revokedJwtServiceURL = getRevokedJwtPersistantEndpoint();
int jwtRetrievalRetriesCount = 0;
const int retryIntervalInMillis = 15000;

http:Client revokedJwtRetrieveClient = new (revokedJwtServiceURL,
{
    cache: {enabled: false},
    secureSocket: {
        trustStore: {
            path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH),
            password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD)
        },
        verifyHostname: getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
    },
    http1Settings : {
        proxy: getClientProxyForInternalServices()
    }
});

task:Scheduler revokedJwtRetievalTimer = new ({
    intervalInMillis: intervalInMillis,
    initialDelayInMillis: 2000
});

function getRevokedJwtPersistantEndpoint() returns string {
    string revokedJwtServiceURL = getConfigValue(PERSISTENT_MESSAGE_INSTANCE_ID, PERSISTENT_MESSAGE_HOSTNAME, "");
    if (revokedJwtServiceURL == "") {
        revokedJwtServiceURL = getConfigValue(PERSISTENT_MESSAGE_INSTANCE_ID, PERSISTENT_MESSAGE_ENDPOINT,
                                DEFAULT_PERSISTENT_MESSAGE_HOSTNAME);
    }
    return revokedJwtServiceURL;
}

public function registerRevokedJwtRetrievalTask() {
    if (enabledPersistentMessage) {
        printDebug(REVOKED_JWT_RETIEVAL_TASK, "Registering revoked jwt retriever task");
        var attachResult = revokedJwtRetievalTimer.attach(revokedJwtRetrievalService);
        if (attachResult is error) {
            printError(REVOKED_JWT_RETIEVAL_TASK, "Error attaching the revoked jwt retrieval service to the task.", attachResult);
            return;
        }
        printDebug(REVOKED_JWT_RETIEVAL_TASK, "Successfully attached the service to revoked jwt retriever task");
        var startResult = revokedJwtRetievalTimer.start();
        if (startResult is error) {
            printError(REVOKED_JWT_RETIEVAL_TASK, "Revoked jwt retrieval task has failed to start.", startResult);
            return;
        }
        printDebug(REVOKED_JWT_RETIEVAL_TASK, "Started the revoked jwt retrieval task.");
    }
}

service revokedJwtRetrievalService = service {

    resource function onTrigger() {
        map<string> revokedMap = {};
        string credentials = etcdUsernameTokenRevocation + ":" + etcdPasswordTokenRevocation;
        string encodedCredentials = credentials.toBytes().toBase64();
        http:Request clientRequest = new;
        clientRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE + encodedCredentials);
        var resp = revokedJwtRetrieveClient->get("/revokedjwt", clientRequest);
        if (resp is http:Response) {
            var revokedJwts = resp.getJsonPayload();
            if (revokedJwts is json) {
                printDebug(REVOKED_JWT_RETIEVAL_TASK, "Revoked token list recieved: " + revokedJwts.toJsonString());
                if (revokedJwts is json[]) {
                    foreach var revokedJwt in revokedJwts {
                        json|error signature = revokedJwt.jwt_signature;
                        // jwt_signature is jwtSignature in APIM 3.1.0
                        if (signature is error) {
                            signature = revokedJwt.jwtSignature;
                        }
                        json|error expiryTime = revokedJwt.expiry_time;
                        // expiry_time is expiryTime in APIM 3.1.0
                        if (expiryTime is error) {
                            expiryTime = revokedJwt.expiryTime;
                        }
                        revokedMap[signature.toString()] = expiryTime.toString();
                    }
                    if (revokedMap.length() > 0) {
                        var status = addToRevokedTokenMap(revokedMap);
                        if (status is boolean) {
                            printDebug(REVOKED_JWT_RETIEVAL_TASK, "Revoked tokens are successfully added to cache");
                        } else {
                            printError(REVOKED_JWT_RETIEVAL_TASK, "Error in  adding revoked token to map");
                        }
                    }
                }
                printDebug(REVOKED_JWT_RETIEVAL_TASK, "Revoked token map : " + revokedMap.toString());
                stopJwtRetieverTask(true);
            } else {
                printDebug(REVOKED_JWT_RETIEVAL_TASK, "Revoked jwts are not found.");
                stopJwtRetieverTask(true);
            }
        } else {
            jwtRetrievalRetriesCount = jwtRetrievalRetriesCount + 1;
            printError(REVOKED_JWT_RETIEVAL_TASK, "Error while retrieving revoked jwts. Retry in 15 seconds", resp);

        }
        if (retriesCount > revokedJWTRetrievalRetries) {
            stopJwtRetieverTask(false);
        }
    }
};

function stopJwtRetieverTask(boolean isSuccess) {
    if (isSuccess) {
        printInfo(REVOKED_JWT_RETIEVAL_TASK, "Revoked jwt retrieval successful. Stopping the timer task ...");
    } else {
        printInfo(REVOKED_JWT_RETIEVAL_TASK, "Revoked jwt retrieval retries are finished. No more retry attempts. " +
        "Stopping the timer task ... ");
    }
    var stopResult = revokedJwtRetievalTimer.stop();
    if (stopResult is error) {
        printError(REVOKED_JWT_RETIEVAL_TASK, "Revoked jwt retrieval task has failed to stop.", stopResult);
    }
    printInfo(REVOKED_JWT_RETIEVAL_TASK, "Revoked jwt retrieval task stopped successfully");
}
