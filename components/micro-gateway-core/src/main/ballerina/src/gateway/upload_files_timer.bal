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

string uploadingUrl = "";
string analyticsUsername = "";
string analyticsPassword = "";


function searchFilesToUpload() returns (error?) {
    int cnt = 0;
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR);
    internal:Path path = new(fileLocation);

    if (!path.exists()) {
        printDebug(KEY_UPLOAD_TASK, "Usage data directory not found");
        return ();
    }

    internal:Path[]|error pathList = path.list();

    if (pathList is error) {
        printError(KEY_UPLOAD_TASK, "Error occured in getting path lists");
        return pathList;
    } else {
        foreach var pathEntry in pathList {
            string fileName = pathEntry.getName();
            if (fileName.contains(ZIP_EXTENSION)) {
                http:Response response = multipartSender(fileLocation + PATH_SEPERATOR, pathEntry.getName(),
                    analyticsUsername, analyticsPassword);
                if (response.statusCode == 201) {
                    printInfo(KEY_UPLOAD_TASK, "Successfully uploaded the file: " + fileName);
                    var result = pathEntry.delete();
                } else {
                    printError(KEY_UPLOAD_TASK, "Error occurred while uploading the file");
                }
                cnt=cnt +1;
            }
        }
        if (cnt == 0) {
            error er = error("No files present to upload.");
            return er;
        } else {
            return ();
        }
    }
}


function informError(error e) {
    printDebug(KEY_UPLOAD_TASK, "Files not present for upload:" + e.reason());
}

function timerTask() {
    map<any> vals= getConfigMapValue(ANALYTICS);
    boolean uploadFiles = <boolean>vals[FILE_UPLOAD_TASK];
    analyticsUsername = <string>vals[USERNAME];
    analyticsPassword = <string>vals[PASSWORD];
    if (uploadFiles) {
        printInfo(KEY_UPLOAD_TASK, "Enabled file uploading task.");
        int|error timeSpan = <int>vals[UPLOADING_TIME_SPAN];
        (function() returns error?) onTriggerFunction = searchFilesToUpload;
        function(error) onErrorFunction = informError;
        if(timeSpan is int){
            // The Task Timer configuration record to configure the Task Listener.
        task:TimerConfiguration timerConfiguration = {
        intervalInMillis: timeSpan,
        initialDelayInMillis: 5000
        };
        
        } else {
            printInfo(KEY_UPLOAD_TASK, "Disabled file uploading task.");
        }
    } else {
        printInfo(KEY_UPLOAD_TASK, "Disabled file uploading task.");
    }
}

// Creating a service on the task Listener.
service timerService on timer {
    // This resource triggers when the timer goes off.
    resource function onTrigger() {
        count = count + 1;
        log:printInfo("Cleaning up...");
        log:printInfo(count.toString());
    }
}

