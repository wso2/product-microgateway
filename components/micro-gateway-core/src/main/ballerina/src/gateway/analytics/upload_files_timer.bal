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

import ballerina/file;
import ballerina/filepath;
import ballerina/http;
import ballerina/stringutils;
import ballerina/task;

string uploadingUrl = "";
string analyticsUsername = "";
string analyticsPassword = "";


function searchFilesToUpload() returns (error?) {
    int cnt = 0;
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR);

    if (!file:exists(fileLocation)) {
        printDebug(KEY_UPLOAD_TASK, "Usage data directory not found");
        return ();
    }

    file:FileInfo[] | error pathList = file:readDir(fileLocation);

    if (pathList is error) {
        printError(KEY_UPLOAD_TASK, "Error occured in getting path lists");
        return pathList;
    } else {
        foreach var pathEntry in pathList {
            string fileName = pathEntry.getName();

            if (contains(fileName, ZIP_EXTENSION)) {
                http:Response response = multipartSender(fileLocation, pathEntry.getName(),
                analyticsUsername, analyticsPassword);
                if (response.statusCode == 201) {
                    printInfo(KEY_UPLOAD_TASK, "Successfully uploaded the file: " + fileName);
                    var result = file:remove(fileLocation + filepath:getPathSeparator() + fileName);
                } else {
                    printError(KEY_UPLOAD_TASK, "Error occurred while uploading the file. Upload request returned" +
                    "with status code : " + response.statusCode.toString());
                }
                cnt = cnt + 1;
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

function timerTask() {
    boolean uploadFiles = <boolean>getConfigBooleanValue(FILE_UPLOAD_ANALYTICS,FILE_UPLOAD_TASK, DEFAULT_TASK_UPLOAD_FILES_ENABLED);
    analyticsUsername = <string>getConfigValue(FILE_UPLOAD_ANALYTICS,USERNAME, DEFAULT_FILE_UPLOAD_ANALYTICS_USERNAME);
    analyticsPassword = <string>getConfigValue(FILE_UPLOAD_ANALYTICS,PASSWORD, DEFAULT_FILE_UPLOAD_ANALYTICS_PASSWORD);
    if (isOldAnalyticsEnabled) {
        //enables config reads for older versions
        uploadFiles = <boolean>getConfigBooleanValue(OLD_FILE_UPLOAD_ANALYTICS,FILE_UPLOAD_TASK, DEFAULT_TASK_UPLOAD_FILES_ENABLED);
        analyticsUsername = <string>getConfigValue(OLD_FILE_UPLOAD_ANALYTICS,USERNAME, DEFAULT_FILE_UPLOAD_ANALYTICS_USERNAME);
        analyticsPassword = <string>getConfigValue(OLD_FILE_UPLOAD_ANALYTICS,PASSWORD, DEFAULT_FILE_UPLOAD_ANALYTICS_PASSWORD);
    }
    if (uploadFiles) {
        printInfo(KEY_UPLOAD_TASK, "Enabled file uploading task.");
        //below config reads enable analytics suppot for old versions
        int | error timeSpan = <int>getConfigIntValue(FILE_UPLOAD_ANALYTICS, UPLOADING_TIME_SPAN, DEFAULT_UPLOADING_TIME_SPAN_IN_MILLIS);
        int delay = <int>getConfigIntValue(FILE_UPLOAD_ANALYTICS,INITIAL_DELAY, DEFAULT_INITIAL_DELAY_IN_MILLIS);
        if (isOldAnalyticsEnabled) {
            timeSpan = <int>getConfigIntValue(OLD_FILE_UPLOAD_ANALYTICS, UPLOADING_TIME_SPAN, DEFAULT_UPLOADING_TIME_SPAN_IN_MILLIS);
            delay = <int>getConfigIntValue(OLD_FILE_UPLOAD_ANALYTICS,INITIAL_DELAY, DEFAULT_INITIAL_DELAY_IN_MILLIS);
        }   
        if (timeSpan is int) {
            // The Task Timer configuration record to configure the Task Listener.
            task:TimerConfiguration timerConfiguration = {
                intervalInMillis: timeSpan,
                initialDelayInMillis: delay
            };
            task:Scheduler timer = new (timerConfiguration);
            var searchResult = timer.attach(searchFiles);
            if (searchResult is error) {
                printError(KEY_UPLOAD_TASK, searchResult.toString());
            }
            var startResult = timer.start();
            if (startResult is error) {
                printError(KEY_UPLOAD_TASK, "Starting the uploading task is failed.");
            }
        }
    } else {
        printInfo(KEY_UPLOAD_TASK, "Disabled file uploading task.");
    }
}

// Creating a service on the task Listener.
service searchFiles = service {
    resource function onTrigger() {
        error? onTriggerFunction = searchFilesToUpload();
        if (onTriggerFunction is error) {
            if (stringutils:equalsIgnoreCase("No files present to upload.", onTriggerFunction.reason())) {
                printDebug(KEY_UPLOAD_TASK, "No files present to upload.");
            } else {
                printError(KEY_UPLOAD_TASK, "Error occured while searching files to Upload: " + onTriggerFunction.toString());
            }
        }
    }
};

