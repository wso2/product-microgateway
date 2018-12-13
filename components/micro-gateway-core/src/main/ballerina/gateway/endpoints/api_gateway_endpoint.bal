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

endpoint http:Client conditionRetrievalEndpoint {
    url: "https://localhost:9443",
    retryConfig: {
        interval: 3000,
        count: 5,
        backOffFactor: 0.5
    }
};
endpoint http:Client keyValidationEndpoint {
    url:getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, "https://localhost:9443"),
    cache: { enabled: false },
    secureSocket:{
       verifyHostname:getConfigBooleanValue(KM_CONF_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
    }
};

endpoint http:Listener tokenListenerEndpoint {
    port:getConfigIntValue(LISTENER_CONF_INSTANCE_ID ,TOKEN_LISTENER_PORT, 9096),
    host: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HOST,"localhost"),
    secureSocket:{
        keyStore: {
            path: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PATH,
                "${ballerina.home}/bre/security/ballerinaKeystore.p12"),
            password: getConfigValue(LISTENER_CONF_INSTANCE_ID,
                LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina")
        }
    }
};

