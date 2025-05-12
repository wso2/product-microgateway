package org.wso2.choreo.connect.enforcer.mcp.response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.wso2.choreo.connect.enforcer.commons.model.ExtendedOperation;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;

import java.util.List;
import java.util.Map;

/**
 * This class is used to generate the payload for the MCP response.
 */
public class PayloadGenerator {
    private static final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    public static String getErrorResponse(Integer id, int code, String message, String data) {
        McpError error = new McpError(code, message, data);
        McpErrorResponse errorResponse = new McpErrorResponse(id, error);
        return gson.toJson(errorResponse);
    }

    public static String getErrorResponse(int code, String message, String data) {
        McpError error = new McpError(code, message, data);
        McpErrorResponse errorResponse = new McpErrorResponse(null, error);
        return gson.toJson(errorResponse);
    }

    public static String getInitializeResponse(String id, String serverName, String serverVersion,
            String serverDescription, boolean toolListChangeNotified) {
        // Create the response object as specified in
        // https://modelcontextprotocol.io/specification/2025-03-26/basic/lifecycle#initialization
        McpResponse response = new McpResponse(id);
        JsonObject responseObject = gson.fromJson(gson.toJson(response), JsonObject.class);
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", McpConstants.PROTOCOL_VERSION_2025_MARCH);

        JsonObject capabilities = new JsonObject();
        JsonObject toolCapabilities = new JsonObject();
        toolCapabilities.addProperty("listChanged", toolListChangeNotified);
        capabilities.add("tools", toolCapabilities);
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", serverName);
        serverInfo.addProperty("version", serverVersion);
        serverInfo.addProperty("description", serverDescription);
        result.add("serverInfo", serverInfo);

        responseObject.add("result", result);
        return gson.toJson(responseObject);
    }

    public static String generateToolListPayload(String id, List<ExtendedOperation> extendedOperations) {
        McpResponse response = new McpResponse(id);
        JsonObject responseObject = gson.fromJson(gson.toJson(response), JsonObject.class);
        JsonObject result = new JsonObject();
        JsonArray toolsArray = new JsonArray();
        for (ExtendedOperation extendedOperation : extendedOperations) {
            JsonObject toolObject = new JsonObject();
            toolObject.addProperty("toolName", extendedOperation.getName());
            toolObject.addProperty("toolDescription", extendedOperation.getDescription());
            String schema = extendedOperation.getSchema();
            if (schema != null) {
                JsonObject schemaObject = gson.fromJson(schema, JsonObject.class);
                toolObject.add("inputSchema", sanitizeInputSchema(schemaObject));
            }
            toolsArray.add(toolObject);
        }
        result.add("tools", toolsArray);
        responseObject.add("result", result);

        return gson.toJson(responseObject);
    }

    private static JsonObject sanitizeInputSchema(JsonObject inputObject) {
        if (inputObject == null || inputObject.isEmpty()) {
            return inputObject;
        }

        JsonArray requiredArray = inputObject.getAsJsonArray("required");
        JsonArray sanitizedArray = new JsonArray();

        // remove the header, query, and path prefixes from the required fields
        for (JsonElement element : requiredArray) {
            String requiredField = element.getAsString();
            String newRequiredField = requiredField.split("_", 2)[1];
            sanitizedArray.add(newRequiredField);
        }
        inputObject.add("required", sanitizedArray);

        // remove the header, query, and path prefixes from the properties keys
        JsonObject propertiesObject = inputObject.getAsJsonObject("properties");
        JsonObject sanitizedPropertiesObject = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
            String key = entry.getKey();
            if ("requestBody".equalsIgnoreCase(key)) {
                sanitizedPropertiesObject.add("requestBody", entry.getValue());
                continue;
            }
            String sanitizedKey = key.split("_", 2)[1];
            sanitizedPropertiesObject.add(sanitizedKey, entry.getValue().getAsJsonObject());
        }
        inputObject.add("properties", sanitizedPropertiesObject);
        return inputObject;
    }

}
