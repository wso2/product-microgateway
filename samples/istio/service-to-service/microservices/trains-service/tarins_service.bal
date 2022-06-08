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

import ballerina/http;

listener http:Listener ep0 = new (8080);

service /'train\-service/v1 on ep0 {

    isolated resource function get trains() returns Train[] {
        lock {
            Train[] filteredTrains = [];
            foreach Train t in trains {
                if t?.trainId != () {
                    filteredTrains.push(t);
                }
            }
            return filteredTrains.clone();
        }
    }

    isolated resource function post trains(@http:Payload {} Train payload) returns http:Ok {
        lock {
            payload.trainId = nextIndex.toString();
        }
        lock {
            trains.push(payload.clone());
        }
        return {body: {message: "Train added successfully"}};
    }

    isolated resource function get trains/[int id]() returns Train|http:NotFound {
        if !isTrainExists(id) {
            return <http:NotFound>{body: {message: "Train Not Found"}};
        } else {
            lock {
                return trains[id - 1].clone();
            }
        }
    }

    isolated resource function put trains/[int id](@http:Payload {} Train payload) returns http:Ok|http:NotFound {
        if !isTrainExists(id) {
            return <http:NotFound>{body: {message: "Train Not Found"}};
        } else {
            payload.trainId = id.toString();
            lock {
                trains[id - 1] = payload.clone();
            }
            return <http:Ok>{body: {message: "Train updated successfully"}};
        }
    }

    isolated resource function delete trains/[int id]() returns http:Ok|http:NotFound {
        if !isTrainExists(id) {
            return <http:NotFound>{body: {message: "Train Not Found"}};
        } else {
            lock {
                trains[id - 1] = {};
            }
            return <http:Ok>{body: {status: "Train deleted successfully"}};
        }
    }
}
