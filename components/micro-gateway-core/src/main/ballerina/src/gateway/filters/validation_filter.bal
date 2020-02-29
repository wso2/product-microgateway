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

 int errorItem = 0;

 boolean enableRequestValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, REQUEST_VALIDATION_ENABLED,
 DEFAULT_REQUEST_VALIDATION_ENABLED);
 boolean enableResponseValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, RESPONSE_VALIDATION_ENABLED,
 DEFAULT_RESPONSE_VALIDATION_ENABLED);

// Validation filter
 public type ValidationFilter object {

  public function filterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext)
                                                                                          returns boolean {
    if (filterContext.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>filterContext.attributes[SKIP_ALL_FILTERS]) {
        printDebug(KEY_ANALYTICS_FILTER, "Skip all filter annotation set in the service. Skip the filter");
        return true;
    }
    if (!enableRequestValidation) {
        return true;
    }
    printDebug(KEY_VALIDATION_FILTER, "The request validation filter");
    boolean result = doValidationFilterRequest(caller, request, filterContext);
    return result;
  }

  public function filterResponse(@tainted http:Response response, http:FilterContext context) returns boolean {
     if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
         printDebug(KEY_ANALYTICS_FILTER, "Skip all filter annotation set in the service. Skip the filter");
         return true;
     }
     if (!enableResponseValidation) {
         return true;
     }
     printDebug(KEY_VALIDATION_FILTER, "The response validation filter");
     boolean result = doValidationFilterResponse(response, context);
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
    string requestPath = "";
    string requestMethod = "";

        printDebug(KEY_VALIDATION_FILTER, "The Request validation is enabled.");
        APIConfiguration? apiConfig = apiConfigAnnotationMap[filterContext.getServiceName()];
        string serviceName = filterContext.getServiceName();
        printDebug(KEY_VALIDATION_FILTER, "Relevent Service name : " + serviceName);
       //getting the method of the request
        requestMethod = request.method.toLowerAscii();
        filterContext.attributes[REQ_METHOD] = requestMethod;
       //getting the resource Name
        string resourceName = filterContext.getResourceName();
        //getting the payload of the request
        string payloadVal = "";

        var reqPayload  = request.getJsonPayload();
        if (reqPayload is map<json>) {
            payloadVal = reqPayload.toJsonString();
        }
        //getting request path
        http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
        if (httpResourceConfig is http:HttpResourceConfig) {
            requestPath = httpResourceConfig.path;
            filterContext.attributes[REQUEST_PATH] = requestPath;
            printDebug(KEY_VALIDATION_FILTER, "The Request resource Path : " + requestPath);
        }
        var valResult = requestValidate(requestPath, requestMethod, payloadVal, serviceName);
        if (valResult is handle && stringutils:equalsIgnoreCase(valResult.toString(), VALIDATION_STATUS)) {
            return true;
        } else {
            http:Response res = new;
            res.statusCode = 400;
            res.setPayload(valResult.toString());
            var rcal = caller->respond(res);
            return false;
        }
 }

 function doValidationFilterResponse(@tainted http:Response response, http:FilterContext context) returns boolean {

     string resPath = "";
     string requestMethod = "";
     string reqMethod = "";

         printDebug(KEY_VALIDATION_FILTER, "The Response validation is enabled.");
         string responseCode = response.statusCode.toString();
         any path = context.attributes[REQUEST_PATH];
         if (path is string) {
             resPath = path;
         }
         any method = context.attributes[REQ_METHOD];
         if (method is string) {
            reqMethod = method;
         }
         string resPayload = response.getJsonPayload().toString();
         string servName = context.getServiceName();
         printDebug(KEY_VALIDATION_FILTER, "The Response payload : " + resPayload);
         var valResult = responseValidate(resPath, requestMethod, responseCode, resPayload, servName);
         if (valResult is handle && stringutils:equalsIgnoreCase(valResult.toString(), VALIDATION_STATUS)) {
             return true;
         } else {
             response.statusCode = 500;
             response.setPayload(valResult.toString());
             return false;
         }
 }
