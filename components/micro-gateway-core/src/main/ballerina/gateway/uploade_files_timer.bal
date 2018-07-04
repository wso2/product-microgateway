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

string uploadingUrl;

future timerFtr = start timerTask();

function searchFilesToUpload() returns error? {
    int cnt = 0;
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR);
    internal:Path ex = new(fileLocation);
    internal:Path[] pathList = check ex.list();
    foreach pathEntry in pathList {
        string fileName = pathEntry.getName();
        if (fileName.contains(ZIP_EXTENSION)) {
            http:Response response =  multipartSender(fileLocation + PATH_SEPERATOR,  pathEntry.getName());
            if (response.statusCode == 201) {
                var result = pathEntry.delete();
            } else {
                log:printError("Error occurred while uploading the file");
            }
            cnt++;
        }
    }
    if ( cnt == 0 ) {
        error er = {message: "No files present to upload."};
        return er;
    } else {
        return ();
    }
}

function informError(error e) {
    log:printDebug("File were not present to upload yet:" + e.message);
}

function timerTask() {
    task:Timer? timer;
    map vals = getConfigMapValue(ANALYTICS);
    boolean uploadFiles = check <boolean>vals[FILE_UPLOAD_TASK];
    if (uploadFiles) {
        log:printInfo("File uploading task is enabled.");
        int timeSpan = check <int>vals[UPLOADING_TIME_SPAN];
        (function() returns error?) onTriggerFunction = searchFilesToUpload;
        function(error) onErrorFunction = informError;
        timer = new task:Timer(onTriggerFunction, onErrorFunction, timeSpan, delay = 5000);
        timer.start();
    } else {
        log:printInfo("File uploading task is disabled.");
    }
}

