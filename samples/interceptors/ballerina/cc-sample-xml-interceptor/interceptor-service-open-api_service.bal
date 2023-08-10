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
import ballerina/xmldata;
import ballerina/lang.array;
import ballerina/lang.value;

listener http:Listener ep0 = new (9081, secureSocket = {
    key: {
        certFile: "/home/ballerina/certs/interceptor.pem",
        keyFile: "/home/ballerina/certs/interceptor.key"
    },

    mutualSsl: {
        verifyClient: http:REQUIRE,
        cert: "/home/ballerina/certs/mg.pem"
    }
});

service /api/v1 on ep0 {
    resource function post 'handle\-request(@http:Payload RequestHandlerRequestBody payload) returns OkRequestHandlerResponseBody {
        return {body: requestHandler(payload)};
    }
    resource function post 'handle\-response(@http:Payload ResponseHandlerRequestBody payload) returns OkResponseHandlerResponseBody {
        return {body: responseHandler(payload)};
    }
}

function requestHandler(RequestHandlerRequestBody payload) returns RequestHandlerResponseBody {
    json|error clientPayload = getClientBodyAsJSON(payload);

    string respBody; // response to be sent to the backend
    if clientPayload is json {
        xml?|error xmlData = xmldata:fromJson(clientPayload); // convert client JSON request body to XML
        if xmlData is xml {
            respBody = xmlData.toString();
        } else if xmlData is () {
            respBody = "<root>nil</root>";
            log:printInfo("Xml data is nil");
        } else {
            log:printError("Error while converting to XML", xmlData);
            respBody = "<error>mediation error</error>";
        }
    } else {
        log:printError("Error while converting request body to json", clientPayload);
        respBody = "<error>mediation error</error>";
    }

    byte[] respBodyBytes = respBody.toBytes();
    string respBodyStr = array:toBase64(respBodyBytes); // base64 encode the response body

    return { // build the response of the interceptor service
        body: respBodyStr,
        headersToAdd: {
            "x-user": "admin"
        },
        headersToReplace: {
            "content-type": "application/xml"
        }
    };
}

function responseHandler(ResponseHandlerRequestBody payload) returns ResponseHandlerResponseBody {
    int statusCode = payload.responseCode; // get backend HTTP status code

    if statusCode == 200 {
        return { // build the response of the interceptor service
            responseCode: 201
        };
    }

    return {}; // if status code is not 200, do not do any changes to backend response
}

function getClientBodyAsJSON(RequestHandlerRequestBody payload) returns json|error {
    string bodyBase64 = payload?.requestBody ?: ""; // read the "requestBody" which is the base64 encoded client request body
    byte[] bodyBytes = check array:fromBase64(bodyBase64); // get decoded client request body
    string bodyStr = check 'string:fromBytes(bodyBytes);
    return value:fromJsonString(bodyStr); // convert client request body string to json
}
