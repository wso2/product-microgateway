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
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.ResourceRepresentation;
import org.wso2.apimgt.gateway.cli.model.route.RouteEndpointConfig;

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

        //another purpose in here is to validate the openAPI definition
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
        api.setTransport(Arrays.asList("http", "https"));
        return api;
    }

    /**
     * list all the available resources from openAPI definition
     * @param projectName project Name
     * @param apiId api Id
     * @return list of string arrays {resource_id, resource name, method}
     */
    public static List<ResourceRepresentation> listResourcesFromSwaggerForAPI(String projectName, String apiId){

        List<ResourceRepresentation> resourceList = new ArrayList<>();
        JsonNode openApiNode = generateJsonNode(GatewayCmdUtils.getProjectSwaggerFilePath(projectName, apiId),
                true);
        addResourcesToListFromSwagger(openApiNode, resourceList);
        return resourceList;
    }


    private static void addResourcesToList(List<ResourceRepresentation> resourcesList, String apiName,
                                           String apiVersion, String resourceName, String method){
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setId(HashUtils.generateResourceId(apiName, apiVersion,resourceName, method));
        resource.setName(resourceName);
        resource.setMethod(method);
        resource.setApi(apiName);
        resource.setVersion(apiVersion);
        resourcesList.add(resource);
    }

    /**
     * Add resources from the provided openAPI definition to existing array list
     * @param apiDefNode   Api Definition as a JsonNode
     * @param resourcesList   String[] arrayList
     */
    private static void addResourcesToListFromSwagger(JsonNode apiDefNode, List<ResourceRepresentation> resourcesList){

        String apiName = apiDefNode.get("info").get("title").asText();
        String apiVersion = apiDefNode.get("info").get("version").asText();

        apiDefNode.get("paths").fields().forEachRemaining( e -> e.getValue().fieldNames().forEachRemaining(operation ->
                addResourcesToList(resourcesList, apiName, apiVersion, e.getKey(), operation)));
    }

    /**
     * List all the resources available in the project
     * @param projectName   project Name
     * @return    String[] Arraylist with all the available resources
     */
    public static List<ResourceRepresentation> getAllResources(String projectName){

        List<ResourceRepresentation> resourcesList = new ArrayList<>();

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
     * get the resource related information if the resource_id is given
     * @param projectName   project name
     * @param resource_id   resource id
     * @return resource object with api name, version, method and key
     */
    public static ResourceRepresentation getResource(String projectName, String resource_id){
        String projectAPIFilesPath = GatewayCmdUtils.getProjectAPIFilesDirectoryPath(projectName);
        ResourceRepresentation resource = new ResourceRepresentation();
        try{
            Files.walk(Paths.get(projectAPIFilesPath)).filter( path -> path.getFileName().toString().equals("swagger.json"))
                    .forEach( path -> {
                        JsonNode openApiNode = generateJsonNode(path.toString(), true);
                        String apiName = openApiNode.get("info").get("title").asText();
                        String apiVersion = openApiNode.get("info").get("version").asText();

                        openApiNode.get("paths").fields().forEachRemaining( e -> e.getValue().fieldNames()
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
        } catch (IOException e){
            throw new CLIInternalException("Error while navigating API Files directory.");
        }
        if(resource.getId() == null){
            return null;
        }
        return resource;
    }

    /**
     * read openAPI definition
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
     * @param api API object
     */
    public static void setAdditionalConfigsDevFirst(ExtendedAPI api) {
        String basePath = MgwDefinitionUtils.getBasePath(api.getName(), api.getVersion());
        MgwEndpointConfigDTO mgwEndpointConfigDTO =
                RouteUtils.convertToMgwServiceMap(MgwDefinitionUtils.getProdEndpointList(basePath),
                        MgwDefinitionUtils.getSandEndpointList(basePath));
        api.setEndpointConfigRepresentation(mgwEndpointConfigDTO);
        // 0th element represents the specific basepath
        api.setSpecificBasepath(basePath);
        api.setApiSecurity(MgwDefinitionUtils.getSecurity(basePath));
        api.setCorsConfiguration(MgwDefinitionUtils.getCorsConfiguration(basePath));
    }

    public static void setAdditionalConfig(ExtendedAPI api){
        RouteEndpointConfig endpointConfig = RouteUtils.parseEndpointConfig(api.getEndpointConfig(),
                api.getEndpointSecurity());
        api.setEndpointConfigRepresentation(RouteUtils.convertToMgwServiceMap(endpointConfig.getProdEndpointList(),
                endpointConfig.getSandboxEndpointList()));
        api.setSpecificBasepath(api.getContext() + "/" + api.getVersion());
    }


}
