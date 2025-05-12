package org.wso2.choreo.connect.enforcer.mcp.response;

import com.google.gson.annotations.SerializedName;

/**
 * This class is used to represent the error response for the MCP.
 */
public class McpErrorResponse extends McpResponse {
    @SerializedName("error")
    McpError error;

    public McpErrorResponse(Integer id, McpError error) {
        super(id);
        this.error = error;
    }
    public void setError(McpError error) {
        this.error = error;
    }
    public McpError getError() {
        return error;
    }
}
