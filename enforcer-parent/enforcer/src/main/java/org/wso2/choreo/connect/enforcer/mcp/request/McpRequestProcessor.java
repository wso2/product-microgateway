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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.commons.model.ExtendedOperation;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;
import org.wso2.choreo.connect.enforcer.mcp.McpException;
import org.wso2.choreo.connect.enforcer.mcp.McpExceptionWithId;
import org.wso2.choreo.connect.enforcer.mcp.response.McpResponseDto;
import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;


/**
 * This class is used to process the MCP requests.
 */
public class McpRequestProcessor {
    private static final Logger logger = LogManager.getLogger(McpRequestProcessor.class);

    public static McpResponseDto processRequest(API matchedMcpApi, String requestBody,
                                                Map<String, String> additionalHeaders, String protocolVersion) {
        try {
            validateRequest(requestBody);
            JsonObject requestObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String method = requestObject.get(McpConstants.RpcConstants.METHOD).getAsString();
            // Get the MCP subtype through operations
            String mcpSubType = "";
            String mcpServerUrl = "";
            for (ExtendedOperation exOps : matchedMcpApi.getAPIConfig().getExtendedOperations()) {
                mcpSubType = exOps.getMode();
                mcpServerUrl = exOps.getBackendEndpoint();
                break;
            }
            if (McpConstants.SubTypeConstants.THIRD_PARTY_SERVER.equals(mcpSubType)) {
                return processThirdPartyRequest(matchedMcpApi, mcpServerUrl, requestObject, method, additionalHeaders);
            } else {
                return processInternalRequest(matchedMcpApi, requestObject, method, additionalHeaders, protocolVersion);
            }
        } catch (McpException e) {
            return new McpResponseDto(e.toJsonRpcErrorPayload(), 200, null);
        }
    }

    /**
     * Processes the MCP requests for existing APIs and direct backends.
     *
     * @param matchedMcpApi     matched API in the gateway
     * @param requestObject     MCP request payload
     * @param method            MCP JSON RPC method
     * @param additionalHeaders additional headers to send
     * @return the response payload as a String
     */
    public static McpResponseDto processInternalRequest(API matchedMcpApi, JsonObject requestObject, String method,
                                                        Map<String, String> additionalHeaders, String protocolVersion) {
        try {
            Object id = -1;
            if (!method.contains("notifications/")) {
                id = requestObject.get(McpConstants.RpcConstants.ID);
            }
            if (!McpConstants.METHOD_INITIALIZE.equals(method)) {
                validateProtocolVersion(id, protocolVersion);
            }
            if (McpConstants.METHOD_INITIALIZE.equals(method)) {
                String negotiatedProtocolVersion = validateInitializeRequest(id, requestObject);
                return handleMcpInitialize(id, matchedMcpApi, negotiatedProtocolVersion);
            } else if (McpConstants.METHOD_TOOL_LIST.equals(method)) {
                return handleMcpToolList(id, matchedMcpApi, false);
            } else if (McpConstants.METHOD_TOOL_CALL.equals(method)) {
                validateToolsCallRequest(requestObject, matchedMcpApi);
                return handleMcpToolsCall(id, matchedMcpApi, requestObject, additionalHeaders);
            } else if (McpConstants.METHOD_PING.equals(method)) {
                return new McpResponseDto(
                        PayloadGenerator.generatePingResponse(id), 200, null);
            } else if (McpConstants.METHOD_RESOURCES_LIST.equals(method)) {
                return new McpResponseDto(PayloadGenerator.generateResourceListResponse(id), 200, null);
            } else if (McpConstants.METHOD_RESOURCE_TEMPLATE_LIST.equals(method)) {
                return new McpResponseDto(
                        PayloadGenerator.generateResourceTemplateListResponse(id), 200, null);
            } else if (McpConstants.METHOD_PROMPTS_LIST.equals(method)) {
                return new McpResponseDto(PayloadGenerator.generatePromptListResponse(id), 200, null);
            } else if (McpConstants.METHOD_NOTIFICATION_INITIALIZED.equals(method)) {
                // We don't need to send a reply when it's a notification
                return null;
            } else {
                throw new McpException(McpConstants.RpcConstants.METHOD_NOT_FOUND_CODE,
                        McpConstants.RpcConstants.METHOD_NOT_FOUND_MESSAGE, "Method not found");
            }
        } catch (McpException e) {
            return new McpResponseDto(e.toJsonRpcErrorPayload(), e.getStatusCode(), null);
        }
    }

    /**
     * Processes the MCP requests for third-party MCP Servers.
     *
     * @param matchedMcpApi     matched API in the gateway
     * @param requestObject     MCP request payload
     * @param method            MCP JSON RPC method
     * @param additionalHeaders additional headers to send
     * @return the response payload as a String
     */
    private static McpResponseDto processThirdPartyRequest(API matchedMcpApi, String mcpServerUrl,
                                                           JsonObject requestObject, String method,
                                                           Map<String, String> additionalHeaders) {
        try {
            Object id = -1;
            if (!method.contains("notifications/")) {
                id = requestObject.get(McpConstants.RpcConstants.ID);
            }
            if (McpConstants.METHOD_TOOL_LIST.equals(method)) {
                return handleMcpToolList(id, matchedMcpApi, true);
            } else if (McpConstants.METHOD_TOOL_CALL.equals(method)) {
                // The validation is done to make sure the called tool is actually exposed through the gateway
                validateToolsCallRequest(requestObject, matchedMcpApi);
                // Process the tool call payload to  replace the tool name with actual tool name
                processAndSwitchToolNames(requestObject, matchedMcpApi);
            }
            return handleThirdPartyMethodCall(id, mcpServerUrl, requestObject, additionalHeaders);
        } catch (McpException e) {
            return new McpResponseDto(e.toJsonRpcErrorPayload(), 400, null);
        }
    }

    public static String processWellKnownRequest(String organization) {
        String endpoint = McpConstants.ASGARDEO_WK_PLACEHOLDER.replace("{organization}", organization);
        try {
            URL url = new URL(endpoint);
            CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol());
            HttpGet httpGet = new HttpGet(endpoint);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                try (InputStream inputStream = entity.getContent()) {
                    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
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
                    throwMissingJsonRpcError();
                }
                if (!McpConstants.RpcConstants.JSON_RPC_VERSION.equals(jsonRpcElement.getAsString())) {
                    throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Invalid JSON-RPC version");
                }
            } else {
                throwMissingJsonRpcError();
            }
            String method = "";
            if (jsonObject.has(McpConstants.RpcConstants.METHOD)) {
                JsonElement methodElement = jsonObject.get(McpConstants.RpcConstants.METHOD);
                if (methodElement == null || methodElement.isJsonNull()) {
                    throwMissingMethodError();
                }
                method = methodElement.getAsString();
                if (method.isEmpty()) {
                    throwMissingMethodError();
                }
            } else {
                throwMissingMethodError();
            }
            if (jsonObject.has(McpConstants.RpcConstants.ID)) {
                JsonElement idElement = jsonObject.get(McpConstants.RpcConstants.ID);
                if (idElement == null || idElement.isJsonNull() || idElement.getAsString().isEmpty()) {
                    throwMissingIdError();
                }
            } else if (!McpConstants.METHOD_NOTIFICATION_INITIALIZED.equals(method)) {
                throwMissingIdError();
            }
        } else {
            throw new McpException(McpConstants.RpcConstants.PARSE_ERROR_CODE,
                    McpConstants.RpcConstants.PARSE_ERROR_MESSAGE, "Invalid JSON format");
        }
    }

    public static String validateInitializeRequest(Object id, JsonObject requestObject) throws McpException {
        if (requestObject.has(McpConstants.PARAMS_KEY)) {
            JsonObject params = requestObject.getAsJsonObject(McpConstants.PARAMS_KEY);
            if (params.has(McpConstants.PROTOCOL_VERSION_KEY)) {
                String protocolVersion = params.get(McpConstants.PROTOCOL_VERSION_KEY).getAsString();
                if (!McpConstants.SUPPORTED_PROTOCOL_VERSIONS.contains(protocolVersion)) {
                    return McpConstants.DEFAULT_NEGOTIATED_PROTOCOL_VERSION;
                } else {
                    return protocolVersion;
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

    /**
     * Validates the protocol version against the supported versions.
     *
     * @param id              the ID of the request
     * @param protocolVersion the protocol version to validate
     * @throws McpException if the protocol version is not supported
     */
    public static void validateProtocolVersion(Object id, String protocolVersion) throws McpException {
        if (!McpConstants.SUPPORTED_PROTOCOL_VERSIONS.contains(protocolVersion)) {
            throw new McpExceptionWithId(id, McpConstants.RpcConstants.INVALID_PARAMS_CODE,
                    McpConstants.PROTOCOL_MISMATCH_ERROR,
                    PayloadGenerator.getInitializeErrorBody(protocolVersion), 400);
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

    /**
     * This method will be used to match the tool name in the gateway to the actual tool name in the MCP Server
     *
     * @param jsonObject the JSON object containing the MCP tool call
     * @param matchedApi the matched API in the gateway
     * @throws McpException if the tool name does not match any of the extended operations
     */
    private static void processAndSwitchToolNames(JsonObject jsonObject, API matchedApi) throws McpException {
        JsonObject params = jsonObject.getAsJsonObject(McpConstants.PARAMS_KEY);
        String toolName = params.get(McpConstants.TOOL_NAME_KEY).getAsString();
        ExtendedOperation extendedOperation = matchedApi.getAPIConfig().getExtendedOperations()
                .stream()
                .filter(operation -> toolName.equals(operation.getName()))
                .findFirst()
                .orElse(null);
        if (extendedOperation != null) {
            // Replace the tool name with the actual tool name
            params.addProperty(McpConstants.TOOL_NAME_KEY, extendedOperation.getBackendTarget());
        } else {
            throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                    McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "The requested tool does not exist");
        }
        jsonObject.add(McpConstants.PARAMS_KEY, params);
    }

    private static McpResponseDto handleMcpInitialize(Object id, API matchedApi, String protocolVersion) {
        String name = matchedApi.getAPIConfig().getName();
        String version = matchedApi.getAPIConfig().getVersion();
        String description = "This is an MCP Server";
        return new McpResponseDto(PayloadGenerator
                .getInitializeResponse(id, name, version, description, false, protocolVersion),
                200, null);

    }

    private static McpResponseDto handleMcpToolList(Object id, API matchedApi, boolean isThirdParty) {
        return new McpResponseDto(
                PayloadGenerator.generateToolListPayload(id, matchedApi.getAPIConfig().getExtendedOperations(),
                        isThirdParty), 200, null);
    }

    private static McpResponseDto handleMcpToolsCall(Object id, API matchedApi, JsonObject jsonObject,
                                                     Map<String, String> additionalHeaders)
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
                .generateTransformationRequestPayload(toolName, vHost, args, extendedOperation, additionalHeaders);

        try {
            EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
            String endpoint = enforcerConfig.getMcpConfig().getServerUrl() + "/mcp";
            URL url = new URL(endpoint);
            CloseableHttpClient httpClient = McpHttpClient.getInstance();
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return new McpResponseDto(
                            PayloadGenerator.generateMcpResponsePayload(id, true,
                                    "Empty response received from the service."), 200, null);
                }
                String resString;
                int code;
                try (InputStream inputStream = entity.getContent()) {
                    String output = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    try {
                        JsonElement element = JsonParser.parseString(output);
                        if (element.isJsonObject()) {
                            JsonObject result = element.getAsJsonObject();
                            code = result.get("code").getAsInt();
                            String resp = result.get("response").getAsString();
                            try {
                                JsonElement respElement = JsonParser.parseString(resp);
                                if (respElement.isJsonObject()) {
                                    JsonObject obj = respElement.getAsJsonObject();
                                    resString = obj.toString();
                                } else if (respElement.isJsonPrimitive()
                                        && respElement.getAsJsonPrimitive().isString()) {
                                    String unescaped = respElement.getAsString();
                                    JsonObject obj = JsonParser.parseString(unescaped).getAsJsonObject();
                                    resString = obj.toString();
                                } else {
                                    resString = respElement.toString();
                                }
                            } catch (JsonSyntaxException e) {
                                resString = resp;
                            }
                        } else {
                            return new McpResponseDto(
                                    PayloadGenerator.generateMcpResponsePayload(id, true,
                                            "Error while processing the results"), 200, null);
                        }
                    } catch (JsonSyntaxException e) {
                        logger.error("Unexpected response when processing the service call", e);
                        return new McpResponseDto(
                                PayloadGenerator.generateMcpResponsePayload(id, true,
                                        "Unexpected response when processing the service call"),
                                200, null);
                    }
                }
                if (response.getStatusLine().getStatusCode() == 200) {
                    return new McpResponseDto(PayloadGenerator.generateMcpResponsePayload(id, false, resString),
                            200, null);
                } else {
                    if (response.getStatusLine().getStatusCode() == 400) {
                        return new McpResponseDto(PayloadGenerator.generateMcpResponsePayload(id, true,
                                "Error while processing the request in gateway"), 200, null);
                    } else if (resString.contains("connection refused")) {
                        return new McpResponseDto(PayloadGenerator.generateMcpResponsePayload(id, true,
                                "Underlying service is unreachable"), 200, null);
                    } else {
                        return new McpResponseDto(PayloadGenerator
                                .generateMcpResponsePayload(id, true, resString), 200, null);
                    }
                }
            }
        } catch (ConnectionPoolTimeoutException e) {
            logger.error("Error while borrowing a connection", e);
            throw new McpException(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                    McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE,
                    "Too many requests. Server is unable to handle this request at the moment");
        } catch (Exception e) {
            logger.error("Error while processing the service call", e);
            throw new McpException(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                    McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE, "Error while processing the service call");
        }
    }

    private static McpResponseDto handleThirdPartyMethodCall(Object id, String mcpServerUrl, JsonObject requestObject,
                                                             Map<String, String> additionalHeaders)
            throws McpException {
        JsonObject payload = PayloadGenerator.generateThirdPartyRequestPayload(mcpServerUrl, requestObject,
                additionalHeaders);
        try {
            EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
            String serverUrl = enforcerConfig.getMcpConfig().getServerUrl();
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
            String endpoint = serverUrl + "/client";
            URL url = new URL(endpoint);
            CloseableHttpClient httpClient = McpHttpClient.getInstance();
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return new McpResponseDto(PayloadGenerator.generateMcpResponsePayload(id, true,
                            "Empty response received from the service."), 500, null);
                }
                String resString;
                String sessionId;
                int code;
                try (InputStream inputStream = entity.getContent()) {
                    String output = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    try {
                        JsonElement element = JsonParser.parseString(output);
                        if (element.isJsonObject()) {
                            JsonObject result = element.getAsJsonObject();
                            code = result.get("code").getAsInt();
                            sessionId = result.get("sessionId").getAsString();
                            boolean error = result.get("error").getAsBoolean();
                            if (error) {
                                return new McpResponseDto(
                                        PayloadGenerator.generateMcpResponsePayload(id, true,
                                                "Error while processing the request in gateway"),
                                        code, sessionId);
                            }
                            JsonElement mcpResponse = result.get("response");
                            if (mcpResponse.isJsonObject()) {
                                JsonObject obj = mcpResponse.getAsJsonObject();
                                resString = obj.toString();
                            } else if (mcpResponse.isJsonPrimitive()
                                    && mcpResponse.getAsJsonPrimitive().isString()) {
                                String unescaped = mcpResponse.getAsString();
                                JsonObject obj = JsonParser.parseString(unescaped).getAsJsonObject();
                                resString = obj.toString();
                            } else {
                                resString = mcpResponse.toString();
                            }
                        } else {
                            return new McpResponseDto(PayloadGenerator.generateMcpResponsePayload(id, true,
                                    "Error while processing the results"), 500, null);
                        }
                    } catch (JsonSyntaxException e) {
                        logger.error("Unexpected response when processing the service call", e);
                        return new McpResponseDto(PayloadGenerator.generateMcpResponsePayload(id, true,
                                "Unexpected response when processing the service call"), 500, null);
                    }
                }
                if (response.getStatusLine().getStatusCode() == 200) {
                    return new McpResponseDto(resString, code, sessionId);
                } else {
                    return new McpResponseDto(PayloadGenerator.generateMcpResponsePayload(id, true,
                            "Error while proxying the request through gateway"), 500, sessionId);
                }
            }
        } catch (ConnectionPoolTimeoutException e) {
            logger.error("Error while borrowing a connection", e);
            throw new McpException(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                    McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE,
                    "Too many requests. Server is unable to handle this request at the moment");
        } catch (Exception e) {
            logger.error("Error while processing the service call", e);
            throw new McpException(McpConstants.RpcConstants.INTERNAL_ERROR_CODE,
                    McpConstants.RpcConstants.INTERNAL_ERROR_MESSAGE, "Error while processing the service call");
        }
    }

    private static void throwMissingJsonRpcError() throws McpException {
        throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing jsonrpc field");
    }

    private static void throwMissingIdError() throws McpException {
        throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing id field");
    }

    private static void throwMissingMethodError() throws McpException {
        throw new McpException(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Missing method field");
    }
}
