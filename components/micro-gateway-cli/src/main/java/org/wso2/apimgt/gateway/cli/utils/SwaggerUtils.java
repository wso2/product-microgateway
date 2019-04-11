/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SwaggerUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static String generateAPIdForSwagger(String apiDefPath){

        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);

        String apiName = openAPI.getInfo().getTitle();
        String apiVersion = openAPI.getInfo().getVersion();

        return HashUtils.generateAPIId(apiName, apiVersion);
    }

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

    private static String findSwaggerVersion(String apiDefinition, boolean isFilePath){

        JsonNode rootNode = generateJsonNode(apiDefinition, isFilePath);
        if(rootNode.has("swagger") && rootNode.get("swagger").asText().trim().startsWith("2")){
            //todo: introduce a constant for swagger version
            return "2";
        }
        else if(rootNode.has("openapi") && rootNode.get("openapi").asText().trim().startsWith("3")){
            return "3";
        }

        throw new CLIRuntimeException("Error while reading the swagger file, check again.");
    }

    //todo: check if this is required
    public static String generateSwaggerString(ExtendedAPI api){

        String swaggerVersion = findSwaggerVersion(api.getApiDefinition(), false);
        switch(swaggerVersion){
            case "2":
                Swagger swagger = new SwaggerParser().parse(api.getApiDefinition());
                //to bring the settings in API Manager
                swagger.setBasePath(api.getContext() + "/" + api.getVersion());
                return Json.pretty(swagger);
            case "3":
                return api.getApiDefinition();
            default:
                throw new CLIRuntimeException("Error: Swagger version is not identified");
        }
    }

    static String[] getAPINameVersionFromSwagger(String apiDefPath){

        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);
        return new String[] {openAPI.getInfo().getTitle(), openAPI.getInfo().getVersion()};

    }

    public static String getBasePathFromSwagger(String apiDefPath){
        String swaggerVersion = findSwaggerVersion(apiDefPath, true);

        if(swaggerVersion.equals("2")){
            Swagger swagger = new SwaggerParser().read(apiDefPath);
            if(!StringUtils.isEmpty(swagger.getBasePath())){
                return swagger.getBasePath();
            }
        }
        return null;
    }

    public static ExtendedAPI generateAPIFromOpenAPIDef(String apiDefPath){

        ExtendedAPI api;
        String apiId = UUID.randomUUID().toString(); //todo: clarify the purpose of this UUID ?

        api = new ExtendedAPI();
        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);

        api.setId(apiId);
        api.setName(openAPI.getInfo().getTitle());
        api.setVersion(openAPI.getInfo().getVersion());
        api.setContext(getBasePathFromSwagger(apiDefPath));
        api.setTransport(Arrays.asList("http", "https"));
        return api;
    }

    public static List<String[]> listResourcesFromSwagger(String apiDefPath){
        OpenAPI openAPI = new OpenAPIV3Parser().read(apiDefPath);
        LinkedHashMap<String, PathItem> pathList = openAPI.getPaths();

        List<String[]> resourceList = new ArrayList<>();
        String apiName = openAPI.getInfo().getTitle();
        String version = openAPI.getInfo().getVersion();

        pathList.entrySet().stream().forEach(e -> {
            //todo: add constants
            addResourcesToList(resourceList, e.getValue().getGet(), apiName, version, e.getKey(), "GET");
            addResourcesToList(resourceList, e.getValue().getPost(), apiName, version, e.getKey(), "POST");
            addResourcesToList(resourceList, e.getValue().getDelete(), apiName, version, e.getKey(), "DELETE");
            addResourcesToList(resourceList, e.getValue().getPut(), apiName, version, e.getKey(), "PUT");
            addResourcesToList(resourceList, e.getValue().getOptions(), apiName, version, e.getKey(), "OPTIONS");
            addResourcesToList(resourceList, e.getValue().getPatch(), apiName, version, e.getKey(), "PATCH");
        } );
        return resourceList;
    }

    private static void addResourcesToList(List<String[]> resourcesList, Operation operation, String apiName,
                                           String apiVersion, String resource, String method){
        if(operation != null){
            String[] row = new String[3];
            row[0] = HashUtils.generateResourceId(apiName, apiVersion,resource, method);
            row[1] = resource;
            row[2] = method;
            resourcesList.add(row);
        }
    }

}
