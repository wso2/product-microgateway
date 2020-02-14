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

// GRPC filter
public type GrpcFilter object {
    //todo: verify https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
    map<string> httpGrpcStatusCodeMap = { "401" : "16", "403" : "7", "404" : "12", "429" : "8", "500" : "2" };
    map<string> httpGrpcErrorMsgMap = { "401" : "Unauthenticated", "403" : "Unauthorized", "404" : "Service not found",
        "429" : "Too many function calls", "500" : "Internal server error" };

    public function filterRequest(http:Caller caller, http:Request request,@tainted http:FilterContext context)
            returns boolean {
        //Setting UUID
        int startingTime = getCurrentTime();
        context.attributes[REQUEST_TIME] = startingTime;
        checkOrSetMessageID(context);
        setHostHeaderToFilterContext(request, context);
        setLatency(startingTime, context, SECURITY_LATENCY_AUTHN);
        if ( request.getContentType() == GRPC_CONTENT_TYPE_HEADER) {
            addGrpcToFilterContext(context);
            printDebug(KEY_GRPC_FILTER, "Grpc filter is applied for request" + context.attributes[MESSAGE_ID].toString());
        }
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        if (!isGrpcResponse(response, context)) {
           return true;
        }
        printDebug(KEY_GRPC_FILTER, "Grpc filter is applied for response" + context.attributes[MESSAGE_ID].toString());
        string statusCode = response.statusCode.toString();
        if (statusCode == "200") {
           return true;
        }
        string grpcStatus = self.httpGrpcStatusCodeMap[statusCode] ?: "";
        string grpcErrorMessage = self.httpGrpcErrorMsgMap[statusCode] ?: "";
        
        if(statusCode == "") {
           response.setHeader(GRPC_STATUS_HEADER, "2");
           response.setHeader(GRPC_MESSAGE_HEADER, "Response is not recognized by the gateway.");
           return true;
        }
        response.setHeader(GRPC_STATUS_HEADER, grpcStatus, mime:TRAILING);
        response.setHeader(GRPC_MESSAGE_HEADER, grpcErrorMessage, mime:TRAILING);
        printDebug(KEY_GRPC_FILTER, "grpc status is " + grpcStatus + " and grpc Message is " + grpcErrorMessage);
        response.setContentType(GRPC_CONTENT_TYPE_HEADER);

        return true;
    }
};

function addGrpcToFilterContext(http:FilterContext context) {
    //todo: decide to use the invocationContext alone
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    context.attributes[IS_GRPC] = true;
    invocationContext.attributes[IS_GRPC] = true;
    printDebug(KEY_GRPC_FILTER, "\"isGrpc\" key is added to the request " + context.attributes[MESSAGE_ID].toString());
}

//todo: change the method name
function isGrpcResponse(http:Response response, http:FilterContext context) returns boolean {
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
