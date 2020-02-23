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
 string requestPath = "";
 string requestMethod = "";

 boolean enableRequestValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, REQUEST_VALIDATION_ENABLED,
 DEFAULT_REQUEST_VALIDATION_ENABLED);
 boolean enableResponseValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, RESPONSE_VALIDATION_ENABLED,
 DEFAULT_RESPONSE_VALIDATION_ENABLED);

 public type ValidationFilter object {

  public map<string> openAPIs = {};

  public function __init(map<string> openAPIs) {
  self.openAPIs = openAPIs;
  }

  public function filterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext)
  returns boolean {
  printDebug(KEY_VALIDATION_FILTER, "entering to the request validation filter");
  boolean result = doValidationFilterRequest(caller, request, filterContext, self.openAPIs);
  return result;
  }

  //public function filterResponse(@tainted http:Response response, http:FilterContext context) returns boolean {
  // printDebug(KEY_VALIDATION_FILTER, "entering to the response validation filter");
  // boolean result = doValidationFilterResponse(response, context, self.openAPIs);
  // return result;
  //}
 };

 function doValidationFilterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext,
  map<json> openAPIs) returns boolean {
  json|error models = {};
  json finalSchema = {};
  string modelName;
  json relElements = {};
  json result = {};
  json|error bodyModels = {};
  json|error paths = {};

  if (enableRequestValidation) {
  printDebug(KEY_VALIDATION_FILTER, "The Request validation is enabled.");
  APIConfiguration? apiConfig = apiConfigAnnotationMap[filterContext.getServiceName()];
  json swagger = openAPIs[filterContext.getServiceName()];
  printDebug(KEY_VALIDATION_FILTER, "The swagger content found in map : " + swagger.toString());

  //getting the payload of the request
  var payload = request.getJsonPayload();
  //getting the method of the request
  requestMethod = request.method.toLowerAscii();
  //getting the resource Name
  string resourceName = filterContext.getResourceName();
  string swaggerCon = openAPIs[filterContext.getServiceName()].toString();
  //getting the payload of the request
  string payloadVal = request.getJsonPayload().toString();

  //getting request path
  http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
  if (httpResourceConfig is http:HttpResourceConfig) {
  requestPath = httpResourceConfig.path;
  filterContext.attributes["requestPath"] = requestPath;
  printDebug(KEY_VALIDATION_FILTER, "The Request resource Path : " + requestPath);
  }
  var valResult = requestValidate(requestPath, requestMethod, swaggerCon, payloadVal);
  if (valResult is handle && stringutils:equalsIgnoreCase(valResult.toString(), "validated")) {
  return true;
  } else {
  http:Response res = new;
  res.statusCode = 500;
  res.setPayload(result);
  return false;

  }
  }
  return false;
  }

 //function doValidationFilterResponse(@tainted http:Response response, http:FilterContext context) returns boolean {
 //
 // return false;
 //
 //}