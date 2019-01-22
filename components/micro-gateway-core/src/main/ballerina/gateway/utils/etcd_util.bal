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
import ballerina/auth;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;
import ballerina/internal;
import ballerina/system;

public map etcdUrls;
public map urlChanged;
map defaultUrls;
string etcdToken;
boolean etcdPeriodicQueryInitialized = false;
public boolean etcdConnectionEstablished = false;
boolean etcdConnectionAttempted = false;
boolean credentialsProvided = false;
boolean etcdAuthenticationEnabled = true;
task:Timer? etcdTimer;
string etcdKVBasePath = "/v3alpha/kv";
string etcdAuthBasePath = "/v3alpha/auth";

@Description {value:"Setting up etcd timer task"}
public function initiateEtcdTimerTask() {
    printDebug(KEY_ETCD_UTIL, "initiateEtcdTimerTask Called");
    int etcdTriggerTime = config:getAsInt("etcdtimer", default = DEFAULT_ETCD_TRIGGER_TIME);
    (function() returns error?) onTriggerFunction = etcdTimerTask;
    function(error) onErrorFunction = etcdError;
    etcdTimer = new task:Timer(onTriggerFunction, onErrorFunction, etcdTriggerTime, delay = 1000);
    etcdTimer.start();
    printInfo(KEY_ETCD_UTIL, "Etcd periodic timer task started with a periodic time of " + <string>etcdTriggerTime + "ms");
}

@Description {value:"Periodic Etcd Query. Trigger function of etcd timer task"}
public function etcdTimerTask() returns error? {
    printDebug(KEY_ETCD_UTIL, "Etcd Periodic Query Initiated");
    if(etcdUrls.count() > 0){
        printDebug(KEY_ETCD_UTIL, "etcdurl map values - start");
        foreach key, value in etcdUrls {
            string currentUrl = <string>value;
            string fetchedUrl = etcdLookup(<string>key);

            if(currentUrl != fetchedUrl){
                etcdUrls[<string>key] = fetchedUrl;
                urlChanged[<string>key] = true;
            }
            printDebug(KEY_ETCD_UTIL, key + " : " + <string>etcdUrls[<string>key]);
        }
        printDebug(KEY_ETCD_UTIL, "etcdurl map values - end");
    }
    else {
        printInfo(KEY_ETCD_UTIL, "No Etcd keys provided. Stopping etcd periodic call");
        etcdTimer.stop();
    }

    return ();
}

@Description {value:"Error function of etcd timer task"}
public function etcdError(error e) {
    printError(KEY_ETCD_UTIL, "Etcd Timer Task failed");
}

@Description {value:"Setting up etcd requirements"}
public function etcdSetup(string key, string etcdConfigKey, string default) returns string {
    string endpointUrl;

    if(!etcdConnectionAttempted){
        establishEtcdConnection();
        etcdConnectionAttempted = true;
        printDebug(KEY_ETCD_UTIL, "Etcd Connection Attempted");
    }

    if(etcdConnectionEstablished){
        if(!etcdPeriodicQueryInitialized){
            etcdPeriodicQueryInitialized = true;
            initiateEtcdTimerTask();
        }
        string etcdKey = retrieveConfig(etcdConfigKey, "");

        if(etcdKey == ""){
            printInfo(KEY_ETCD_UTIL, "Etcd Key not provided for: " + key);
            endpointUrl = retrieveConfig(key, default);
        }
        else {
            printDebug(KEY_ETCD_UTIL, "Etcd Key provided for: " + key);
            defaultUrls[etcdKey] = retrieveConfig(key, default);
            urlChanged[etcdKey] = false;
            etcdUrls[etcdKey] = etcdLookup(etcdKey);
            endpointUrl = <string>etcdUrls[etcdKey];
        }
    }
    else {
        endpointUrl = retrieveConfig(key, default);
    }

    return endpointUrl;
}

@Description {value:"Establish etcd connection by authenticating etcd"}
public function establishEtcdConnection() {
    printDebug(KEY_ETCD_UTIL, "Establishing Etcd Connection");
    string etcdurl = retrieveConfig("etcdurl", "");
    if(etcdurl != ""){
        printDebug(KEY_ETCD_UTIL, "etcdurl CLI parameter has not been provided");
        etcdAuthenticate();
    } else {
        printError(KEY_ETCD_UTIL, "Etcd URL not provided");
        etcdConnectionEstablished = false;
    }
}

@Description {value:"Query etcd passing the key and retrieves value"}
public function etcdLookup(string base10EncodedKey) returns string {
    string base64EncodedKey;
    string base64EncodedValue;
    string endpointUrl;
    http:Request req;
    boolean valueNotFound = false;

    base64EncodedKey = encodeValueToBase64(base10EncodedKey);
    req.setPayload({"key": untaint base64EncodedKey});

    if(etcdAuthenticationEnabled){
        printDebug(KEY_ETCD_UTIL, "Setting authorization header for etcd requests");
        req.setHeader("Authorization", etcdToken);
    }

    var response = etcdEndpoint->post(etcdKVBasePath + "/range", req);
    match response {
        http:Response resp => {
            printDebug(KEY_ETCD_UTIL, "Http Response object obtained");
            var msg = resp.getJsonPayload();
            match msg {
                json jsonPayload => {
                    printDebug(KEY_ETCD_UTIL, "etcd responded with a payload");
                    var payloadValue = <string>jsonPayload.kvs[0].value;
                    match payloadValue {
                        string matchedValue => base64EncodedValue = matchedValue;
                        error err => valueNotFound = true;
                    }
                }
                error err => printError(KEY_ETCD_UTIL, err.message);
            }
        }
        error err => {
            printDebug(KEY_ETCD_UTIL, "Error object obtained");
            valueNotFound = true;
            printError(KEY_ETCD_UTIL, err.message);
        }
    }

    if(valueNotFound){
        printDebug(KEY_ETCD_UTIL, "value not found at etcd");
        endpointUrl = <string>defaultUrls[base10EncodedKey];
    } else {
        printDebug(KEY_ETCD_UTIL, "value found at etcd");
        endpointUrl = decodeValueToBase10(base64EncodedValue);
    }
    return endpointUrl;
}

@Description {value:"Authenticate etcd by providing username and password and retrieve etcd token"}
public function etcdAuthenticate() {
    printDebug(KEY_ETCD_UTIL, "Authenticating Etcd");
    http:Request req;

    string username = retrieveConfig("etcdusername", "");
    string password = retrieveConfig("etcdpassword", "");

    if(username == "" && password == ""){
        printDebug(KEY_ETCD_UTIL, "etcdusername and etcdpassword CLI parameters has not been provided");
        credentialsProvided = false;
    } else {
        printDebug(KEY_ETCD_UTIL, "both or one of etcdusername and etcdpassword CLI parameters have been provided");
        credentialsProvided = true;
    }

    req.setPayload({ "name": untaint username, "password": untaint password });

    var response = etcdEndpoint->post(etcdAuthBasePath + "/authenticate", req);
    match response {
        http:Response resp => {
            printDebug(KEY_ETCD_UTIL, "Http Response object obtained");
            var msg = resp.getJsonPayload();
            match msg {
                json jsonPayload => {
                    if(jsonPayload.token!= null){
                        printDebug(KEY_ETCD_UTIL, "etcd has responded with a token");
                        var token = <string>jsonPayload.token;
                        match token {
                            string value => {
                                etcdToken = untaint value;
                                etcdConnectionEstablished = true;
                                printInfo(KEY_ETCD_UTIL, "Etcd Authentication Successful");
                            }
                            error err => {
                                etcdConnectionEstablished = false;
                                printError(KEY_ETCD_UTIL, err.message);
                            }
                        }
                    }
                    if(jsonPayload.error!=null){
                        printDebug(KEY_ETCD_UTIL, "etcd has responded with an error");
                        var authenticationError = <string>jsonPayload.error;
                        match authenticationError {
                            string value => {
                                if(value.contains("authentication is not enabled")){
                                    printDebug(KEY_ETCD_UTIL, "etcd authentication is not enabled");
                                    etcdAuthenticationEnabled = false;
                                    etcdConnectionEstablished = true;
                                    if(credentialsProvided){
                                        printInfo(KEY_ETCD_UTIL, value);
                                    }
                                }
                                if(value.contains("authentication failed, invalid user ID or password")){
                                    etcdConnectionEstablished = false;
                                    printError(KEY_ETCD_UTIL, value);
                                }
                            }
                            error err => {
                                etcdConnectionEstablished = false;
                                printError(KEY_ETCD_UTIL, err.message);
                            }
                        }
                    }
                }
                error err => {
                    etcdConnectionEstablished = false;
                    printError(KEY_ETCD_UTIL, err.message);
                }
            }
        }
        error err => {
            printDebug(KEY_ETCD_UTIL, "Error object obtained");
            etcdConnectionEstablished = false;
            printError(KEY_ETCD_UTIL, err.message);
        }
    }
}
