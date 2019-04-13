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
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.mgwServiceMap.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class OpenAPICodegenUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(OpenAPICodegenUtils.class);

    private static final String openAPISpec2 = "2";
    private static final String openAPISpec3 = "3";

    /**
     * Generate API Id for a given OpenAPI definition.
     * @param apiDefPath path to OpenAPI definition
     * @return API Id
     */
    public static String generateAPIdForSwagger(String apiDefPath){

        //todo: optimize
        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);

        String apiName = openAPI.getInfo().getTitle();
        String apiVersion = openAPI.getInfo().getVersion();

        return HashUtils.generateAPIId(apiName, apiVersion);
    }

    /**
     * Generate JsonNode object for a given API definition.
     * @param apiDefinition API definition (as a file path or String content)
     * @param isFilePath   If the given api Definition is a file path
     * @return  JsonNode object for the api definition
     */
    private static JsonNode generateJsonNode(String apiDefinition, boolean isFilePath){
        try{
            if(isFilePath){
                //if filepath to the swagger is provided
                return objectMapper.readTree(new File(apiDefinition));
            }
            //if the raw string of the swagger is provided
            return objectMapper.readTree(apiDefinition);
        } catch (IOException e){
            throw new CLIRuntimeException("Api Definition cannot be parsed.");
        }
    }

    /**
     * Discover the openAPI version of the given API definition
     * @param apiDefinition API definition (as a file path or String content)
     * @param isFilePath If the given api Definition is a file path
     * @return openAPI version number (2 or 3)
     */
    private static String findSwaggerVersion(String apiDefinition, boolean isFilePath){

        JsonNode rootNode = generateJsonNode(apiDefinition, isFilePath);
        if(rootNode.has("swagger") && rootNode.get("swagger").asText().trim().startsWith("2")){
            //todo: introduce a constant for swagger version
            return openAPISpec2;
        }
        else if(rootNode.has("openapi") && rootNode.get("openapi").asText().trim().startsWith("3")){
            return openAPISpec3;
        }
        throw new CLIRuntimeException("Error while reading the swagger file, check again.");
    }

    /**
     * Extract the openAPI definition as String from an ExtendedAPI object.
     * @param api ExtendedAPI object
     * @return  openAPI definition as a String
     */
    static String generateSwaggerString(ExtendedAPI api){

        String swaggerVersion = findSwaggerVersion(api.getApiDefinition(), false);
        switch(swaggerVersion){
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
     * @param apiDefPath path to openAPI definition
     * @return String array {API_name, Version}
     */
    static String[] getAPINameVersionFromSwagger(String apiDefPath){
        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);
        return new String[] {openAPI.getInfo().getTitle(), openAPI.getInfo().getVersion()};

    }

    /**
     * get basePath from the openAPI definition
     * @param apiDefPath path to openAPI definition
     * @return basePath (if the swagger version is 2 and it includes )
     */
    public static String getBasePathFromSwagger(String apiDefPath){
        String swaggerVersion = findSwaggerVersion(apiDefPath, true);

        //openAPI version 2 contains basePath
        if(swaggerVersion.equals(openAPISpec2)){
            Swagger swagger = new SwaggerParser().read(apiDefPath);
            if(!StringUtils.isEmpty(swagger.getBasePath())){
                return swagger.getBasePath();
            }
        }
        return null;
    }

    /**
     * generate ExtendedAPI object from openAPI definition
     * @param apiDefPath path to openAPI definition
     * @return Extended API object
     */
    public static ExtendedAPI generateAPIFromOpenAPIDef(String apiDefPath){

        ExtendedAPI api;
        String apiId = UUID.randomUUID().toString();

        api = new ExtendedAPI();
        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);

        api.setId(apiId);
        api.setName(openAPI.getInfo().getTitle());
        api.setVersion(openAPI.getInfo().getVersion());
        api.setContext(getBasePathFromSwagger(apiDefPath));
        api.setTransport(Arrays.asList("http", "https"));
        return api;
    }

    /**
     * list all the available resources from openAPI definition
     * @param projectName project Name
     * @param apiId api Id
     * @return list of string arrays {resource_id, resource name, method}
     */
    public static List<String[]> listResourcesFromSwaggerForAPI(String projectName, String apiId){

        List<String[]> resourceList = new ArrayList<>();
        JsonNode openApiNode = generateJsonNode(GatewayCmdUtils.getProjectSwaggerFilePath(projectName, apiId),
                true);
        addResourcesToListFromSwagger(openApiNode, resourceList);
        return resourceList;
    }


    private static void addResourcesToList(List<String[]> resourcesList, String apiName,
                                           String apiVersion, String resource, String method){
        String[] row = new String[5];
        row[0] = HashUtils.generateResourceId(apiName, apiVersion,resource, method);
        row[1] = resource;
        row[2] = method;
        row[3] = apiName;
        row[4] = apiVersion;
        resourcesList.add(row);
    }

    /**
     * Add resources from the provided openAPI definition to existing array list
     * @param apiDefNode   Api Definition as a JsonNode
     * @param resourcesList   String[] arrayList
     */
    private static void addResourcesToListFromSwagger(JsonNode apiDefNode, List<String[]> resourcesList){

        String apiName = apiDefNode.get("info").get("title").asText();
        String apiVersion = apiDefNode.get("info").get("version").asText();

        apiDefNode.get("paths").fields().forEachRemaining( e -> {
            e.getValue().fieldNames().forEachRemaining( operation -> {
                addResourcesToList(resourcesList, apiName, apiVersion, e.getKey(), operation);
            });
        });
    }

    /**
     * List all the resources available in the project
     * @param projectName   project Name
     * @return    String[] Arraylist with all the available resources
     */
    public static List<String[]> getAllResources(String projectName){

        List<String[]> resourcesList = new ArrayList<>();

        String projectAPIFilesPath = GatewayCmdUtils.getProjectAPIFilesDirectoryPath(projectName);
        try{
            Files.walk(Paths.get(projectAPIFilesPath)).filter( path -> path.getFileName().toString().equals("swagger.json"))
                    .forEach( path -> {
                        JsonNode openApiNode = generateJsonNode(path.toString(), true);
                        OpenAPICodegenUtils.addResourcesToListFromSwagger(openApiNode,resourcesList);
                    });
            return resourcesList;
        } catch (IOException e){
            throw new CLIInternalException("Error while navigating API Files directory.");
        }

    }

    /**
     * read openAPI definition
     * @param filePath path to openAPI definition
     * @return openAPI as a String
     */
    public static String readApi(String filePath) {
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
     * @param projectName  project Name
     * @param api API object
     */
    public static void setAdditionalConfigs(String projectName, ExtendedAPI api) {
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        MgwEndpointConfigDTO mgwEndpointConfigDTO =
                RouteUtils.convertRouteToMgwServiceMap(RouteUtils.getGlobalEpConfig( apiId,
                        GatewayCmdUtils.getProjectRoutesConfFilePath(projectName)));
        api.setEndpointConfigRepresentation(mgwEndpointConfigDTO);
        // 0th element represents the specific basepath
        api.setSpecificBasepath(RouteUtils.getBasePath(apiId,
                GatewayCmdUtils.getProjectRoutesConfFilePath(projectName)) [0]);
        api.setApiSecurity(JsonProcessingUtils.getAPIMetadata(projectName, apiId).getSecurity());
        api.setCorsConfiguration(JsonProcessingUtils.getAPIMetadata(projectName, apiId).getCorsConfigurationDTO());
    }

}
