import ballerina/http;
import ballerina/log;

listener http:Listener ep = new (9080);

service / on ep {
    resource function post books(http:Caller caller, http:Request request) returns error? {
        log:printInfo("Backend service is called");
        http:Response resp = new;

        string|http:HeaderNotFoundError pwHeader = request.getHeader("password");
        if !(pwHeader is string && pwHeader == "admin") {
            resp.setXmlPayload(xml `<response>Error</response>`);
            resp.statusCode = 401;
            check caller->respond(resp);
        }

        xml xmlPayload = check request.getXmlPayload();
        log:printInfo("Received payload", (), {"message": xmlPayload.toString()});

        resp.setTextPayload("created", "text/plain");
        resp.statusCode = 200;
        check caller->respond(resp);
    }
}
