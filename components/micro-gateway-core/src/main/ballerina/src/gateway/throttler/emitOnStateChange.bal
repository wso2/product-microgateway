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

public type EmitOnStateChange object {
    *streams:Window;

    public string key = "";
    public boolean|error isThrottled = false;
    public any[] stateParameters = [];
    public map<boolean> throttleStateMap = {};
    public string streamName = "";

    public function (streams:StreamEvent[])? nextProcessPointer;

    public function __init(function (streams:StreamEvent?[])? nextProcessPointer, any[] stateParameters) {
        self.nextProcessPointer = nextProcessPointer;
        self.stateParameters = stateParameters;
        self.initParameters(stateParameters);
    }

    public function initParameters(any[] stateParameters) {
        if (stateParameters.length() != 3) {
            error err = error("Must have only 3 parameters");
            panic err;
        } else {
            string|error keys = <string>stateParameters[0];
            if (keys is error) {
                error err = error("Key should be a string");
                panic err;
            } else {
                self.key = keys;
            }
            string|error streamName = <string>stateParameters[2];
            if (streamName is error) {
                error err = error("Stream name should be a string");
                panic err;
            } else {
                self.streamName = streamName;
            }
        }
    }

    public function getCandidateEvents(
                            streams:StreamEvent originEvent,
                            (function (map<anydata> e1Data, map<anydata> e2Data) returns boolean)? conditionFunc,
                            boolean isLHSTrigger = true)
                            returns [streams:StreamEvent?, streams:StreamEvent?][] {
            // do nothing;
        return [[(), ()]];
    }

    public function process(streams:StreamEvent?[] streamEvents) {
        streams:StreamEvent[] streamEventsCopy = [];
        foreach var event in streamEvents {
            streams:StreamEvent streamEvent = <streams:StreamEvent>event;
            boolean currentThrottleState = <boolean>streamEvent.data[self.streamName + ".isThrottled"];
            string newKey = <string>streamEvent.data[self.streamName + ".throttleKey"];
            boolean? prevThrottleState = self.throttleStateMap[newKey];
            if (prevThrottleState is boolean) {
                if (prevThrottleState != currentThrottleState || currentThrottleState) {
                    self.throttleStateMap[newKey] = currentThrottleState;
                    //emit
                    streamEventsCopy[streamEventsCopy.length()] = streamEvent;
                }
            } else {
                self.throttleStateMap[newKey] = currentThrottleState;
                //emit
                streamEventsCopy[streamEventsCopy.length()] = streamEvent;
            }
        }
        if (streamEvents.length() > 0) {
            function (streams:StreamEvent?[]) nextProcessor =
                    <function (streams:StreamEvent?[])>self.nextProcessPointer;
            nextProcessor(streamEvents);
        }
    }
};

public function emitOnStateChange(any[] stateParameters, function (streams:StreamEvent?[])? nextProcessPointer = ())
                    returns streams:Window {
    EmitOnStateChange stateChangeProcessor = new(nextProcessPointer, stateParameters);
    return stateChangeProcessor;
}
