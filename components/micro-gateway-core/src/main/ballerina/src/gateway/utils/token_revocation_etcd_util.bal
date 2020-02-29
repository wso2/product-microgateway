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

import ballerina/http;

string etcdPasswordTokenRevocation = getConfigValue(PERSISTENT_MESSAGE_INSTANCE_ID, PERSISTENT_MESSAGE_PASSWORD,
     DEFAULT_PERSISTENT_MESSAGE_PASSWORD);
string etcdUsernameTokenRevocation = getConfigValue(PERSISTENT_MESSAGE_INSTANCE_ID, PERSISTENT_MESSAGE_USERNAME,
     DEFAULT_PERSISTENT_MESSAGE_USERNAME);

# Query etcd by passing the revoked token and retrieves relevant value
#
# + tokenKey - revoked token key to query from etcd
# + return - string
public function etcdRevokedTokenLookup(string tokenKey) returns string {
    http:Request req = new;
    string finalResponse = "";
    boolean valueNotFound = false;
    string payloadValue = "";

    string key = etcdUsernameTokenRevocation + ":" + etcdPasswordTokenRevocation;
    string encodedKey = key.toBytes().toBase64();
    string basicAuthHeader = BASIC_PREFIX_WITH_SPACE + encodedKey;
    printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Setting authorization header for etcd requests");
    req.setHeader(AUTHORIZATION_HEADER, etcdToken);

    var response = etcdTokenRevocationEndpoint->get(tokenKey, message = req);

    if (response is http:Response) {
        printError(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Http Response object obtained");
        var msg = response.getJsonPayload();
        if (msg is json) {
            json jsonPayload = msg;
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "etcd responded with a payload");
            finalResponse = jsonPayload.node.value.toString();

        } else {
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error in retrieving json object");
            valueNotFound = true;
            printError(KEY_TOKEN_REVOCATION_ETCD_UTIL, msg.reason());
        }
    } else {
        printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error object obtained");
        valueNotFound = true;
        printError(KEY_TOKEN_REVOCATION_ETCD_UTIL, response.reason());
    }
    return finalResponse;
}

# Query all etcd revoked keys
#
# + return - string []
public function etcdAllRevokedTokenLookup() returns map<string> {
    http:Request req = new;
    map<string> finalResponse = {};
    string payloadValue;

    string key = etcdUsernameTokenRevocation + ":" + etcdPasswordTokenRevocation;
    string encodedKey = key.toBytes().toBase64();
    string basicAuthHeader = BASIC_PREFIX_WITH_SPACE + encodedKey;
    printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Setting authorization header for etcd requests");
    req.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);

    var response = etcdTokenRevocationEndpoint->get("", message = req);

    if (response is http:Response) {
        printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Http Response object obtained");
        var msg = response.getJsonPayload();

        if (msg is json) {
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "etcd responded with a payload");
            var nodes = msg.node.nodes;
            if (nodes is json) {
                json[] jsonNodes = <json[]>nodes;
                foreach json node in jsonNodes {
                    string revokedTokenReceived = node.key.toString();
                    string revokedTokenTTL = node.ttl.toString();
                    int tokenLength = revokedTokenReceived.length();
                    int lastIndexOfSlash = lastIndexOf(revokedTokenReceived, "/") + 1;
                    string revokedToken = revokedTokenReceived.substring(lastIndexOfSlash, tokenLength);
                    finalResponse[revokedToken] = revokedTokenTTL;
                }
            } else {
                printError(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error in reading json response", nodes);
            }
        } else {
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error in retrieving json object");
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, msg.reason());
        }
    } else {
        printError(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error object obtained", response);
    }
    return finalResponse;
}

# One Time Etcd Query. Trigger function of etcd revoked tokens retrieval task
public function etcdRevokedTokenRetrieverTask() {
    boolean enabledPersistentMessage = getConfigBooleanValue(PERSISTENT_MESSAGE_INSTANCE_ID,
        PERSISTENT_MESSAGE_ENABLED, DEFAULT_TOKEN_REVOCATION_ENABLED);

    if (enabledPersistentMessage) {
        printInfo(KEY_TOKEN_REVOCATION_ETCD_UTIL, "One time ETCD revoked token retriever task initiated");
        map<string> response = etcdAllRevokedTokenLookup();
        if (response.length() > 0) {
            var status = addToRevokedTokenMap(response);
            if (status is boolean) {
                printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Revoked tokens are successfully added to cache");
            } else {
                printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error in  adding revoked token to map");
            }

        } else {
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL,
                "No ETCD revoked tokens provided. Continuing ETCD revoked token retrieval call");
        }
    } else {
        printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "ETCD retrieval task is disabled");
    }
}
