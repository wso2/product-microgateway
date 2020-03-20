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

# Invoke callhome.
#
# + trustStoreLocation - truststore location
# + trustStorePassword - truststore password
public function runCallHome(handle trustStoreLocation, handle trustStorePassword ) = @java:Method  {
    name: "runCallHome",
    class: "org.wso2.micro.gateway.core.callhome.Callhome"
} external;

public function invokeCallHome() {
    handle trustStoreLocationUnresolve = java:fromString(getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH));
    handle trustStoreLocation = getTrustStoreLocation(trustStoreLocationUnresolve);
    string trustStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD);

    runCallHome(trustStoreLocation,java:fromString(trustStorePassword));
}


public function getTrustStoreLocation(handle pathsubstring) returns handle = @java:Method  {
    name: "getTrustStoreLocation",
    class: "org.wso2.micro.gateway.core.callhome.Callhome"
} external;
