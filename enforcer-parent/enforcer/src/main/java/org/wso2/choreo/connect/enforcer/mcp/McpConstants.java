/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
    public static final String METHOD_PING = "ping";
    public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";
    public static final String METHOD_RESOURCES_LIST = "resources/list";
    public static final String METHOD_RESOURCE_TEMPLATE_LIST = "resources/templates/list";
    public static final String METHOD_PROMPTS_LIST = "prompts/list";
    public static final List<String> ALLOWED_METHODS = Arrays.asList(METHOD_INITIALIZE, METHOD_TOOL_LIST,
            METHOD_TOOL_CALL, METHOD_PING, METHOD_NOTIFICATION_INITIALIZED, METHOD_RESOURCES_LIST, METHOD_PROMPTS_LIST,
            METHOD_RESOURCE_TEMPLATE_LIST);
    public static final String PROTOCOL_VERSION_KEY = "protocolVersion";
    public static final String PROTOCOL_VERSION_2024_NOVEMBER = "2024-11-05";
    public static final String PROTOCOL_VERSION_2025_MARCH = "2025-03-26";
    public static final List<String> SUPPORTED_PROTOCOL_VERSIONS = Arrays.asList(PROTOCOL_VERSION_2025_MARCH);
    public static final String PARAMS_KEY = "params";
    public static final String TOOL_NAME_KEY = "name";
    public static final String TOOL_DESC_KEY = "description";
    public static final String REQUIRED_KEY = "required";
    public static final String PROPERTIES_KEY = "properties";
    public static final String ARGUMENTS_KEY = "arguments";
    public static final String RESULT_KEY = "result";
    public static final String VHOST_HEADER = "x-wso2-mcp-vhost";
    public static final String BASEPATH_HEADER = "x-wso2-mcp-basepath";
    public static final String VERSION_HEADER = "x-wso2-mcp-version";
    public static final String ORG_HEADER = "x-wso2-mcp-organization";
    public static final String PAYLOAD_TOOL_NAME = "tool_name";
    public static final String PAYLOAD_SCHEMA = "schema";
    public static final String PAYLOAD_API_NAME = "api_name";
    public static final String PAYLOAD_CONTEXT = "context";
    public static final String PAYLOAD_VERSION = "version";
    public static final String PAYLOAD_PATH = "path";
    public static final String PAYLOAD_VERB = "verb";
    public static final String PAYLOAD_AUTH = "auth";
    public static final String PAYLOAD_ENDPOINT = "endpoint";
    public static final String ASGARDEO_WK_PLACEHOLDER
            = "https://api.asgardeo.io/t/{organization}/oauth2/token/.well-known/openid-configuration";
    public static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";

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
