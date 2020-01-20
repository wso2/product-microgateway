import ballerina/grpc;
import ballerina/log;
import ballerina/task;

grpc:StreamingClient gRPCEp = new grpc:StreamingClient();
boolean gRPCConnection = false; //check gRPC connection
int reConnectTime =  <int>getConfigIntValue(GRPC_ANALYTICS,gRPC_RetryTimeMilliseconds,6000);
boolean isTaskStarted = false;    //to check gRPC reconnect task

task:Scheduler gRPCConnectTimer = new({
        intervalInMillis :  reConnectTime,
        initialDelayInMillis:0
});

service connectGRPC = service {
    resource function onTrigger(){
        printDebug(KEY_ANALYTICS_FILTER,"gRPC Reconnect Task Still Running.");
        isTaskStarted = true;
        if (!gRPCConnection) {
            initGRPCService();
            log:printWarn("Connection will retry again in "+ reConnectTime.toString() +" milliseconds.");
            pingMessage(gRPCPingMessage);
        } else {
            log:printInfo("Successfully connected to gRPC server.");
            // terminates the timer if gRPPCConnection variable assigned as false
            var stop = gRPCConnectTimer.stop();
            if (stop is error) {
                log:printError("Stopping the gRPC reconnect task is failed.");
                return;
            }
            isTaskStarted = false;
        }
    }
};

//gRPC secured client endpoint configuraion  
AnalyticsSendServiceClient nonblockingGRPCAnalyticsClient = new(getConfigValue(GRPC_ANALYTICS, GRPC_ENDPOINT_URL, "https://localhost:9806"),
config = {
    secureSocket: {
        keyStore: {
            path : getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PATH, "${ballerina.home}/bre/security/ballerinaKeystore.p12"), 
            password : getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina") 
        },
        trustStore: {
            path : getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PATH, "${ballerina.home}/bre/security/ballerinaTruststore.p12"), 
            password :  getConfigValue(LISTENER_CONF_INSTANCE_ID, TRUST_STORE_PASSWORD, "ballerina") 
        },
        verifyHostname:false //to avoid SSL certificate validation error
    },
    timeoutInMillis : 2147483647
} );

# `initGRPCService` function binds gRPC streaming client endpoint with server message listner.

public function initGRPCService(){
    //registers server message listner (AnalyticsSendServiceMessageListener)
    var attachResult = gRPCConnectTimer.attach(connectGRPC);
     if (attachResult is error) {
        log:printError("Error attaching the gRPC reconnect service.");
        return;
    }
    var gRPCres = nonblockingGRPCAnalyticsClient -> sendAnalytics(AnalyticsSendServiceMessageListener);
    if (gRPCres is grpc:Error) {
        log:printError("Error from Connector: " + gRPCres.reason() + " - " + <string> gRPCres.detail()["message"]);
        return;
    } else {
        printDebug(KEY_ANALYTICS_FILTER,"Initialized gRPC connection sucessfully.");
        gRPCEp = gRPCres;
    }
}

# `pingMessage` function checks whether gRPC server is available when there is a connection failure
# It sends gRPCPingMessage dfined below to check the connection
# 
# + message - 'AnalyticsStreamMessage' Message structure defined in the Analytics.proto file
# 
public function pingMessage(AnalyticsStreamMessage message){
    //ping Message used to check gRPC server availability
    printDebug(KEY_ANALYTICS_FILTER,"gRPC reconnect Ping Message executed.");
    grpc:Error? connErr = gRPCEp->send(message);
        if (connErr is grpc:Error) {
            printDebug(KEY_ANALYTICS_FILTER,"Error from Connector: " + connErr.reason() + " - " + <string> connErr.detail()["message"]);
        } else {
            printDebug(KEY_ANALYTICS_FILTER,"Completed Sending gRPC Analytics data: ");
            gRPCConnection = true;
        }
}

# `dataToAnalytics` function sends analytics data to the gRPC server
# It sends AnalyticsStream message to  APIM_EVENT_RECEIVER Siddhi app's gRPCStream
# 
# + message - 'AnalyticsStreamMessage' Message structure defined in the Analytics.proto file
# 
public function dataToAnalytics(AnalyticsStreamMessage message){
    //publishes data to relevant stream
    printDebug(KEY_ANALYTICS_FILTER,"gRPC analytics data publishing method executed.");
    grpc:Error? connErr = gRPCEp->send(message);
        if (connErr is grpc:Error) {
            log:printInfo("Error from Connector: " + connErr.reason() + " - " + <string> connErr.detail()["message"]);
           
        } else {
            printDebug(KEY_ANALYTICS_FILTER,"gRPC analytics data published successfully: ");
        }
}

service AnalyticsSendServiceMessageListener = service {
    //server message listner
    resource function onMessage(string message){
    }

    resource function onError(error err) {
        printDebug(KEY_ANALYTICS_FILTER,"On error method in gRPC listner.");
        gRPCConnection = false;
        //Triggers when there is a gRPC connection error.
        if (err.reason() == "{ballerina/grpc}UnavailableError" && gRPCConnection == false) {
            printDebug(KEY_ANALYTICS_FILTER,"gRPC unavaliable error identified.");
            log:printError("Error reported from server: " + err.reason() + " - " + <string> err.detail()["message"]);
            //starts gRPC reconnect task
            if (isTaskStarted == false) {
                var startResult = gRPCConnectTimer.start();
                if (startResult is error ) {
                    printDebug(KEY_ANALYTICS_FILTER,"Starting the gRPC reconnect task is failed.");
                    return;
                }   
            }
        }
    }

    resource function onComplete() {
    }
};

AnalyticsStreamMessage gRPCPingMessage = {
    //Ping message used to check gRPC connection
    messageStreamName: "PingMessage",
    meta_clientType : "" ,
    applicationConsumerKey : "" ,
    applicationName : "" ,
    applicationId : "" ,
    applicationOwner : "" ,
    apiContext : "" ,
    apiName : "" ,
    apiVersion : "" ,
    apiResourcePath : "" ,
    apiResourceTemplate : "" ,
    apiMethod : "" ,
    apiCreator : "" ,
    apiCreatorTenantDomain : "" ,
    apiTier : "" ,
    apiHostname : "" ,
    username : "" ,
    userTenantDomain : "" ,
    userIp : "" ,
    userAgent : "" ,
    requestTimestamp : 0 ,
    throttledOut : false ,
    responseTime :0 ,
    serviceTime : 0 ,
    backendTime : 0 ,
    responseCacheHit : false,
    responseSize : 0 ,
    protocol : "" ,
    responseCode  : 0 ,
    destination : "" ,
    securityLatency  : 0 ,
    throttlingLatency  : 0 , 
    requestMedLat : 0 ,
    responseMedLat : 0 , 
    backendLatency : 0 , 
    otherLatency : 0 , 
    gatewayType : "" , 
    label  : "",

    subscriber : "",
    throttledOutReason : "",
    throttledOutTimestamp : 0,
    hostname : "",
 
    errorCode : "",
    errorMessage : ""
};

