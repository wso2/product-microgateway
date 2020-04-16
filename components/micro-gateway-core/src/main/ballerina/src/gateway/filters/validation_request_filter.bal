// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 import ballerina/stringutils;
 import ballerina/runtime;

// Validation Request filter
public type ValidationRequestFilter object {

    *http:RequestFilter;
    
    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context)
                                                                                          returns boolean {
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_VALIDATION_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        //skip validation filter if the request is gRPC
        if (isGrpcRequest(context)) {
            printDebug(KEY_VALIDATION_FILTER, "Skip the filter as the request is GRPC");
            return true;
        }
        if (!enableRequestValidation) {
            return true;
        }
        printDebug(KEY_VALIDATION_FILTER, "The request validation filter");
        boolean result = doValidationFilterRequest(caller, request, context);
        return result;
    }
};

function doValidationFilterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext)
                                                                                                    returns boolean {
    json|error models = {};
    json finalSchema = {};
    string modelName;
    json relElements = {};
    json result = {};
    json|error bodyModels = {};
    json|error paths = {};
    string requestPath = getRequestPathFromFilterContext(filterContext);
    string requestMethod = getRequestMethodFromFilterContext(filterContext);
    boolean validationResult = false;

    //todo: Accept only the content types which are mentioned in the openAPI definition
    //If the content-type is not application/json, validation fiter is not applied.
    if (!stringutils:equalsIgnoreCase(request.getContentType(), APPLICATION_JSON)) {
        printDebug(KEY_VALIDATION_FILTER, "Validation Filter is not applied as the request content type is : " + 
            request.getContentType());
        return true;
    }    

    printDebug(KEY_VALIDATION_FILTER, "The Request validation is enabled.");
    string serviceName = filterContext.getServiceName();
    printDebug(KEY_VALIDATION_FILTER, "Relevent Service name : " + serviceName);

    printDebug(KEY_VALIDATION_FILTER, "The Request resource Path : " + requestPath + ", method : " + requestMethod);
       
    //getting the payload of the request
    string payloadVal = "";

    var reqPayload  = request.getJsonPayload();
    if (reqPayload is json) {
        payloadVal = reqPayload.toJsonString();
    }
    var valResult = requestValidate(requestPath, requestMethod, payloadVal, serviceName);
    if (valResult is handle && stringutils:equalsIgnoreCase(valResult.toString(), VALIDATION_STATUS)) {
        return true;
    } else {
        string errorMessage = "Bad Request";
        string errorDescription = valResult.toString();
        json newPayload = { fault: {
            code: http:STATUS_BAD_REQUEST,
            message: errorMessage,
            description: errorDescription
        } };
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        invocationContext.attributes["error_response_code"] = http:STATUS_BAD_REQUEST;
        invocationContext.attributes["error_response"] = errorDescription;
        invocationContext.attributes[FILTER_FAILED] = true;
        http:Response res = new;
        res.statusCode = http:STATUS_BAD_REQUEST;
        res.setJsonPayload(newPayload);
        var rcal = caller->respond(res);
        if (rcal is error) {
            printError(KEY_VALIDATION_FILTER, "Error occurred while sending the error response", rcal);
        }
        return false;
    }
}
