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

isolated map<Train> trains = {
    "1": {
        trainId: "1",
        engineModel: "Heavier",
        numberOfCarriage: 7,
        facilities: "Canteen",
        imageURL: "https://abc.train.org/resources/images/0931.png"
    },
    "2": {
        trainId: "2",
        engineModel: "Heavier",
        numberOfCarriage: 12,
        facilities: "WiFi, Canteen",
        imageURL: "https://abc.train.org/resources/images/19234.png"
    },
    "3": {
        trainId: "3",
        engineModel: "TigerJet",
        numberOfCarriage: 8,
        facilities: "Canteen",
        imageURL: "https://abc.train.org/resources/images/87622.png"
    },
    "4": {
        trainId: "4",
        engineModel: "TigerJet",
        numberOfCarriage: 10,
        facilities: "WiFi, Canteen",
        imageURL: "https://abc.train.org/resources/images/47102.png"
    }
};

isolated int nextIndex = 5;
