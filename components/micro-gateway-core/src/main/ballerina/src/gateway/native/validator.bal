import ballerinax/java;


public function requestValidate(string resPath, string requestMethod, string swagger, string payload)
                                                                              returns handle | error {
 handle resourcePath = java:fromString(resPath);
 handle reqMethod = java:fromString(requestMethod);
 handle swaggerContent = java:fromString(swagger);
 handle requestPayload = java:fromString(payload);

 return jRequestValidate(resourcePath, reqMethod, swaggerContent, requestPayload);
}

function jRequestValidate(handle resourcePath, handle reqMethod, handle swaggerContent, handle requestPayload)
                        returns handle | error = @java:Method {
 name: "validateRequest",
 class: "org.wso2.micro.gateway.core.validation.Validate"
} external;
