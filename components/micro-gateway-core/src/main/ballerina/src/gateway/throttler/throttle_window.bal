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
import ballerina/task;
import ballerina/time;

# ThrottleWindow `throttle(quota, windowSize, partitionAttr)` is a fixed rate time window, which keeps track of
# number of events received (per partition) for a given time period, and notifies whether that number exceeds a given
# quota. And this window will gets updated and emit current events for every input event.
#
# E.g.
#       type Request record {
#           string ip;
#           int resetTimestamp = 0;      // Since ThrottleWindow injects resetTimestamp, remainingQuota
#           int remainingQuota = 0;      // & isThrottled within the window, we need fields
#           boolean isThrottled = false; // to refer to that in selector.
#       };
#
#       type ThrottledRequest record{
#           string ip;
#           int? resetTimestamp;    // Since ThrottleWindow injects resetTimestamp, remainingQuota
#           int? remainingQuota;    // & isThrottled within the window, we need fields
#           boolean? isThrottled;   // to refer to that in selector.
#       };
#
#       stream<Request> inputStream = new;
#       stream<ThrottledRequest > outputStream = new;
#       stream<ThrottledRequest > finaloutputStream = new;
#
#       forever {
#           from inputStream window throttle(5, 1000, inputStream.name)
#           select inputStream.ip, inputStream.resetTimestamp, inputStream.remainingQuota, inputStream.isThrottled
#           => (ThrottledRequest [] requests) {
#               foreach var r in requests {
#                   outputStream.publish(r);
#               }
#           }
#       }
#
#
#
# + quota - quota limit
# + windowSizeInMilliSeconds - size of the window in ms
# + initialDelayInMilliSeconds - time until the first scheduled event
# + partitionAttribute - partition attribute (i.e ip address, session, etc)
# + windowParameters - params for the window
# + nextProcessPointer - next processor pointer
# + scheduler - scheduler which emits timer events
# + counts - counts map to hold current event count per partition
#
#x
public type ThrottleWindow object {
    *streams:Window;
    public int quota = 0;
    public int windowSizeInMilliSeconds = 0;
    public int initialDelayInMilliSeconds = 0;
    public string? partitionAttribute = ();
    public any[] windowParameters;
    public function (streams:StreamEvent?[])? nextProcessPointer = ();
    public task:Scheduler? scheduler = ();
    public map<int> counts = {};

    public function __init(function (streams:StreamEvent?[])? nextProcessPointer, any[] windowParameters) {
        self.nextProcessPointer = nextProcessPointer;
        self.windowParameters = windowParameters;
        self.initParameters(self.windowParameters);
        self.scheduler = new ({
            intervalInMillis: self.windowSizeInMilliSeconds,
            initialDelayInMillis: self.initialDelayInMilliSeconds
        });
        checkpanic self.getScheduler().attach(eventInjectorService, self);
        checkpanic self.getScheduler().start();
    }

    public function getScheduler() returns task:Scheduler {
        return <task:Scheduler>self.scheduler;
    }

    # The `initParameters` function verify and sets the parameters for the ThrottleWindow.
    # + parameters - Parameters for the ThrottleWindow.
    public function initParameters(any[] parameters) {
        if (parameters.length() == 2 || parameters.length() == 3) {
            any parameter0 = parameters[0];
            if (parameter0 is int) {
                self.quota = parameter0;
            } else {
                error err = error("ThrottleWindow `quota` expects an int parameter");
                panic err;
            }

            any parameter1 = parameters[1];
            if (parameter1 is int) {
                self.windowSizeInMilliSeconds = parameter1;
                int currentTimeMs = time:currentTime().time;
                self.initialDelayInMilliSeconds = self.getStartOfNextBatch(parameter1) - currentTimeMs;
            } else {
                error err = error("ThrottleWindow `windowSizeInMs` expects an int parameter");
                panic err;
            }

            if (parameters.length() == 3) {
                any parameter2 = parameters[2];
                if (parameter2 is string) {
                    self.partitionAttribute = parameter2;
                } else {
                    error err = error("ThrottleWindow `partitionAttribute` expects an string parameter");
                    panic err;
                }
            }
        } else {
            error err = error("ThrottleWindow should only have two parameters (<int> quota, <int> windowSizeInMs, " +
            "<string> partitionAttribute), but found " + parameters.length().toString() + " input attributes");
            panic err;
        }
    }

    # The `process` function process the incoming events to the events and update the current state of the window.
    # + streamEvents - The array of stream events to be processed.
    public function process(streams:StreamEvent?[] streamEvents) {
        streams:StreamEvent?[] currentEvents = [];
        foreach var evt in streamEvents {
            streams:StreamEvent event = <streams:StreamEvent>evt;
            string pk = self.getPartitionKey(event, self.partitionAttribute);
            if (event.eventType == "CURRENT") {
                self.counts[pk] = <int>self.counts[pk] + 1;
                self.addThrottleData(event, <int>self.counts[pk]);
                currentEvents[currentEvents.length()] = event;
            }
            if (event.eventType == "TIMER") {
                self.counts.removeAll();
            } else {
                continue;
            }
        }

        any nextProcessFuncPointer = self.nextProcessPointer;
        if (nextProcessFuncPointer is function(streams:StreamEvent?[])) {
            nextProcessFuncPointer(currentEvents);
        }
    }

    # Add throttle data (resetTimestamp, remainingQuota, isThrottled) to the current event.
    # + evt - Stream event to inject throttle data.
    # + currentCount - Current usage (count).
    public function addThrottleData(streams:StreamEvent evt, int currentCount) {
        map<anydata> throttleData = {};
        int resetTimestamp = self.getStartOfNextBatch(self.windowSizeInMilliSeconds);
        int remainingQuota = self.quota - currentCount;
        boolean isThrottled = self.quota < currentCount;
        throttleData[evt.getStreamName() + ".resetTimestamp"] = resetTimestamp;
        throttleData[evt.getStreamName() + ".remainingQuota"] = remainingQuota;
        throttleData[evt.getStreamName() + ".isThrottled"] = isThrottled;
        evt.addData(throttleData);
    }

    # Get the start timestamp of the next timeBatch
    # + batchSizeMs - Size of the time bucket in milliseconds.
    # + return - Returns the starting timestamp of the next time bucket.
    public function getStartOfNextBatch(int batchSizeMs) returns int {
        int timestampMs = time:currentTime().time;
        int startOfNextSec = timestampMs + (batchSizeMs - (timestampMs % batchSizeMs));
        return startOfNextSec;
    }

    # Get partition key.
    # + evt - Stream event to inject throttle data.
    # + partitionAttribute - Attribute key.
    # + return - returns partition key.
    public function getPartitionKey(streams:StreamEvent evt, string? partitionAttribute) returns string {
        anydata tmp = "default";
        if (partitionAttribute is string) {
            tmp = <anydata>evt.get(partitionAttribute);
        }
        string pk = tmp.toString();
        if (!self.counts.hasKey(pk)) {
            self.counts[pk] = 0;
        }
        return pk;
    }

    # Returns the events(State) which match with the where condition in the join clause for a given event.
    # + originEvent - The event against which the state or the events being held by the window is matched.
    # + conditionFunc - The function pointer to the lambda function which contain the condition logic in where clause.
    # + isLHSTrigger - Specify if the join is triggered when the lhs stream received the events, if so it should be
    #                  true. Most of the time it is true. In rare cases, where the join is triggered when the rhs
    #                   stream receives events this should be false.
    # + return - Returns an array of 2 element tuples of events. A tuple contains the matching events one from lhs
    #            stream and one from rhs stream.
    public function getCandidateEvents(
    streams:StreamEvent originEvent,
 (function (map<anydata> e1Data, map<anydata> e2Data) returns boolean)? conditionFunc,
    public boolean isLHSTrigger = true)
    returns @tainted [streams:StreamEvent?, streams:StreamEvent?][] {
        // do nothing;
        return [[(), ()]];
    }

    # Return current state to be saved as a map of `any` typed values.
    # + return - A map of `any` typed values.
    public function saveState() returns map<any> {
        // do nothing;
        return {};
    }

    # Restores the saved state which is passed as a map of `any` typed values.
    # + state - A map of typed `any` values. This map contains the values to be restored from the persisted data.
    public function restoreState(map<any> state) {
    // do nothing;
    }

};

# Triggers the timer event generation at the given timestamp.
service eventInjectorService = service {
    resource function onTrigger(@tainted ThrottleWindow throttleWindow) {
        map<anydata> data = {};
        int currentTime = time:currentTime().time;
        streams:StreamEvent timerEvent = new (["timer", data], "TIMER", currentTime);
        streams:StreamEvent?[] timerEventWrapper = [];
        timerEventWrapper[0] = timerEvent;
        throttleWindow.process(timerEventWrapper);
    }
};

# The `throttle` function creates a `ThrottleWindow` object and returns it.
# + windowParameters - Arguments which should be passed with the window function in the streams query in the order
#                      they appear in the argument list.
# + nextProcessPointer - The function pointer to the `process` function of the next processor.
# + return - Returns the created window.
public function throttle(any[] windowParameters, public function (streams:StreamEvent?[])? nextProcessPointer = ())
returns streams:Window {
    ThrottleWindow throttleWindow = new (nextProcessPointer, windowParameters);
    return throttleWindow;
}
