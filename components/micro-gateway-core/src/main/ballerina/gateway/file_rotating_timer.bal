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
import ballerina/internal;
import ballerina/task;
import ballerina/math;
import ballerina/runtime;
import ballerina/log;



function sendFileRotatingEvent() returns error? {
    int cnt = 0;
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR) + PATH_SEPERATOR;
    internal:Path path = new(fileLocation + API_USAGE_FILE);

    if (path.exists()) {
        var result = rotateFile(API_USAGE_FILE);
        if(result is string) {
            printInfo(KEY_ROTATE_TASK, "File rotated successfully.");
        } else {
            printFullError(KEY_ROTATE_TASK, result);
        }
        return;
    } else {
        error er = error("No files present to rotate.");
        return er;
    }
}

function errorOnRotating(error e) {
    printDebug(KEY_ROTATE_TASK, "File not present to rotate:" + e.reason());
}

function rotatingTask() {
    task:Timer? rotatinTimer;
    map<any> vals = getConfigMapValue(ANALYTICS);
    // Todo: handle error if it returns one.
    int timeSpan =  <int> vals[ROTATING_TIME];
    (function() returns error?) onTriggerFunction = sendFileRotatingEvent;
    function(error) onErrorFunction = errorOnRotating;
    rotatinTimer = new task:Timer(onTriggerFunction, onErrorFunction, timeSpan, delay = timeSpan + 5000);
    rotatinTimer.start();
    printDebug(KEY_ROTATE_TASK, "File rotating task initialized");
}

