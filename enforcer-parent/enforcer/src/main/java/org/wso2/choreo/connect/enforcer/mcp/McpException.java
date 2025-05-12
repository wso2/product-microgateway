package org.wso2.choreo.connect.enforcer.mcp;

import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;

/**
 * This class is used to throw MCP related errors
 */
public class McpException extends Exception {
    private final int errorCode;
    private final String errorMessage;
    private final String data;

    public McpException(int errorCode, String errorMessage, String data) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getData() {
        return data;
    }

    public String toJsonRpcErrorPayload() {
        return PayloadGenerator.getErrorResponse(errorCode, errorMessage, data);
    }
}
