// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/encoding;
import ballerina/io;
import ballerina/lang.'string as strings;
import ballerina/log;

public function getDecodedJWTPayload(string encodedJWTPayload) returns @tainted (json | error) {
    string jwtPayload = check strings:fromBytes(check encoding:decodeBase64Url(encodedJWTPayload));
    io:StringReader reader = new (jwtPayload);
    json jwtPayloadJson = {};

    var result = reader.readJson();
    if (result is json) {
        jwtPayloadJson = result;
    }
    else {
        return result;
    }
    return jwtPayloadJson;
}

public function urlDecode(string encodedString) returns (string) {
    string decodedString = replaceAll(encodedString, "-", "+");
    decodedString = replaceAll(decodedString, "_", "/");
    return decodedString;
}

public function getEncodedJWTPayload(string jwtToken) returns (string) | error {
    string[] jwtPayload = split(jwtToken, "\\.");
    if (jwtPayload.length() != 3) {
        log:printDebug("Invalid JWT token :" + jwtToken);
        error err = error("Invalid JWT token");
        return err;
    }
    return jwtPayload[1];
}
