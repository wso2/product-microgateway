/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.micro.gateway.core.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * This class is for validating request/response payload against schema.
 */
public class Validate {
    private static final Log logger = LogFactory.getLog("ballerina");
    private static JsonNode rootNode;
    private static String swaggerObject;
    private static Map<String, String> swaggers = new HashMap<>();

    /**
     * Validate request message.
     *
     * @param requestPath API request resource path
     * @param reqMethod   API request method
     * @param payload     Request payload
     * @return Status of the validation
     */
    public static String validateRequest(String requestPath, String reqMethod, String payload, String serviceName)
            throws IOException {
        String swagger = swaggers.get(serviceName);
        if ("get".equals(reqMethod) || "GET".equals(reqMethod)) {
            return Constants.VALIDATED_STATUS;
        }
        String schema = extractSchemaFromRequest(requestPath, reqMethod, swagger);
        if (schema != null && !Constants.EMPTY_ARRAY.equals(schema)) {
            return validateContent(payload, schema);
        } else  {
            return Constants.VALIDATED_STATUS;
        }
    }

    /***
     * Validate response message.
     * @param resourcePath request resource path
     * @param reqMethod request method
     * @param responseCode response message code
     * @param response response payload
     * @return Status of the validation result
     */
    public static String validateResponse(String resourcePath, String reqMethod, String responseCode, String response,
                                          String serviceName) {
        String swagger = swaggers.get(serviceName);
        String responseSchema = extractResponse(resourcePath, reqMethod, responseCode, swagger);
        if (responseSchema != null && !Constants.EMPTY_ARRAY.equals(responseSchema)) {
            return validateContent(response, responseSchema);
        } else {
            return Constants.VALIDATED_STATUS;
        }
    }

    /***
     * Extract resource artifacts from the jar file
     * @param projectName project name.
     * @param serviceName ballerina service name
     * @throws IOException
     */
    public static void extractResources(String projectName, String serviceName) throws IOException {
        String path = "resources/wso2/" + projectName + "/";
        CodeSource src = Validate.class.getProtectionDomain().getCodeSource();
        StringBuffer stringBuffer;
        if (src != null) {
            URL jar = src.getLocation();
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            while (true) {
                ZipEntry e = zip.getNextEntry();
                if (e == null) {
                    break;
                }
                String name = e.getName();
                if (name.startsWith(path)) {
                    InputStream in = Validate.class.getResourceAsStream("/" + name);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    stringBuffer = new StringBuffer();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line).append("\n");
                    }
                    swaggers.put(serviceName, stringBuffer.toString());
                }
            }
        }
    }

    private static String extractSchemaFromRequest(String resourcePath, String requestMethod, String swagger)
            throws IOException {
        String schema;
        ObjectMapper objectMapper = new ObjectMapper();
        rootNode = objectMapper.readTree(swagger.getBytes());
        swaggerObject = swagger;
        String value = JsonPath.read(swagger, Constants.JSON_PATH +
                Constants.OPEN_API).toString();
        if (value != null && !value.equals(Constants.EMPTY_ARRAY)) {
            //refer schema
            StringBuilder jsonPath = new StringBuilder();
            jsonPath.append(Constants.PATHS)
                    .append(resourcePath).append(Constants.JSONPATH_SEPARATE)
                    .append(requestMethod.toLowerCase())
                    .append(Constants.BODY_CONTENT);
            schema = JsonPath.read(swagger, jsonPath.toString()).toString();
            if (schema == null || Constants.EMPTY_ARRAY.equals(schema)) {
                // refer request bodies
                StringBuilder requestBodyPath = new StringBuilder();
                requestBodyPath.append(Constants.PATHS).append(resourcePath).
                        append(Constants.JSONPATH_SEPARATE).
                        append(requestMethod.toLowerCase()).append(Constants.REQUEST_BODY);
                schema = JsonPath.read(swagger, requestBodyPath.toString()).toString();
            }
        } else {
            StringBuilder schemaPath = new StringBuilder();
            schemaPath.append(Constants.PATHS).append(resourcePath).
                    append(Constants.JSONPATH_SEPARATE)
                    .append(requestMethod.toLowerCase()).append(Constants.PARAM_SCHEMA);
            schema = JsonPath.read(swagger, schemaPath.toString()).toString();
        }
        return extractReference(schema);
    }

    /**
     * Extract the reference.
     *
     * @param schemaNode Schema node to be extracted
     * @return extracted schema
     */
    private static String extractReference(String schemaNode) {
        String schemaContent = null;
        String[] val = schemaNode.split("" + Constants.HASH);
        String path = val[1].replaceAll("\"|}|]|\\\\", "");
        String searchLastIndex = null;
        if (StringUtils.isNotEmpty(path)) {
            int index = path.lastIndexOf(Constants.FORWARD_SLASH);
            searchLastIndex = path.substring(index + 1);
        }

        String nodeVal = path.replaceAll("" + Constants.FORWARD_SLASH, ".");
        String name = null;
        Object object = JsonPath.read(swaggerObject, Constants.JSON_PATH + nodeVal);
        String value;
        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonSchema = mapper.convertValue(object, JsonNode.class);
        if (jsonSchema.get(0) != null) {
            value = jsonSchema.get(0).toString();
        } else {
            value = jsonSchema.toString();
        }
        if (value.contains(Constants.SCHEMA_REFERENCE) &&
                !nodeVal.contains(Constants.DEFINITIONS)) {
            if (nodeVal.contains(Constants.REQUESTBODIES)) {
                StringBuilder extractRefPath = new StringBuilder();
                extractRefPath.append(Constants.JSON_PATH).append(Constants.REQUESTBODY_SCHEMA).
                        append(searchLastIndex).append(Constants.JSON_SCHEMA);
                String res = JsonPath.read(swaggerObject, extractRefPath.toString()).toString();
                if (res.contains(Constants.ITEMS)) {
                    StringBuilder requestSchemaPath = new StringBuilder();
                    requestSchemaPath.append(Constants.JSON_PATH).
                            append(Constants.REQUESTBODY_SCHEMA).append(
                            searchLastIndex).append(Constants.JSON_SCHEMA).
                            append(Constants.JSONPATH_SEPARATE).append(Constants.ITEMS).
                            append(Constants.JSONPATH_SEPARATE).append(Constants.SCHEMA_REFERENCE);
                    name = JsonPath.read(swaggerObject, requestSchemaPath.toString()).toString();
                    extractReference(name);
                } else {
                    StringBuilder jsonSchemaRef = new StringBuilder();
                    jsonSchemaRef.append(Constants.JSON_PATH).append(
                            Constants.REQUESTBODY_SCHEMA).append(searchLastIndex).append(
                            Constants.CONTENT).append(Constants.JSON_CONTENT);
                    name = JsonPath.read(swaggerObject, jsonSchemaRef.toString()).toString();
                    if (name.contains(Constants.COMPONENT_SCHEMA)) {
                        Object componentSchema = JsonPath.read(swaggerObject,
                                Constants.JSONPATH_SCHEMAS + searchLastIndex);
                        mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.convertValue(componentSchema, JsonNode.class);
                        generateSchema(jsonNode);
                        if (jsonNode.get(0) != null) {
                            name = jsonNode.get(0).toString();
                        } else {
                            name = jsonNode.toString();
                        }
                        schemaContent = name;
                    } else {
                        extractReference(name);
                    }
                }
            } else if (nodeVal.contains(Constants.SCHEMA)) {
                Object componentSchema = JsonPath.read(swaggerObject,
                        Constants.JSONPATH_SCHEMAS + searchLastIndex);
                mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.convertValue(componentSchema, JsonNode.class);
                generateSchema(jsonNode);
                if (jsonNode.get(0) != null) {
                    name = jsonNode.get(0).toString();
                } else {
                    name = jsonNode.toString();
                }
                schemaContent = name;
            }
        } else if (nodeVal.contains(Constants.DEFINITIONS)) {
            StringBuilder requestSchemaPath = new StringBuilder();
            requestSchemaPath.append(Constants.JSON_PATH).
                    append(Constants.DEFINITIONS).append(Constants.JSONPATH_SEPARATE
            ).append(searchLastIndex);
            Object nameObj = JsonPath.read(swaggerObject, requestSchemaPath.toString());
            mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.convertValue(nameObj, JsonNode.class);
            generateSchema(jsonNode);
            if (jsonNode.get(0) != null) {
                name = jsonNode.get(0).toString();
            } else {
                name = jsonNode.toString();
            }
            schemaContent = name;
        } else {
            schemaContent = value;
            return schemaContent;
        }
        return schemaContent;
    }

    /**
     * Replace $ref references with relevant schemas and recreate the swagger definition.
     *
     * @param parent Swagger definition parent Node
     */
    private static void generateSchema(JsonNode parent) {
        JsonNode schemaProperty;
        Iterator<Map.Entry<String, JsonNode>> schemaNode;
        if (parent.get(0) != null) {
            schemaNode = parent.get(0).fields();
        } else {
            schemaNode = parent.fields();
        }
        while (schemaNode.hasNext()) {
            Map.Entry<String, JsonNode> entry = schemaNode.next();
            if (entry.getValue().has(Constants.SCHEMA_REFERENCE)) {
                JsonNode refNode = entry.getValue();
                Iterator<Map.Entry<String, JsonNode>> refItems = refNode.fields();
                while (refItems.hasNext()) {
                    Map.Entry<String, JsonNode> entryRef = refItems.next();
                    if (entryRef.getKey().equals(Constants.SCHEMA_REFERENCE)) {
                        JsonNode schemaObject = extractSchemaObject(entryRef.getValue());
                        if (schemaObject != null) {
                            entry.setValue(schemaObject);
                        }
                    }
                }
            }
            schemaProperty = entry.getValue();
            if (JsonNodeType.OBJECT == schemaProperty.getNodeType()) {
                generateSchema(schemaProperty);
            }
            if (JsonNodeType.ARRAY == schemaProperty.getNodeType()) {
                generateArraySchemas(entry);
            }
        }
    }

    /**
     * Extract the schema Object.
     *
     * @param refNode JSON node to be extracted
     * @return Extracted schema
     */
    private static JsonNode extractSchemaObject(JsonNode refNode) {
        String[] val = refNode.toString().split("" + Constants.HASH);
        String path = val[1].replace("\\{^\"|\"}", Constants.EMPTY).replace
                ("\"", Constants.EMPTY).replace("}", Constants.EMPTY)
                .replaceAll(Constants.BACKWARD_SLASH, Constants.EMPTY);
        return rootNode.at(path);
    }

    /**
     * Replace $ref array elements.
     *
     * @param entry Array reference to be replaced from actual value.
     */
    private static void generateArraySchemas(Map.Entry<String, JsonNode> entry) {
        JsonNode entryRef;
        JsonNode ref;
        JsonNode schemaProperty;
        if (entry.getValue() != null) {
            schemaProperty = entry.getValue();
            if (schemaProperty == null) {
                return;
            }
            Iterator<JsonNode> arrayElements = schemaProperty.elements();
            List<JsonNode> nodeList = Lists.newArrayList(arrayElements);
            for (int i = 0; i < nodeList.size(); i++) {
                entryRef = nodeList.get(i);
                if (entryRef.has(Constants.SCHEMA_REFERENCE)) {
                    ref = extractSchemaObject(entryRef);
                    nodeList.remove(i);
                    nodeList.add(ref);
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode array = mapper.valueToTree(nodeList);
            entry.setValue(array);
        }
    }

    /**
     * Validate the Request/response content.
     *
     * @param payload      Request/response payload
     * @param schemaString Schema which uses to validate request/response messages
     * @return Returns "validated" or everit error logs
     */
    private static String validateContent(String payload, String schemaString) {

        StringBuilder finalMessage = new StringBuilder();
        List<String> errorMessages;
        JSONObject jsonSchema = new JSONObject(schemaString);
        JSONObject payloadObject = null;

        //if payload is not a valid json string
        try {
            payloadObject = new JSONObject(payload);
        } catch (JSONException e) {
            try {
                new JSONArray(payload);
                logger.warn("Request/Response validation is not applied for JSON Arrays. payload : " + payload);
                return Constants.VALIDATED_STATUS;
            } catch (JSONException e1) {
                finalMessage.append("Provided payload is not a valid json. " + e.getMessage());
                return finalMessage.toString();
            }
        }

        Schema schema = SchemaLoader.load(jsonSchema);
        if (schema == null) {
            return null;
        }
        try {
            schema.validate(payloadObject);
            return Constants.VALIDATED_STATUS;
        } catch (ValidationException e) {
            errorMessages = e.getAllMessages();
            for (String message : errorMessages) {
                finalMessage.append(message).append(", ");
            }
            return finalMessage.toString();
        }
    }

    /**
     * Extract the response schema from swagger according to the response code.
     *
     * @return response schema
     */
    private static String extractResponse(String reqPath, String reqMethod, String responseCode, String swagger) {
        Object resourceSchema;
        Object resource;
        Object content = null;
        Object schemaCon = null;
        ObjectMapper mapper = new ObjectMapper();
        String name;
        Object schema;
        String value;

        StringBuilder responseSchemaPath = new StringBuilder();
        responseSchemaPath.append(Constants.PATHS).append(reqPath).
                append(Constants.JSONPATH_SEPARATE).append(reqMethod.toLowerCase()).
                append(Constants.JSON_RESPONSES).append(responseCode);
        resource = JsonPath.read(swagger, responseSchemaPath.toString());
        swaggerObject = swagger;
        try {
            rootNode = mapper.readTree(swagger.getBytes());
        } catch (IOException e) {
            logger.error("Error occurred while reading the swagger.", e);
        }
        if (resource != null) {
            responseSchemaPath.append(Constants.CONTENT);
            content = JsonPath.read(swagger, responseSchemaPath.toString());
        }
        if (content != null) {
            responseSchemaPath.append(Constants.JSON_CONTENT);
            schemaCon = JsonPath.read(swagger, responseSchemaPath.toString());
        }
        if (schemaCon != null) {
            if (!schemaCon.toString().equals(Constants.EMPTY_ARRAY)) {
                return extractReference(schemaCon.toString());
            } else {
                StringBuilder pathBuilder = new StringBuilder();
                pathBuilder.append(Constants.PATHS).append(reqPath).
                        append(Constants.JSONPATH_SEPARATE).append(reqMethod.toLowerCase()).
                        append(Constants.JSON_RESPONSES).
                        append(responseCode).append(Constants.JSON_SCHEMA);

                schema = JsonPath.read(swagger, pathBuilder.toString()).toString();
                JsonNode jsonNode = mapper.convertValue(schema, JsonNode.class);
                if (jsonNode.get(0) != null) {
                    value = jsonNode.get(0).toString();
                } else {
                    value = jsonNode.toString();
                }
                if (value.contains(Constants.ITEMS)) {
                    StringBuilder requestSchemaPath = new StringBuilder();
                    requestSchemaPath.append(Constants.PATHS).append(reqPath).
                            append(Constants.JSONPATH_SEPARATE).append(reqMethod.toLowerCase()).
                            append(Constants.JSON_RESPONSES).append(responseCode).
                            append(Constants.JSON_SCHEMA).append(
                            Constants.JSONPATH_SEPARATE).append(Constants.ITEMS);
                    name = JsonPath.read(swagger, requestSchemaPath.toString()).toString();
                    if (name.contains(Constants.SCHEMA_REFERENCE)) {
                        requestSchemaPath.append(Constants.JSONPATH_SEPARATE).
                                append(Constants.SCHEMA_REFERENCE);
                        return extractReference(name);
                    }
                    return value;
                }
            }
        }
        StringBuilder resPath = new StringBuilder();
        resPath.append(Constants.PATHS).append(reqPath).append(
                Constants.JSONPATH_SEPARATE).append(reqMethod.toLowerCase()).
                append(Constants.JSON_RESPONSES).append(responseCode).append
                (Constants.SCHEMA);
        resource = JsonPath.read(swagger, resPath.toString());
        JsonNode json = mapper.convertValue(resource, JsonNode.class);
        if (json.get(0) != null && !Constants.EMPTY_ARRAY.equals(json.get(0))) {
            value = json.get(0).toString();
        } else {
            value = json.toString();
        }
        if (value != null && !Constants.EMPTY_ARRAY.equals(value)) {
            if (value.contains(Constants.SCHEMA_REFERENCE)) {
                byte[] bytes = value.getBytes();
                try {
                    JsonNode node = mapper.readTree(bytes);
                    Iterator<JsonNode> schemaNode = node.findParent(
                            Constants.SCHEMA_REFERENCE).elements();
                    JsonNode nodeNext = schemaNode.next();
                    if (nodeNext != null) {
                        return extractReference(nodeNext.toString());
                    }
                } catch (IOException e) {
                    logger.error("Error occurred while converting bytes from json node");
                }
            } else {
                return value;
            }
        } else {
            StringBuilder responseDefaultPath = new StringBuilder();
            responseDefaultPath.append(Constants.PATHS).append(reqPath).
                    append(Constants.JSONPATH_SEPARATE).append(reqMethod.toLowerCase()).
                    append(Constants.JSON_RESPONSES).append(Constants.DEFAULT);
            resourceSchema = JsonPath.read(swagger, responseDefaultPath.toString());
            JsonNode jnode = mapper.convertValue(resourceSchema, JsonNode.class);
            if (jnode.get(0) != null && !Constants.EMPTY_ARRAY.equals(jnode)) {
                value = jnode.get(0).toString();
            } else {
                value = jnode.toString();
            }
            if (resourceSchema != null) {
                if (value.contains(Constants.SCHEMA_REFERENCE)) {
                    byte[] bytes = value.getBytes();
                    try {
                        JsonNode node = mapper.readTree(bytes);
                        if (node != null) {
                            Iterator<JsonNode> schemaNode = node.findParent(
                                    Constants.SCHEMA_REFERENCE).elements();
                            return extractRef(schemaNode);
                        }
                    } catch (IOException e) {
                        logger.error("Error occurred while reading the schema.", e);
                    }
                } else {
                    return value;
                }
            } else {
                return value;
            }
        }
        return value;
    }

    /**
     * Get Schema path from $ref.
     *
     * @param schemaNode Swagger schema content
     * @return $ref path
     */
    private static String extractRef(Iterator<JsonNode> schemaNode) {
        while (schemaNode.hasNext()) {
            String nodeVal = schemaNode.next().toString();
            String[] val = nodeVal.split("" + Constants.HASH);
            if (val.length > 0) {
                String path = val[1].replaceAll("^\"|\"$", Constants.EMPTY);
                if (StringUtils.isNotEmpty(path)) {
                    int c = path.lastIndexOf(Constants.FORWARD_SLASH);
                    return path.substring(c + 1);
                }
            }
            return null;
        }
        return null;
    }
}
