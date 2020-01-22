

public function getHttpVersion() returns string {
    boolean val = getConfigBooleanValue(HTTP2_INSTANCE_ID, HTTP2_PROPERTY, DEFAULT_HTTP2_ENABLED);
    if (val) {
        return HTTP2;
    } else {
        return HTTP11;
    }
}
