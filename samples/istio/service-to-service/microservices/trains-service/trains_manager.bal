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

isolated Train[] trains = [
    {
        trainId: "1",
        engineModel: "F",
        numberOfCarriage: 10,
        facilities: "WiFi",
        imageURL: "foo.com"
    }
];
isolated int nextIndex = 2;

isolated function isTrainExists(int id) returns boolean {
    lock {
        if id > trains.length() {
            return false;
        }
        return trains[id - 1]?.trainId != ();
    }
}
