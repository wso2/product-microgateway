import ballerina/grpc;
import ballerina/io;
import ballerina/log;
import ballerina/task;

//gRPC endpoint for http connections
//AnalyticsSendServiceClient nonblockingGRPCAnalyticsClient = new(getConfigValue(GRPC_ANALYTICS, GRPC_ENDPOINT_URL, "http://localhost:9806"));

grpc:StreamingClient gRPCEp = new grpc:StreamingClient();
boolean gRPCConnection = false;
map<any> gRPCConfigs = getConfigMapValue(GRPC_ANALYTICS);
int reConnectTime =  <int>gRPCConfigs[gRPC_RetryTimeMilliseconds];

task:Scheduler gRPCConnectTimer = new({
        intervalInMillis :  reConnectTime,
        initialDelayInMillis:0
});

service connectGRPC = service {
    resource function onTrigger(){
        if(gRPCConnection == false){
            initGRPCService();
            io:println("Connection will retry again in "+ reConnectTime.toString() +" milliseconds if there is a connection error.");
        }
    }
};

//gRPC secured client endpoint configuraion  
AnalyticsSendServiceClient nonblockingGRPCAnalyticsClient = new(getConfigValue(GRPC_ANALYTICS, GRPC_ENDPOINT_URL, "https://localhost:9806"),
config = {
            secureSocket: {
                 keyStore: {
                       path : getConfigValue(GRPC_ANALYTICS, gRPCKeyStoreFile, "${ballerina.home}/bre/security/ballerinaKeystore.p12"), 
                       password : getConfigValue(GRPC_ANALYTICS, gRPCKeyStorePassword, "ballerina") 
                },
                trustStore: {
                    path : getConfigValue(GRPC_ANALYTICS, gRPCTrustStoreFile, "${ballerina.home}/bre/security/ballerinaTruststore.p12"), 
                    password :  getConfigValue(GRPC_ANALYTICS, gRPCTrustStorePassword, "ballerina") 
                },
                verifyHostname:false //to avoid SSL certificate validation error
            },
            timeoutInMillis : 2147483647
} );

//registers server message listner (AnalyticsSendServiceMessageListener)
public function initGRPCService(){
    var attachResult = gRPCConnectTimer.attach(connectGRPC);
     if (attachResult is error) {
        io:println("Error attaching the service1.");
        return;
    }

    io:println("gRPC Analytics initiating...");
    var gRPCres = nonblockingGRPCAnalyticsClient -> sendAnalytics(AnalyticsSendServiceMessageListener);
    if (gRPCres is grpc:Error) {
        io:println("Error from Connector: " + gRPCres.reason() + " - "
                                           + <string> gRPCres.detail()["message"]);
        return;
    } else {
        log:printDebug("Initialized gRPC connection sucessfully.");
        gRPCEp = gRPCres;
    }
}

//publishes data to relevant stream
public function dataToAnalytics(AnalyticsStreamMessage message){
    grpc:Error? connErr = gRPCEp->send(message);
        if (connErr is grpc:Error) {
            io:println("Error from Connector: " + connErr.reason() + " - "
                                       + <string> connErr.detail()["message"]);
            
        } else {
            log:printDebug("Completed Sending gRPC Analytics data: ");
            if(gRPCConnection == false){
                //terminates the timer if gRPPCConnection variable assigned as false
                var stop = gRPCConnectTimer.stop();
                if (stop is error) {
                    io:println("Stopping the task is failed.");
                    return;
                }
            }
            gRPCConnection = true;
        }
}

//server message listner
service AnalyticsSendServiceMessageListener = service {

    resource function onMessage(string message) {
    }

    resource function onError(error err) {
        //Triggers @ when startup when gRPC connection is closed.
        if (err.reason() == "{ballerina/grpc}UnavailableError" && gRPCConnection == false){
            io:println("Error reported from server: " + err.reason() + " - " + <string> err.detail()["message"]);
            
            var startResult = gRPCConnectTimer.start();
                if (startResult is error ) {
                    log:printDebug("Starting the task is failed.");
                    return;
            }   
            gRPCConnection = false;
        }
        //starts the timer if error is gRPC unavailable and gRPCConnection has established previously.
        //(Triggers when wroked gRPC connection get closed)
        if (err.reason() == "{ballerina/grpc}UnavailableError" && gRPCConnection == true){
            gRPCConnection = false;
            io:println("Error reported from server: " + err.reason() + " - " + <string> err.detail()["message"]);
            var startResult = gRPCConnectTimer.start();
                if (startResult is error ) {
                    io:println("Starting the task is failed.");
                    return;
            }
        }
    }

    resource function onComplete() {
    }
};

