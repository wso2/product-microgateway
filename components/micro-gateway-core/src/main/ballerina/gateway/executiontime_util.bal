import ballerina/log;
import ballerina/io;
import ballerina/http;


function getExecutionTimePayload(ExecutionTimeDTO executionTimeDTO) returns string {
    string output = executionTimeDTO.apiName + OBJ + executionTimeDTO.apiVersion + OBJ + executionTimeDTO.tenantDomain +
        OBJ + executionTimeDTO.provider + OBJ + executionTimeDTO.apiResponseTime + OBJ + executionTimeDTO.context + OBJ
        + executionTimeDTO.securityLatency + OBJ + executionTimeDTO.throttlingLatency + OBJ + executionTimeDTO.
        requestMediationLatency + OBJ +
        executionTimeDTO.responseMediationLatency + OBJ + executionTimeDTO.backEndLatency + OBJ + executionTimeDTO.
        otherLatency + OBJ + executionTimeDTO.eventTime;

    return output;
}

function getMetaDataForExecutionTimeDTO(ExecutionTimeDTO dto) returns string {
    return "{\\\"keyType\\\":\"" + dto.keyType + "\",\\\"correlationID\\\":\"" + dto.correleationID + "\"}";
}

function generateEventFromExecutionTime(ExecutionTimeDTO executionTimeDTO) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.execution.time:1.0.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getMetaDataForExecutionTimeDTO(executionTimeDTO);
    eventDTO.correlationData = executionTimeDTO.correleationID;
    eventDTO.payloadData = getExecutionTimePayload(executionTimeDTO);
    return eventDTO;
}

function generateExecutionTimeEvent(http:FilterContext context) returns ExecutionTimeDTO {
    ExecutionTimeDTO executionTimeDTO;
    boolean isSecured = check <boolean>context.attributes[IS_SECURED];
    if (isSecured && context.attributes.hasKey(AUTHENTICATION_CONTEXT)) {
        AuthenticationContext authContext = check <AuthenticationContext>context.attributes[AUTHENTICATION_CONTEXT];
        executionTimeDTO.provider = authContext.apiPublisher;
        executionTimeDTO.keyType = authContext.keyType;
    } else {
        executionTimeDTO.provider = getAPIDetailsFromServiceAnnotation(
                                        reflect:getServiceAnnotations(context.serviceType)).publisher;
        executionTimeDTO.keyType = PRODUCTION_KEY_TYPE;
    }
    executionTimeDTO.apiName = getApiName(context);
    executionTimeDTO.apiVersion = getAPIDetailsFromServiceAnnotation(reflect:getServiceAnnotations(context.serviceType)).
    apiVersion;
    executionTimeDTO.tenantDomain = getTenantDomain(context);
    executionTimeDTO.context = getContext(context);
    executionTimeDTO.correleationID = <string> context.attributes[MESSAGE_ID];

    executionTimeDTO.securityLatency = check <int> context.attributes[SECURITY_LATENCY];
    executionTimeDTO.eventTime = getCurrentTime();
    executionTimeDTO.throttlingLatency = check <int> context.attributes[THROTTLE_LATENCY];
    executionTimeDTO.requestMediationLatency = 0;
    executionTimeDTO.otherLatency = 0;
    executionTimeDTO.responseMediationLatency = 0;
    int timeRequestOut = check <int> runtime:getInvocationContext().attributes[TS_REQUEST_OUT];
    int timeResponseIn = check <int> runtime:getInvocationContext().attributes[TS_RESPONSE_IN];
    executionTimeDTO.backEndLatency = timeResponseIn - timeRequestOut;

    return executionTimeDTO;
}

