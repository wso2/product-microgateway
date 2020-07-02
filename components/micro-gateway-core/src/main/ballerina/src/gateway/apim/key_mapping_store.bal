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

import ballerina/http;

# Data holder for Key Mapping details available in Gateway Pilot node. KeyMapping object keeps
# the mapping between an `Application` and a consumer key generated for an oauth application.
# 
# + keyMaps - Map of `KeyMap` objects
type KeyMappingDataStore object {
    map<KeyMap> keyMaps = {};

    private string pilotUsername;
    private string pilotPassword;
    private string serviceContext;

    public function __init(string username, string password, string context) {
        self.pilotUsername = username;
        self.pilotPassword = password;
        self.serviceContext = context + "/application-key-mappings";
        if (apimEventHubEnabled) {
            future<()> keyMappingsFetch = start self.fetchKeyMappings();
        }
    }

    # Retrieve a specific `KeyMap` object from the KeyMapping Data Store.
    #
    # + consumerKey - Consumer key of an oauth application that belongs to an `Application`
    # + return - `KeyMap` object for a provided consumer key. If no match was found `()` is returned.
    function getMapping(string consumerKey) returns (KeyMap | ()) {
        if (self.keyMaps.hasKey(consumerKey)) {
            return self.keyMaps.get(consumerKey);
        }
        return ();
    }

    function addKeyMapping(KeyMap keyMap) {
        string mapKey = keyMap.consumerKey;
        lock {
            //Writing event should be locked, due to worker threads are reading the map during request validations
            self.keyMaps[mapKey] = keyMap;
        }
    }

    function removeKeyMapping(KeyMap keyMap) {
        lock {
            //Remove event should be locked, due to worker threads are reading the map during request validations
            KeyMap removedKey = self.keyMaps.remove(keyMap.consumerKey);
        }
    }

    private function fetchKeyMappings() {
        string basicAuthHeader = buildBasicAuthHeader(self.pilotUsername, self.pilotPassword);
        http:Request keyReq = new;
        keyReq.setHeader(AUTHORIZATION_HEADER, basicAuthHeader);
        var response = gatewayPilotEndpoint->get(self.serviceContext, message = keyReq);
        if (response is http:Response) {
            map<KeyMap> keyMap = {};
            var payload = response.getJsonPayload();
            if (payload is json) {
                printDebug(KEY_KEYMAP_STORE, "key map list is : " + payload.toJsonString());
                json[] list = <json[]>payload.list;
                printDebug(KEY_KEYMAP_STORE, "Received valid key mapping details");
                foreach json jsonMap in list {
                    KeyMap mapping = {
                        appId: <int>jsonMap.applicationId,
                        consumerKey: jsonMap.consumerKey.toString(),
                        keyType: jsonMap.keyType.toString(),
                        keyManager : jsonMap.keyManager.toString()
                    };
                    self.keyMaps[mapping.consumerKey] = mapping;
                }
            } else {
                printError(KEY_KEYMAP_STORE, "Received invalid key mapping data", payload);
            }
        } else {
            printError(KEY_KEYMAP_STORE, "Failed to retrieve key mapping data", response);
        }
    }
};
