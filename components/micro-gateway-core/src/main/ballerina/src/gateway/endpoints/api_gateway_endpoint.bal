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

import ballerina/http;




http:Client analyticsFileUploadEndpoint = new (
    getConfigValue(ANALYTICS, UPLOADING_EP, "https://localhost:9444/analytics/v1.0/usage/upload-file"),
    {cache: { enabled: false },
        secureSocket:{
          trustStore: {
              path: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH,
                  "${ballerina.home}/bre/security/ballerinaTruststore.p12"),
              password: getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina")
          },
          verifyHostname:getConfigBooleanValue(HTTP_CLIENTS_INSTANCE_ID, ENABLE_HOSTNAME_VERIFICATION, true)
      }
    }
);

listener http:Listener tokenListenerEndpoint = new (
    getConfigIntValue(LISTENER_CONF_INSTANCE_ID ,TOKEN_LISTENER_PORT, 9096), config = {
        host: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_HOST, "localhost"),
        secureSocket: {
            keyStore: {
                path: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PATH,
                    "${ballerina.home}/bre/security/ballerinaKeystore.p12"),
                password: getConfigValue(LISTENER_CONF_INSTANCE_ID,
                    LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina")
            }
        }
    }
);
