import ballerina/log;
import ballerina/io;
import ballerina/http;


function getReponseDataPayload(ResponseDTO responseDTO) returns string {
    string output = responseDTO.consumerKey + OBJ + responseDTO.context + OBJ + responseDTO.apiVersion +
        OBJ + responseDTO.api + OBJ  + responseDTO.resourcePath + OBJ + responseDTO.resourceTemplate + OBJ
        + responseDTO.method + OBJ + responseDTO.versionOnly + OBJ + responseDTO.response + OBJ +
        responseDTO.responseTime + OBJ + responseDTO.serviceTime + OBJ + responseDTO.backendTime +OBJ+responseDTO.userName
        + OBJ + responseDTO.eventTime + OBJ + responseDTO.tenantDomain + OBJ + responseDTO.hostname + OBJ + responseDTO.apiPublisher
    + OBJ + responseDTO.appName + OBJ + responseDTO.appId + OBJ + responseDTO.cacheHit + OBJ + responseDTO.responseSize
    + OBJ + responseDTO.protoco + OBJ + responseDTO.responseCode + OBJ + responseDTO.destination;

    return output;
}

function getMetaDataForResponseData(ResponseDTO dto) returns string {
    return "{\\\"keyType\\\":\"" + dto.keyType + "\",\\\"correlationID\\\":\"" + dto.correleationID + "\"}";
}

function generateEventFromResponseDTO(ResponseDTO responseDTO) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.response:1.1.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getMetaDataForResponseData(responseDTO);
    eventDTO.correlationData = responseDTO.correleationID;
    eventDTO.payloadData = getReponseDataPayload(responseDTO);
    return eventDTO;
}

function generateResponseDataEvent(http:FilterContext context) returns ResponseDTO {
    ResponseDTO responseDto;
    AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
    if (authContext != null ) {
        responseDto.provider = authContext.apiPublisher;
        responseDto.keyType = authContext.keyType;
    }
    responseDto.apiName = getApiName(context);
    responseDto.apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
    apiVersion;
    responseDto.tenantDomain = getTenantDomain(context);
    responseDto.context = getContext(context);
    responseDto.correleationID = "71c60dbd-b2be-408d-9e2e-4fd11f60cfbc";

    //todo:calculate each laterncy time
    //responseDto.securityLatency = check <int> context.attributes[SECURITY_LATENCY];
    responseDto.eventTime = getCurrentTime();
    //io:print(context.attributes["securityLatency"]);
    //responseDto.throttlingLatency = check <int> context.attributes[THROTTLE_LATENCY];
    //io:println(context.attributes["throttlingLatency"]);
    responseDto.responseTime = 0;
    responseDto.serviceTime = 0;
    responseDto.backEndLatency = 43543;

    return responseDto;
}



