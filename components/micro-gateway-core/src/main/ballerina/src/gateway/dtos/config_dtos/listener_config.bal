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

public type ListenerConfigDTO record {
    string keyStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID,
                                        KEY_STORE_PATH,
                                        DEFAULT_KEY_STORE_PATH);
    string keyStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID,
                                            KEY_STORE_PASSWORD,
                                            DEFAULT_KEY_STORE_PASSWORD);
    string trustStorePath = getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH);
    string trustStorePassword = getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD,
                                                DEFAULT_TRUST_STORE_PASSWORD);                                        
};