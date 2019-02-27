// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public function getDecodedJWTPayload(string encodedJWTPayload) returns (json|error) {
    string jwtPayload = check urlDecode(encodedJWTPayload).base64Decode();
    json jwtPayloadJson = {};

    var result = internal:parseJson(jwtPayload);
    if(result is json) {
        jwtPayloadJson = result;
    }
    else {
        return result;
    }
    return jwtPayloadJson;
}

public function urlDecode(string encodedString) returns (string) {
    string decodedString = encodedString.replaceAll("-", "+");
    decodedString = decodedString.replaceAll("_", "/");
    return decodedString;
}

public function getEncodedJWTPayload(string jwtToken) returns (string)|error {
    string[] jwtPayload = jwtToken.split("\\.");
    if (jwtPayload.length() != 3) {
        log:printDebug("Invalid JWT token :" + jwtToken);
        error err = {message:"Invalid JWT token"};
        return err;
    }
    return jwtPayload[1];
}
