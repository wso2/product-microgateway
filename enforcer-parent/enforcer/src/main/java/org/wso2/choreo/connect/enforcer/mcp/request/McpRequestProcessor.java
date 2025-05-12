package org.wso2.choreo.connect.enforcer.mcp.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;
import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;

/**
 * This class is used to process the MCP requests.
 */
public class McpRequestProcessor {

    public static String processRequest(String requestBody) {
        String apikey = "localhost:/somevalue/zfzu/defaultsdfdsf/v1.0:v1.0";
        String errorResponse = validateRequest(requestBody);
        if (errorResponse != null) {
            return errorResponse;
        }
        JsonObject requestObject = JsonParser.parseString(requestBody).getAsJsonObject();
        String id = requestObject.get(McpConstants.RpcConstants.ID).getAsString();
        String method = requestObject.get(McpConstants.RpcConstants.METHOD).getAsString();
        if (McpConstants.METHOD_INITIALIZE.equals(method)) {
            return handleMcpInitialize(id, apikey);
        } else if (McpConstants.METHOD_TOOL_LIST.equals(method)) {
            return handleMcpToolList(id, apikey);
        }

        return null;
    }

    private static String validateRequest(String requestBody) {
        try {
            JsonParser.parseString(requestBody);
        } catch (JsonSyntaxException e) {
            return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.PARSE_ERROR_CODE,
                    McpConstants.RpcConstants.PARSE_ERROR_MESSAGE, e.getMessage());

        }
        JsonElement jsonElement = JsonParser.parseString(requestBody);
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has(McpConstants.RpcConstants.JSON_RPC)) {
                String jsonRpcVersion = jsonObject.get(McpConstants.RpcConstants.JSON_RPC).getAsString();
                if (!McpConstants.RpcConstants.JSON_RPC_VERSION.equals(jsonRpcVersion)) {
                    return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Invalid JSON-RPC version");
                }
            } else {
                return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing jsonrpc field");
            }
            if (jsonObject.has(McpConstants.RpcConstants.ID)) {
                String id = jsonObject.get(McpConstants.RpcConstants.ID).getAsString();
                if (id == null || id.isEmpty()) {
                    return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing id field");
                }
            } else {
                return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing id field");
            }
            if (jsonObject.has(McpConstants.RpcConstants.METHOD)) {
                String method = jsonObject.get(McpConstants.RpcConstants.METHOD).getAsString();
                if (method == null || method.isEmpty()) {
                    return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing method field");
                }
                if (!McpConstants.ALLOWED_METHODS.contains(method)) {
                    return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.METHOD_NOT_FOUND_CODE,
                            McpConstants.RpcConstants.METHOD_NOT_FOUND_MESSAGE, "Method not allowed");
                }
                if (McpConstants.METHOD_INITIALIZE.equals(method)) {
                    if (validateInitializeRequest(jsonObject) != null) {
                        return validateInitializeRequest(jsonObject);
                    }
                }
            } else {
                return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing method field");
            }
        } else {
            return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                    McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Invalid JSON format");
        }
        return null;
    }

    public static String validateInitializeRequest(JsonObject requestObject) {
        if (requestObject.has("params")) {
            JsonObject params = requestObject.getAsJsonObject("params");
            if (params.has("protocolVersion")) {
                String protocolVersion = params.get("protocolVersion").getAsString();
                if (!McpConstants.SUPPORTED_PROTOCOL_VERSIONS.contains(protocolVersion)) {
                    return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_PARAMS_CODE,
                            McpConstants.RpcConstants.INVALID_PARAMS_MESSAGE, "Supported protocol versions are: "
                                    + McpConstants.SUPPORTED_PROTOCOL_VERSIONS);
                }
            } else {
                return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                        McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing protocolVersion field");
            }
        } else {
            return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                    McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing params field");
        }
        return null;
    }

    private static String handleMcpInitialize(String id, String apikey) {
        API matchedMcpApi = APIFactory.getInstance().getMatchedAPIByKey(apikey);
        if (matchedMcpApi == null) {
            // Handle the case where the API is not found
            // This cannot happen as the gateway will return 404 before this point
            return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                    McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE, "MCP Proxy is not available");
        } else {
            String name = matchedMcpApi.getAPIConfig().getName();
            String version = matchedMcpApi.getAPIConfig().getVersion();
            String description = "This is an MCP Server";
            return PayloadGenerator
                    .getInitializeResponse(id, name, version, description, false);
        }
    }

    private static String handleMcpToolList(String id, String apikey) {
        API matchedMcpApi = APIFactory.getInstance().getMatchedAPIByKey(apikey);
        if (matchedMcpApi == null) {
            // Handle the case where the API is not found
            // This cannot happen as the gateway will return 404 before this point
            return PayloadGenerator.getErrorResponse(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                    McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE, "MCP Proxy is not available");
        } else {
            return PayloadGenerator
                    .generateToolListPayload(id, matchedMcpApi.getAPIConfig().getExtendedOperations());
        }
    }
}
