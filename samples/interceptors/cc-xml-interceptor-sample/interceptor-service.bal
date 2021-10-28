import ballerina/http;
import ballerina/log;
import ballerina/xmldata;
import ballerina/lang.array;
import ballerina/lang.value;

listener http:Listener ep = new (9081, secureSocket = {
    key: {
        certFile: "certs/interceptor.crt",
        keyFile: "certs/interceptor.key"
    },

    mutualSsl: {
        verifyClient: http:REQUIRE,
        cert: "certs/mg.pem"
    }
});

service /api/v1 on ep {

    resource function post handle\-request(http:Caller caller, http:Request request) returns error? {
        log:printInfo("Interceptor service is called - Reqeust Flow");

        error? requestHandlerResult = requestHandler(caller, request);
        if requestHandlerResult is error {
            log:printError("Error in handle-request", requestHandlerResult);
        }
    }

    resource function post handle\-response(http:Caller caller, http:Request request) returns error? {
        log:printInfo("Interceptor service is called - Response Flow");

        error? responseHandlerResult = responseHandler(caller, request);
        if responseHandlerResult is error {
            log:printError("Error in handle-response", responseHandlerResult);
        }
    }
}

function requestHandler(http:Caller caller, http:Request request) returns error? {
    json|error clientPayload = getClientBodyAsJSON(request);

    string respBody; // response to be sent to the backend
    if clientPayload is json {
        xml?|error xmlData = xmldata:fromJson(clientPayload); // convert client JSON request body to XML
        if xmlData is xml {
            respBody = xmlData.toString();
        } else if xmlData is () {
            respBody = "<root>nil</root>";
            log:printInfo("Xml data is nil");
        } else {
            log:printError("Error while converting to XML", xmlData);
            respBody = "<error>mediation error</error>";
        }
    } else {
        log:printError("Error while converting request body to json", clientPayload);
        respBody = "<error>mediation error</error>";
    }

    byte[] respBodyBytes = respBody.toBytes();
    string respBodyStr = array:toBase64(respBodyBytes); // base64 encode the response body

    json respPayload = { // build the respose of the interceptor service
        body: respBodyStr,
        headersToReplace: {
            "content-type": "application/xml"
        }
    };

    http:Response resp = new;
    resp.setJsonPayload(respPayload);
    check caller->respond(resp);
}

function responseHandler(http:Caller caller, http:Request request) returns error? {
    json payloadJSON = check request.getJsonPayload(); // request payload from Choreo Connect Router
    int statusCode = check payloadJSON.responseCode; // get backend HTTP status code

    json respPayload = {}; // if status code is not 200, do not do any changes to backend response
    if statusCode == 200 {
        respPayload = { // build the respose of the interceptor service
            responseCode: 201
        };
    }

    http:Response resp = new;
    resp.setJsonPayload(respPayload);
    check caller->respond(resp);
}

function getClientBodyAsJSON(http:Request request) returns json|error {
    json interceptorRequestJSON = check request.getJsonPayload(); // read request received from Choreo Connect Router
    string bodyBase64 = check interceptorRequestJSON.requestBody; // read the "requestBody" which is the base64 encoded client request body
    byte[] bodyBytes = check array:fromBase64(bodyBase64); // get decoded client request body
    string bodyStr = check 'string:fromBytes(bodyBytes);
    return value:fromJsonString(bodyStr); // convert client request body string to json
}
