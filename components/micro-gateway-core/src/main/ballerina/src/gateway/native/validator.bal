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

# Validate payload against the schema.
#
# + jsonSchema - json Schema which is defined in the swagger.
# + payload - request/response payload
# + return - An error if an error occurs during the validation process
public function validator(string jsonSchema, string payload) returns error? {
    handle schema = java:fromString(jsonSchema);
    handle payloadObject = java:fromString(payload);
    return jValidator(schema, payloadObject);

}

function jValidator(handle schema, handle payloadObject) returns error? = @java:Method {
    name: "validator",
    class: "org.wso2.micro.gateway.core.validation.Validator"
} external;
