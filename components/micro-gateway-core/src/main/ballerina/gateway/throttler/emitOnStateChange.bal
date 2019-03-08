// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/streams;
import ballerina/time;

public type EmitOnStateChange object {

    public string|error key = "";
    public boolean|error isThrottled = false;
    public any[] stateParameters= [];
    public map<any> throttleStateMap = {};

    public function (streams:StreamEvent?[])? nextProcessPointer;

    public function __init(function(streams:StreamEvent?[])? nextProcessPointer, any[] stateParameters) {

        self.nextProcessPointer = nextProcessPointer;
        self.stateParameters = stateParameters;
        self.initParameters(stateParameters);
    }

    public function initParameters(any[] stateParameters) {
        if (stateParameters.length() != 2) {
            error err = error("Must have only 2 parameters");
            panic err;
        } else {
            self.key = <string>stateParameters[0];
            if (self.key is error || self.isThrottled is error){
                error err = error("Key should be a string");
                panic err;
            }

            self.isThrottled = <boolean>stateParameters[1];

            if (self.isThrottled is error){
                error err = error("Value should be boolean");
                panic err;
            }
        }
    }

    public function getCandidateEvents(
                        streams:StreamEvent originEvent,
                        (function (map<anydata> e1Data, map<anydata> e2Data) returns boolean)? conditionFunc,
                        boolean isLHSTrigger = true)
                        returns (streams:StreamEvent?, streams:StreamEvent?)[] {
        // do nothing;
        return [((), ())];
    }


    public function process(streams:StreamEvent?[] streamEvents) {

        streams:StreamEvent?[] streamEventsCopy = [];

        foreach var event in streamEvents {
            streams:StreamEvent streamEvent = <streams:StreamEvent>event;

            boolean currentThrottleState = <boolean>event.data.isThrottled;

            string newKey = <string>event.data.key;

            boolean lastThrottleState = <boolean>self.throttleStateMap.newKey;

            if(currentThrottleState== lastThrottleState && !currentThrottleState){
                //


            }else {
                self.throttleStateMap[newKey] = currentThrottleState;
                streamEventsCopy[streamEventsCopy.length()] = event;

            }


        }
        if (streamEventsCopy.length() > 0) {
            self.nextProcessPointer.call(streamEventsCopy);
        }


    }


};

public function emitOnStateChange(any[] stateParameters, function (streams:StreamEvent?[])? nextProcessPointer = ())
                    returns EmitOnStateChange {
    EmitOnStateChange stateChangeProcessor = new(nextProcessPointer, stateParameters);
    return stateChangeProcessor;
}