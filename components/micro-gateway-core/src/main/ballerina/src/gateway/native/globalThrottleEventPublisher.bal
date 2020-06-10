// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/java;
import ballerina/config;
import ballerina/stringutils;

# Initialize the Binary Throttle Data Publisher
public function initBinaryThrottleDataPublisher() {
    loadTMBinaryAgentConfiguration();
    loadTMBinaryPublisherConfiguration();
    jinitBinaryThrottleDataPublisher();
}

# Publish the throttleEvent
# +throttleEvent - throttle Event
public function publishBinaryGlobalThrottleEvent(RequestStreamDTO throttleEvent) {
    jPublishGlobalThrottleEvent(throttleEvent);
}

# set configurations related to binary publisher
function loadTMBinaryPublisherConfiguration() {
    string receiverURLGroup;
    string authURLGroup;
    TMBinaryPublisherConfigDto dto = {};
    [receiverURLGroup, authURLGroup] = processTMPublisherURLGroup();
    dto.receiverURLGroup = receiverURLGroup;
    dto.authURLGroup = authURLGroup;
    jSetTMBinaryPublisherConfiguration(dto);
}

# The receiverURLGroup and the authURLGroup is preprocessed such that to make them compatible with the binary agent.
# + return - [receiverURLGroup , authURLGroup]
function processTMPublisherURLGroup () returns [string, string] {
    string restructuredReceiverURL = "";
    string restructuredAuthURL = "";
    map<anydata>[] | error urlGroups = map<anydata>[].constructFrom(config:getAsArray(TM_BINARY_URL_GROUP));

    if (urlGroups is map<anydata>[] && urlGroups.length() > 0) {
        foreach map<anydata> urlGroup in urlGroups {

            string urlType = "";
            if (urlGroup.hasKey(TM_BINARY_URL_GROUP_TYPE)) {
                if (urlGroup[TM_BINARY_URL_GROUP_TYPE] is string) {
                    urlType = <string> urlGroup[TM_BINARY_URL_GROUP_TYPE];
                } else {
                    printWarn(KEY_GLOBAL_THROTTLE_EVENT_PUBLISHER, TM_BINARY_URL_GROUP + " element's "
                        + TM_BINARY_URL_GROUP_TYPE + " property is not a valid string. Hence proceeding as a "
                        + TM_BINARY_FAILOVER + " configuration.");
                    urlType = TM_BINARY_FAILOVER;
                }
            } else {
                printWarn(KEY_GLOBAL_THROTTLE_EVENT_PUBLISHER, TM_BINARY_URL_GROUP_TYPE + " property value is not "
                    + TM_BINARY_LOADBALANCE + " or " + TM_BINARY_FAILOVER + ". Hence proceeding as a "
                    + TM_BINARY_FAILOVER + " configuration.");
                urlType = TM_BINARY_FAILOVER;
            }

            string | error receiverUrl = processSingleURLGroup(urlGroup[TM_BINARY_RECEIVER_URL], urlType);
            string | error authUrl = processSingleURLGroup (urlGroup[TM_BINARY_AUTH_URL], urlType);
            if (receiverUrl is string && authUrl is string) {
                restructuredReceiverURL += receiverUrl + ",";
                restructuredAuthURL += authUrl + ",";
            } else {
                if (receiverUrl is error) {
                    printError(KEY_GLOBAL_THROTTLE_EVENT_PUBLISHER, TM_BINARY_URL_GROUP + " element is skipped as "
                        + TM_BINARY_RECEIVER_URL + " property is not provided correctly.", receiverUrl);
                } else if (authUrl is error) {
                    printError(KEY_GLOBAL_THROTTLE_EVENT_PUBLISHER, TM_BINARY_URL_GROUP + " element is skipped as "
                        + TM_BINARY_AUTH_URL + " property is not provided correctly.", authUrl);
                }
            }
        }
        //to remove the final ',' in the URLs
        if(restructuredReceiverURL != "" && restructuredAuthURL != "") {
            restructuredReceiverURL = restructuredReceiverURL.substring(0, restructuredReceiverURL.length() - 1);
            restructuredAuthURL = restructuredAuthURL.substring(0, restructuredAuthURL.length() - 1);
            return [restructuredReceiverURL, restructuredAuthURL];
        }
    } else {
        printDebug(KEY_GLOBAL_THROTTLE_EVENT_PUBLISHER, TM_BINARY_AUTH_URL + " property is not identified " +
            "in the configuration file.");
    }
    //If receiverURLGroup and AuthURLGroup is not set, task will proceed with the default configurations.
    printDebug(KEY_GLOBAL_THROTTLE_EVENT_PUBLISHER, "Proceeding with the default parameters for " +
        TM_BINARY_URL_GROUP);
    return [DEFAULT_TM_RECEIVER_URL_GROUP,DEFAULT_TM_AUTH_URL_GROUP];
}

function processSingleURLGroup(anydata urlArrayConfigValue, string urlType) returns string | error {

    if (urlArrayConfigValue is anydata[]) {
        anydata[] urlArray = <anydata[]> urlArrayConfigValue;
        string concatenatedURLString = "{";
        if (urlArray.length() == 1) {
            string | error url = <string> urlArray[0];
            if (url is string) {
                return "{" + url + "}";
            }
            return url;
        }
        foreach var urlItem in urlArray {
            string | error url = <string> urlItem;
            if (url is string) {
                if (stringutils:equalsIgnoreCase(urlType, TM_BINARY_LOADBALANCE)) {
                    concatenatedURLString += url + TM_BINARY_LOADBALANCE_SEPARATOR;
                } else if (stringutils:equalsIgnoreCase(urlType, TM_BINARY_FAILOVER)) {
                    concatenatedURLString += url + TM_BINARY_FAILOVER_SEPARATOR;
                } else {
                    concatenatedURLString += url + TM_BINARY_FAILOVER_SEPARATOR;
                }
            } else {
                return url;
            }
        }
        //to remove the trailing '|' or ','
        concatenatedURLString = concatenatedURLString.substring(0, concatenatedURLString.length() - 1) + "}";
        return concatenatedURLString;
    } else {
        string | error url = <string> urlArrayConfigValue;
        if (url is string) {
            return "{" + url + "}";
        } else {
            //explicitly throwing an error
            return url;
        }
    }
}

# set configurations related to binary agent
function loadTMBinaryAgentConfiguration() {
    TMBinaryAgentConfigDto dto = {};
    jSetTMBinaryAgentConfiguration(dto);
}

function jinitBinaryThrottleDataPublisher() = @java:Method {
    name: "startThrottlePublisherPool",
    class: "org.wso2.micro.gateway.core.globalthrottle.ThrottleAgent"
} external;

function jPublishGlobalThrottleEvent(RequestStreamDTO throttleEvent) = @java:Method {
        name: "publishNonThrottledEvent",
        class: "org.wso2.micro.gateway.core.globalthrottle.ThrottleAgent"
} external;

function jSetTMBinaryAgentConfiguration(TMBinaryAgentConfigDto dto) = @java:Method {
        name: "setTMBinaryAgentConfiguration",
        class: "org.wso2.micro.gateway.core.globalthrottle.ThrottleAgent"
} external;

function jSetTMBinaryPublisherConfiguration(TMBinaryPublisherConfigDto dto) = @java:Method {
        name: "setTMBinaryPublisherConfiguration",
        class: "org.wso2.micro.gateway.core.globalthrottle.ThrottleAgent"
} external;
