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

# Validate request payload.
#
# + reqPath - project Name
# + requestMethod - Service Name
# + payload - request payload
# + serviceName - serviceName
# +return - status of the validation
public function requestValidate(string reqPath, string requestMethod,  string payload, string serviceName)
    returns handle | error {
     handle requestPath = java:fromString(reqPath);
     handle reqMethod = java:fromString(requestMethod);
     handle requestPayload = java:fromString(payload);
     handle servName = java:fromString(serviceName);
     return jRequestValidate(requestPath, reqMethod, requestPayload, servName);
}

# Validate response payload.
#
# + reqPath - project Name
# + requestMethod - Service Name
# + responseCode - Service Name
# + response - Service Name
# + serviceNme - Service Name
# + return - Extracted resources map
public function responseValidate(string reqPath, string requestMethod, string responseCode, string response,
                                                                           string serviceNme) returns handle | error {
     handle requestPath = java:fromString(reqPath);
     handle reqMethod = java:fromString(requestMethod);
     handle resCode = java:fromString(responseCode);
     handle responsePayload = java:fromString(response);
     handle servName = java:fromString(serviceNme);
     return jResponseValidate(requestPath, reqMethod, resCode, responsePayload, servName);
}

# Extract Resource artifcats.
#
# + projectName - project Name
# + serviceName - Service Name
# + return - Extracted resources map
public function extractJAR(string projectName, string serviceName) returns error? {
     handle prjtName = java:fromString(projectName);
     handle servName = java:fromString(serviceName);
     return extract(prjtName, servName);
}

function extract(handle Name, handle serv) returns error?  = @java:Method {
     name: "extractResources",
     class: "org.wso2.micro.gateway.core.validation.Validate"

} external;

function jRequestValidate(handle resourcePath, handle reqMethod, handle requestPayload, handle serviceName)
                                                                            returns handle | error = @java:Method {
     name: "validateRequest",
     class: "org.wso2.micro.gateway.core.validation.Validate"
} external;

function jResponseValidate(handle resourcePath, handle reqMethod, handle resCode, handle res, handle serName)
                        returns handle | error = @java:Method {
     name: "validateResponse",
     class: "org.wso2.micro.gateway.core.validation.Validate"
} external;
