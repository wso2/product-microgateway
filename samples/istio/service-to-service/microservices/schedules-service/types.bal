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

public type ScheduleItem record {
    # Id of the schedule item.
    string entryId?;
    # Train starting time.
    string startTime?;
    # Train destination arrival time.
    string endTime?;
    # Train starting station.
    string 'from?;
    # Train destination station.
    string to?;
    # Id of the train.
    string trainId?;
};

public type ScheduleItemInfo record {
    # Id of the schedule item.
    string entryId?;
    # Train starting time.
    string startTime?;
    # Train destination arrival time.
    string endTime?;
    # Train starting station.
    string 'from?;
    # Train destination station.
    string to?;
    # Id of the train.
    string trainId?;
    # Facilities provided in the train.
    string facilities?;
    # Image URL of the train.
    string imageURL?;
};

public type Train record {
    # Id of the schedule item.
    string trainId?;
    # Number of train carriages.
    int numberOfCarriage?;
    # Image URL of the train.
    string imageURL?;
    # Engine model.
    string engineModel?;
    # Facilities provided in the train.
    string facilities?;
};
