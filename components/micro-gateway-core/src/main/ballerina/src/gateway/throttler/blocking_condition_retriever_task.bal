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
import ballerina/stringutils;
import ballerina/task;

int blockConditionRetriesCount = 0;
http:Client blockingConditionRetrieveClient = new (keyTemplateSrerviceURL,
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

task:Scheduler blockingConditionRetievalTimer = new ({
    intervalInMillis: intervalInMillis,
    initialDelayInMillis: 1000
});

public function registerBlockingConditionRetrievalTask() {
    printDebug(KEY_TEMPLATE_RETIEVAL_TASK, "Registering blocking condition task");
    var attachResult = blockingConditionRetievalTimer.attach(blockingConditionRetrievalService);
    if (attachResult is error) {
        printError(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Error attaching the blocking condition retrieval service to the task.", attachResult);
        return;
    }
    printDebug(KEY_TEMPLATE_RETIEVAL_TASK, "Successfully attached the service to blocking condition task");
    var startResult = blockingConditionRetievalTimer.start();
    if (startResult is error) {
        printError(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition retrieval task has failed to start.", startResult);
        return;
    }
    printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Started the blocking condition retrieval task.");
}

service blockingConditionRetrievalService = service {

    resource function onTrigger() {
        http:Request clientRequest = new;
        clientRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE + encodedCredentials);
        var resp = blockingConditionRetrieveClient->get("/block", clientRequest);
        if (resp is http:Response) {
            var blockingConditions = resp.getJsonPayload();
            if (blockingConditions is json) {
                if (blockingConditions is map<json>) {
                    printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition json from the service : " + blockingConditions.toString());
                    foreach var key in blockingConditions.keys() {
                        string conditionKey = key.toString();
                        printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition key : " + key.toString());
                        var jsonArray = blockingConditions[key];
                        if (jsonArray is json[] && jsonArray.length() > 0) {
                            printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition value : " + jsonArray.toJsonString());
                            foreach var condition in jsonArray {
                                if (stringutils:equalsIgnoreCase(conditionKey, BLOCKING_CONDITION_IP) ||
                                stringutils:equalsIgnoreCase(conditionKey, BLOCKING_CONDITION_IP_RANGE)) {
                                    printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "IP Blocking condition value : " + condition.toJsonString());
                                    if (condition is map<json>) {
                                        addIpDataToBlockConditionTable(condition);
                                    } else {
                                        printWarn(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Could not add IP block condition to the table");
                                    }
                                } else {
                                    string blockingCondition = condition.toString();
                                    blockConditionsMap[blockingCondition] = <@untainted>blockingCondition;
                                    blockConditionExist = true;
                                }
                            }
                        }
                    }
                }
                printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition map : " + blockConditionsMap.toJsonString());
                printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking IP condition map : " + IpBlockConditionsMap.toString());
                stopBlockingConditionTask(true);
            } else {
                printDebug(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking conditions are not found.");
                stopBlockingConditionTask(true);
            }
        } else {
            blockConditionRetriesCount = blockConditionRetriesCount + 1;
            printError(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Error while retrieving blocking conditions. Retry in 15 seconds", resp);

        }
        if (blockConditionRetriesCount > keyTemplateRetrievalRetries) {
            stopBlockingConditionTask(false);
        }
    }
};

function stopBlockingConditionTask(boolean isSuccess) {
    if (isSuccess) {
        printInfo(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition retrieval successful. Stopping the timer task ...");
    } else {
        printInfo(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition retrieval retries are finished. No more retry attempts. " +
        "Stopping the timer task ... ");
    }
    var stopResult = blockingConditionRetievalTimer.stop();
    if (stopResult is error) {
        printError(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition retrieval task has failed to stop.", stopResult);
    }
    printInfo(KEY_BLOCKING_CONDITION_RETRIEVAL_TASK, "Blocking condition retrieval task stopped successfully");
}
