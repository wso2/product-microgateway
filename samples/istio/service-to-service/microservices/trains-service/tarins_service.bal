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

service /'trains\-service/v1 on ep0 {

    isolated resource function get trains() returns Train[] {
        lock {
            return trains.toArray().clone();
        }
    }

    isolated resource function post trains(@http:Payload {} Train payload) returns http:Ok {
        string id;
        lock {
            id = nextIndex.toString();
            payload.trainId = id;
        }
        lock {
            trains[id] = payload.clone();
        }
        return {body: {message: "Train added successfully"}};
    }

    isolated resource function get trains/[int id]() returns Train|http:NotFound {
        lock {
            Train? train = trains[id.toString()];
            if train is Train {
                return train.clone();
            } else {
                return <http:NotFound>{body: {message: "Train Not Found"}};
            }
        }
    }

    isolated resource function put trains/[int id](@http:Payload {} Train payload) returns http:Ok|http:BadRequest {
        lock {
            Train? train = trains[id.toString()];
            if train is Train {
                payload.trainId = id.toString();
                trains[id.toString()] = payload.clone();
                return <http:Ok>{body: {status: "Train updated successfully"}};
            } else {
                return <http:BadRequest>{body: {message: "Train Not Found"}};
            }
        }
    }

    isolated resource function delete trains/[int id]() returns http:Ok|http:BadRequest {
        lock {
            Train? train = trains[id.toString()];
            if train is Train {
                _ = trains.remove(id.toString());
                return <http:Ok>{body: {status: "Train deleted successfully"}};
            } else {
                return <http:BadRequest>{body: {message: "Train Not Found"}};
            }
        }
    }
}
