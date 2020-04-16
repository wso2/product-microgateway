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

// Validation Response filter
public type ValidationResponseFilter object {

    *http:ResponseFilter;

    public function filterResponse(@tainted http:Response response, http:FilterContext context) returns boolean {
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_VALIDATION_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        //skip validation filter if the request is gRPC
        if (isGrpcRequest(context)) {
            printDebug(KEY_VALIDATION_FILTER, "Skip the filter as the request is GRPC");
            return true;
        }
        if (!enableResponseValidation) {
            return true;
        }
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        boolean filterFailed = <boolean>invocationContext.attributes[FILTER_FAILED];
        if (filterFailed) {
            printDebug(KEY_VALIDATION_FILTER, "The response validation filter is skipped as microgateway filter" +
                "has been failed");
            return true;
        }
        printDebug(KEY_VALIDATION_FILTER, "The response validation filter");
        boolean result = doValidationFilterResponse(response, context);
        return result;
    }
 };

function doValidationFilterResponse(@tainted http:Response response, http:FilterContext filterContext) returns boolean {

    string reqestPath = getRequestPathFromFilterContext(filterContext);
    string requestMethod = getRequestMethodFromFilterContext(filterContext);
    string resPayload = "";

    //todo: Accept only the content types which are mentioned in the openAPI definition
    //If the content-type is not application/json, validation fiter is not applied.
    if (!stringutils:equalsIgnoreCase(response.getContentType(), APPLICATION_JSON)) {
        printDebug(KEY_VALIDATION_FILTER, "Validation Filter is not applied as the response content type is : " + 
            response.getContentType());
        return true;
    }    

    printDebug(KEY_VALIDATION_FILTER, "The Response validation is enabled.");
    string responseCode = response.statusCode.toString();
    var payload = response.getJsonPayload();
    if (payload is json)  {
        resPayload = payload.toJsonString();
    }
    string servName = filterContext.getServiceName();
    var valResult = responseValidate(reqestPath, requestMethod, responseCode, resPayload, servName);
    if (valResult is handle && stringutils:equalsIgnoreCase(valResult.toString(), VALIDATION_STATUS)) {
        return true;
    } else {
        string errorMessage = "Bad Response";
        string errorDescription = valResult.toString();
        json newPayload = { fault: {
                                code: http:STATUS_INTERNAL_SERVER_ERROR,
                                message: errorMessage,
                                description: valResult.toString()
                            } };
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        invocationContext.attributes["error_response_code"] = http:STATUS_INTERNAL_SERVER_ERROR;
        invocationContext.attributes["error_response"] = errorDescription;
        response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
        response.setJsonPayload(newPayload);
        return true;
    }
}
