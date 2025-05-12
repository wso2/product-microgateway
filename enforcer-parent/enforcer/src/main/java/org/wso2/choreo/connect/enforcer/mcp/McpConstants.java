package org.wso2.choreo.connect.enforcer.mcp;

import java.util.Arrays;
import java.util.List;

/**
 * This class contains constants used for MCP processing
 */
public class McpConstants {
    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_TOOL_LIST = "tools/list";
    public static final String METHOD_TOOL_CALL = "tools/call";
    public static final List<String> ALLOWED_METHODS = Arrays.asList(METHOD_INITIALIZE, METHOD_TOOL_LIST,
            METHOD_TOOL_CALL);
    public static final String PROTOCOL_VERSION_KEY = "protocolVersion";
    public static final String PROTOCOL_VERSION_2025_MARCH = "2025-03-26";
    public static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of(PROTOCOL_VERSION_2025_MARCH);
    public static final String PARAMS_KEY = "params";
    public static final String TOOL_NAME_KEY = "toolName";
    public static final String REQUIRED_KEY = "required";
    public static final String PROPERTIES_KEY = "properties";
    public static final String ARGUMENTS_KEY = "arguments";

    /**
     * This class contains constants used for RPC processing
     */
    public static class RpcConstants {
        public static final String JSON_RPC = "jsonrpc";
        public static final String METHOD = "method";
        public static final String ID = "id";
        public static final String JSON_RPC_VERSION = "2.0";
        public static final int PARSE_ERROR_CODE = -32700;
        public static final int INVALID_REQUEST_CODE = -32600;
        public static final int METHOD_NOT_FOUND_CODE = -32601;
        public static final int INVALID_PARAMS_CODE = -32602;
        public static final int INTERNAL_ERROR_CODE = -32603;
        public static final String PARSE_ERROR_MESSAGE = "Parse error";
        public static final String INVALID_REQUEST_MESSAGE = "Invalid Request";
        public static final String METHOD_NOT_FOUND_MESSAGE = "Method not found";
        public static final String INVALID_PARAMS_MESSAGE = "Invalid params";
        public static final String INTERNAL_ERROR_MESSAGE = "Internal error";
    }
}
