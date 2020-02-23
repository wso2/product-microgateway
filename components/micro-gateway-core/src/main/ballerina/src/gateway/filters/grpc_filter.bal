// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/mime;
import ballerina/runtime;

//todo: verify https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
map<string> httpGrpcStatusCodeMap = { "401" : "16", "403" : "7", "404" : "12", "429" : "8", "500" : "2" };
map<string> httpGrpcErrorMsgMap = { "404" : "Unimplemeted", "500" : "Internal server error" };

// GRPC filter
public type GrpcFilter object {
    public function filterRequest(http:Caller caller, http:Request request,@tainted http:FilterContext context)
            returns boolean {
        if ( request.getContentType() == GRPC_CONTENT_TYPE_HEADER) {
            addGrpcToFilterContext(context);
            printDebug(KEY_GRPC_FILTER, "Grpc filter is applied for request" + context.attributes[MESSAGE_ID].toString());
        }
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        if (!needGrpcResponseFiltering(response, context)) {
           return true;
        }
        printDebug(KEY_GRPC_FILTER, "Grpc filter is applied for response" + context.attributes[MESSAGE_ID].toString());
        string statusCode = response.statusCode.toString();
        if (statusCode == "200") {
           return true;
        }
        //if the grpc status and error message is set, it is not required to further process
        if (response.hasHeader(GRPC_STATUS_HEADER, mime:TRAILING) &&
                response.hasHeader(GRPC_MESSAGE_HEADER, mime:TRAILING)) {
            return true;
        }
        attachGenericGrpcErrorMsg (response);
        return true;
    }
};

function addGrpcToFilterContext(http:FilterContext context) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    context.attributes[IS_GRPC] = true;
    invocationContext.attributes[IS_GRPC] = true;
    printDebug(KEY_GRPC_FILTER, "\"isGrpc\" key is added to the request " + context.attributes[MESSAGE_ID].toString());
}

function needGrpcResponseFiltering(http:Response response, http:FilterContext context) returns boolean {
    //todo: check if needs to check the content type as well.
    //if backend sends the grpc error message, it is not required to be modified from the gateway
    if(response.hasHeader(GRPC_STATUS_HEADER)) {
        string grpcStatus = response.getHeader(GRPC_STATUS_HEADER).toUpperAscii();
        if(grpcStatus != "UNIMPLEMENTED") {
            return false;
        }
    }
    any isGrpcAttr = context.attributes[IS_GRPC];
    if(isGrpcAttr is boolean){
        return isGrpcAttr;
    }
    return false;
}

public function attachGrpcErrorHeaders(http:Response response, string errorMsg) {
    string statusCode = response.statusCode.toString();
    string grpcStatus = httpGrpcStatusCodeMap[statusCode] ?: "";
    string grpcErrorMessage = errorMsg;
    if (grpcStatus == ""){
        response.setHeader(GRPC_STATUS_HEADER, "2");
        response.setHeader(GRPC_MESSAGE_HEADER, "Response is not recognized by the gateway.");
        return;
    }
    response.setHeader(GRPC_STATUS_HEADER, grpcStatus, mime:TRAILING);
    response.setHeader(GRPC_MESSAGE_HEADER, grpcErrorMessage, mime:TRAILING);
    response.setContentType(GRPC_CONTENT_TYPE_HEADER);
    printDebug(KEY_GRPC_FILTER, "grpc status is " + grpcStatus + " and grpc Message is " + grpcErrorMessage);

}

function attachGenericGrpcErrorMsg(http:Response response) {
    if (response.hasHeader(GRPC_MESSAGE_HEADER)) {
        return;
    }
    string statusCode = response.statusCode.toString();
    string grpcErrorMessage = httpGrpcErrorMsgMap[statusCode] ?: "";
    attachGrpcErrorHeaders(response, grpcErrorMessage);
}
