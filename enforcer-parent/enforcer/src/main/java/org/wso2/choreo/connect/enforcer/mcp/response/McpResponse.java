package org.wso2.choreo.connect.enforcer.mcp.response;

import com.google.gson.annotations.SerializedName;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;

/**
 * This class is used to represent the response for the MCP.
 */
public class McpResponse {
    @SerializedName("jsonrpc")
    private final String jsonRpcVersion = McpConstants.RpcConstants.JSON_RPC_VERSION;
    @SerializedName("id")
    private Object id;

    public McpResponse(Object id) {
        this.id = id;
    }

    public String getJsonRpcVersion() {
        return jsonRpcVersion;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }
}
