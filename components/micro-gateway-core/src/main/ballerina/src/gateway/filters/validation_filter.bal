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

int errorItem = 0;
string? requestPath = "";
string requestMethod = "";
boolean isType = false;
json|error pathKeys = {};
string pathType = "";

boolean enableRequestValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, REQUEST_VALIDATION_ENABLED, false
);
boolean enableResponseValidation = getConfigBooleanValue(VALIDATION_CONFIG_INSTANCE_ID, RESPONSE_VALIDATION_ENABLED,
    false);

public type ValidationFilter object {

    public map<json> openAPIs = {};

    public function __init(map<json> openAPIs) {
        self.openAPIs = openAPIs;
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext)
                        returns boolean {
        int startingTime = getCurrentTime();
        checkOrSetMessageID(filterContext);
        boolean result =  doValidationFilterRequest(caller, request, filterContext, self.openAPIs);
        setLatency(startingTime, filterContext, SECURITY_LATENCY_VALIDATION);
        return result;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        int startingTime = getCurrentTime();
        boolean result = doValidationFilterResponse(response, context, self.openAPIs);
        setLatency(startingTime, context, SECURITY_LATENCY_VALIDATION);
        return result;
    }

};

function doValidationFilterRequest(http:Caller caller, http:Request request, http:FilterContext filterContext, map<json> openAPIs)
             returns boolean {
    if (enableRequestValidation) {
        printDebug(KEY_VALIDATION_FILTER, "The Request validation is enabled..");
        //getting the payload of the request
        var payload = request.getJsonPayload();
        isType = false;
        APIConfiguration? apiConfig = apiConfigAnnotationMap[filterContext.getServiceName()];
        json swagger = openAPIs[filterContext.getServiceName()];
        printDebug(KEY_VALIDATION_FILTER, "The swagger content found in map : " + swagger.toString());
        json|error model = {};
        json|error models = {};
        string modelName = "";
        //getting all the keys defined under the paths in the swagger
        pathKeys = swagger.PATHS;
        //getting the methnted>  od of the request
        requestMethod = toLowerCase(request.method);
        //getting the path hit by the request
        string resourceName = filterContext.attributes["ResourceName"].toString();
        http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
        if (httpResourceConfig is http:HttpResourceConfig) {
           requestPath =  httpResourceConfig.path;
        }
        //getting the name of the model hit by the request
        if (swagger.components.schemas != null) {//In swagger 3.0 models are defined under the components.schemas
            models = swagger.components.schemas;
        } else if (swagger.definitions != null) {//In swagger 2.0 models are defined under the definitions
            //getting all models defined in the schemas
            models = swagger.definitions;
        }
        //loop each key defined under the paths in swagger and compare whether it contain the path hit by the request
            map<json> pathMap = <map<json>>pathKeys;
            if (models is json) {
                foreach var i in pathMap {
                    if (requestPath == i) {
                        json|error parameters = swagger.PATHS.i.requestMethod.PARAMETERS;
                        //go through each item in parameters array and find the schema property
                        //go through each item in parameters array and find the schema property
                        if (parameters != null) {
                            map<json> para = <map<json>>parameters;
                            //json[] para = <json[]>parameters;
                                foreach var k in para {
                                    if (k.SCHEMA != null) {
                                    if (k.SCHEMA.REFERENCE != null)  {
                                    //getting the reference to the model
                                     string modelReference = k.SCHEMA.REFERENCE.toString();
                                //getting the model name
                                modelName = <@untainted>  replaceModelPrefix(modelReference);
                                //check whether there is a model available from the assigned model name
                                if (models.modelName!= null) {
                                    model = models.modelName;
                                }
                            } else {
                                //getting inline model
                                model = k.SCHEMA;
                            }
                        } else {
                            //getting inline model
                            model = k.SCHEMA;
                        }
                    }
                    } else {
                     string requestReference = swagger.PATHS.i.requestMethod.REQUESTBODY.REFERENCE.toString();
                     //getting the model name
                      modelName = <@untainted>  replaceModelPrefix(requestReference);
                      //check whether there is a model available from the assigned model name
                       if (models.modelName != null) {
                           model = models.modelName;
                       }
                   }
                }
               }
             }

        //payload can be of type json or error
        if(payload is json && model is json && models is json) {
            //do the validation if only there is a payload and a model available
            if (model != null && payload != null)  {
                printDebug(KEY_VALIDATION_FILTER, "The swagger models : " + swagger.toString());
                //validate the payload against the model and return the result
                Result finalResult = validate(modelName, payload, model, models);
                if (!finalResult.valid) {
                    //setting the error message to the context
                    setErrorMessageToFilterContext(filterContext, INVALID_ENTITY);
                    error? errorResult = finalResult.resultErr[0];
                    if (errorResult is error) {
                        filterContext.attributes[ERROR_DESCRIPTION] = <@untainted>  errorResult.reason();
                    }
                    //sending the error response to the client
                    sendErrorResponse(caller, request, filterContext);
                    return false;//avoid sending the invalid request to the backend by returning false.
                }
            }
        } else {}
    }
    return true;
}

public function doValidationFilterResponse(http:Response response, http:FilterContext context, map<json> openAPIs) returns boolean {
    if (enableResponseValidation) {
        printDebug(KEY_VALIDATION_FILTER, "The Response validation is enabled..");
        //getting the payload of the response
        var payload = response.getJsonPayload();
        APIConfiguration? apiConfig = apiConfigAnnotationMap[context.getServiceName()];
        json swagger = openAPIs[context.getServiceName()];
        json|error model = {};
        json|error models = {};
        string modelName = "";
        string responseStatusCode = response.statusCode.toString();
        if (swagger.components.schemas != null) {//getting the schemas from a swagger 3.0 version file
            models = swagger.components.schemas;
        } else if (swagger.definitions != null) {//getting schemas from a swagger 2.0 version file
            models = swagger.definitions;
        }
        map<json> pathMap = <map<json>>pathKeys;
        foreach var i in pathMap {
           if (requestPath == i && models is json) {
               string modelReference;
               if (swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA != null) {
                   if (swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.ITEMS != null) {
                         if (swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.ITEMS.REFERENCE != null) {
                                //getting referenced model
                                modelReference = swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.
                                SCHEMA.ITEMS.REFERENCE.toString();
                                modelName = replaceModelPrefix(modelReference);
                                if (models.modelName != null) {
                                    model = models.modelName;
                                }
                         } else {
                                //getting inline model defined under the items
                                model = swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.ITEMS;
                         }
                   } else {
                         if (swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.
                             REFERENCE != null) {
                                //getting referenced model
                                modelReference = swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.
                                SCHEMA.REFERENCE.toString();
                                modelName = replaceModelPrefix(modelReference);
                                if (models.modelName != null) {
                                    model = models.modelName;
                                }
                         } else {
                                //getting inline model defined under the schema
                                model = swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA;
                         }
                   }
                        if (swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.TYPE != null) {
                            isType = true;
                            pathType = <@untainted>  swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.
                            SCHEMA.TYPE.toString();
                        }
               } else {
                   if (swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.CONTENT.APPLICATION_JSON.
                       SCHEMA != null) {
                       json|error schema = swagger.PATHS.i.requestMethod.RESPONSES.responseStatusCode.CONTENT.APPLICATION_JSON.SCHEMA;
                        if (schema is json) {
                           if (schema.ITEMS != null) {
                               if (schema.ITEMS.REFERENCE != null) {
                                   //getting referenced model
                                   modelReference = schema.ITEMS.REFERENCE.toString();
                                   modelName = replaceModelPrefix(modelReference);
                                   if (models.modelName != null) {
                                       model = models.modelName;
                                   }
                               } else {
                                   //getting inline model defined under the items
                                     model = schema.ITEMS;
                               }
                           } else {
                               if (schema.REFERENCE != null) {
                                  //getting referenced model
                                  modelReference = schema.REFERENCE.toString();
                                  modelName = replaceModelPrefix(modelReference);
                                  if (models.modelName != null) {
                                      model = models.modelName;
                                  }
                               } else {
                                         //getting inline model defined under the schema
                                           model = schema;
                               }
                           }
                             if (schema.TYPE != null) {
                               isType = true;
                               pathType = <@untainted>  schema.TYPE.toString();
                             }
                    }
                   }
               }
           }
        }

        //payload can be of type json or error
        if(payload is json) {
            //do the validation if only there is a payload and a model available. prevent validating error
            //responses sent from the filterRequest if the request is invalid.
            if (model != null && payload != null && payload.fault == null && model is json && models is json) {
                //validate the payload against the model and return the result
                Result finalResult = validate(modelName, payload, model, models);
                if (!finalResult.valid) {
                    //setting the error message to the context
                    setErrorMessageToFilterContext(context, INVALID_RESPONSE);
                     error? errorResult = finalResult.resultErr[0];
                    if (errorResult is error) {
                    context.attributes[ERROR_DESCRIPTION] = <@untainted>  errorResult.reason();
                }
                    //getting attributes from the context
                    int statusCode = <int>context.attributes[HTTP_STATUS_CODE];
                    string errorDescription = <string>context.attributes[ERROR_DESCRIPTION];
                    string errorMsg = <string>context.attributes[ERROR_MESSAGE];
                    int errorCode = <int>context.attributes[ERROR_CODE];
                    //changing the response
                    response.statusCode = statusCode;
                    response.setContentType(APPLICATION_JSON);
                    //creating a new payload which is having the error message
                    json newPayload = { fault: {
                        code: errorCode,
                        message: errorMsg,
                        description: errorDescription
                    } };
                    //setting the new payload to the response
                    response.setJsonPayload(<@untainted>  newPayload);
                    return true;//send the changed response(error response) to the user
                }
            }
        }
        else {}
    }
    return true;
}
