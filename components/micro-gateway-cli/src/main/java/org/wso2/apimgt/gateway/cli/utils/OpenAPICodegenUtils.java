/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.config.BasicAuth;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.ResourceRepresentation;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;
import org.wso2.apimgt.gateway.cli.model.route.RouteEndpointConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAPICodegenUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(OpenAPICodegenUtils.class);

    private static final String openAPISpec2 = "2";
    private static final String openAPISpec3 = "3";
    private static final Map<String, String> basePathMap = new HashMap<>();
    private static Map<String, String> requestInterceptorMap = new HashMap<>();
    private static Map<String, String> responseInterceptorMap = new HashMap<>();
    private static Map<String, String> apiNameVersionMap = new HashMap<>();
    private static List<String> oauthSecuritySchemaList = new ArrayList<>();
    private static List<String> basicSecuritySchemaList = new ArrayList<>();

    enum APISecurity {
        basic,
        oauth2
    }

    /**
     * Generate API Id for a given OpenAPI definition.
     *
     * @param apiDefPath path to OpenAPI definition
     * @return API Id
     */
    public static String generateAPIdForSwagger(String apiDefPath) {

        //another purpose in here is to validate the openAPI definition
        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);

        String apiName = openAPI.getInfo().getTitle();
        String apiVersion = openAPI.getInfo().getVersion();

        return HashUtils.generateAPIId(apiName, apiVersion);
    }

    /**
     * Generate JsonNode object for a given API definition.
     *
     * @param apiDefinition API definition (as a file path or String content)
     * @param isFilePath    If the given api Definition is a file path
     * @return JsonNode object for the api definition
     */
    private static JsonNode generateJsonNode(String apiDefinition, boolean isFilePath) {
        try {
            if (isFilePath) {
                //if filepath to the swagger is provided
                return objectMapper.readTree(new File(apiDefinition));
            }
            //if the raw string of the swagger is provided
            return objectMapper.readTree(apiDefinition);
        } catch (IOException e) {
            throw new CLIRuntimeException("Api Definition cannot be parsed.");
        }
    }

    /**
     * Discover the openAPI version of the given API definition
     *
     * @param apiDefinition API definition (as a file path or String content)
     * @param isFilePath    If the given api Definition is a file path
     * @return openAPI version number (2 or 3)
     */
    private static String findSwaggerVersion(String apiDefinition, boolean isFilePath) {

        JsonNode rootNode = generateJsonNode(apiDefinition, isFilePath);
        if (rootNode.has("swagger") && rootNode.get("swagger").asText().trim().startsWith("2")) {
            return openAPISpec2;
        } else if (rootNode.has("openapi") && rootNode.get("openapi").asText().trim().startsWith("3")) {
            return openAPISpec3;
        }
        throw new CLIRuntimeException("Error while reading the swagger file, check again.");
    }

    /**
     * Extract the openAPI definition as String from an ExtendedAPI object.
     *
     * @param api ExtendedAPI object
     * @return openAPI definition as a String
     */
    static String generateSwaggerString(ExtendedAPI api) {

        String swaggerVersion = findSwaggerVersion(api.getApiDefinition(), false);
        switch (swaggerVersion) {
            case "2":
                Swagger swagger = new SwaggerParser().parse(api.getApiDefinition());
                //to save the basepath settings as provided in API Manager
                swagger.setBasePath(api.getContext() + "/" + api.getVersion());
                return Json.pretty(swagger);
            case "3":
                return api.getApiDefinition();
            default:
                throw new CLIRuntimeException("Error: Swagger version is not identified");
        }
    }

    /**
     * get API name and version from the given openAPI definition.
     *
     * @param apiDefPath path to openAPI definition
     * @return String array {API_name, Version}
     */
    static String[] getAPINameVersionFromSwagger(String apiDefPath) {
        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);
        return new String[]{openAPI.getInfo().getTitle(), openAPI.getInfo().getVersion()};

    }

    /**
     * get basePath from the openAPI definition
     *
     * @param apiDefPath path to openAPI definition
     * @return basePath (if the swagger version is 2 and it includes )
     */
    public static String getBasePathFromSwagger(String apiDefPath) {
        String swaggerVersion = findSwaggerVersion(apiDefPath, true);

        //openAPI version 2 contains basePath
        if (swaggerVersion.equals(openAPISpec2)) {
            Swagger swagger = new SwaggerParser().read(apiDefPath);
            if (!StringUtils.isEmpty(swagger.getBasePath())) {
                return swagger.getBasePath();
            }
        }
        return null;
    }

    /**
     * generate ExtendedAPI object from openAPI definition
     *
     * @param openAPI {@link OpenAPI} object
     * @return Extended API object
     */
    public static ExtendedAPI generateAPIFromOpenAPIDef(OpenAPI openAPI) {

        ExtendedAPI api;
        String apiId = UUID.randomUUID().toString();

        api = new ExtendedAPI();

        api.setId(apiId);
        api.setName(openAPI.getInfo().getTitle());
        api.setVersion(openAPI.getInfo().getVersion());
        api.setTransport(Arrays.asList("http", "https"));
        return api;
    }

    /**
     * list all the available resources from openAPI definition
     *
     * @param projectName project Name
     * @param apiId       api Id
     * @return list of string arrays {resource_id, resource name, method}
     */
    public static List<ResourceRepresentation> listResourcesFromSwaggerForAPI(String projectName, String apiId) {

        List<ResourceRepresentation> resourceList = new ArrayList<>();
        JsonNode openApiNode = generateJsonNode(GatewayCmdUtils.getProjectSwaggerFilePath(projectName, apiId),
                true);
        addResourcesToListFromSwagger(openApiNode, resourceList);
        return resourceList;
    }


    private static void addResourcesToList(List<ResourceRepresentation> resourcesList, String apiName,
                                           String apiVersion, String resourceName, String method) {
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setId(HashUtils.generateResourceId(apiName, apiVersion, resourceName, method));
        resource.setName(resourceName);
        resource.setMethod(method);
        resource.setApi(apiName);
        resource.setVersion(apiVersion);
        resourcesList.add(resource);
    }

    /**
     * Add resources from the provided openAPI definition to existing array list
     *
     * @param apiDefNode    Api Definition as a JsonNode
     * @param resourcesList String[] arrayList
     */
    private static void addResourcesToListFromSwagger(JsonNode apiDefNode, List<ResourceRepresentation> resourcesList) {

        String apiName = apiDefNode.get("info").get("title").asText();
        String apiVersion = apiDefNode.get("info").get("version").asText();

        apiDefNode.get("paths").fields().forEachRemaining(e -> e.getValue().fieldNames().forEachRemaining(operation ->
                addResourcesToList(resourcesList, apiName, apiVersion, e.getKey(), operation)));
    }

    /**
     * List all the resources available in the project
     *
     * @param projectName project Name
     * @return String[] Arraylist with all the available resources
     */
    public static List<ResourceRepresentation> getAllResources(String projectName) {

        List<ResourceRepresentation> resourcesList = new ArrayList<>();

        String projectAPIFilesPath = GatewayCmdUtils.getProjectAPIFilesDirectoryPath(projectName);
        try {
            Files.walk(Paths.get(projectAPIFilesPath)).filter(path -> path.getFileName().toString().equals("swagger.json"))
                    .forEach(path -> {
                        JsonNode openApiNode = generateJsonNode(path.toString(), true);
                        OpenAPICodegenUtils.addResourcesToListFromSwagger(openApiNode, resourcesList);
                    });
            return resourcesList;
        } catch (IOException e) {
            throw new CLIInternalException("Error while navigating API Files directory.");
        }

    }

    /**
     * get the resource related information if the resource_id is given
     *
     * @param projectName project name
     * @param resource_id resource id
     * @return resource object with api name, version, method and key
     */
    public static ResourceRepresentation getResource(String projectName, String resource_id) {
        String projectAPIFilesPath = GatewayCmdUtils.getProjectAPIFilesDirectoryPath(projectName);
        ResourceRepresentation resource = new ResourceRepresentation();
        try {
            Files.walk(Paths.get(projectAPIFilesPath)).filter(path -> path.getFileName().toString().equals("swagger.json"))
                    .forEach(path -> {
                        JsonNode openApiNode = generateJsonNode(path.toString(), true);
                        String apiName = openApiNode.get("info").get("title").asText();
                        String apiVersion = openApiNode.get("info").get("version").asText();

                        openApiNode.get("paths").fields().forEachRemaining(e -> e.getValue().fieldNames()
                                .forEachRemaining(operation -> {
                                    if (HashUtils.generateResourceId(apiName, apiVersion, e.getKey(), operation)
                                            .equals(resource_id)) {
                                        resource.setId(resource_id);
                                        resource.setName(e.getKey());
                                        resource.setMethod(operation);
                                        resource.setApi(apiName);
                                        resource.setVersion(apiVersion);
                                    }
                                }));
                    });
        } catch (IOException e) {
            throw new CLIInternalException("Error while navigating API Files directory.");
        }
        if (resource.getId() == null) {
            return null;
        }
        return resource;
    }

    /**
     * read openAPI definition
     *
     * @param filePath path to openAPI definition
     * @return openAPI as a String
     */
    public static String readJson(String filePath) {
        String responseStr;
        try {
            responseStr = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error while reading api definition.", e);
            throw new CLIInternalException("Error while reading api definition.");
        }
        return responseStr;
    }

    /**
     * set additional configurations for an api for code generation process (basePath and CORS configuration)
     *
     * @param api API object
     */
    public static void setAdditionalConfigsDevFirst(ExtendedAPI api) {
        String basePath = MgwDefinitionBuilder.getBasePath(api.getName(), api.getVersion());
        MgwEndpointConfigDTO mgwEndpointConfigDTO =
                RouteUtils.convertToMgwServiceMap(MgwDefinitionBuilder.getProdEndpointList(basePath),
                        MgwDefinitionBuilder.getSandEndpointList(basePath));
        api.setEndpointConfigRepresentation(mgwEndpointConfigDTO);
        // 0th element represents the specific basepath
        api.setSpecificBasepath(basePath);
        String security = MgwDefinitionBuilder.getSecurity(basePath);
        if (security == null) {
            security = "oauth2";
        }
        api.setMgwApiSecurity(security);
        api.setCorsConfiguration(MgwDefinitionBuilder.getCorsConfiguration(basePath));
        if(api.getCorsConfiguration() != null) {
            //Setting the value true here so user do not need to add the "corsConfigurationEnabled" property to the
            // definition.yaml
            api.getCorsConfiguration().setCorsConfigurationEnabled(true);
        }
    }

    public static void setAdditionalConfig(ExtendedAPI api) {
        RouteEndpointConfig endpointConfig = RouteUtils.parseEndpointConfig(api.getEndpointConfig(),
                api.getEndpointSecurity());
        api.setEndpointConfigRepresentation(RouteUtils.convertToMgwServiceMap(endpointConfig.getProdEndpointList(),
                endpointConfig.getSandboxEndpointList()));
        if (api.getIsDefaultVersion()) {
            api.setSpecificBasepath(api.getContext());
        } else {
            api.setSpecificBasepath(api.getContext() + "/" + api.getVersion());
        }
    }

    public static void setAdditionalConfigsDevFirst(ExtendedAPI api, OpenAPI openAPI, String openAPIFilePath) {

        EndpointListRouteDTO prodEndpointListDTO = objectMapper.convertValue(openAPI.getExtensions()
                .get(OpenAPIConstants.PRODUCTION_ENDPOINTS), EndpointListRouteDTO.class);
        EndpointListRouteDTO sandEndpointListDTO = objectMapper.convertValue(openAPI.getExtensions()
                .get(OpenAPIConstants.SANDBOX_ENDPOINTS), EndpointListRouteDTO.class);
        MgwEndpointConfigDTO mgwEndpointConfigDTO = RouteUtils.convertToMgwServiceMap(prodEndpointListDTO,
                sandEndpointListDTO);
        api.setEndpointConfigRepresentation(mgwEndpointConfigDTO);

        setMgwAPISecurityAndScopes(api, openAPI);
        api.setSpecificBasepath(openAPI.getExtensions().get(OpenAPIConstants.BASEPATH).toString());
        try {
            api.setCorsConfiguration(objectMapper.convertValue(openAPI.getExtensions().get(OpenAPIConstants.CORS),
                    APICorsConfigurationDTO.class));
        } catch (IllegalArgumentException e) {
            throw new CLIRuntimeException("'" + OpenAPIConstants.CORS + "' property is not properly set for the " +
                    "openAPI definition file. \n" + openAPIFilePath);
        }
    }

    /**
     * get resource Endpoint configuration in the format of {@link MgwEndpointConfigDTO} to match the mustache
     * template
     *
     * @param operation {@link Operation} object
     * @return {@link MgwEndpointConfigDTO} object
     */
    public static MgwEndpointConfigDTO getResourceEpConfigForCodegen(Operation operation) {
        EndpointListRouteDTO prodEndpointListDTO = objectMapper.convertValue(operation.getExtensions()
                .get(OpenAPIConstants.PRODUCTION_ENDPOINTS), EndpointListRouteDTO.class);
        EndpointListRouteDTO sandEndpointListDTO = objectMapper.convertValue(operation.getExtensions()
                .get(OpenAPIConstants.SANDBOX_ENDPOINTS), EndpointListRouteDTO.class);
        return RouteUtils.convertToMgwServiceMap(prodEndpointListDTO, sandEndpointListDTO);
    }

    /**
     * Validate basePath to avoid having the same basePath for two or more APIs
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openApiFilePath OpenAPI definition file
     */
    private static void validateBasepath(OpenAPI openAPI, String openApiFilePath) {
        String basePath = (String) openAPI.getExtensions().get(OpenAPIConstants.BASEPATH);
        if (basePath == null || basePath.isEmpty()) {
            throw new CLIRuntimeException("'" + OpenAPIConstants.BASEPATH + "' property is not included in openAPI " +
                    "definition '" + openApiFilePath + "'.");
        }
        basePath = basePath.startsWith("/") ? basePath : "/" + basePath;
        if (basePathMap.containsKey(basePath)) {
            throw new CLIRuntimeException("The valude for '" + OpenAPIConstants.BASEPATH + "' " + basePath +
                    " property is duplicated in the following openAPI definitions.\n" + basePathMap.get(basePath) +
                    "\n" + openApiFilePath);
        }
        basePathMap.put(basePath, openApiFilePath);
    }

    /**
     * Store all the interceptor functions included in interceptors directory.
     *
     * @throws IOException if an error occurred while reading the bal files inside interceptor directory
     */
    public static void setInterceptors(String projectName) throws IOException {
        String interceptorsDirectoryPath = GatewayCmdUtils.getProjectInterceptorsDirectoryPath(projectName);
        Files.walk(Paths.get(interceptorsDirectoryPath)).filter(path -> path.getFileName().toString()
                .endsWith(GatewayCliConstants.EXTENSION_BAL)).forEach(path -> {
            String balSrcCode = null;
            try {
                balSrcCode = GatewayCmdUtils.readFileAsString(path.toString(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            findRequestInterceptors(balSrcCode, path.toString());
            findResponseInterceptors(balSrcCode, path.toString());
        });
    }

    /**
     * Find and store the ballerina functions included in a ballerina source code.
     *
     * @param balSrcCode           the ballerina source code
     * @param interceptorFilePath  the file path of the ballerina source code
     * @param isRequestInterceptor true if request interceptors are required, false if response interceptors.
     * @param interceptorMap       interceptor map (request or response)
     */
    private static void findInterceptors(String balSrcCode, String interceptorFilePath, boolean isRequestInterceptor,
                                         Map<String, String> interceptorMap) {
        String functionParameter;
        //Set the function parameter of the ballerina code based on whether it is request or response
        if (isRequestInterceptor) {
            functionParameter = "http:Request";
        } else {
            functionParameter = "http:Response";
        }
        //Regular expression to identify the ballerina function
        //captures function xxx (http:Caller xxx, http:Request xxx)
        String functionRegex = "function\\s+\\w+\\s*\\(\\s*http:Caller\\s+\\w+\\s*,\\s*" + functionParameter +
                "\\s+\\w+\\s*\\)";
        //Regular expression to identify commented functions
        String commentFunctionRegex = "//( |\\S)*" + functionRegex;
        Pattern p = Pattern.compile(functionRegex);
        Matcher m = p.matcher(balSrcCode);
        ArrayList<String> functionStringArray = new ArrayList<>();
        //to identify and store the substrings matching functionRegex
        //in this case, it matches both commented functions and actual functions
        while (m.find()) {
            String matchedString = m.group();
            functionStringArray.add(matchedString);
        }

        p = Pattern.compile(commentFunctionRegex);
        m = p.matcher(balSrcCode);
        //to identify the strings matching commentedFunctionRegex and remove the false positives from the function
        // string array
        while (m.find()) {
            String matchedString = m.group();
            //if any commentedFunctionRegex is found, the corresponding false positive record is removed
            functionStringArray.stream().filter(matchedString::endsWith).findFirst().ifPresent(
                    functionStringArray::remove);
        }

        //iterate through function string array which only contains true positives and update the interceptor map
        functionStringArray.forEach(f -> {
            //function name
            String functionName = f.split(" ")[1];
            //if the function is declared more than one time, throws an runtime exception as it causes ballerina
            // compilation error
            if (interceptorMap.containsKey(functionName)) {
                throw new CLIRuntimeException("The function '" + functionName + "' is declared twice in the " +
                        "following files. Please remove one of the.\n" + interceptorMap.get(functionName) + "\n" +
                        interceptorFilePath);
            }
            interceptorMap.put(functionName, interceptorFilePath);
        });
    }

    /**
     * Find and store the request interceptors included in a ballerina source code.
     *
     * @param balSrcCode          the ballerina source code
     * @param interceptorFilePath the file path of the ballerina source code
     */
    private static void findRequestInterceptors(String balSrcCode, String interceptorFilePath) {
        findInterceptors(balSrcCode, interceptorFilePath, true, requestInterceptorMap);
    }

    /**
     * Find and store the response interceptors included in a ballerina source code.
     *
     * @param balSrcCode          the ballerina source code
     * @param interceptorFilePath the file path of the ballerina source code
     */
    private static void findResponseInterceptors(String balSrcCode, String interceptorFilePath) {
        findInterceptors(balSrcCode, interceptorFilePath, false, responseInterceptorMap);
    }

    /**
     * Validate the existence of the interceptor in ballerina source files inside interceptors directory.
     * Throws an runtime error if the interceptor is not found.
     * if the provided interceptor name is null, 'null' will be returned.
     *
     * @param isRequestInterceptor true if it is a request interceptor, false if it is a response interceptor
     * @param interceptorName      name of the interceptor
     * @param openAPIFilePath      path of the openAPI definition file
     * @param path                 path of the resource (if interceptor is Api level keep null)
     * @param operation            operation of the resource (if interceptor is Api level keep null)
     */
    private static void validateInterceptorAvailability(String interceptorName, boolean isRequestInterceptor,
                                                        String openAPIFilePath, String path, String operation) {
        if (interceptorName == null) {
            return;
        }
        Map<String, String> interceptorMap;
        if (isRequestInterceptor) {
            interceptorMap = requestInterceptorMap;
        } else {
            interceptorMap = responseInterceptorMap;
        }
        //if the interceptor map does not contain the interceptor, the interceptor is not available
        if (!interceptorMap.containsKey(interceptorName)) {
            String errorMsg = "The interceptor '" + interceptorName + "' mentioned in openAPI definition:'" +
                    openAPIFilePath + "' ";
            //if the interceptor is resource level
            if (path != null && operation != null) {
                errorMsg += "under path:'" + path + "' operation:'" + operation + "' ";
            }
            errorMsg += "is not available in the " + GatewayCliConstants.PROJECT_INTERCEPTORS_DIR + " directory.";
            throw new CLIRuntimeException(errorMsg);
        }
    }

    /**
     * validate API level interceptors
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openAPIFilePath file path to openAPI definition
     */
    private static void validateAPIInterceptors(OpenAPI openAPI, String openAPIFilePath) {
        Optional<Object> apiRequestInterceptor = Optional.ofNullable(openAPI.getExtensions()
                .get(OpenAPIConstants.REQUEST_INTERCEPTOR));
        apiRequestInterceptor.ifPresent(value -> validateInterceptorAvailability(value.toString(),
                true, openAPIFilePath, null, null));
        Optional<Object> apiResponseInterceptor = Optional.ofNullable(openAPI.getExtensions()
                .get(OpenAPIConstants.RESPONSE_INTERCEPTOR));
        apiResponseInterceptor.ifPresent(value -> validateInterceptorAvailability(value.toString(),
                false, openAPIFilePath, null, null));
    }

    /**
     * validate all resource extensions for single path
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openAPIFilePath file path to openAPI definition
     */
    private static void validateResourceExtensionsForSinglePath(OpenAPI openAPI, String openAPIFilePath) {
        openAPI.getPaths().entrySet().forEach(entry -> {
            validateSingleResourceExtensions(entry.getValue().getGet(), entry.getKey(), "get", openAPIFilePath);
            validateSingleResourceExtensions(entry.getValue().getPost(), entry.getKey(), "post", openAPIFilePath);
            validateSingleResourceExtensions(entry.getValue().getPut(), entry.getKey(), "put", openAPIFilePath);
            validateSingleResourceExtensions(entry.getValue().getPatch(), entry.getKey(), "patch", openAPIFilePath);
            validateSingleResourceExtensions(entry.getValue().getHead(), entry.getKey(), "head", openAPIFilePath);
            validateSingleResourceExtensions(entry.getValue().getDelete(), entry.getKey(), "delete", openAPIFilePath);
            validateSingleResourceExtensions(entry.getValue().getOptions(), entry.getKey(), "options", openAPIFilePath);
            validateSingleResourceExtensions(entry.getValue().getTrace(), entry.getKey(), "trace", openAPIFilePath);
        });
    }

    /**
     * Validate Resource level extensions for Single Resource
     *
     * @param operation       {@link Operation} object
     * @param pathItem        path name
     * @param operationName   operation name
     * @param openAPIFilePath file path to openAPI definition
     */
    private static void validateSingleResourceExtensions(Operation operation, String pathItem, String operationName,
                                                         String openAPIFilePath) {
        if (operation == null || operation.getExtensions() == null) {
            return;
        }
        //todo: validate policy
        validateSingleResourceInterceptors(operation, pathItem, operationName, openAPIFilePath);
    }

    /**
     * Validate Resource interceptors for Single Resource
     *
     * @param operation       {@link Operation} object
     * @param pathItem        path name
     * @param operationName   operation name
     * @param openAPIFilePath file path to openAPI definition
     */
    private static void validateSingleResourceInterceptors(Operation operation, String pathItem, String operationName,
                                                           String openAPIFilePath) {
        //validate request interceptor
        Optional<Object> requestInterceptor = Optional.ofNullable(operation.getExtensions()
                .get(OpenAPIConstants.REQUEST_INTERCEPTOR));
        requestInterceptor.ifPresent(value -> validateInterceptorAvailability(value.toString(), true,
                openAPIFilePath, pathItem, operationName));
        //validate response interceptor
        Optional<Object> responseInterceptor = Optional.ofNullable(operation.getExtensions()
                .get(OpenAPIConstants.RESPONSE_INTERCEPTOR));
        responseInterceptor.ifPresent(value -> validateInterceptorAvailability(value.toString(), false,
                openAPIFilePath, pathItem, operationName));
    }

    /**
     * set the security for API from 'security' section in openAPI.
     * Default value is "oauth2".
     *
     * @param api     {@link ExtendedAPI} object
     * @param openAPI {@link OpenAPI} object
     */
    private static void setMgwAPISecurityAndScopes(ExtendedAPI api, OpenAPI openAPI) {
        String[] securitySchemasAndScopes = generateMgwSecuritySchemasAndScopes(openAPI.getSecurity());
        String securitySchemas = securitySchemasAndScopes[0];
        String scopes = securitySchemasAndScopes[1];
        //if securitySchemas String is null, set to oauth2
        if (StringUtils.isEmpty(securitySchemas)) {
            securitySchemas = APISecurity.oauth2.name();
        }
        api.setMgwApiSecurity(securitySchemas);
        api.setMgwApiScope(scopes);
    }

    /**
     * generate String array with security schema and scopes when the {@link SecurityRequirement} list mentioned
     * under {@link OpenAPI} object or {@link Operation} object is provided.
     * Resulted array contains two elements.
     * First element contains set of comma separated security schema types (oauth2 and basic).
     * Second element contains set of comma separated scopes.
     *
     * @param securityRequirementList {@link List<SecurityRequirement>} object
     * @return String array with two elements {'schema','scopes'} (ex. {"oauth2", "read:pets,write:pets"}
     */
    private static String[] generateMgwSecuritySchemasAndScopes(List<SecurityRequirement> securityRequirementList) {
        String securitySchemas = null;
        String scopes = null;
        List<String> securitySchemaList = new ArrayList<>(2);
        List<String> scopeList = new ArrayList<>();
        if (securityRequirementList != null) {
            securityRequirementList.forEach(value -> value.forEach((k, v) -> {
                //check if the key's type is oauth2
                if (oauthSecuritySchemaList.contains(k)) {
                    if (!securitySchemaList.contains(APISecurity.oauth2.name())) {
                        securitySchemaList.add(APISecurity.oauth2.name());
                    }
                    //if oauth2, add all the available scopes
                    v.forEach(scope -> {
                        if (!scopeList.contains(scope)) {
                            scopeList.add(scope);
                        }
                    });
                    //if the key's type is basic
                } else if (basicSecuritySchemaList.contains(k) &&
                        !securitySchemaList.contains(APISecurity.basic.name())) {
                    securitySchemaList.add(APISecurity.basic.name());
                }
            }));
            //generate security schema String
            for (String schema : securitySchemaList) {
                securitySchemas = StringUtils.isEmpty(securitySchemas) ? schema : securitySchemas + "," + schema;
            }
            //generate scopes string
            for (String scope : scopeList) {
                scopes = StringUtils.isEmpty(scopes) ? scope : scopes + "," + scope;
            }
        }
        return new String[]{securitySchemas, scopes};
    }

    public static BasicAuth getMgwResourceBasicAuth(Operation operation) {
        String securitySchemas = generateMgwSecuritySchemasAndScopes(operation.getSecurity())[0];
        if(StringUtils.isEmpty(securitySchemas)){
            return null;
        }
        return generateBasicAuthFromSecurity(securitySchemas);
    }

    /**
     * Get resource level security scopes of {@link Operation} if the security scheme is oauth2.
     *
     * @param operation {@link Operation}
     * @return comma separated set of scopes (ex. 'read:pets, write:pets')
     */
    public static String getMgwResourceScope(Operation operation) {
        return generateMgwSecuritySchemasAndScopes(operation.getSecurity())[1];
    }

    /**
     * When the security schema string is provided as a comma separated set of values
     * generate the corresponding schema string.
     *
     * @param schemas comma separated security security schema types (ex. basic,oauth2)
     * @return {@link BasicAuth} object
     */
    public static BasicAuth generateBasicAuthFromSecurity(String schemas){
        BasicAuth basicAuth = new BasicAuth();
        boolean basic = false;
        boolean oauth2 = false;
        String[] schemasArray = schemas.trim().split("\\s*,\\s*");
        for (String s : schemasArray) {
            if (s.equalsIgnoreCase("basic")) {
                basic = true;
            } else if (s.equalsIgnoreCase("oauth2")) {
                oauth2 = true;
            }
        }
        if (basic && oauth2) {
            basicAuth.setOptional(true);
            basicAuth.setRequired(false);
        } else if (basic) {
            basicAuth.setRequired(true);
            basicAuth.setOptional(false);
        } else if (oauth2) {
            basicAuth.setOptional(false);
            basicAuth.setRequired(false);
        }
        return basicAuth;
    }

    /**
     * validate API name and version.
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openAPIFilePath file path to openAPI definition
     */
    private static void validateAPINameAndVersion(OpenAPI openAPI, String openAPIFilePath) {
        String apiNameVersion = openAPI.getInfo().getTitle() + ":" + openAPI.getInfo().getVersion();
        if (apiNameVersionMap.containsKey(apiNameVersion)) {
            throw new CLIRuntimeException("The API '" + openAPI.getInfo().getTitle() + "' version '" +
                    openAPI.getInfo().getVersion() + "' is duplicated across multiple openAPI definitions. \n" +
                    apiNameVersionMap.get(apiNameVersion) + "\n" + openAPIFilePath);
        }
        apiNameVersionMap.put(apiNameVersion, openAPIFilePath);
    }

    /**
     * validate the openAPI definition
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openAPIFilePath file path to openAPI definition
     */
    public static void validateOpenAPIDefinition(OpenAPI openAPI, String openAPIFilePath) {
        validateAPINameAndVersion(openAPI, openAPIFilePath);
        validateBasepath(openAPI, openAPIFilePath);
        validateAPIInterceptors(openAPI, openAPIFilePath);
        validateResourceExtensionsForSinglePath(openAPI, openAPIFilePath);
        setOauthSecuritySchemaList(openAPI);
        setBasicSecuritySchemaList(openAPI);
    }

    /**
     * store the security schemas of type "oauth2"
     *
     * @param openAPI {@link OpenAPI} object
     */
    private static void setOauthSecuritySchemaList(OpenAPI openAPI) {
        //Since the security schema list needs to instantiated per each API
        oauthSecuritySchemaList = new ArrayList<>();
        if (openAPI.getComponents() == null || openAPI.getComponents().getSecuritySchemes() == null) {
            return;
        }
        openAPI.getComponents().getSecuritySchemes().forEach((key, value1) -> {
            if (value1.getType() == SecurityScheme.Type.OAUTH2 ||
                    (value1.getType() == SecurityScheme.Type.HTTP && value1.getScheme().toLowerCase().equals("jwt"))) {
                oauthSecuritySchemaList.add(key);
            }
        });
    }

    /**
     * store the security schemas of type "basic"
     *
     * @param openAPI {@link OpenAPI} object
     */
    private static void setBasicSecuritySchemaList(OpenAPI openAPI) {
        //Since the security schema list needs to instantiated per each API
        basicSecuritySchemaList = new ArrayList<>();
        if (openAPI.getComponents() == null || openAPI.getComponents().getSecuritySchemes() == null) {
            return;
        }
        openAPI.getComponents().getSecuritySchemes().forEach((key, value1) -> {
            if (value1.getType() == SecurityScheme.Type.HTTP && value1.getScheme().toLowerCase().equals("basic")) {
                basicSecuritySchemaList.add(key);
            }
        });
    }
}
