// // public function main (string... args) {
// //     EventServiceBlockingClient blockingEp = new("http://localhost:9090");
// // }



// import ballerina/grpc;
// import ballerina/log;
// //import ballerina/io;

// EventServiceClient analyticsClient = new(getConfigValue(GRPC_ANALYTICS, GRPC_ENDPOINT_URL, "https://localhost:9806"),
// config = {
//             secureSocket: {
//                  keyStore: {
//                        path : getConfigValue(GRPC_ANALYTICS, keyStoreFile, "/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaKeystore.p12") , //"/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaKeystore.p12",
//                        password : getConfigValue(GRPC_ANALYTICS, keyStorePassword, "ballerina") //"ballerina"
//                 },
//                 trustStore: {
//                     path : getConfigValue(GRPC_ANALYTICS, trustStoreFile, "/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaTruststore.p12") //"ballerina"
//                 , //"/home/lahiru/Desktop/TestZip/mcgw/runtime/bre/security/ballerinaTruststore.p12",
//                     password :  getConfigValue(GRPC_ANALYTICS, trustStorePassword, "ballerina") //"ballerina"
//                 },
//                 verifyHostname:false
//             }
//     } );


// service EventServiceMessageListner = service {
//         resource function onMessage(string message) {
//         // total = 1;
//         log:printDebug("Response received from server: " + message);
//     }

//     resource function onError(error err) {
//         log:printDebug("Error reported from server: " + err.reason() + " - "
//                                            + <string> err.detail()["message"]);
// }

//     resource function onComplete() {
//         // total = 1;
//         log:printDebug("Server Complete Sending Responses.");
//     }
// };

// public function dataToAnalytics(string payloadString, string streamId){
//     //io:println("3 -->  Inside method call");
//     log:printDebug("Grpc :" + streamId +"triggered------------------------>>>>>>>>>>>>>>>>>" );
//     grpc:StreamingClient ep;
//     var res = analyticsClient->consume(EventServiceMessageListner);
//     // var res = analyticsClient->consume();
//     if(res is grpc:Error){
//         log:printDebug("Error from connector :" + res.reason()+ " - " + <string>res.detail()["message"]);
//         return ;
//     }
//     else{
//         log:printDebug("Initialized Connection Successfully");
//         ep = res;
//     }
//     Event event = {payload:payloadString,
//     headers: [{key:"stream.id", value:streamId}]};
//     grpc:Error? connErr = ep->send(event);
//         if (connErr is grpc:Error) {
//             log:printDebug("Error from Connector: " + connErr.reason() + " - "
//                                        + <string> connErr.detail()["message"]);
//         } else {
//             log:printDebug("gRPC analyitics sent successfully");
//         }
// }


