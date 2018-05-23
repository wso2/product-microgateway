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

import ballerina /io;


GatewayConf gatewayConf = new;
public type GatewayConf object {
    private {
        KeyManagerConf keyManagerConf;
        ThrottleConf throttleConf;
    }
    public function getGatewayConf() returns (GatewayConf) {
        return gatewayConf;
    }

    public function setKeyManagerConf(KeyManagerConf keyManagerConf) {
        gatewayConf.keyManagerConf = keyManagerConf;
    }
    public function getKeyManagerConf() returns(KeyManagerConf) {
        return gatewayConf.keyManagerConf;
    }
    public function setThrottleConf(ThrottleConf throttleConf) {
        gatewayConf.throttleConf = throttleConf;
    }
    public function getThrottleConf() returns(ThrottleConf) {
        return gatewayConf.throttleConf;
    }
};

   public function getGatewayConfInstance() returns (GatewayConf) {
       return gatewayConf;
   }
