package org.wso2.apimgt.gateway.cli.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.commons.lang3.StringUtils;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

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

}
