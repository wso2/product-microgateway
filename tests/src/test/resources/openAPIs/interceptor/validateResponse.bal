import ballerina/io;
import ballerina/http;

public function validateResponse (http:Caller caller, http:Response res) {
    res.setHeader("ResponseHeader","header");
}