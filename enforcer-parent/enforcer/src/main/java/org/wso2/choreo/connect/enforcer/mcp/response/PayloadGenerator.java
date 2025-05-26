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

    public static String getInitializeResponse(Object id, String serverName, String serverVersion,
                                               String serverDescription, boolean toolListChangeNotified) {
        // Create the response object as specified in
        // https://modelcontextprotocol.io/specification/2025-03-26/basic/lifecycle#initialization
        McpResponse response = new McpResponse(id);
        JsonObject responseObject = gson.fromJson(gson.toJson(response), JsonObject.class);
        JsonObject result = new JsonObject();
        result.addProperty(McpConstants.PROTOCOL_VERSION_KEY, McpConstants.PROTOCOL_VERSION_2025_MARCH);

        JsonObject capabilities = new JsonObject();
        JsonObject toolCapabilities = new JsonObject();
        toolCapabilities.addProperty("listChanged", toolListChangeNotified);
        capabilities.add("tools", toolCapabilities);
        // Add empty objects for unsupported capabilities
        capabilities.add("resources", new JsonObject());
        capabilities.add("prompts", new JsonObject());
        capabilities.add("logging", new JsonObject());
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", serverName);
        serverInfo.addProperty("version", serverVersion);
        serverInfo.addProperty("description", serverDescription);
        result.add("serverInfo", serverInfo);

        responseObject.add(McpConstants.RESULT_KEY, result);
        return gson.toJson(responseObject);
    }

    public static String generateToolListPayload(Object id, List<ExtendedOperation> extendedOperations) {
        McpResponse response = new McpResponse(id);
        JsonObject responseObject = gson.fromJson(gson.toJson(response), JsonObject.class);
        JsonObject result = new JsonObject();
        JsonArray toolsArray = new JsonArray();
        for (ExtendedOperation extendedOperation : extendedOperations) {
            JsonObject toolObject = new JsonObject();
            toolObject.addProperty(McpConstants.TOOL_NAME_KEY, extendedOperation.getName());
            toolObject.addProperty(McpConstants.TOOL_DESC_KEY, extendedOperation.getDescription());
            String schema = extendedOperation.getSchema();
            if (schema != null) {
                JsonObject schemaObject = gson.fromJson(schema, JsonObject.class);
                toolObject.add("inputSchema", sanitizeInputSchema(schemaObject));
            }
            toolsArray.add(toolObject);
        }
        result.add("tools", toolsArray);
        responseObject.add(McpConstants.RESULT_KEY, result);

        return gson.toJson(responseObject);
    }

    private static JsonObject sanitizeInputSchema(JsonObject inputObject) {
        if (inputObject == null || inputObject.isEmpty()) {
            JsonObject emptyObject = new JsonObject();
            emptyObject.addProperty("type", "object");
            emptyObject.add("properties", new JsonObject());
            return emptyObject;
        }
        inputObject.remove("contentType");

        if (inputObject.has(McpConstants.REQUIRED_KEY)) {
            JsonArray requiredArray = inputObject.getAsJsonArray(McpConstants.REQUIRED_KEY);
            JsonArray sanitizedArray = new JsonArray();

            // remove the header, query, and path prefixes from the required fields
            for (JsonElement element : requiredArray) {
                String newRequiredField;
                String requiredField = element.getAsString();
                if (requiredField.contains("_")) {
                    newRequiredField = requiredField.split("_", 2)[1];
                } else {
                    newRequiredField = requiredField;
                }
                sanitizedArray.add(newRequiredField);
            }
            inputObject.add(McpConstants.REQUIRED_KEY, sanitizedArray);
        }

        // remove the header, query, and path prefixes from the properties keys
        JsonObject propertiesObject = inputObject.getAsJsonObject(McpConstants.PROPERTIES_KEY);
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
        inputObject.add(McpConstants.PROPERTIES_KEY, sanitizedPropertiesObject);
        return inputObject;
    }

    public static JsonObject generateTransformationRequestPayload(String toolName, String vHost, String args,
                                                                  ExtendedOperation extendedOperation,
                                                                  String authParam) {
        StringBuilder sb = new StringBuilder("https://");
        JsonObject payload = new JsonObject();
        payload.addProperty(McpConstants.PAYLOAD_TOOL_NAME, toolName);
        payload.addProperty(McpConstants.PAYLOAD_SCHEMA, extendedOperation.getSchema());

        JsonObject apiInfo = new JsonObject();
        String context = "/" + extendedOperation.getApiContext().split("/", 3)[2];
        context = context.substring(0, context.lastIndexOf('/'));
        apiInfo.addProperty(McpConstants.PAYLOAD_API_NAME, extendedOperation.getApiName());
        apiInfo.addProperty(McpConstants.PAYLOAD_CONTEXT, context);
        apiInfo.addProperty(McpConstants.PAYLOAD_VERSION, extendedOperation.getApiVersion());
        apiInfo.addProperty(McpConstants.PAYLOAD_PATH, extendedOperation.getApiTarget());
        apiInfo.addProperty(McpConstants.PAYLOAD_VERB, extendedOperation.getApiVerb());
        if (!authParam.isEmpty()) {
            apiInfo.addProperty(McpConstants.PAYLOAD_AUTH, authParam);
        }
        if ("localhost".equals(vHost)) {
            sb.append("router").append(":").append("9095");
        } else {
            sb.append(vHost);
        }
        apiInfo.addProperty(McpConstants.PAYLOAD_ENDPOINT, sb.toString());
        payload.add("api", apiInfo);

        payload.addProperty(McpConstants.ARGUMENTS_KEY, args);
        return payload;
    }

    public static String generateMcpResponsePayload(Object id, boolean isError, String body) {
        McpResponse response = new McpResponse(id);
        JsonObject responseObject = gson.fromJson(gson.toJson(response), JsonObject.class);

        JsonObject result = new JsonObject();
        result.addProperty("isError", isError);

        JsonArray content = new JsonArray();
        JsonObject contentObject = new JsonObject();
        contentObject.addProperty("type", "text");
        contentObject.addProperty("text", body);

        content.add(contentObject);
        result.add("content", content);
        responseObject.add(McpConstants.RESULT_KEY, result);

        return gson.toJson(responseObject);
    }

    public static String generatePingResponse(Object id) {
        return generateEmptyResult(id);
    }

    public static String generateResourceListResponse(Object id) {
        // Resources are not supported at the moment
        return generateEmptyResult(id);
    }

    public static String generateResourceTemplateListResponse(Object id) {
        // Resource templates are not supported at the moment
        return generateEmptyResult(id);
    }

    public static String generatePromptListResponse(Object id) {
        // Prompts are not supported at the moment
        return generateEmptyResult(id);
    }

    private static String generateEmptyResult(Object id) {
        McpResponse response = new McpResponse(id);
        JsonObject responseObject = gson.fromJson(gson.toJson(response), JsonObject.class);
        JsonObject result = new JsonObject();
        responseObject.add(McpConstants.RESULT_KEY, result);

        return gson.toJson(responseObject);
    }


}
