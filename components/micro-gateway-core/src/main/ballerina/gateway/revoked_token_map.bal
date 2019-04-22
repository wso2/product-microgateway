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

map<string> revokedTokenMap ={};

public function getRevokedTokenMap() returns map<string>{
    return revokedTokenMap;
}

public function addToRevokedTokenMap(map<string> revokedTokens) returns (boolean|()){
    foreach var (revokedTokenKey,revokedTokenValue) in revokedTokens{
        revokedTokenMap[<string>revokedTokenKey]=<string>revokedTokenValue;
    }
    return true;
}

public function retrieveFromRevokedTokenMap(string token) returns (boolean|()){
    foreach var (revokedTokenKey,revokedTokenValue) in revokedTokenMap{
        if(token==revokedTokenKey){
            return true;
        }
    }
    return false;
}
