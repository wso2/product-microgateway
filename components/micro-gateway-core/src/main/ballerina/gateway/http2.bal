import ballerina/io;
import ballerina/config;

public function getHttpVersion() returns string {
    boolean val = getConfigBooleanValue(HTTP2_INSTANCE_ID, HTTP2_PROPERTY, false);
    if (val) {
        return "2.0";
    } else {
        return "1.1";
    }
}
