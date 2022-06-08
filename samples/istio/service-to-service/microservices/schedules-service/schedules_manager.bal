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

final string trainServiceURL = os:getEnv("");
final http:Client httpClient = check new (trainServiceURL == "" ? "http://localhost:8080/trains-service/v1" : trainServiceURL);

isolated ScheduleItem[] schedules = [
    {
        entryId: "1",
        startTime: "",
        endTime: "d",
        'from: "X",
        to: "Y",
        trainId: "2"
    }
];
isolated int nextIndex = 2;

isolated function isScheduleExists(int id) returns boolean {
    lock {
        if id > schedules.length() {
            return false;
        }
        return schedules[id - 1]?.entryId != ();
    }
}

isolated function getScheduleInfo(ScheduleItem schedule) returns ScheduleItemInfo|error {
    if schedule?.trainId == () || schedule?.trainId == "" {
        return {};
    }
    string id = schedule?.trainId ?: "1";
    Train train = check httpClient->get("/trains/" + id, {});
    ScheduleItemInfo info = schedule;
    info.imageURL = train?.imageURL ?: "";
    info.facilities = train?.facilities ?: "";
    return info;
}
