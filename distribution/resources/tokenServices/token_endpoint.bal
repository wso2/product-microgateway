// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file   except
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
import wso2/gateway;

@http:ServiceConfig {
    basePath:"/token"
}
service tokenService on tokenListenerEndpoint {

    @http:ResourceConfig {
        path: "/*"
    }
    resource function tokenResource(http:Caller caller, http:Request req) {
        gateway:checkExpectHeaderPresent(req);
        http:Client tokenEndpointClient = gateway:getTokenEndpoint();
        var response = tokenEndpointClient->forward(gateway:getConfigValue(gateway:KM_CONF_INSTANCE_ID, gateway:KM_TOKEN_CONTEXT, "/oauth2") +
                 req.rawPath, req);
        http:Response forwardedResponse = new;
        if(response is http:Response) {
            forwardedResponse = response;
        } else {
            http:Response errorResponse = new;
            json errMsg = { "error": "error occurred while invoking the token endpoint" };
            errorResponse.setJsonPayload(errMsg);
            forwardedResponse = errorResponse;
        }
        var result = caller->respond(forwardedResponse);
        if (result is error) {
           log:printError("Error when responding during the token endpoint request", err = result);
        }
    }
}