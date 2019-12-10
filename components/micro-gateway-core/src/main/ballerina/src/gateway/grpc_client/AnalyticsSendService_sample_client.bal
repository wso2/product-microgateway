import ballerina/grpc;
import ballerina/io;



// AnalyticsSendServiceClient nonblockingGRPCAnalyticsClient = new(getConfigValue(GRPC_ANALYTICS, GRPC_ENDPOINT_URL, "http://localhost:9806"));

AnalyticsSendServiceClient nonblockingGRPCAnalyticsClient = new(getConfigValue(GRPC_ANALYTICS, GRPC_ENDPOINT_URL, "https://localhost:9806"),
config = {
            secureSocket: {
                 keyStore: {
                       path : getConfigValue(GRPC_ANALYTICS, keyStoreFile, "/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaKeystore.p12") , //"/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaKeystore.p12",
                       password : getConfigValue(GRPC_ANALYTICS, keyStorePassword, "ballerina") //"ballerina"
                },
                trustStore: {
                    path : getConfigValue(GRPC_ANALYTICS, trustStoreFile, "/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaTruststore.p12") //"ballerina"
                , //"/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaTruststore.p12",
                    password :  getConfigValue(GRPC_ANALYTICS, trustStorePassword, "ballerina") //"ballerina"
                },
                verifyHostname:false
            },
            timeoutInMillis : 2147483647
            // ,poolConfig : { maxActiveStreamsPerConnection : 100  }
    } );

   

    grpc:StreamingClient gRPCEp = new grpc:StreamingClient();

public function initGRPCService(){
    io:println("Init gRPC method called");
    var gRPCres = nonblockingGRPCAnalyticsClient -> sendAnalytics(AnalyticsSendServiceMessageListener);
    if (gRPCres is grpc:Error) {
        io:println("Error from Connector: " + gRPCres.reason() + " - "
                                           + <string> gRPCres.detail()["message"]);
        return;
    } else {
        // io:println("Initialized connection sucessfully.");
        gRPCEp = gRPCres;
    }
}


public function dataToAnalytics(AnalyticsStreamMessage message){
    grpc:Error? connErr = gRPCEp->send(message);
        if (connErr is grpc:Error) {
            io:println("Error from Connector: " + connErr.reason() + " - "
                                       + <string> connErr.detail()["message"]);
            
        } else {
            // io:println("Completed Sending: ");
        }
}

service AnalyticsSendServiceMessageListener = service {

    resource function onMessage(string message) {
        // Implementation goes here.
    }

    resource function onError(error err) {
        io:println("Error reported from server: " + err.reason() + " - "
                                           + <string> err.detail()["message"]);
        
    }

    resource function onComplete() {
        // Implementation goes here.
    }
};

