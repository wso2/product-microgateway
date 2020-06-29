import ballerina/http;
import ballerina/io;
import ballerina/time;

http:Client clientEndpoint = new("");

# Description
#
# + startedTime - startedTime Parameter Description
# + project_name - project_name Parameter Description
# + url - url Parameter Description
# + qualifiedService - qualifiedService Parameter Description

public function mainService(time:Time startedTime, string project_name, string url , string qualifiedService) {


 time:Time current = time:currentTime();
    int currentTimeMills = current.time;
    int hour1 = time:getHour(startedTime);
    int minute1 = time:getMinute(startedTime);
    int second1 = time:getSecond(startedTime);
    int milliSecond1 = time:getMilliSecond(startedTime);
    int currentsecond = time:getSecond(current);


    time:Time tmSub = time:subtractDuration(current,0,0,0,hour1,minute1,second1,milliSecond1);
    string runningtime = time:toString(tmSub);

    map<json> p2 = {
            apiVersion: "1.2.0",
            mgwversion: "3.1.0",
            gatewayURL: url,
            projectName: project_name,
            uptime: runningtime,
            status: "ONLINE",
            services: qualifiedService
       };
       io:println(p2.toJsonString());


    io:println("\nPOST request:");

     var response = clientEndpoint->post("/post", "POST: Microgateway Dashboard Data");

    handleResponse(response);

    io:println("\nUse custom HTTP verbs:");

    response = clientEndpoint->execute("COPY", "/get", "CUSTOM: Retrieve data");


    response = clientEndpoint->get("/get", p2);
    if (response is http:Response) {
        string contentType = response.getHeader("Content-Type");
        io:println("Content-Type: " + contentType);

        int statusCode = response.statusCode;
        io:println("Status code: " + statusCode.toString());

    } else {
        io:println("Error when calling the backend: " , response.reason());
    }
}

function handleResponse(http:Response|error response) {
    if (response is http:Response) {
        var msg = response.getJsonPayload();
        if (msg is json) {

            io:println(msg.toJsonString());
        } else {
            io:println("Invalid payload received:" , msg.reason());
        }
    } else {
        io:println("Error when calling the backend: ", response.reason());
    }
}
