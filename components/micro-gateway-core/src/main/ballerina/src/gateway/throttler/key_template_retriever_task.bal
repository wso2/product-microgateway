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
import ballerina/task;

const int keyTemplateRetrievalRetries = 15;
string keyTemplateSrerviceURL = getConfigValue(THROTTLE_CONF_KEY_TEMPLATE_INSTANCE_ID, KM_SERVER_URL,
DEFAULT_THROTTLE_KEY_TEMPLATE_URL);
string username = getConfigValue(THROTTLE_CONF_KEY_TEMPLATE_INSTANCE_ID, USERNAME, DEFAULT_USERNAME);
string password = getConfigValue(THROTTLE_CONF_KEY_TEMPLATE_INSTANCE_ID, PASSWORD, DEFAULT_PASSWORD);
string credentials = username + ":" + password;
string encodedCredentials = credentials.toBytes().toBase64();
int retriesCount = 0;
const int intervalInMillis = 15000;

http:Client keyTemplateRetrieveClient = new (keyTemplateSrerviceURL,
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

task:Scheduler keyTemplateRetievalTimer = new ({
    intervalInMillis: intervalInMillis,
    initialDelayInMillis: 1000
});

public function registerkeyTemplateRetrievalTask() {
    printDebug(KEY_TEMPLATE_RETIEVAL_TASK, "Registering key template task");
    var attachResult = keyTemplateRetievalTimer.attach(keyTemplateRetrievalService);
    if (attachResult is error) {
        printError(KEY_TEMPLATE_RETIEVAL_TASK, "Error attaching the key template retrieval service to the task.", attachResult);
        return;
    }
    printDebug(KEY_TEMPLATE_RETIEVAL_TASK, "Successfully attached the service to key template task");
    var startResult = keyTemplateRetievalTimer.start();
    if (startResult is error) {
        printError(KEY_TEMPLATE_RETIEVAL_TASK, "Key template retrieval task has failed to start.", startResult);
        return;
    }
    printDebug(KEY_TEMPLATE_RETIEVAL_TASK, "Started the key template retrieval task.");
}

service keyTemplateRetrievalService = service {

    resource function onTrigger() {
        http:Request clientRequest = new;
        clientRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE + encodedCredentials);
        var resp = keyTemplateRetrieveClient->get("/keyTemplates", clientRequest);
        if (resp is http:Response) {
            var keyTemplates = resp.getJsonPayload();
            if (keyTemplates is json) {
                if (keyTemplates is json[]) {
                    foreach var key in keyTemplates {
                        string keyTempalteValue = key.toString();
                        keyTemplateMap[keyTempalteValue] = <@untainted>keyTempalteValue;
                    }
                }
                printDebug(KEY_TEMPLATE_RETIEVAL_TASK, "Key template map : " + keyTemplateMap.toString());
                stopKeyTemplateTask(true);
            } else {
                retriesCount = retriesCount + 1;
                printError(KEY_TEMPLATE_RETIEVAL_TASK, "Error while retrieving key templates. Retry in 15 seconds", keyTemplates);
            }
        } else {
            retriesCount = retriesCount + 1;
            printError(KEY_TEMPLATE_RETIEVAL_TASK, "Error while retrieving key templates. Retry in 15 seconds", resp);

        }
        if (retriesCount > keyTemplateRetrievalRetries) {
            stopKeyTemplateTask(false);
        }
    }
};

function stopKeyTemplateTask(boolean isSuccess) {
    if (isSuccess) {
        printInfo(KEY_TEMPLATE_RETIEVAL_TASK, "Key template retrieval successful. Stopping the timer task ...");
    } else {
        printInfo(KEY_TEMPLATE_RETIEVAL_TASK, "Key template retrieval retries are finished. No more retry attempts. " +
        "Stopping the timer task ... ");
    }
    var stopResult = keyTemplateRetievalTimer.stop();
    if (stopResult is error) {
        printError(KEY_TEMPLATE_RETIEVAL_TASK, "Key template retrieval task has failed to stop.", stopResult);
    }
    printInfo(KEY_TEMPLATE_RETIEVAL_TASK, "Key template retrieval task stopped successfully");
}
