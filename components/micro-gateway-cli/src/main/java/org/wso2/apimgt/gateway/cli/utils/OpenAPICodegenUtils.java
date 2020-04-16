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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.CLICompileTimeException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.config.APIKey;
import org.wso2.apimgt.gateway.cli.model.config.ApplicationSecurity;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;
import org.wso2.apimgt.gateway.cli.model.route.RouteEndpointConfig;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions to be used by MGW code generation related component.
 */
public class OpenAPICodegenUtils {
    private static final Logger logger = LoggerFactory.getLogger(OpenAPICodegenUtils.class);
    private static PrintStream outStream = System.out;

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final String openAPISpec2 = "2";
    private static final String openAPISpec3 = "3";
    private static final Map<String, String> basePathMap = new HashMap<>();
    private static Map<String, String> requestInterceptorMap = new HashMap<>();
    private static Map<String, String> responseInterceptorMap = new HashMap<>();
    private static Map<String, String> apiNameVersionMap = new HashMap<>();
    private static List<Map<Object, Object>> endPointReferenceExtensions;
    private static List<String> oauthSecuritySchemaList = new ArrayList<>();
    private static List<String> basicSecuritySchemaList = new ArrayList<>();
    private static Map apiKeySecuritySchemaMap = new HashMap();

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
     * Discover the openAPI version of the given API definition.
     *
     * @param apiDefinition API definition (as a file path or String content)
     * @param isFilePath    If the given api Definition is a file path
     * @return openAPI version number (2 or 3)
     */
    public static String findSwaggerVersion(String apiDefinition, boolean isFilePath) {

        JsonNode rootNode = generateJsonNode(apiDefinition, isFilePath);
        if (rootNode.has("swagger") && rootNode.get("swagger").asText().trim().startsWith("2")) {
            return openAPISpec2;
        } else if (rootNode.has("openapi") && rootNode.get("openapi").asText().trim().startsWith("3")) {
            return openAPISpec3;
        }
        throw new CLIRuntimeException("Error while reading the open API version from file, Check the open API format");
    }

    /**
     * Extract the openAPI definition as String from an ExtendedAPI object.
     *
     * @param api ExtendedAPI object
     * @return openAPI definition as a String
     */
    static String generateSwaggerString(ExtendedAPI api, boolean isExpand) {

        String swaggerVersion = findSwaggerVersion(api.getApiDefinition(), false);
        RouteEndpointConfig mgwEndpointConfigDTO = null;
        if (isExpand) {
            try {
                mgwEndpointConfigDTO = getEndpointObjectFromAPI(api);
            } catch (MalformedURLException e) {
                //Providing API Name and version would be enough since this path is executed only if APIM 2.6.0 is used
                throw new CLIRuntimeException("The provided endpoints in the imported API \"" + api.getName() + " : " +
                        api.getVersion() + "\" are invalid.");
            } catch (CLICompileTimeException e) {
                throw new CLIRuntimeException("The provided endpoints in the imported API \"" + api.getName() + " : " +
                        api.getVersion() + "\" are invalid.\n\t-" + e.getTerminalMsg(), e);
            }
        }

        switch (swaggerVersion) {
            case "2":
                Swagger swagger = new SwaggerParser().parse(api.getApiDefinition());
                //Sets title name similar to API name in swagger definition.
                //Without this modification, two seperate rows will be added to APIM analytics dashboard tables.
                //(For APIM and Microgateway API invokes)
                swagger.getInfo().setTitle(api.getName());
                if (isExpand) {
                    swagger.setVendorExtensions(getExtensionMap(api, mgwEndpointConfigDTO));
                }
                return Json.pretty(swagger);
            case "3":
                SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(api.getApiDefinition());
                OpenAPI openAPI = swaggerParseResult.getOpenAPI();
                //Sets title similar to API name in open API definition
                //Without this modification, two seperate rows will be added to analytics dashboard tables.
                //(For APIM and Microgateway API invokes)
                openAPI.getInfo().setTitle(api.getName());
                if (isExpand) {
                    openAPI.extensions(getExtensionMap(api, mgwEndpointConfigDTO));
                }
                return Yaml.pretty(openAPI);

            default:
                throw new CLIRuntimeException("Error: Swagger version is not identified");
        }
    }

    /**
     * Convert the v2 or v3 open API definition in yaml or json format into json format of the respective format.
     * v2/YAML -> v2/JSON
     * v3/YAML -> v3/JSON
     *
     * @param openAPIContent open API as a string content
     * @return openAPI definition as a JSON String
     */
    public static String getOpenAPIAsJson(OpenAPI openAPI, String openAPIContent, Path openAPIPath) {
        String jsonOpenAPI = Json.pretty(openAPI);
        String openAPIVersion;
        Path fileName = openAPIPath.getFileName();

        if (fileName == null) {
            throw new CLIRuntimeException("Error: Couldn't resolve OpenAPI file name.");
        } else if (fileName.toString().endsWith("json")) {
            openAPIVersion = findSwaggerVersion(openAPIContent, false);
        } else {
            openAPIVersion = findSwaggerVersion(jsonOpenAPI, false);
        }

        switch (openAPIVersion) {
            case "2":
                Swagger swagger = new SwaggerParser().parse(openAPIContent);
                return Json.pretty(swagger);
            case "3":
                return jsonOpenAPI;

            default:
                throw new CLIRuntimeException("Error: Swagger version is not identified");
        }
    }

    public static String convertYamlToJson(String yaml) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }


        private static Map<String, Object> getExtensionMap(ExtendedAPI api, RouteEndpointConfig mgwEndpointConfigDTO) {
        Map<String, Object> extensionsMap = new HashMap<>();
        String basePath = api.getContext() + "/" + api.getVersion();
        extensionsMap.put(OpenAPIConstants.BASEPATH, basePath);
        if (mgwEndpointConfigDTO.getProdEndpointList() != null) {
            extensionsMap.put(OpenAPIConstants.PRODUCTION_ENDPOINTS, mgwEndpointConfigDTO.getProdEndpointList());
        }
        if (mgwEndpointConfigDTO.getSandboxEndpointList() != null) {
            extensionsMap.put(OpenAPIConstants.SANDBOX_ENDPOINTS, mgwEndpointConfigDTO.getSandboxEndpointList());
        }
        if (api.getCorsConfiguration() != null) {
            extensionsMap.put(OpenAPIConstants.CORS, api.getCorsConfiguration());
        }
        if (api.getAuthorizationHeader() != null) {
            extensionsMap.put(OpenAPIConstants.AUTHORIZATION_HEADER, api.getAuthorizationHeader());
        }
        if (api.getProvider() != null) {
            extensionsMap.put(OpenAPIConstants.API_OWNER, api.getProvider());
        }

        return extensionsMap;
    }

    private static RouteEndpointConfig getEndpointObjectFromAPI(ExtendedAPI api)
            throws MalformedURLException, CLICompileTimeException {
        return RouteUtils.parseEndpointConfig(api.getEndpointConfig(), api.getEndpointSecurity());

    }

    /**
     * generate ExtendedAPI object from openAPI definition.
     *
     * @param openAPI {@link OpenAPI} object with all required properties
     * @return {@link ExtendedAPI} object
     */
    public static ExtendedAPI generateAPIFromOpenAPIDef(OpenAPI openAPI, String openAPIContent) throws IOException {
        ExtendedAPI api = generateAPIFromOpenAPI(openAPI);
        //open API content should be set in json in order to validation filter to work.
        api.setApiDefinition(openAPIContent);
        return api;
    }

    /**
     * Generate {@link ExtendedAPI} object from generated OpenAPI from protobuf file. This is used for the ballerina
     * service generation.
     *
     * @param openAPI {@link OpenAPI} object with all required properties
     * @return {@link ExtendedAPI} object for protobuf
     */
    public static ExtendedAPI generateGrpcAPIFromOpenAPI(OpenAPI openAPI) {
        ExtendedAPI api = generateAPIFromOpenAPI(openAPI);
        api.setGrpc(true);
        return api;
    }

    private static ExtendedAPI generateAPIFromOpenAPI(OpenAPI openAPI) {
        String apiId = HashUtils.generateAPIId(openAPI.getInfo().getTitle(), openAPI.getInfo().getVersion());
        ExtendedAPI api = new ExtendedAPI();
        api.setId(apiId);
        api.setName(openAPI.getInfo().getTitle());
        api.setVersion(openAPI.getInfo().getVersion());
        populateTransportSecurity(api, openAPI);
        return api;
    }

    /**
     * Get http, https and mutualSSL from API Definition x-wso2-transport and x-wso2-mutual-ssl extensions.
     *
     * @param api       Extended api
     * @param openAPI   API definition
     */
    private static void populateTransportSecurity(ExtendedAPI api, OpenAPI openAPI) throws CLIRuntimeException {
        List<String> transports = new ArrayList<>();
        String mutualSSL = null;
        Map<String, Object> apiDefExtensions = openAPI.getExtensions();
        if (apiDefExtensions.containsKey(OpenAPIConstants.MUTUAL_SSL)) {
            if (logger.isDebugEnabled()) {
                logger.debug(OpenAPIConstants.MUTUAL_SSL + " extension found in the API Definition");
            }
            mutualSSL = validateMutualSSL(apiDefExtensions.get(OpenAPIConstants.MUTUAL_SSL), openAPI);
            api.setMutualSSL(mutualSSL);
        }
        if (apiDefExtensions.containsKey(OpenAPIConstants.TRANSPORT_SECURITY)) {
            if (logger.isDebugEnabled()) {
                logger.debug(OpenAPIConstants.TRANSPORT_SECURITY + " extension found in the API Definition");
            }
            transports = validateTransports(apiDefExtensions.get(OpenAPIConstants.TRANSPORT_SECURITY), mutualSSL,
                    openAPI);
        }
        if (transports.isEmpty()) {
            transports = Arrays.asList(OpenAPIConstants.TRANSPORT_HTTP, OpenAPIConstants.TRANSPORT_HTTPS);
        }
        api.setTransport(transports);
    }

    private static List<String> validateTransports(Object transportExtension, String mutualSSL, OpenAPI openAPI)
            throws CLIRuntimeException {
        List<String> transports;
        try {
            transports = (List<String>) transportExtension;
        } catch (ClassCastException exception) {
            throw new CLIRuntimeException("The API '" + openAPI.getInfo().getTitle() + "' version '" +
                    openAPI.getInfo().getVersion() + "' contains " + OpenAPIConstants.TRANSPORT_SECURITY +
                    " extension but failed to match to the required format.");
        }
        for (String transport : transports) {
            if (!(OpenAPIConstants.TRANSPORT_HTTP.equalsIgnoreCase(transport) ||
                    OpenAPIConstants.TRANSPORT_HTTPS.equalsIgnoreCase(transport))) {
                throw new CLIRuntimeException("The API '" + openAPI.getInfo().getTitle() + "' version '" +
                        openAPI.getInfo().getVersion() + "' contains " + OpenAPIConstants.TRANSPORT_SECURITY +
                        " extension but only http and https are allowed as transports");
            }
        }
        if (OpenAPIConstants.MANDATORY.equalsIgnoreCase(mutualSSL)
                && !transports.contains(OpenAPIConstants.TRANSPORT_HTTPS)) {
            // add https if mutualssl is mandatory
            transports.add(OpenAPIConstants.TRANSPORT_HTTPS);
        }
        return transports;
    }

    private static String validateMutualSSL(Object mutualSSLExtension, OpenAPI openAPI) throws CLIRuntimeException {
        String mutualSSL;
        try {
            mutualSSL = (String) mutualSSLExtension;
        } catch (ClassCastException exception) {
            throw new CLIRuntimeException("The API '" + openAPI.getInfo().getTitle() + "' version '" +
                    openAPI.getInfo().getVersion() + "' contains " + OpenAPIConstants.MUTUAL_SSL +
                    " extension but failed to match to the required format.");
        }
        if (!(OpenAPIConstants.MANDATORY.equalsIgnoreCase(mutualSSL)
                || OpenAPIConstants.OPTIONAL.equalsIgnoreCase(mutualSSL))) {
                throw new CLIRuntimeException("The API '" + openAPI.getInfo().getTitle() + "' version '" +
                        openAPI.getInfo().getVersion() + "' contains " + OpenAPIConstants.MUTUAL_SSL +
                        " but only optional and mandatory are allowed for mutual SSL");
        }
        return mutualSSL;
    }

    public static void setAdditionalConfigsDevFirst(ExtendedAPI api, OpenAPI openAPI, String openAPIFilePath) {
        Map<String, Object> extensions = openAPI.getExtensions();
        EndpointListRouteDTO prodEndpointListDTO = null;
        try {
            prodEndpointListDTO = extractEndpointFromOpenAPI(
                    extensions != null ? extensions.get(OpenAPIConstants.PRODUCTION_ENDPOINTS) : null,
                    openAPI.getServers());
        } catch (CLICompileTimeException e) {
            throw new CLIRuntimeException("Error while parsing the openAPI defintion for the API \"" +
                    openAPI.getInfo().getTitle() + " : " + openAPI.getInfo().getVersion() + "\".\n\t-" +
                    e.getTerminalMsg(), e);
        }
        // if endpoint name is empty set api id as the name
        if (prodEndpointListDTO != null && prodEndpointListDTO.getName() == null) {
            prodEndpointListDTO.setName(api.getId());
        }
        EndpointListRouteDTO sandEndpointListDTO = null;
        try {
            //Servers object is set to null as the servers object is considered as a production endpoint
            sandEndpointListDTO = extractEndpointFromOpenAPI(
                    extensions != null ? extensions.get(OpenAPIConstants.SANDBOX_ENDPOINTS) : null, null);
        } catch (CLICompileTimeException e) {
            throw new CLIRuntimeException("Error while parsing the openAPI defintion for the API \"" +
                    openAPI.getInfo().getTitle() + " : " + openAPI.getInfo().getVersion() + "\".\n\t-" +
                    e.getTerminalMsg(), e);
        }
        if (sandEndpointListDTO != null && sandEndpointListDTO.getName() == null) {
            sandEndpointListDTO.setName(api.getId());
        }
        MgwEndpointConfigDTO mgwEndpointConfigDTO = RouteUtils
                .convertToMgwServiceMap(prodEndpointListDTO, sandEndpointListDTO);
        api.setEndpointConfigRepresentation(mgwEndpointConfigDTO);

        setMgwAPISecurityAndScopes(api, openAPI);
        api.setSpecificBasepath(resolveTemplateBasePath(extensions, openAPI.getInfo().getVersion()));
        //assigns x-wso2-owner value to API provider
        if (extensions.containsKey(OpenAPIConstants.API_OWNER)) {
            api.setProvider(extensions.get(OpenAPIConstants.API_OWNER).toString());
        }
        try {
            if (extensions.get(OpenAPIConstants.CORS) != null) {
                api.setCorsConfiguration(objectMapper.convertValue(extensions.get(OpenAPIConstants.CORS),
                        APICorsConfigurationDTO.class));
                // explicitly set the cors enabled value to true if cors config found in the open API definition
                api.getCorsConfiguration().setCorsConfigurationEnabled(true);
            }
            // set authorization header from the open API extension
            Object authHeader = extensions.get(OpenAPIConstants.AUTHORIZATION_HEADER);
            if (authHeader != null) {
                api.setAuthorizationHeader(authHeader.toString());
            }
        } catch (IllegalArgumentException e) {
            throw new CLIRuntimeException("'" + OpenAPIConstants.CORS + "' property is not properly set for the " +
                    "openAPI definition file. \n" + openAPIFilePath);
        }
    }

    /**
     * get resource Endpoint configuration in the format of {@link MgwEndpointConfigDTO} to match the mustache
     * template.
     *
     * @param operation {@link Operation} object
     * @return {@link MgwEndpointConfigDTO} object
     */
    public static MgwEndpointConfigDTO getResourceEpConfigForCodegen(Operation operation)
            throws CLICompileTimeException {
        Map<String, Object> extensions = operation.getExtensions();
        EndpointListRouteDTO prodEndpointListDTO = extractEndpointFromOpenAPI(
                extensions != null ? operation.getExtensions().get(OpenAPIConstants.PRODUCTION_ENDPOINTS) : null,
                operation.getServers());
        // if endpoint name is empty set operation id as the name
        if (prodEndpointListDTO != null && prodEndpointListDTO.getName() == null) {
            prodEndpointListDTO.setName(operation.getOperationId());
            //Validate the endpointList and throw the exception as it is.
            prodEndpointListDTO.validateEndpoints();
        }
        EndpointListRouteDTO sandEndpointListDTO = extractEndpointFromOpenAPI(
                extensions != null ? operation.getExtensions().get(OpenAPIConstants.SANDBOX_ENDPOINTS) : null, null);
        if (sandEndpointListDTO != null && sandEndpointListDTO.getName() == null) {
            sandEndpointListDTO.setName(operation.getOperationId());
            sandEndpointListDTO.validateEndpoints();
        }

        return RouteUtils.convertToMgwServiceMap(prodEndpointListDTO, sandEndpointListDTO);
    }

    private static EndpointListRouteDTO extractEndpointFromOpenAPI(Object endpointExtensionObject,
            List<Server> servers) throws CLICompileTimeException {
        EndpointListRouteDTO endpointListRouteDTO = null;
        if (endpointExtensionObject != null) {
            String endpointExtensionObjectValue = endpointExtensionObject.toString();
            if (endpointExtensionObjectValue.contains(OpenAPIConstants.ENDPOINTS_REFERENCE)) {
                String referencePath = endpointExtensionObjectValue.split(OpenAPIConstants.ENDPOINTS_REFERENCE)[1];
                for (Map<Object, Object> value : endPointReferenceExtensions) {
                    if (value.containsKey(referencePath)) {
                        try {
                            endpointListRouteDTO = objectMapper
                                    .convertValue(value.get(referencePath), EndpointListRouteDTO.class);
                            endpointListRouteDTO.setName(referencePath);
                            try {
                                endpointListRouteDTO.validateEndpoints();
                            } catch (CLICompileTimeException e) {
                                throw new CLICompileTimeException("The provided endpoint using the reference " +
                                        referencePath + " is invalid.\n\t-" + e.getTerminalMsg(), e);
                            }
                            return endpointListRouteDTO;
                        } catch (IllegalArgumentException e) {
                            throw new CLICompileTimeException("Error while parsing the referenced endpoint object "
                                    + endpointExtensionObjectValue + ". The endpoint \"" + referencePath
                                    + "\" defined under " + OpenAPIConstants.ENDPOINTS + " is incompatible : "
                                    + value.get(referencePath).toString(), e);
                        }
                    }
                }
                throw new CLICompileTimeException("The referenced endpoint value : \"" + endpointExtensionObjectValue
                        + "\" is not defined under the open API extension " + OpenAPIConstants.ENDPOINTS);

            } else {
                try {
                    endpointListRouteDTO = objectMapper
                            .convertValue(endpointExtensionObject, EndpointListRouteDTO.class);
                } catch (IllegalArgumentException e) {
                    throw new CLICompileTimeException("Error while parsing the endpoint object. The "
                            + OpenAPIConstants.PRODUCTION_ENDPOINTS + " or "
                            + OpenAPIConstants.SANDBOX_ENDPOINTS + " format is incompatible : "
                            + endpointExtensionObjectValue, e);
                }
                try {
                    endpointListRouteDTO.validateEndpoints();
                } catch (CLICompileTimeException e) {
                    throw new CLICompileTimeException("The provided endpoint using the extension " +
                            OpenAPIConstants.PRODUCTION_ENDPOINTS + " or " + OpenAPIConstants.SANDBOX_ENDPOINTS +
                            " is invalid.\n\t-" + e.getTerminalMsg(), e);
                }
            }
        } else if (servers != null) {
            endpointListRouteDTO = new EndpointListRouteDTO();
            for (Server server : servers) {
                //server url templating can have urls similar to 'https://{customerId}.saas-app.com:{port}/v2'
                endpointListRouteDTO.addEndpoint(replaceOpenAPIServerTemplate(server));
            }
            try {
                endpointListRouteDTO.validateEndpoints();
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("The provided endpoint using the \"servers\" object " +
                        "is invalid.\n\t-" + e.getTerminalMsg(), e);
            }
        }
        return endpointListRouteDTO;
    }

    /**
     * Validate basePath to avoid having the same basePath for two or more APIs.
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openApiFilePath OpenAPI definition file
     * @param openAPIVersion OpenAPI version
     */
    private static void validateBasePath(OpenAPI openAPI, String openApiFilePath, String openAPIVersion) {
        List<Server> servers = openAPI.getServers();
        Map<String, Object> openAPIExtensions = openAPI.getExtensions();
        String basePath = null;
        if (openAPIExtensions != null && openAPIExtensions.get(OpenAPIConstants.BASEPATH) != null) {
            basePath = (String) openAPIExtensions.get(OpenAPIConstants.BASEPATH);
        } else if (servers != null) {
            //We  need to check only the first server url to derive the base path. Each and every server url
            // should contains the same base path.
            Server server = servers.get(0);
            if (server.getUrl() != null) {
                String url = replaceOpenAPIServerTemplate(server);
                try {
                    basePath = new URI(url).getPath();
                } catch (URISyntaxException e) {
                    String message;
                    if (openAPISpec2.equals(openAPIVersion)) {
                        message = "Scheme, host, base path combination of openAPI '" + openApiFilePath
                                + "' resolves to : " + url + ", which is a malformed url";
                    } else {
                        message = "Server url: " + url + " of openAPI '" + openApiFilePath + "' is a malformed url";
                    }
                    throw new CLIRuntimeException(message);
                }
            }
        }

        if (basePath == null || basePath.isEmpty()) {
            basePath = "/";
            String message;
            if (openAPISpec2.equals(openAPIVersion)) {
                message = "basePath not found in the open API '" + openApiFilePath + "'. Hence defaults to '/'";
            } else {
                message = "servers url in the open API '" + openApiFilePath
                        + "' does not adhere to pattern '<scheme>://<host>/<basePath>/'. "
                        + "Hence the base path defaults to '/'";
            }
            outStream.println(CliConstants.WARN_LOG_PATTERN + message);
        } else {
            basePath = basePath.startsWith("/") ? basePath : "/" + basePath;
            if (basePathMap.containsKey(basePath)) {
                throw new CLIRuntimeException("The derived value for the base path '" + basePath
                        + "'  is duplicated in the following openAPI definitions.\n" + basePathMap.get(basePath) + "\n"
                        + openApiFilePath);
            }
        }
        basePathMap.put(basePath, openApiFilePath);
        openAPI.addExtension(OpenAPIConstants.BASEPATH, basePath);
    }

    /**
     * Store all the interceptor functions included in interceptors directory.
     *
     * @throws IOException if an error occurred while reading the bal files inside interceptor directory
     */
    public static void setInterceptors(String projectName) throws IOException {
        String interceptorsDirectoryPath = CmdUtils.getProjectInterceptorsPath(projectName);
        Files.walk(Paths.get(interceptorsDirectoryPath)).filter(path -> {
            Path fileName = path.getFileName();
            return fileName != null && fileName.toString().endsWith(CliConstants.EXTENSION_BAL);
        }).forEach(path -> {
            String balSrcCode = null;
            try {
                balSrcCode = CmdUtils.readFileAsString(path.toString(), false);
            } catch (IOException e) {
                logger.error("Error occurred while reading interceptors", e);
            }

            findInterceptors(balSrcCode, path.toString(), true, requestInterceptorMap);
            findInterceptors(balSrcCode, path.toString(), false, responseInterceptorMap);
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
            String functionName = (f.replace("(", " ")).split(" ")[1];
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
     * Open API server object can have server templating. Ex: https://{customerId}.saas-app.com:{port}/v2.
     * When adding the back end url this method will replace the template values with the default value.
     *
     * @param server  {@link Server} object of the open API definition
     * @return templated server url replaced with default values
     */
    private static String replaceOpenAPIServerTemplate(Server server) {
        //server url templating can have urls similar to 'https://{customerId}.saas-app.com:{port}/v2'
        String url = server.getUrl();
        Pattern serverTemplate = Pattern.compile("\\{.*?}");
        Matcher matcher = serverTemplate.matcher(url);
        while (matcher.find()) {
            if (server.getVariables() != null && server.getVariables()
                    .containsKey(matcher.group(0).substring(1, matcher.group(0).length() - 1))) {
                String defaultValue = server.getVariables()
                        .get(matcher.group(0).substring(1, matcher.group(0).length() - 1)).getDefault();
                url = url.replaceAll("\\" + matcher.group(0), defaultValue);
            } else {
                outStream.println(
                        CliConstants.WARN_LOG_PATTERN + "Open API server url templating is used for the url : " + url
                                + ". But default values is not provided for the variable '" + matcher.group(0)
                                + "'. Hence correct url will not be resolved during the runtime "
                                + "unless url is overridden during the runtime");
            }
        }
        return url;
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
        if (!interceptorName.startsWith(OpenAPIConstants.INTERCEPTOR_JAVA_PREFIX) && !interceptorMap
                .containsKey(interceptorName)) {
            String errorMsg = "The interceptor '" + interceptorName + "' mentioned in openAPI definition:'" +
                    openAPIFilePath + "' ";
            //if the interceptor is resource level
            if (path != null && operation != null) {
                errorMsg += "under path:'" + path + "' operation:'" + operation + "' ";
            }
            errorMsg += "is not available in any function in the " + CliConstants.PROJECT_INTERCEPTORS_DIR +
                    " directory.";
            throw new CLIRuntimeException(errorMsg);
        }
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
       validateInterceptors(operation.getExtensions(), pathItem, operationName, openAPIFilePath);
    }

    /**
     * Validate API Level and Resource Level interceptors
     *
     * @param extensions      {@link Map} object to access api level and operation level extensions
     * @param pathItem        path name
     * @param operationName   operation name
     * @param openAPIFilePath file path to openAPI definition
     */
    private static void validateInterceptors(Map<String, Object> extensions, String pathItem, String operationName,
                                        String openAPIFilePath) {
        if (extensions != null) {
             Optional<Object> requestInterceptor = Optional.ofNullable(extensions
                     .get(OpenAPIConstants.REQUEST_INTERCEPTOR));
             requestInterceptor.ifPresent(value -> {
                 if (!value.toString().contains(OpenAPIConstants.INTERCEPTOR_PATH_SEPARATOR)) {
                     validateInterceptorAvailability(extensions.get(OpenAPIConstants.REQUEST_INTERCEPTOR).toString(),
                             true, openAPIFilePath, pathItem, operationName);
                 }
             });
             Optional<Object> responseInterceptor = Optional.ofNullable(extensions
                     .get(OpenAPIConstants.RESPONSE_INTERCEPTOR));
             responseInterceptor.ifPresent(value -> {
                 if (!value.toString().contains(OpenAPIConstants.INTERCEPTOR_PATH_SEPARATOR)) {
                     validateInterceptorAvailability(extensions.get(OpenAPIConstants.RESPONSE_INTERCEPTOR).toString(),
                             false, openAPIFilePath, pathItem, operationName);
                 }
             });
        }
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
        api.setMgwApiSecurity(securitySchemas);
        api.setMgwApiScope(scopes);
        if (logger.isDebugEnabled()) {
            logger.debug("Getting Application security by the extension for API '" + openAPI.getInfo().getTitle()
                    + "' version '" + openAPI.getInfo().getVersion() + "'");
        }
        ApplicationSecurity appSecurityfromDef = populateApplicationSecurity(api.getName(), api.getVersion(),
                openAPI.getExtensions(), api.getMutualSSL());
        api.setApplicationSecurity(appSecurityfromDef != null ? appSecurityfromDef : new ApplicationSecurity());
    }

    /**
     * Get application security from API Definition extension.
     *
     * @param apiDefExtensions          API definition extesnsions
     * @return ApplicationSecurity/null if not present returns null
     */
    public static ApplicationSecurity populateApplicationSecurity(String apiName, String version,
                                                                  Map<String, Object> apiDefExtensions,
                                                                  String mutualSSL) {
        ApplicationSecurity appSecurity = null;
        if (apiDefExtensions != null && apiDefExtensions.containsKey(OpenAPIConstants.APPLICATION_SECURITY)) {
            if (logger.isDebugEnabled()) {
                logger.debug(OpenAPIConstants.APPLICATION_SECURITY + " extension found in the API '" + apiName
                        + "' version '" + version + "'");
            }
            try {
                appSecurity = new ObjectMapper().convertValue(apiDefExtensions
                        .get(OpenAPIConstants.APPLICATION_SECURITY), ApplicationSecurity.class);

            } catch (Exception exception) {
                throw new CLIRuntimeException("The API '" + apiName + "' version '" + version + "' contains "
                        + OpenAPIConstants.APPLICATION_SECURITY + " extension but failed to match "
                        + OpenAPIConstants.APPLICATION_SECURITY_TYPES + " to the required format.");
            }
            if (!validateAppSecurityOptionality(appSecurity, mutualSSL)) {
                throw new CLIRuntimeException("Application security is given as optional but Mutual SSL is not " +
                        "mandatory for the API '" + apiName + "' version '" + version + "'");
            }
        }
        return appSecurity;
    }

    private static boolean validateAppSecurityOptionality(ApplicationSecurity appSecurity,
                                                          String mutualSSL) {
        // if application security is optional, mutual ssl must be mandatory
        return !((appSecurity.isOptional() != null && appSecurity.isOptional()) &&
                !OpenAPIConstants.MANDATORY.equalsIgnoreCase(mutualSSL));
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
                    if (!securitySchemaList.contains(OpenAPIConstants.APISecurity.oauth2.name())) {
                        securitySchemaList.add(OpenAPIConstants.APISecurity.oauth2.name());
                    }
                    //if oauth2, add all the available scopes
                    v.forEach(scope -> {
                        if (!scopeList.contains(scope)) {
                            scopeList.add(scope);
                        }
                    });
                    //if the key's type is basic
                } else if (basicSecuritySchemaList.contains(k) &&
                        !securitySchemaList.contains(OpenAPIConstants.APISecurity.basic.name())) {
                    securitySchemaList.add(OpenAPIConstants.APISecurity.basic.name());
                }  else if (apiKeySecuritySchemaMap.containsKey(k) &&
                        !securitySchemaList.contains(OpenAPIConstants.APISecurity.apikey.name())) {
                    securitySchemaList.add(OpenAPIConstants.APISecurity.apikey.name());
                }

            }));
            //generate security schema String
            StringBuilder secSchemaBuilder = new StringBuilder();
            StringBuilder scopeBuilder = new StringBuilder();
            for (String schema : securitySchemaList) {
                if (secSchemaBuilder.length() == 0) {
                    secSchemaBuilder.append(schema);
                } else {
                    secSchemaBuilder.append(',' + schema);
                }
            }
            //generate scopes string
            for (String scope : scopeList) {
                scope = "\"" + scope + "\"";
                if (scopeBuilder.length() == 0) {
                    scopeBuilder.append(scope);
                } else {
                    scopeBuilder.append(',' + scope);
                }
            }

            securitySchemas = secSchemaBuilder.toString();
            scopes = scopeBuilder.toString();
        }
        return new String[]{securitySchemas, scopes};
    }

    public static List<String> getMgwResourceSecurity(Operation operation, ApplicationSecurity appSecurity) {
        String securitySchemas = generateMgwSecuritySchemasAndScopes(operation.getSecurity())[0];
        return getAuthProviders(securitySchemas, appSecurity);
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
     * Provide api keys for a given security requirement list
     *
     * @param securityRequirementList {@link List<SecurityRequirement>} object
     * @return list of API Keys
     */
    public static List<APIKey> generateAPIKeysFromSecurity(List<SecurityRequirement> securityRequirementList,
                                                           boolean isAPIKeyEnabled) {
        List<APIKey> apiKeys = new ArrayList<>();
        if (securityRequirementList != null) {
            securityRequirementList.forEach(value -> value.forEach((k, v) -> {
                //check if the key is in apikey list
                if (apiKeySecuritySchemaMap.containsKey(k)) {
                    apiKeys.add((APIKey) apiKeySecuritySchemaMap.get(k));
                }
            }));
        }
        if (isAPIKeyEnabled && apiKeys.isEmpty()) {
            apiKeys.add(new APIKey(SecurityScheme.In.HEADER, OpenAPIConstants.DEFAULT_API_KEY_HEADER_QUERY));
            apiKeys.add(new APIKey(SecurityScheme.In.QUERY, OpenAPIConstants.DEFAULT_API_KEY_HEADER_QUERY));
        }
        return apiKeys;
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
     * validate the openAPI definition.
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openAPIFilePath file path to openAPI definition
     */
    public static void validateOpenAPIDefinition(OpenAPI openAPI, String openAPIFilePath, String openAPIVersion) {
        validateAPINameAndVersion(openAPI, openAPIFilePath);
        validateBasePath(openAPI, openAPIFilePath, openAPIVersion);
        validateEndpointAvailability(openAPI, openAPIFilePath, openAPIVersion);
        // validates API level interceptors
        validateInterceptors(openAPI.getExtensions(), null, null, openAPIFilePath);
        validateResourceExtensionsForSinglePath(openAPI, openAPIFilePath);
        setOauthSecuritySchemaList(openAPI);
        setSecuritySchemaList(openAPI);
        setOpenAPIDefinitionEndpointReferenceExtensions(openAPI.getExtensions());
    }

    /**
     * store the security schemas of type "oauth2".
     *
     * @param openAPI {@link OpenAPI} object
     */
    public static void setOauthSecuritySchemaList(OpenAPI openAPI) {
        //Since the security schema list needs to instantiated per each API
        oauthSecuritySchemaList = new ArrayList<>();
        if (openAPI.getComponents() == null || openAPI.getComponents().getSecuritySchemes() == null) {
            return;
        }
        openAPI.getComponents().getSecuritySchemes().forEach((key, val) -> {
            if (val.getType() == SecurityScheme.Type.OAUTH2 ||
                    (val.getType() == SecurityScheme.Type.HTTP &&
                            val.getScheme().toLowerCase(Locale.getDefault()).equals("jwt"))) {
                oauthSecuritySchemaList.add(key);
            }
        });
    }

    /**
     * store the security schemas of type "basic" and "apikey"
     *
     * @param openAPI {@link OpenAPI} object
     */
    public static void setSecuritySchemaList(OpenAPI openAPI) {
        //Since the security schema list needs to instantiated per each API
        basicSecuritySchemaList = new ArrayList<>();
        apiKeySecuritySchemaMap = new HashMap();
        if (openAPI.getComponents() == null || openAPI.getComponents().getSecuritySchemes() == null) {
            return;
        }
        openAPI.getComponents().getSecuritySchemes().forEach((key, val) -> {
            if (val.getType() == SecurityScheme.Type.HTTP &&
                    val.getScheme().toLowerCase(Locale.getDefault()).equals("basic")) {
                basicSecuritySchemaList.add(key);
            } else if (val.getType() == SecurityScheme.Type.APIKEY) {
                APIKey apiKey = new APIKey(val.getIn(), val.getName());
                apiKeySecuritySchemaMap.put(key, apiKey);
            }
        });
    }

    /**
     * store the endpoint extensions which are used as references.
     *
     * @param extensions {@link Map<String,Object>} object
     */
    public static void setOpenAPIDefinitionEndpointReferenceExtensions(Map<String, Object> extensions) {
        if (extensions != null && extensions.get(OpenAPIConstants.ENDPOINTS) != null) {
            try {
                TypeReference<List<Map<Object, Object>>> typeRef1 = new TypeReference<List<Map<Object, Object>>>() {

                };
                endPointReferenceExtensions = objectMapper
                        .convertValue(extensions.get(OpenAPIConstants.ENDPOINTS), typeRef1);
            } catch (IllegalArgumentException e) {
                throw new CLIRuntimeException(
                        "Open API \"" + OpenAPIConstants.ENDPOINTS + "\" extension format is " + "wrong : " + e
                                .getMessage(), e);
            }
        }
    }

    /**
     * validate the availability of endpoints.
     * If the api-level endpoints are not provided and there are resources with no endpoints assigned,
     * an exception will be thrown.
     *
     * @param openAPI         {@link OpenAPI} object
     * @param openAPIFilePath file path to openAPI definition
     * @param openAPIVersion the version of the open API used
     */
    private static void validateEndpointAvailability(OpenAPI openAPI, String openAPIFilePath, String openAPIVersion) {
        if (openAPI.getServers() != null && openAPI.getServers().get(0).getUrl() != null) {
            return;
        }
        if (openAPI.getExtensions().get(OpenAPIConstants.PRODUCTION_ENDPOINTS) != null ||
                openAPI.getExtensions().get(OpenAPIConstants.SANDBOX_ENDPOINTS) != null) {
            return;
        }

        boolean epsUnavailableForAll = openAPI.getPaths().entrySet().stream().anyMatch(path ->
                isResourceEpUnavailable(path.getValue().getGet()) ||
                        isResourceEpUnavailable(path.getValue().getPost()) ||
                        isResourceEpUnavailable(path.getValue().getPut()) ||
                        isResourceEpUnavailable(path.getValue().getTrace()) ||
                        isResourceEpUnavailable(path.getValue().getHead()) ||
                        isResourceEpUnavailable(path.getValue().getDelete()) ||
                        isResourceEpUnavailable(path.getValue().getPatch()) ||
                        isResourceEpUnavailable(path.getValue().getOptions())
        );

        if (epsUnavailableForAll) {
            String message;
            if (openAPISpec2.equals(openAPIVersion)) {
                message = "Either open API default attributes 'host' and 'basePath' values or ";
            } else {
                message = "Either open API default attribute 'servers' or ";
            }
            throw new CLIRuntimeException(
                    message + "wso2 specific extensions '" + OpenAPIConstants.PRODUCTION_ENDPOINTS + "' and '"
                            + OpenAPIConstants.SANDBOX_ENDPOINTS
                            + "' properties are not included under API Level in openAPI " + "definition '"
                            + openAPIFilePath + "'. Please include at least one of them under API Level or "
                            + "provide those properties for all the resources to overcome this issue.");
        }
    }

    private static boolean isResourceEpUnavailable(Operation operation) {
        if (operation != null && operation.getExtensions().get(OpenAPIConstants.PRODUCTION_ENDPOINTS) == null &&
                operation.getExtensions().get(OpenAPIConstants.SANDBOX_ENDPOINTS) == null) {
            return true;
        } else if (operation != null && operation.getServers() != null && operation.getServers().get(0) != null) {
            return operation.getServers().get(0).getUrl() != null;
        }
        return false;
    }

    /**
     * Get auth providers for given schema
     * @param schemas oas definition schemas
     * @param appSecurity security defined by the extension
     * @return list of auth providers
     */
    public static List<String> getAuthProviders(String schemas, ApplicationSecurity appSecurity) {
        List<String> authProviders = new ArrayList<>();
        // Support api manager application level security.
        // Give priority to extensions security types.
        if (appSecurity != null && !appSecurity.getSecurityTypes().isEmpty()) {
            for (String securityType : appSecurity.getSecurityTypes()) {
                if (OpenAPIConstants.APPLICATION_LEVEL_SECURITY.containsKey(securityType)) {
                    getAuthProvidersForSecurityType(
                            OpenAPIConstants.APPLICATION_LEVEL_SECURITY.get(securityType), authProviders);
                }
            }
        }
        // Note that if application security extension provided auth security types,
        // then swagger defined security schemes will be ignored.
        if (authProviders.isEmpty() && schemas != null) {
            String[] schemasArray = schemas.trim().split("\\s*,\\s*");
            for (String securityType : schemasArray) {
                getAuthProvidersForSecurityType(securityType, authProviders);
            }
        }
        return authProviders;
    }

    private static void getAuthProvidersForSecurityType(String securityType, List<String> authProviders) {
        if (securityType.equalsIgnoreCase(OpenAPIConstants.APISecurity.basic.name())) {
            if (!authProviders.contains(OpenAPIConstants.APISecurity.basic.name())) {
                authProviders.add(OpenAPIConstants.APISecurity.basic.name());
            }
        } else if (securityType.equalsIgnoreCase(OpenAPIConstants.APISecurity.apikey.name())) {
            if (!authProviders.contains(OpenAPIConstants.APISecurity.apikey.name())) {
                authProviders.add(OpenAPIConstants.APISecurity.apikey.name());
            }
        } else if (securityType.equalsIgnoreCase(OpenAPIConstants.APISecurity.oauth2.name())) {
            if (!authProviders.contains(OpenAPIConstants.APISecurity.oauth2.name())) {
                authProviders.add(OpenAPIConstants.APISecurity.oauth2.name());
                authProviders.add(OpenAPIConstants.APISecurity.jwt.name());
            }
        }
    }

     public static void addDefaultAuthProviders(List<String> authProviders) {
        authProviders.add(OpenAPIConstants.APISecurity.oauth2.name());
        authProviders.add(OpenAPIConstants.APISecurity.jwt.name());
    }

    private static String resolveTemplateBasePath(Map<String, Object> extensions, String version) {
        String basePath = extensions.get(OpenAPIConstants.BASEPATH).toString();
        return basePath.replace(OpenAPIConstants.VERSION_PLACEHOLDER, version);
    }


}
