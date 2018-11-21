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
public map defaultUrls;
public boolean etcdPeriodicQueryInitialized = false;
public boolean etcdConnectionEstablished = false;
public boolean etcdConnectionAttempted = false;
public string etcdToken;
public boolean etcdUrlValid = false;
task:Timer? etcdTimer;

@Description {value:"Setting up etcd timer task"}
public function initiateEtcdPeriodicQuery()
{
    int etcdTriggerTime = config:getAsInt("etcdtimer", default = DEFAULT_ETCD_TRIGGER_TIME);
    (function() returns error?) onTriggerFunction = etcdPeriodicQuery;
    function(error) onErrorFunction = etcdError;
    etcdTimer = new task:Timer(onTriggerFunction, onErrorFunction, etcdTriggerTime, delay = 1000);
    etcdTimer.start();
    printInfo(KEY_ETCD_UTIL, "Etcd Periodic Timer Task Started");
}

@Description {value:"Periodic Etcd Query. Trigger function of etcd timer task"}
public function etcdPeriodicQuery() returns error? {
    if(etcdUrls.count() > 0)
    {
        foreach k, v in etcdUrls {

            string currentUrl = <string>v;
            string fetchedUrl = etcdLookup(<string>k);

            if(currentUrl != fetchedUrl)
            {
                etcdUrls[<string>k] = fetchedUrl;
                urlChanged[<string>k] = true;
            }
        }
        io:println(etcdUrls);
    }
    else
    {
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
public function etcdSetup(string key, string default, string configKey) returns string
{
    string endpointUrl;
    if(!etcdConnectionAttempted)
    {
        establishEtcdConnection();
        etcdConnectionAttempted = true;
    }

    if(etcdConnectionEstablished)
    {
        if(!etcdPeriodicQueryInitialized)
        {
            etcdPeriodicQueryInitialized = true;
            initiateEtcdPeriodicQuery();
        }
        string etcdKey = retrieveConfig(configKey, "");

        if(etcdKey == "")
        {
            printInfo(KEY_ETCD_UTIL, "Etcd Key not provided for: " + key);
            endpointUrl = retrieveConfig(key, default);
        }
        else
        {
            defaultUrls[etcdKey] = default;
            urlChanged[etcdKey] = false;
            etcdUrls[etcdKey] = etcdLookup(etcdKey);
            endpointUrl = <string>etcdUrls[etcdKey];
        }
    }
    else
    {
        endpointUrl = retrieveConfig(key, default);
    }

    return endpointUrl;
}

@Description {value:"Establish etcd connection by authenticating etcd"}
public function establishEtcdConnection()
{
    string etcdurl = retrieveConfig("etcdurl", "");
    boolean authenticated;
    if(etcdurl != "")
    {
        authenticated = etcdAuthenticate();
        if(etcdUrlValid)
        {
            if(authenticated)
            {
                printInfo(KEY_ETCD_UTIL, "Etcd Authentication Successful");
                etcdConnectionEstablished = true;
            }
            else
            {
                printInfo(KEY_ETCD_UTIL, "Etcd Authentication Failed");
                etcdConnectionEstablished = false;
            }
        }
        else
        {
            printInfo(KEY_ETCD_UTIL, "Invalid Etcd Url Provided");
            etcdConnectionEstablished = false;
        }
    }
    else
    {
        printInfo(KEY_ETCD_UTIL, "Etcd URL not provided");
        etcdConnectionEstablished = false;
    }
}

@Description {value:"Query etcd passing the key and retrieves value"}
public function etcdLookup(string key10) returns string
{
    string key64;
    string value64;
    string endpointUrl;
    http:Request req;

    var key = key10.base64Encode(charset = "utf-8");
    match key {
        string matchedKey => key64 = matchedKey;
        error err => log:printError(err.message, err = err);
    }

    req.setPayload({"key": untaint key64});
    req.setHeader("Authorization", etcdToken);

    var response = etcdEndpoint->post("/v3alpha/kv/range",req);
    match response {
        http:Response resp => {
            var msg = resp.getJsonPayload();
            match msg {
                json jsonPayload => {
                    var val64 = <string>jsonPayload.kvs[0].value;
                    match val64 {
                        string matchedValue => value64 = matchedValue;
                        error err => { value64 = "Not found"; }
                    }
                }
                error err => {
                    log:printError(err.message, err = err);
                }
            }
        }
        error err => {
            log:printError(err.message, err = err);
            value64 = "Not found";
        }
    }

    if(value64 == "Not found")
    {
        endpointUrl = <string>defaultUrls[key10];
    }
    else
    {
        var value10 = value64.base64Decode(charset = "utf-8");
        match value10 {
            string matchedValue10 => endpointUrl = untaint matchedValue10;
            error err => log:printError(err.message, err = err);
        }
    }
    return endpointUrl;
}

@Description {value:"Authenticate etcd by providing username and password and retrieve etcd token"}
public function etcdAuthenticate() returns boolean
{
    http:Request req;
    boolean etcdAuthenticated = false;

    string username = retrieveConfig("etcdusername", "");
    string password = retrieveConfig("etcdpassword", "");

    req.setPayload({"name": untaint username, "password": untaint password});

    var response = etcdEndpoint->post("/v3alpha/auth/authenticate",req);
    match response {
        http:Response resp => {
            var msg = resp.getJsonPayload();
            match msg {
                json jsonPayload => {
                    etcdUrlValid = true;
                    var token = <string>jsonPayload.token;
                    match token {
                        string value => {
                            etcdToken = untaint value;
                            etcdAuthenticated = true;
                        }
                        error err => {
                            etcdAuthenticated = false;
                        }
                    }
                }
                error err => {
                    string errorMessage = err.message;
                    if(errorMessage.contains("Connection refused"))
                    {
                        etcdUrlValid = false;
                    }
                }
            }
        }
        error err => {
            log:printError(err.message, err = err);
        }
    }

    return etcdAuthenticated;
}