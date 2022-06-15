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

import ballerina/log;
import ballerina/http;

listener http:Listener ep0 = new (8081);

service /'schedules\-service/v1 on ep0 {
    isolated resource function get schedules(string? 'from, string? to, string? startTime, string? endTime) returns ScheduleItemInfo[]|http:InternalServerError {
        lock {
            ScheduleItemInfo[] scheduleInfo = [];
            foreach ScheduleItem item in schedules.toArray() {
                ScheduleItemInfo|error info = getScheduleInfo(item);
                if info is ScheduleItemInfo {
                    scheduleInfo.push(info);
                } else {
                    log:printError("Error retriving train info.", info);
                    return <http:InternalServerError>{body: {message: "Internal Server Error"}};
                }
            }
            return scheduleInfo.clone();
        }
    }

    isolated resource function post schedules(@http:Payload {} ScheduleItem payload) returns http:Ok {
        string id;
        lock {
            id = nextIndex.toString();
            payload.entryId = id;
        }
        lock {
            schedules[id] = payload.clone();
        }
        return {body: {status: "Success"}};
    }

    isolated resource function get schedules/[int id]() returns ScheduleItemInfo|http:NotFound|http:InternalServerError {
        lock {
            ScheduleItem? schedule = schedules[id.toString()];
            if schedule is ScheduleItem {
                ScheduleItemInfo|error scheduleInfo = getScheduleInfo(schedule);
                if scheduleInfo is ScheduleItemInfo {
                    return scheduleInfo.clone();
                } else {
                    log:printError("Error retriving train info.", scheduleInfo);
                    return <http:InternalServerError>{body: {message: "Internal Server Error"}};
                }
            } else {
                return <http:NotFound>{body: {status: "Schedule Entry Not Found"}};
            }
        }
    }

    isolated resource function put schedules/[int id](@http:Payload {} ScheduleItem payload) returns http:Ok|http:BadRequest {
        lock {
            ScheduleItem? schedule = schedules[id.toString()];
            if schedule is ScheduleItem {
                payload.entryId = id.toString();
                schedules[id.toString()] = payload.clone();
                return <http:Ok>{body: {status: "Success"}};
            } else {
                return <http:BadRequest>{body: {message: "Schedule Entry Not Found"}};
            }
        }
    }

    isolated resource function delete schedules/[int id]() returns http:Ok|http:BadRequest {
        lock {
            ScheduleItem? schedule = schedules[id.toString()];
            if schedule is ScheduleItem {
                _ = schedules.remove(id.toString());
                return <http:Ok>{body: {status: "Success"}};
            } else {
                return <http:BadRequest>{body: {message: "Schedule Entry Not Found"}};
            }
        }
    }
}
