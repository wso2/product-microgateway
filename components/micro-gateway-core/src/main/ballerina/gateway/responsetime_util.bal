import ballerina/log;
import ballerina/io;
import ballerina/http;
import ballerina/runtime;


function getReponseDataPayload(ResponseDTO responseDTO) returns string {
    string output = responseDTO.consumerKey + OBJ + responseDTO.context + OBJ + responseDTO.apiVersion +
        OBJ + responseDTO.api + OBJ + responseDTO.resourcePath + OBJ + responseDTO.resourceTemplate + OBJ
        + responseDTO.method + OBJ + responseDTO.versionOnly + OBJ + responseDTO.response + OBJ +
        responseDTO.responseTime + OBJ + responseDTO.serviceTime + OBJ + responseDTO.backendTime + OBJ + responseDTO.
        userName
        + OBJ + responseDTO.eventTime + OBJ + responseDTO.tenantDomain + OBJ + responseDTO.hostname + OBJ + responseDTO.
        apiPublisher
        + OBJ + responseDTO.appName + OBJ + responseDTO.appId + OBJ + responseDTO.cacheHit + OBJ + responseDTO.
        responseSize
        + OBJ + responseDTO.protocol + OBJ + responseDTO.responseCode + OBJ + responseDTO.destination;

    return output;
}

function getMetaDataForResponseData(ResponseDTO dto) returns string {
    return "{\\\"keyType\\\":\"" + dto.keyType + "\",\\\"correlationID\\\":\"" + dto.correlationID + "\"}";
}

function generateEventFromResponseDTO(ResponseDTO responseDTO) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.response:1.1.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getMetaDataForResponseData(responseDTO);
    eventDTO.correlationData = "null";
    eventDTO.payloadData = getReponseDataPayload(responseDTO);
    return eventDTO;
}

function generateResponseDataEvent(http:Response response, http:FilterContext context) returns ResponseDTO {
    ResponseDTO responseDto;
    AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
    if (authContext != null) {
        responseDto.apiPublisher = authContext.apiPublisher;
        responseDto.keyType = authContext.keyType;
        responseDto.consumerKey = authContext.consumerKey;
        responseDto.userName = authContext.username;
        responseDto.appId = authContext.applicationId;
        responseDto.appName = authContext.applicationName;
    }
    responseDto.api = getApiName(context);
    string versionOfApi = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
    apiVersion;
    responseDto.versionOnly = versionOfApi;
    responseDto.apiVersion = responseDto.api + ":" + versionOfApi;
    responseDto.tenantDomain = getTenantDomain(context);
    responseDto.context = getContext(context);
    responseDto.correlationID = <string> context.attributes[MESSAGE_ID];

    responseDto.eventTime = getCurrentTime();
    responseDto.responseTime = 0;
    var res = response.cacheControl.noCache;
    match res   {
         boolean val => {
            responseDto.cacheHit = val;
        }
        () => {
            //todo: cacheHit does not gives boolean
        }
    }
    responseDto.hostname = "127.0.0.1";
    responseDto.response = 1;
    //todo: Response size is yet to be decided
    responseDto.responseSize = 0;
    responseDto.responseCode = response.statusCode;
    responseDto.resourcePath = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName)).path;
    responseDto.resourceTemplate = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName)).path;
    //request method
    responseDto.method = <string> context.attributes[METHOD];
    int initTime = check <int> context.attributes[REQUEST_TIME];
    int timeRequestOut = check <int> runtime:getInvocationContext().attributes[TS_REQUEST_OUT];
    int timeResponseIn = check <int> runtime:getInvocationContext().attributes[TS_RESPONSE_IN];
    responseDto.serviceTime = timeRequestOut - initTime;
    responseDto.backendTime = timeResponseIn - timeRequestOut;
    responseDto.responseTime = timeResponseIn - initTime;
    //dummy values for protocol and destination for now
    responseDto.protocol = "http";
    responseDto.destination = "https://dummyDestination";

    return responseDto;
}



