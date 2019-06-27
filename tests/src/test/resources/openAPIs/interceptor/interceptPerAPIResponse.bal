import ballerina/io;
import ballerina/http;

public function interceptPerAPIResponse (http:Caller caller, http:Response res) {
    res.setHeader("PerAPIResponse_Header","header");
}