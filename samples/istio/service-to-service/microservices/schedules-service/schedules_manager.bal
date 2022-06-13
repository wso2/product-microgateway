// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import ballerina/os;
import ballerina/http;

final string trainServiceURL = os:getEnv("TRAIN_SERVICE_URL");
final string trainServiceApiKey = os:getEnv("TRAIN_SERVICE_API_KEY");
final string trainServiceHost = os:getEnv("TRAIN_SERVICE_HOST");
final http:Client httpClient = check new (trainServiceURL == "" ? "http://localhost:8080/trains-service/v1" : trainServiceURL);

isolated map<ScheduleItem> schedules = {
    "1": {
        entryId: "1",
        startTime: "14:50",
        endTime: "19:59",
        'from: "London",
        to: "Glasgow",
        trainId: "1"
    },
    "2": {
        entryId: "2",
        startTime: "14:50",
        endTime: "19:20",
        'from: "London",
        to: "Edinburgh",
        trainId: "2"
    },
    "3": {
        entryId: "3",
        startTime: "07:10",
        endTime: "15:20",
        'from: "London",
        to: "Cardiff",
        trainId: "1"
    },
    "4": {
        entryId: "3",
        startTime: "08:30",
        endTime: "08:30",
        'from: "London",
        to: "Manchester",
        trainId: "4"
    }
};
isolated int nextIndex = 5;

isolated function getScheduleInfo(ScheduleItem schedule) returns ScheduleItemInfo|error {
    if schedule?.trainId == () || schedule?.trainId == "" {
        return {};
    }
    string id = schedule?.trainId ?: "1";
    Train train = check httpClient->get("/trains/" + id, {apikey: trainServiceApiKey, host: trainServiceHost});
    ScheduleItemInfo info = schedule;
    info.imageURL = train?.imageURL ?: "";
    info.facilities = train?.facilities ?: "";
    return info;
}
