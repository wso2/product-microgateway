package org.wso2.choreo.connect.mockbackend;

public class InterceptorConstants {
    public enum Handler {
        NONE,
        REQUEST_ONLY,
        RESPONSE_ONLY,
        BOTH
    }

    public static class StatusPayload {
        public static final String HANDLER = "handler";
        public static final String REQUEST_FLOW_REQUEST_BODY = "requestFlowRequestBody";
        public static final String RESPONSE_FLOW_REQUEST_BODY = "responseFlowRequestBody";
    }
}
