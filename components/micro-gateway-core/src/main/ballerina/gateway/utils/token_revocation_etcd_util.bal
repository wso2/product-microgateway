import ballerina/encoding;
import ballerina/http;
import ballerina/log;
import ballerina/auth;
import ballerina/config;
import ballerina/runtime;
import ballerina/io;
import ballerina/internal;
import ballerina/system;

string etcdPasswordTokenRevocation = getConfigValue(PERSISTENT_MESSAGE_INSTANCE_ID, PERSISTENT_MESSAGE_PASSWORD, "");
string etcdUsernameTokenRevocation = getConfigValue(PERSISTENT_MESSAGE_INSTANCE_ID, PERSISTENT_MESSAGE_USERNAME, "");

#value:"Query etcd by passing the revoked token and retrieves relevant value"
# + return - string
public function etcdRevokedTokenLookup(string tokenKey) returns string {
    http:Request req = new;
    string finalResponse = "";
    boolean valueNotFound = false;
    string payloadValue = "";

    string key = etcdUsernameTokenRevocation + ":" + etcdPasswordTokenRevocation;
    string encodedKey = encoding:encodeBase64(key.toByteArray(UTF_8));
    string basicAuthHeader = BASIC_PREFIX_WITH_SPACE + encodedKey;
    printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Setting authorization header for etcd requests");
    req.setHeader(AUTHORIZATION_HEADER, etcdToken);

    var response = etcdTokenRevocationEndpoint->get(tokenKey, message = req);

    if (response is http:Response) {
        printError(KEY_ETCD_UTIL, "Http Response object obtained");
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

#value:"Query all etcd revoked keys"
# + return - string []
public function etcdAllRevokedTokenLookup() returns map<string> {
    http:Request req = new;
    map<string> finalResponse = {};
    string payloadValue;

    string key = etcdUsernameTokenRevocation + ":" + etcdPasswordTokenRevocation;
    string encodedKey = encoding:encodeBase64(key.toByteArray(UTF_8));
    string basicAuthHeader = BASIC_PREFIX_WITH_SPACE + encodedKey;
    printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Setting authorization header for etcd requests");
    req.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);

    var response = etcdTokenRevocationEndpoint->get("", message = req);

    if (response is http:Response) {
        printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Http Response object obtained");
        var msg = response.getJsonPayload();
        if (msg is json) {
            json jsonPayload = msg;
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "etcd responded with a payload");
            json nodes = jsonPayload.node.nodes;
            int length = nodes.length();
            int i = 0;
            while (i < length) {
                string revokedTokenReceived = nodes[i].key.toString();
                string revokedTokenTTL = nodes[i].ttl.toString();
                int tokenLength = revokedTokenReceived.length();
                int lastIndexOfSlash = revokedTokenReceived.lastIndexOf("/") + 1;
                string revokedToken = revokedTokenReceived.substring(lastIndexOfSlash, tokenLength);
                finalResponse[revokedToken] = revokedTokenTTL;
                i = i + 1;
            }
        } else {
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error in retrieving json object");
            printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, msg.reason());
        }
    } else {
        printDebug(KEY_TOKEN_REVOCATION_ETCD_UTIL, "Error object obtained");
        printFullError(KEY_TOKEN_REVOCATION_ETCD_UTIL, response);
    }
    return finalResponse;
}

#value:"One Time Etcd Query. Trigger function of etcd revoked tokens retrieval task"
#
public function etcdRevokedTokenRetrieverTask() {
    boolean enabledPersistentMessage = getConfigBooleanValue(PERSISTENT_MESSAGE_INSTANCE_ID,
        PERSISTENT_MESSAGE_ENABLED, false);

    if (enabledPersistentMessage) {
        printInfo(KEY_ETCD_UTIL, "One time ETCD revoked token retriever task initiated");
        map<string> response = etcdAllRevokedTokenLookup();
        if (response.count() > 0) {
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
