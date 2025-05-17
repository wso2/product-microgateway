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

package org.wso2.choreo.connect.enforcer.mcp.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.commons.model.ExtendedOperation;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;
import org.wso2.choreo.connect.enforcer.mcp.McpException;
import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;


/**
 * This class is used to process the MCP requests.
 */
public class McpRequestProcessor {

    public static String processRequest(String apikey, String requestBody, String authParam) {
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
                return handleMcpToolsCall(id, matchedMcpApi, requestObject, authParam);
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
                if (idElement == null || idElement.isJsonNull()) {
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
            if (params.has("name")) {
                JsonElement toolNameElement = params.get("name");
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

    private static String handleMcpToolsCall(String id, API matchedApi, JsonObject jsonObject, String authParam)
            throws McpException {
        String toolName = jsonObject.getAsJsonObject(McpConstants.PARAMS_KEY)
                .get("name").getAsString();
        ExtendedOperation extendedOperation = matchedApi.getAPIConfig().getExtendedOperations()
                .stream()
                .filter(operation -> operation.getName().equals(toolName))
                .findFirst()
                .orElse(null);
        String args;
        String vHost = matchedApi.getAPIConfig().getVhost();
        JsonObject params = jsonObject.getAsJsonObject(McpConstants.PARAMS_KEY);
        if (params.has(McpConstants.ARGUMENTS_KEY)) {
            args = params.get(McpConstants.ARGUMENTS_KEY).toString();
        } else {
            args = "{}";
        }
        JsonObject payload = PayloadGenerator
                .generateTransformationRequestPayload(toolName, vHost, args, extendedOperation, authParam);

        try {
            EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
            String endpoint = enforcerConfig.getMcpConfig().getServerUrl() + "/mcp";
            URL url = new URL(endpoint);
            CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol());
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 500) {
                return PayloadGenerator.generateMcpResponsePayload(id, true,
                        "Error while processing the request");
            } else if (response.getStatusLine().getStatusCode() == 404) {
                return PayloadGenerator.generateMcpResponsePayload(id, true,
                        "Error occurred while accessing the requested service");
            } else if (response.getStatusLine().getStatusCode() >= 400) {
                return PayloadGenerator.generateMcpResponsePayload(id, true,
                        "Authentication error while processing the request");
            } else {
                HttpEntity entity = response.getEntity();
                try (InputStream inputStream = entity.getContent()) {
                    String output = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    JsonElement element = JsonParser.parseString(output);
                    String resString;
                    if (element.isJsonObject()) {
                        JsonObject obj = element.getAsJsonObject();
                        resString = obj.toString();
                    } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                        String unescaped = element.getAsString();
                        JsonObject obj = JsonParser.parseString(unescaped).getAsJsonObject();
                        resString = obj.toString();
                    } else {
                        resString = element.toString();
                    }
                    return PayloadGenerator.generateMcpResponsePayload(id, false, resString);
                }
            }
        } catch (Exception e) {
            throw new McpException(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                    McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE, "Error while processing the service call");
        }
    }
}
