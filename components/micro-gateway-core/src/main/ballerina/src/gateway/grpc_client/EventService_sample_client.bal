// public function main (string... args) {
//     EventServiceBlockingClient blockingEp = new("http://localhost:9090");
// }



import ballerina/grpc;
import ballerina/io;

EventServiceClient analyticsClient = new("https://localhost:9806",
config = {
            secureSocket: {
                 keyStore: {
                       path : "/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaKeystore.p12",
                       password : "ballerina"
                },
                trustStore: {
                    path : "/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaTruststore.p12",
                    password : "ballerina"
                }
            }
    } );


service EventServiceMessageListner = service {
        resource function onMessage(string message) {
        // total = 1;
        io:println("Response received from server: " + message);
    }

    resource function onError(error err) {
        io:println("Error reported from server: " + err.reason() + " - "
                                           + <string> err.detail()["message"]);
}

    resource function onComplete() {
        // total = 1;
        io:println("Server Complete Sending Responses.");
    }
};

public function dataToAnalytics(string payloadString, string streamId){
    io:println("Grpc :" + streamId +"triggered------------------------>>>>>>>>>>>>>>>>>" );
    grpc:StreamingClient ep;
    var res = analyticsClient->consume(EventServiceMessageListner);
    // var res = analyticsClient->consume();
    if(res is grpc:Error){
        io:println("Error from connector :" + res.reason()+ " - " + <string>res.detail()["message"]);
        return ;
    }
    else{
        io:println("Initialized Connection Successfully");
        ep = res;
    }
    Event event = {payload:payloadString,
    headers: [{key:"stream.id", value:streamId}]};
    grpc:Error? connErr = ep->send(event);
        if (connErr is grpc:Error) {
            io:println("Error from Connector: " + connErr.reason() + " - "
                                       + <string> connErr.detail()["message"]);
        } else {
            io:println("send greeting successfully");
        }
}


