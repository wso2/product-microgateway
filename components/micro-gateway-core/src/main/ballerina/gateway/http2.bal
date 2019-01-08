import ballerina/io;
import ballerina/config;

public function getHttpVersion() returns string {
    boolean val = config:getAsBoolean("http2.http2Enable");
    if(val) {
        return "2.0";
    } else {
        return "1.1";
    }
}