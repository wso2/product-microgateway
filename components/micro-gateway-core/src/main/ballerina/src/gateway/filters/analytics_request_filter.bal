// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
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
import ballerina/runtime;
import ballerina/log;

public type AnalyticsRequestFilter object {

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_ANALYTICS_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        //Filter only if analytics is enabled.
        if (isAnalyticsEnabled || isgRPCAnalyticsEnabled) {
            checkOrSetMessageID(context);
            context.attributes[PROTOCOL_PROPERTY] = caller.protocol;
            doFilterRequest(request, context);
        }
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        log:printDebug("FilterResponse method in analytics_request_filter invoked.");
        if (isAnalyticsEnabled || isgRPCAnalyticsEnabled) {
            boolean filterFailed = <boolean>context.attributes[FILTER_FAILED];
            if (context.attributes.hasKey(IS_THROTTLE_OUT)) {
                boolean isThrottleOut = <boolean>context.attributes[IS_THROTTLE_OUT];
                if (isThrottleOut) {
                    ThrottleAnalyticsEventDTO|error throttleAnalyticsEventDTO = trap populateThrottleAnalyticsDTO(context);
                    if(throttleAnalyticsEventDTO is ThrottleAnalyticsEventDTO) {
                        if(isgRPCAnalyticsEnabled != false){
                            // ####################################################
                            AnalyticsStreamMessage message = createThrottleMessage(throttleAnalyticsEventDTO);
                            log:printDebug("\n\n>>> gRPC throttle stream message created.");
                                    future<()> publishedGRPCThrottleStream = start dataToAnalytics(message);
                                    log:printDebug( "gRPC throttle stream message published.");
                            // ####################################################
                        }
                        EventDTO|error eventDTO  = trap getEventFromThrottleData(throttleAnalyticsEventDTO);
                        if(eventDTO is EventDTO) {
                            if(isAnalyticsEnabled != false){
                                eventStream.publish(eventDTO);
                                log:printDebug( "File upload throttle stream data published.");
                            }
                             
                            
                        } else {
                            printError(KEY_ANALYTICS_FILTER, "Error while creating throttle analytics event");
                            printFullError(KEY_ANALYTICS_FILTER, eventDTO);
                        }
                    } else {
                        printError(KEY_ANALYTICS_FILTER, "Error while populating throttle analytics event data");
                        printFullError(KEY_ANALYTICS_FILTER, throttleAnalyticsEventDTO);
                    }
                } else {
                    if (!filterFailed) {
                        doFilterAll(response, context);
                    }
                }
            } else {
                if (!filterFailed) {
                    context.attributes[THROTTLE_LATENCY] = 0;
                    doFilterAll(response, context);
                }
            }
        }
        return true;
    }

};


function doFilterRequest(http:Request request, http:FilterContext context) {
    log:printDebug( "doFilterRequestMehtod called");
    error? result = trap setRequestAttributesToContext(request, context);
    if (result is error) {
        printError(KEY_ANALYTICS_FILTER, "Error while setting analytics data in request path");
        printFullError(KEY_ANALYTICS_FILTER, result);
    }
}

function doFilterFault(http:FilterContext context, string errorMessage) {
    FaultDTO|error faultDTO = trap populateFaultAnalyticsDTO(context, errorMessage);
    if(faultDTO is FaultDTO) {
        log:printDebug( "doFilterFalut method called. Client type : " + faultDTO. metaClientType + " applicationName :"+faultDTO.applicationName);
        if(isgRPCAnalyticsEnabled != false ){
            log:printDebug("gRPC fault stream message creating --->");
            AnalyticsStreamMessage message = createFaultMessage(faultDTO);
                future<()> publishedGRPCFaultStream = start dataToAnalytics(message);
            return;
        }
        EventDTO|error eventDTO = trap getEventFromFaultData(faultDTO);
        if(eventDTO is EventDTO) {
            if(isAnalyticsEnabled != false){
                log:printDebug("F_Upload eventFaultStream called");
                eventStream.publish(eventDTO);
            }
            
        } else {
            printError(KEY_ANALYTICS_FILTER, "Error while genaratting analytics data for fault event");
            printFullError(KEY_ANALYTICS_FILTER, eventDTO);
        }
    } else {
        printError(KEY_ANALYTICS_FILTER, "Error while populating analytics fault event data");
        printFullError(KEY_ANALYTICS_FILTER, faultDTO);
    }
}

function doFilterResponseData(http:Response response, http:FilterContext context) {
    log:printDebug( "doFilterResponseData method called");
    //Response data publishing
    RequestResponseExecutionDTO|error requestResponseExecutionDTO = trap generateRequestResponseExecutionDataEvent(response,
        context);
    if(isgRPCAnalyticsEnabled != false  && requestResponseExecutionDTO is RequestResponseExecutionDTO){
        AnalyticsStreamMessage message = createResponseMessage(requestResponseExecutionDTO);
        log:printDebug("\n\ngRPC response stream Data starting to publish");
        future<()> publishedGRPCResponseStream = start dataToAnalytics(message);
        return;
    }
    if(requestResponseExecutionDTO is RequestResponseExecutionDTO) {
        EventDTO|error event = trap generateEventFromRequestResponseExecutionDTO(requestResponseExecutionDTO);
        if(event is EventDTO) {
            if(isAnalyticsEnabled != false){
                log:printDebug("F_Upload eventRequestStream called");
                eventStream.publish(event);
            }
        } else {
            printError(KEY_ANALYTICS_FILTER, "Error while genarating analytics data event");
            printFullError(KEY_ANALYTICS_FILTER, event);
        }
    } else {
        printError(KEY_ANALYTICS_FILTER, "Error while publishing analytics data");
        printFullError(KEY_ANALYTICS_FILTER, requestResponseExecutionDTO);
    }

}

function doFilterAll(http:Response response, http:FilterContext context) {
    var resp = runtime:getInvocationContext().attributes[ERROR_RESPONSE];
    log:printDebug( "doFilterAll method resp value : "+ resp.toString());
    if (resp is ()) {
        printDebug(KEY_ANALYTICS_FILTER, "No any faulty analytics events to handle.");
        doFilterResponseData(response, context);
    } else if (resp is string) {
        printDebug(KEY_ANALYTICS_FILTER, "Error response value present and handling faulty analytics events");
        doFilterFault(context, resp);
    }
}
//new Implementations for gRPC Analytics
//#####################################################################

public function createResponseMessage(RequestResponseExecutionDTO requestResponseExecutionDTO) returns AnalyticsStreamMessage {


    log:printDebug( "createResponse stream method called.");
    AnalyticsStreamMessage responseAnalyticsMessage = {

     messageStreamName: "InComingRequestStream",
     meta_clientType : <string>requestResponseExecutionDTO.metaClientType ,
     applicationConsumerKey : <string>requestResponseExecutionDTO.applicationConsumerKey ,
     applicationName : <string>requestResponseExecutionDTO.applicationName ,
     applicationId : <string>requestResponseExecutionDTO.applicationId ,
     applicationOwner : <string>requestResponseExecutionDTO.applicationOwner ,
     apiContext : <string>requestResponseExecutionDTO.apiContext ,
     apiName : <string> requestResponseExecutionDTO.apiName ,
     apiVersion : <string>requestResponseExecutionDTO.apiVersion ,
     apiResourcePath : <string>requestResponseExecutionDTO.apiResourcePath ,
     apiResourceTemplate : <string>requestResponseExecutionDTO.apiResourceTemplate ,
     apiMethod : <string>requestResponseExecutionDTO.apiMethod ,
     apiCreator : <string>requestResponseExecutionDTO.apiCreator ,
     apiCreatorTenantDomain : <string>requestResponseExecutionDTO.apiCreatorTenantDomain ,
     apiTier : <string>requestResponseExecutionDTO.apiTier ,
     apiHostname : <string>requestResponseExecutionDTO.apiHostname ,
     username : <string>requestResponseExecutionDTO.userName ,
     userTenantDomain : <string>requestResponseExecutionDTO.userTenantDomain ,
     userIp : <string>requestResponseExecutionDTO.userIp ,
     userAgent : <string>requestResponseExecutionDTO.userAgent ,
     requestTimestamp : requestResponseExecutionDTO.requestTimestamp ,
     throttledOut : requestResponseExecutionDTO.throttledOut ,
     responseTime :requestResponseExecutionDTO.responseTime ,
     serviceTime : requestResponseExecutionDTO.serviceTime ,
     backendTime : requestResponseExecutionDTO.backendTime ,
     responseCacheHit : requestResponseExecutionDTO.responseCacheHit ,
     responseSize : requestResponseExecutionDTO.responseSize ,
     protocol : requestResponseExecutionDTO.protocol ,
     responseCode  : requestResponseExecutionDTO.responseCode ,
     destination : requestResponseExecutionDTO.destination ,
     securityLatency  : requestResponseExecutionDTO.executionTime.securityLatency ,
     throttlingLatency  : requestResponseExecutionDTO.executionTime.throttlingLatency , 
     requestMedLat : requestResponseExecutionDTO.executionTime.requestMediationLatency ,
     responseMedLat : requestResponseExecutionDTO.executionTime.responseMediationLatency , 
     backendLatency : requestResponseExecutionDTO.executionTime.backEndLatency , 
     otherLatency : requestResponseExecutionDTO.executionTime.otherLatency , 
     gatewayType : <string>requestResponseExecutionDTO.gatewayType , 
     label  : <string>requestResponseExecutionDTO.label,



     subscriber : "",
     throttledOutReason : "",
     throttledOutTimestamp : 0,
     hostname : "",
 
    errorCode : "",
    errorMessage : ""
    };

    return responseAnalyticsMessage;
}


public function createThrottleMessage(ThrottleAnalyticsEventDTO throttleAnalyticsEventDTO) returns AnalyticsStreamMessage{
    log:printDebug( "createThrottleMessage method called");
    AnalyticsStreamMessage throttleAnalyticsMessage = {

     messageStreamName: "ThrottledOutStream",
     meta_clientType : throttleAnalyticsEventDTO.metaClientType,
     applicationConsumerKey : "",
     applicationName : throttleAnalyticsEventDTO.applicationName,
     applicationId : throttleAnalyticsEventDTO.applicationId,
     applicationOwner : "",
     apiContext : throttleAnalyticsEventDTO.apiContext,
     apiName : throttleAnalyticsEventDTO.apiName,
     apiVersion : throttleAnalyticsEventDTO.apiVersion,
     apiResourcePath : "",
     apiResourceTemplate : "",
     apiMethod : "",
     apiCreator : throttleAnalyticsEventDTO.apiCreator,
     apiCreatorTenantDomain : throttleAnalyticsEventDTO.apiCreatorTenantDomain,
     apiTier : "",
     apiHostname : "",
     username : throttleAnalyticsEventDTO.userName,
     userTenantDomain : throttleAnalyticsEventDTO.userTenantDomain,
     userIp : "",
     userAgent : "",
     requestTimestamp : 0,
     throttledOut : false,
     responseTime :0,
     serviceTime : 0,
     backendTime : 0,
     responseCacheHit : false,
     responseSize : 0,
     protocol : "",
     responseCode  : 0,
     destination : "",
     securityLatency  : 0,
     throttlingLatency  : 0,
     requestMedLat : 0 ,
     responseMedLat : 0,
     backendLatency : 0 ,
     otherLatency : 0,
     gatewayType : throttleAnalyticsEventDTO.gatewayType,
     label  : "",



     subscriber : throttleAnalyticsEventDTO.subscriber,
     throttledOutReason : throttleAnalyticsEventDTO.throttledOutReason,
     throttledOutTimestamp : throttleAnalyticsEventDTO.throttledTime,
     hostname : throttleAnalyticsEventDTO.hostname,
 
    errorCode : "",
    errorMessage : ""
    };
    return throttleAnalyticsMessage;
}



public function createFaultMessage(FaultDTO faultDTO)returns AnalyticsStreamMessage{
    log:printDebug( "createFaultMessage method called.");
    int errorCodeValue = faultDTO.errorCode;

    AnalyticsStreamMessage faultAnalyticsMessage = {

     messageStreamName: "FaultStream",
     meta_clientType : faultDTO. metaClientType,
     applicationConsumerKey : faultDTO.consumerKey,
     applicationName : faultDTO.applicationName,
     applicationId : faultDTO.applicationId,
     applicationOwner : "",
     apiContext : faultDTO.apiContext,
     apiName : faultDTO.apiName,
     apiVersion : faultDTO.apiVersion,
     apiResourcePath : faultDTO.resourcePath,
     apiResourceTemplate : "",
     apiMethod : faultDTO.method,
     apiCreator : faultDTO.apiCreator,
     apiCreatorTenantDomain : faultDTO.apiCreatorTenantDomain,
     apiTier : "",
     apiHostname : "",
     username : faultDTO.userName,
     userTenantDomain : faultDTO.userTenantDomain,
     userIp : "",
     userAgent : "",
     requestTimestamp : faultDTO.faultTime,
     throttledOut : false,
     responseTime :0,
     serviceTime : 0,
     backendTime : 0,
     responseCacheHit : false,
     responseSize : 0,
     protocol : faultDTO.protocol,
     responseCode  : 0,
     destination : "",
     securityLatency  : 0,
     throttlingLatency  : 0,
     requestMedLat : 0 ,
     responseMedLat : 0,
     backendLatency : 0 ,
     otherLatency : 0,
     gatewayType : "",
     label  : "",



     subscriber : "",
     throttledOutReason : "",
     throttledOutTimestamp : 0,
     hostname : faultDTO.hostName,
 
    errorCode : errorCodeValue.toString(),
    errorMessage : faultDTO.errorMessage
    };


    return faultAnalyticsMessage;
}


//###################################################################################################

    // RequestResponseExecutionDTO|error requestResponseExecutionDTO = trap generateRequestResponseExecutionDataEvent(response,
    //     context);
    // if(requestResponseExecutionDTO is RequestResponseExecutionDTO) {
    //     EventDTO|error event = trap generateEventFromRequestResponseExecutionDTO(requestResponseExecutionDTO);
    //     if(event is EventDTO) {
    //         if(isAnalyticsEnabled != false){
    //             log:printDebug("F_Upload eventRequestStream called");
    //             eventStream.publish(event);
    //         }
    //         // ###############################################
    //         log:printDebug("This is the Response event DTO --->");
    //         json analyticsResponseJSON = createAnalyticsJSON(requestResponseExecutionDTO);
    //         //json|error analyticsResponseJSON = json.constructFrom(requestResponseExecutionDTO);
    //         log:printDebug("\n\n###########GRPC Enable Value in response stream : ");
    //         //      log:printDebug(isgRPCAnalyticsEnabled);  && analyticsResponseJSON is json
    //         //io:println("1 -->  Before method call");
    //         if(isgRPCAnalyticsEnabled != false ){
    //             //io:println("2 -->  Inside if call");
    //             log:printDebug("\n\n###########GRPC Data going to publish");
    //             future<()> publishedGRPCResponseStream = start dataToAnalytics(analyticsResponseJSON.toJsonString() , "InComingRequestStream");
    //         }
    //         //io:println("4 -->  After method call");
    //         // io:println("\n\n\nTo json string : " + analyticsResponseJSON.toJsonString() );
    //         // io:println("\n\n\nTo  string : " + analyticsResponseJSON.toString() );
    //         // ##############################################
    //     } else {
    //         printError(KEY_ANALYTICS_FILTER, "Error while genarating analytics data event");
    //         printFullError(KEY_ANALYTICS_FILTER, event);
    //     }
    // } else {
    //     printError(KEY_ANALYTICS_FILTER, "Error while publishing analytics data");
    //     printFullError(KEY_ANALYTICS_FILTER, requestResponseExecutionDTO);
    // }




// ##################################################################################################


//    json throttleJSON = {
//             meta_clientType : throttleAnalyticsEventDTO.metaClientType,
//             username : throttleAnalyticsEventDTO.userName,
//             userTenantDomain : throttleAnalyticsEventDTO.userTenantDomain,

//             apiName :throttleAnalyticsEventDTO.apiName,
//             apiVersion : throttleAnalyticsEventDTO.apiVersion,
//             apiContext : throttleAnalyticsEventDTO.apiContext,

//             apiCreator : throttleAnalyticsEventDTO.apiCreator,
//             apiCreatorTenantDomain : throttleAnalyticsEventDTO.apiCreatorTenantDomain,
//             applicationId : throttleAnalyticsEventDTO.applicationId,

//             applicationName : throttleAnalyticsEventDTO.applicationName,
//             subscriber : throttleAnalyticsEventDTO.subscriber,
//             throttledOutReason : throttleAnalyticsEventDTO.throttledOutReason,

//             gatewayType : throttleAnalyticsEventDTO.gatewayType,
//             throttledOutTimestamp : throttleAnalyticsEventDTO.throttledTime,
//             hostname : throttleAnalyticsEventDTO.hostname
//         };




// ##################################################################################################
// json falutJSON = {
//         meta_clientType : faultDTO. metaClientType,
//         applicationConsumerKey : faultDTO.consumerKey,
//         apiName : faultDTO.apiName,
//         apiVersion : faultDTO.apiVersion,
//         apiContext : faultDTO.apiContext,
//         apiResourcePath : faultDTO.resourcePath,
//         apiMethod : faultDTO.method,
//         apiCreator : faultDTO.apiCreator,
//         username : faultDTO.userName,
//         userTenantDomain : faultDTO.userTenantDomain,
//         apiCreatorTenantDomain : faultDTO.apiCreatorTenantDomain,
//         hostname : faultDTO.hostName,
//         applicationId : faultDTO.applicationId,
//         applicationName : faultDTO.applicationName,
//         protocol : faultDTO.protocol,
//         errorCode : faultDTO.errorCode,
//         errorMessage : faultDTO.errorMessage,
//         requestTimestamp : faultDTO.faultTime
//     };



// ############################################################



//  json analyticsJSON = {
//         meta_clientType : <string>requestResponseExecutionDTO.metaClientType ,
//         applicationConsumerKey : <string>requestResponseExecutionDTO.applicationConsumerKey ,
//         applicationName : <string>requestResponseExecutionDTO.applicationName ,
//         applicationId : <string>requestResponseExecutionDTO.applicationId ,
//         applicationOwner : <string>requestResponseExecutionDTO.applicationOwner ,

//         apiContext : <string>requestResponseExecutionDTO.apiContext ,
//         apiName : <string> requestResponseExecutionDTO.apiName ,
//         apiVersion : <string>requestResponseExecutionDTO.apiVersion ,
//         apiResourcePath : <string>requestResponseExecutionDTO.apiResourcePath ,
//         apiResourceTemplate : <string>requestResponseExecutionDTO.apiResourceTemplate ,

//         apiMethod : <string>requestResponseExecutionDTO.apiMethod ,
//         apiCreator : <string>requestResponseExecutionDTO.apiCreator ,
//         apiCreatorTenantDomain : <string>requestResponseExecutionDTO.apiCreatorTenantDomain ,
//         apiTier :  <string>requestResponseExecutionDTO.apiTier ,
//         apiHostname : <string>requestResponseExecutionDTO.apiHostname ,

//         username : <string>requestResponseExecutionDTO.userName ,
//         userTenantDomain :  <string>requestResponseExecutionDTO.userTenantDomain ,
//         userIp :  <string>requestResponseExecutionDTO.userIp ,
//         userAgent : <string>requestResponseExecutionDTO.userAgent ,
//         requestTimestamp : requestResponseExecutionDTO.requestTimestamp ,

//         throttledOut : requestResponseExecutionDTO.throttledOut ,
//         responseTime : requestResponseExecutionDTO.responseTime ,
//         serviceTime : requestResponseExecutionDTO.serviceTime ,
//         backendTime : requestResponseExecutionDTO.backendTime ,
//         responseCacheHit : requestResponseExecutionDTO.responseCacheHit ,

//         responseSize : requestResponseExecutionDTO.responseSize ,
//         protocol : requestResponseExecutionDTO.protocol ,
//         responseCode : requestResponseExecutionDTO.responseCode ,
//         destination : requestResponseExecutionDTO.destination ,
//         securityLatency : requestResponseExecutionDTO.executionTime.securityLatency ,

//         throttlingLatency : requestResponseExecutionDTO.executionTime.throttlingLatency , 
//         requestMedLat : requestResponseExecutionDTO.executionTime.requestMediationLatency ,
//         responseMedLat :requestResponseExecutionDTO.executionTime.responseMediationLatency , 
//         backendLatency : requestResponseExecutionDTO.executionTime.backEndLatency , 
//         otherLatency : requestResponseExecutionDTO.executionTime.otherLatency , 

//         gatewayType : <string>requestResponseExecutionDTO.gatewayType , 
//         label : <string>requestResponseExecutionDTO.label
//     };  

//     return analyticsJSON;