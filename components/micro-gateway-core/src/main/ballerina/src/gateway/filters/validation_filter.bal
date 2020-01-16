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
boolean isType = false;
json pathKeys = {};
string pathType = "";
json|error requestBodyModels = {};
json|error targetModel = {};
json finalModel = {};

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
        printDebug(KEY_VALIDATION_FILTER, "entering to the request validation filter");
        int startingTime = getCurrentTime();
        checkOrSetMessageID(filterContext);
        boolean result =  doValidationFilterRequest(caller, request, filterContext, self.openAPIs);
        setLatency(startingTime, filterContext, SECURITY_LATENCY_VALIDATION);
        return result;
    }
    public function filterResponse(@tainted http:Response response, http:FilterContext context) returns boolean {
        printDebug(KEY_VALIDATION_FILTER, "entering to the response validation filter");
        int startingTime = getCurrentTime();
        boolean result = doValidationFilterResponse(response, context, self.openAPIs);
        setLatency(startingTime, context, SECURITY_LATENCY_VALIDATION);
        return result;
    }

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
        //getting the method  of the request
        requestMethod = request.method.toLowerAscii();
        //getting the resource Name
        string resourceName = filterContext.getResourceName();

        //getting request path
        http:HttpResourceConfig? httpResourceConfig = resourceAnnotationMap[resourceName];
        if (httpResourceConfig is http:HttpResourceConfig) {
           requestPath =  httpResourceConfig.path;
           printDebug(KEY_VALIDATION_FILTER, "The Request resource Path : " + requestPath);
        }
        //get schema models
        if (swagger.components.schemas != null) {//In swagger 3.0 models are defined under the components.schemas
            printDebug(KEY_VALIDATION_FILTER, "Retriving models in swagger 3.x ..");
            models = swagger.components.schemas;
        } else if (swagger.definitions != null) {//In swagger 2.0 models are defined under the definitions
            //getting all models defined in the schemas
            printDebug(KEY_VALIDATION_FILTER, "Retriving models in swagger 2.x ..");
            models = swagger.definitions;
        }
        if (swagger.components.requestBodies != null) {
            printDebug(KEY_VALIDATION_FILTER, "swagger.components.requestBodies");
            bodyModels = swagger.components.requestBodies;
        }
        //getting all the keys defined under the paths in the swagger
        paths = swagger.paths;
        if (paths is json) {
            pathKeys = paths;

        //iterates all the paths.
        if (pathKeys is map<json> && models is json) {
           map<json> pathkeysMap = <map<json>>pathKeys;
           foreach var [k, v] in pathkeysMap.entries() {
              if (requestPath == k) {
                 printDebug(KEY_VALIDATION_FILTER, "Request Path : " + requestPath.toString());
                 if (v is map<json>) {
                    json|error reqMethod = v[requestMethod];
                       if (reqMethod is json) {
                          printDebug(KEY_VALIDATION_FILTER, "Request Method : " + reqMethod.toJsonString());
                          json|error params = reqMethod.parameters;
                          if (params is json) {
                             json[] paramsT = <json[]>params;
                             int length = paramsT.length();
                             int i = 0;
                             while (i < length) {
                             json|error paramVal = paramsT[i];
                                if (paramVal is map<json>) {
                                  json|error schema = paramVal["schema"];
                                  if (schema is json) {
                                     finalSchema = schema;
                                  }
                                }
                                 i = i + 1;
                             }
                          } else {
                             json|error reqBody = reqMethod.requestBody;
                             if (reqBody is json) {
                                if (reqBody is map<json>) {
                                   json|error reqBodyReff = reqBody[REFERENCE];
                                   if (reqBodyReff != null) {
                                      if (reqBodyReff is json) {
                                         string refs = reqBodyReff.toString();
                                         modelName = replaceModelPrefix(refs);
                                         if (bodyModels is json) {
                                            if (bodyModels is map<json>) {
                                               json|error contents = bodyModels[modelName];
                                               if (contents is json) {
                                                  json|error schemContent = contents.content;
                                                     if (schemContent is json) {
                                                        if (schemContent is map<json>) {
                                                           json|error schemaRef = schemContent[APPLICATION_JSON];
                                                              if (schemaRef is json) {
                                                                 json|error schemaContent = schemaRef.schema;
                                                                 if (schemaContent is json) {
                                                                    printDebug(KEY_VALIDATION_FILTER, "Schema content : " +
                                                                    schemaContent.toJsonString());
                                                                    string schemaContentStr = schemaContent.toString();
                                                                    string requestPayload = payload.toString();
                                                                    error? errorResult =  validator(schemaContentStr,
                                                                     requestPayload);
                                                                    if (errorResult is error) {
                                                                       filterContext.attributes[ERROR_DESCRIPTION] =
                                                                        <@untainted>  errorResult.reason();
                                                                    }
                                                                 }
                                                              }
                                                        }
                                                     }
                                                }
                                            }
                                         }
                                      } else {
                                          json|error content = reqBody["content"];
                                          if (content is json) {
                                             if (content is map<json>) {
                                                json|error schemaRef = content[APPLICATION_JSON];
                                                if (schemaRef is json) {
                                                   json|error schemaContent = schemaRef.schema;
                                                   if (schemaContent is json) {
                                                      string schemaCOntent = schemaContent.toString();
                                                   }
                                                }
                                             }
                                          }
                                      }
                                   }
                                }
                                   return true;
                             }
                          }
                       }
                 }
              }
           }
        }
        }
    }
    return true;
}

function doValidationFilterResponse(http:Response response, http:FilterContext context, map<json> openAPIs)
                                                                                                  returns boolean {
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
         map<json> pathkeysMap = <map<json>>pathKeys;
         foreach var i in pathkeysMap {
            if (requestPath == i && models is json) {
                string modelReference;
                if (swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA != null) {
                    if (swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.ITEMS != null) {
                          if (swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.
                          SCHEMA.ITEMS.REFERENCE != null) {
                                 //getting referenced model
                                 modelReference = swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.
                                 SCHEMA.ITEMS.REFERENCE.toString();
                                 modelName = replaceModelPrefix(modelReference);
                                 if (models.modelName != null) {
                                     model = models.modelName;
                                 }
                          } else {
                                 //getting inline model defined under the items
                                 model = swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.ITEMS;
                          }
                    } else {
                          if (swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.
                              REFERENCE != null) {
                                 //getting referenced model
                                 modelReference = swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.
                                 SCHEMA.REFERENCE.toString();
                                 modelName = replaceModelPrefix(modelReference);
                                 if (models.modelName != null) {
                                     model = models.modelName;
                                 }
                          } else {
                                 //getting inline model defined under the schema
                                 model = swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA;
                          }
                    }
                         if (swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.SCHEMA.TYPE != null) {
                             isType = true;
                             pathType = <@untainted>  swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.
                             SCHEMA.TYPE.toString();
                         }
                } else {
                    if (swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.CONTENT.APPLICATION_JSON.
                        SCHEMA != null) {
                        json|error schema = swagger.paths.i.requestMethod.RESPONSES.responseStatusCode.
                        CONTENT.APPLICATION_JSON.SCHEMA;
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
              printDebug(KEY_VALIDATION_FILTER, "The swagger models : " + payload.toString());
             //do the validation if only there is a payload and a model available. prevent validating error
             //responses sent from the filterRequest if the request is invalid.
             if (model != null && payload != null && payload.fault == null && model is json && models is json) {
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
         return true;
     }

# Extract the schema name.
#
# + name - Schema string to be extracted
# + return - extreacted schema
public function replaceModelPrefix(string name) returns (string) {
   string newName = "";
   if (contains(name, DEFINITIONS)) {
       newName = replaceFirst(name,DEFINITIONS, "");
   }
   if (contains(name, COMPONENTS_SCHEMAS)) {
       newName = replaceFirst(name, COMPONENTS_SCHEMAS, "");
   }
   if (contains(name, COMPONENTS_REQUESTBODIES)) {
       newName = replaceFirst(name, COMPONENTS_REQUESTBODIES, "");
   }
   printDebug(KEY_VALIDATION_FILTER, "replaceModelPrefix return value" + newName);

   return newName;
}

# Finalize the schema from complex schema.
#
# + schema - complex schema
# + models - relevent model
# + return - simple schema
public function finalizeSchema(json schema, json models) returns (json) {
    string modelName;
    json|error modelReference = {};
    if (schema is map<json> && models is map<json>) {
        foreach var [k, v] in schema.entries() {
           if (stringutils:equalsIgnoreCase(k, REFERENCE)) {
           string reference = v.toString();
           modelName = replaceModelPrefix(reference);
           modelReference = models[modelName];
           if (modelReference is json) {
               finalModel = modelReference;
           }
        }
           if (v is map<json>) {
              json value = finalizeSchema(v, models);
              string keyValue = k.toString();
              schema[keyValue] = finalModel;
           }
        }
    }
    return schema;
}
