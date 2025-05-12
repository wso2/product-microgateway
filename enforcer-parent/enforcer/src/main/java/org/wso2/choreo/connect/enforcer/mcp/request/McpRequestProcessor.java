package org.wso2.choreo.connect.enforcer.mcp.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.commons.model.ExtendedOperation;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;
import org.wso2.choreo.connect.enforcer.mcp.McpException;
import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;

/**
 * This class is used to process the MCP requests.
 */
public class McpRequestProcessor {

    public static String processRequest(String requestBody) {
        String apikey = "localhost:/somevalue/zfzu/defaultsdfdsf/v1.0:v1.0";
        try {
            validateRequest(requestBody);
            JsonObject requestObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String id = requestObject.get(McpConstants.RpcConstants.ID).getAsString();
            String method = requestObject.get(McpConstants.RpcConstants.METHOD).getAsString();
            API matchedMcpApi = APIFactory.getInstance().getMatchedAPIByKey(apikey);
            if (matchedMcpApi == null) {
                // Handle the case where the API is not found
                // This cannot happen as the gateway will return 404 before this point
                return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                        McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE, "MCP Proxy is not available");
            }
            if (McpConstants.METHOD_INITIALIZE.equals(method)) {
                validateInitializeRequest(requestObject);
                return handleMcpInitialize(id, matchedMcpApi);
            } else if (McpConstants.METHOD_TOOL_LIST.equals(method)) {
                return handleMcpToolList(id, matchedMcpApi);
            } else if (McpConstants.METHOD_TOOL_CALL.equals(method)) {
                validateToolsCallRequest(requestObject, matchedMcpApi);
                // Handle the tool call method
                // This is not implemented yet
                return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.METHOD_NOT_FOUND_CODE,
                        McpConstants.RpcConstants.METHOD_NOT_FOUND_MESSAGE, "Method not implemented");

            }
        } catch (McpException e) {
            return e.toJsonRpcErrorPayload();
        }
        return null;
    }

    private static void validateRequest(String requestBody) throws McpException {
        JsonElement jsonElement;
        try {
            jsonElement = JsonParser.parseString(requestBody);
        } catch (JsonSyntaxException e) {
            throw new McpException(McpConstants.RpcConstants.PARSE_ERROR_CODE,
                    McpConstants.RpcConstants.PARSE_ERROR_MESSAGE, e.getMessage());

        }
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has(McpConstants.RpcConstants.JSON_RPC)) {
                JsonElement jsonRpcElement = jsonObject.get(McpConstants.RpcConstants.JSON_RPC);
                if (jsonRpcElement == null || jsonRpcElement.isJsonNull()) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing jsonrpc field");
                }
                if (!McpConstants.RpcConstants.JSON_RPC_VERSION.equals(jsonRpcElement.getAsString())) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Invalid JSON-RPC version");
                }
            } else {
                throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing jsonrpc field");
            }
            if (jsonObject.has(McpConstants.RpcConstants.ID)) {
                JsonElement idElement = jsonObject.get(McpConstants.RpcConstants.ID);
                if (idElement == null || idElement.isJsonNull() ) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing id field");
                }
                if (idElement.getAsString().isEmpty()) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing id field");
                }
            } else {
                throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing id field");
            }
            if (jsonObject.has(McpConstants.RpcConstants.METHOD)) {
                JsonElement methodElement = jsonObject.get(McpConstants.RpcConstants.METHOD);
                if (methodElement == null || methodElement.isJsonNull()) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing method field");
                }
                String method = methodElement.getAsString();
                if (method.isEmpty()) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing method field");
                }
                if (!McpConstants.ALLOWED_METHODS.contains(method)) {
                    throw new McpException(McpConstants.RpcConstants.METHOD_NOT_FOUND_CODE,
                            McpConstants.RpcConstants.METHOD_NOT_FOUND_MESSAGE, "Method not found");
                }
            } else {
                throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing method field");
            }
        } else {
            throw new McpException(McpConstants.RpcConstants.PARSE_ERROR_CODE,
                    McpConstants.RpcConstants.PARSE_ERROR_MESSAGE, "Invalid JSON format");
        }
    }

    public static void validateInitializeRequest(JsonObject requestObject) throws McpException {
        if (requestObject.has(McpConstants.PARAMS_KEY)) {
            JsonObject params = requestObject.getAsJsonObject(McpConstants.PARAMS_KEY);
            if (params.has(McpConstants.PROTOCOL_VERSION_KEY)) {
                String protocolVersion = params.get(McpConstants.PROTOCOL_VERSION_KEY).getAsString();
                if (!McpConstants.SUPPORTED_PROTOCOL_VERSIONS.contains(protocolVersion)) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_PARAMS_CODE,
                            McpConstants.RpcConstants.INVALID_PARAMS_MESSAGE, "Supported protocol versions are: "
                            + McpConstants.SUPPORTED_PROTOCOL_VERSIONS);
                }
            } else {
                throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing protocolVersion field");
            }
        } else {
            throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                    McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing params field");
        }
    }

    private static void validateToolsCallRequest(JsonObject jsonObject, API matchedApi) throws McpException {
        if (jsonObject.has(McpConstants.PARAMS_KEY)) {
            JsonObject params = jsonObject.getAsJsonObject(McpConstants.PARAMS_KEY);
            if (params.has(McpConstants.TOOL_NAME_KEY)) {
                JsonElement toolNameElement = params.get(McpConstants.TOOL_NAME_KEY);
                if (toolNameElement == null || toolNameElement.isJsonNull()) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing toolName field");
                }
                String toolName = toolNameElement.getAsString();
                if (toolName == null || toolName.isEmpty()) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing toolName field");
                } else {
                    if (!validateToolName(toolName, matchedApi)) {
                        throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                                McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "The requested tool does not exist");
                    }
                }
            } else {
                throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing toolName field");
            }
        } else {
            throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                    McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing params field");
        }
    }

    private static boolean validateToolName(String toolName, API matchedApi) {
        return matchedApi.getAPIConfig().getExtendedOperations()
                .stream()
                .anyMatch(operation -> toolName.equals(operation.getName()));
    }

    private static String handleMcpInitialize(String id, API matchedApi) {
        String name = matchedApi.getAPIConfig().getName();
        String version = matchedApi.getAPIConfig().getVersion();
        String description = "This is an MCP Server";
        return PayloadGenerator
                .getInitializeResponse(id, name, version, description, false);
    }

    private static String handleMcpToolList(String id, API matchedApi) {
        return PayloadGenerator
                .generateToolListPayload(id, matchedApi.getAPIConfig().getExtendedOperations());
    }

    private static void handleMcpToolsCall(String id, API matchedApi, JsonObject jsonObject) {
        String toolName = jsonObject.getAsJsonObject(McpConstants.PARAMS_KEY)
                .get(McpConstants.TOOL_NAME_KEY).getAsString();
        ExtendedOperation extendedOperation = matchedApi.getAPIConfig().getExtendedOperations()
                .stream()
                .filter(operation -> operation.getName().equals(toolName))
                .findFirst()
                .orElse(null);
        String args;
        String vHost = matchedApi.getAPIConfig().getVhost();
        JsonObject params = jsonObject.getAsJsonObject(McpConstants.PARAMS_KEY);
        if (params.has(McpConstants.ARGUMENTS_KEY)) {
            args = params.get(McpConstants.ARGUMENTS_KEY).getAsString();
        } else {
            args = "{}";
        }
    }
}
