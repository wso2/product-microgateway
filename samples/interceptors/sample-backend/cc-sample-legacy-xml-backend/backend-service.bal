// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import ballerina/http;
import ballerina/log;

listener http:Listener ep = new (9080);

service / on ep {
    resource function post books(http:Caller caller, http:Request request) returns error? {
        log:printInfo("Backend service is called");
        http:Response resp = new;

        string|http:HeaderNotFoundError pwHeader = request.getHeader("x-user");
        if !(pwHeader is string && pwHeader == "admin") {
            resp.setXmlPayload(xml `<response>Error</response>`);
            resp.statusCode = 401;
            return caller->respond(resp);
        }

        xml xmlPayload = check request.getXmlPayload();
        log:printInfo("Received payload", (), {"message": xmlPayload.toString()});

        resp.setTextPayload("created", "text/plain");
        resp.statusCode = 200;
        return caller->respond(resp);
    }
}
