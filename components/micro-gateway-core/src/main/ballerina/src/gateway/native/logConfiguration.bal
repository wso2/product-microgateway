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

import ballerinax/java;

public function initNativeImpLog4jConfig() {
    string logLevel = getConfigValue(B7A_LOG, LOG_LEVEL, INFO);
    jInitNativeImpLog4jConfig(java:fromString(logLevel));
}

public function jInitNativeImpLog4jConfig(handle isDebugEnabled) = @java:Method {
    name: "initialize",
    class: "org.wso2.micro.gateway.core.logging.Log4j2Configuration"
} external;
