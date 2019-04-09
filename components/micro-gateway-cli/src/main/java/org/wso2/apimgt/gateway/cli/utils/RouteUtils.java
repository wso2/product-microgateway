package org.wso2.apimgt.gateway.cli.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.util.Json;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.route.APIRouteEndpointConfig;
import org.wso2.apimgt.gateway.cli.model.route.EndpointConfig;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;
import org.wso2.apimgt.gateway.cli.model.route.EndpointType;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RouteUtils {
    //todo: rename variable name
    private static final ObjectMapper OBJECT_MAPPER_YAML = new ObjectMapper(new YAMLFactory());
    //todo: set routesConfigPath as class variable
    private static final ObjectMapper OBJECT_MAPPER_JSON = new ObjectMapper();

    private static final String IN = "in";
    private static final String OUT = "out";

    public static void saveGlobalEpAndBasepath(String apiDefPath, String routesConfigPath, String basePath,
                                        String endpointConfigJson){
        String apiId = SwaggerUtils.generateAPIdForSwagger(apiDefPath);
        String[] apiNameAndVersion = SwaggerUtils.getAPINameVersionFromSwagger(apiDefPath);
        String apiName = apiNameAndVersion[0];
        String apiVersion = apiNameAndVersion[1];

        JsonNode routesConfig = getRoutesConfig(routesConfigPath);
        addBasePath(routesConfig, apiId, basePath);
        addGlobalEndpoint(routesConfig, apiName, apiVersion, apiId, endpointConfigJson);
        writeRoutesConfig(routesConfig, routesConfigPath);
    }

    public static void saveGlobalEpAndBasepath(List<ExtendedAPI> apiList, String routesConfigPath) {
        JsonNode routesConfig = getRoutesConfig(routesConfigPath);
        for(ExtendedAPI api : apiList){
            APIRouteEndpointConfig apiEpConfig = new APIRouteEndpointConfig();
            apiEpConfig.setApiName(api.getName());
            apiEpConfig.setApiVersion(api.getVersion());

            EndpointConfig endpointConfig = getEndpointConfig(api.getEndpointConfig(), api.getEndpointSecurity());

            apiEpConfig.setProdEndpointList(endpointConfig.getProdEndpointList());
            apiEpConfig.setSandboxEndpointList(endpointConfig.getSandboxEndpointList());

            String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
            addBasePath(routesConfig, api);

            addAPIRouteEndpointConfigAsGlobalEp(routesConfig, apiId, apiEpConfig);
        }
        writeRoutesConfig(routesConfig, routesConfigPath);
    }

//    //todo: decide whether we are going to use same endpointConfig json structure or not
//    private static EndpointListRouteDTO[] convertEndpointConfig(EndpointConfig endpointConfig, APIEndpointSecurityDTO
//            securityDTO){
//        EndpointListRouteDTO prodList = new EndpointListRouteDTO();
//        EndpointListRouteDTO sandboxList = new EndpointListRouteDTO();
//
//        try {
//            prodList.setType(EndpointType.valueOf(endpointConfig.getEndpointType()));
//            sandboxList.setType(EndpointType.valueOf(endpointConfig.getEndpointType()));
//        } catch (IllegalArgumentException e){
//            throw new CLIRuntimeException("The provided Endpoint type is not valid.", e);
//        }
//
//        //all the endpoints are added to same list to make structure simple
//        for(Endpoint endpoint: endpointConfig.getProdEndpoints().getEndpoints()){
//            prodList.addEndpoint(endpoint.getEndpointUrl());
//        }
//        for(Endpoint endpoint: endpointConfig.getProdFailoverEndpoints().getEndpoints()){
//            prodList.addEndpoint(endpoint.getEndpointUrl());
//        }
//        for(Endpoint endpoint: endpointConfig.getSandEndpoints().getEndpoints()){
//            sandboxList.addEndpoint(endpoint.getEndpointUrl());
//        }
//        for(Endpoint endpoint: endpointConfig.getSandFailoverEndpoints().getEndpoints()){
//            sandboxList.addEndpoint(endpoint.getEndpointUrl());
//        }
//
//        prodList.setSecurityConfig(securityDTO);
//        sandboxList.setSecurityConfig(securityDTO);
//
//        return new EndpointListRouteDTO[]{prodList, sandboxList};
//    }

    private static void addBasePath(JsonNode rootNode, String apiId, String basePath){

        JsonNode basePathsNode = rootNode.get("basepaths");
        String modifiedBasePath = basePath.startsWith("/") ? basePath : ( "/" + basePath);
        //todo: validate whether the basePath is already available
        //todo: validate basepath syntax
        ArrayNode arrayNode = ((ObjectNode) basePathsNode).putArray(apiId);
        arrayNode.add(modifiedBasePath);
    }

    private static void addBasePath(JsonNode rootNode, ExtendedAPI api){
        JsonNode basePathsNode = rootNode.get("basepaths");
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        ArrayNode arrayNode = ((ObjectNode) basePathsNode).putArray(apiId);
        arrayNode.add(api.getContext() + "/" + api.getVersion());
        if(api.getIsDefaultVersion()){
            arrayNode.add(api.getContext());
        }
    }

    private static void addGlobalEndpoint(JsonNode rootNode, String apiName, String apiVersion, String apiId,
                                   String endpointConfigJson){

        //todo: validate the endpointConfig
        EndpointConfig endpointConfig;
        try {
            //todo: bring yaml file
            endpointConfig = OBJECT_MAPPER_JSON.readValue(endpointConfigJson, EndpointConfig.class);
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while parsing the provided endpointConfig Json");
        }

        APIRouteEndpointConfig apiEpConfig = new APIRouteEndpointConfig();
        apiEpConfig.setApiName(apiName);
        apiEpConfig.setApiVersion(apiVersion);
        apiEpConfig.setProdEndpointList(endpointConfig.getProdEndpointList());
        apiEpConfig.setSandboxEndpointList(endpointConfig.getSandboxEndpointList());
        addAPIRouteEndpointConfigAsGlobalEp(rootNode, apiId, apiEpConfig);
    }

    private static void addAPIRouteEndpointConfigAsGlobalEp(JsonNode rootNode, String apiId,
                                                            APIRouteEndpointConfig apiEpConfig){
        JsonNode globalEpsNode = rootNode.get("global_endpoints");
        //todo: check if the apiId is unique
        ((ObjectNode) globalEpsNode).set(apiId, OBJECT_MAPPER_YAML.valueToTree(apiEpConfig));
    }

    private static void writeRoutesConfig(JsonNode routesConfig, String routesConfigPath){
        try{
            OBJECT_MAPPER_YAML.writeValue(new File(routesConfigPath), routesConfig);
        }catch(IOException e){
            throw new CLIInternalException("Error while writing to the routes.yaml");
        }
    }


    private static JsonNode getRoutesConfig(String routesConfigPath){
        if(routesConfigPath == null){
            throw new CLIInternalException("routes.yaml is not provided");
        }
        JsonNode rootNode;
        try {
            rootNode = OBJECT_MAPPER_YAML.readTree(new File(routesConfigPath));
        } catch (IOException e) {
            throw new CLIInternalException("Error while reading the routesConfiguration in path : " + routesConfigPath);
        }
        if(rootNode == null){
            rootNode = OBJECT_MAPPER_YAML.createObjectNode();
        }
        JsonNode basePathsNode = null;
        JsonNode globalEpsNode = null;
        JsonNode resourcesNode = null;
        JsonNode globalFunctionNode = null;

        if(!rootNode.isNull()){
            basePathsNode = rootNode.get("basepaths");
            globalEpsNode = rootNode.get("global_endpoints");
            resourcesNode = rootNode.get("resources");
            globalFunctionNode = rootNode.get("global_function");
        }

        if(basePathsNode == null){
            basePathsNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) rootNode).set("basepaths", basePathsNode);
        }

        if(globalEpsNode == null){
            globalEpsNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) rootNode).set("global_endpoints", globalEpsNode);
        }

        if(resourcesNode == null){
            resourcesNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) rootNode).set("resources", resourcesNode);
        }

        if(globalFunctionNode == null){
            globalFunctionNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) rootNode).set("global_function", globalFunctionNode);
        }
        return rootNode;
    }

    public static String[] getBasePath(String apiName, String apiVersion, String routesConfigPath){
        String apiId = HashUtils.generateAPIId(apiName, apiVersion);
        JsonNode rootNode = getRoutesConfig(routesConfigPath);
        ArrayNode arrayNode = (ArrayNode) rootNode.get("basepaths").get(apiId);

        if(arrayNode.size() == 2){
            return new String[] {arrayNode.get(0).asText(), arrayNode.get(1).asText()};
        }

        return new String[] {arrayNode.get(0).asText()};
    }

    public static APIRouteEndpointConfig getGlobalEpConfig(String apiName, String apiVersion, String routesConfigPath){
        String apiId = HashUtils.generateAPIId(apiName, apiVersion);
        return getGlobalEpConfig(apiId, routesConfigPath);
    }

    public static APIRouteEndpointConfig getGlobalEpConfig(String apiId, String routesConfigPath){
        JsonNode rootNode = getRoutesConfig(routesConfigPath);
        JsonNode globalEpConfig = rootNode.get("global_endpoints").get(apiId);
        APIRouteEndpointConfig apiRouteEndpointConfig;

        try {
            apiRouteEndpointConfig = OBJECT_MAPPER_YAML.readValue(globalEpConfig.toString(),
                    APIRouteEndpointConfig.class);
        } catch (IOException e) {
            throw new CLIInternalException("Error while mapping Global Endpoint JsonNode object to " +
                    "APIRouteEndpointConfig object");
        }
        return apiRouteEndpointConfig;
    }

    private static EndpointConfig getEndpointConfig(String endpointConfigJson, APIEndpointSecurityDTO endpointSecurity){

        EndpointConfig endpointconfig = new EndpointConfig();
        EndpointListRouteDTO prodEndpointConfig = new EndpointListRouteDTO();
        EndpointListRouteDTO sandEndpointConfig = new EndpointListRouteDTO();

        //set securityConfig to the both environments
        prodEndpointConfig.setSecurityConfig(endpointSecurity);
        sandEndpointConfig.setSecurityConfig(endpointSecurity);

        JsonNode rootNode;
        try {
            rootNode = OBJECT_MAPPER_JSON.readTree(endpointConfigJson);
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while parsing the endpointConfig JSON string");
        }

        JsonNode endpointTypeNode = rootNode.get(RESTServiceConstants.ENDPOINT_TYPE);

        String endpointType = endpointTypeNode.asText();

        if (RESTServiceConstants.HTTP.equalsIgnoreCase(endpointType) || RESTServiceConstants.FAILOVER.
                equalsIgnoreCase(endpointType)) {

            JsonNode prodEndpointNode = rootNode.get(RESTServiceConstants.PRODUCTION_ENDPOINTS);

            if (prodEndpointNode != null) {
                prodEndpointConfig.addEndpoint(prodEndpointNode.get(RESTServiceConstants.URL).asText());
            }

            JsonNode sandEndpointNode = rootNode.get(RESTServiceConstants.SANDBOX_ENDPOINTS);
            if (sandEndpointNode != null) {
                sandEndpointConfig.addEndpoint(sandEndpointNode.get(RESTServiceConstants.URL).asText());
            }

            if (RESTServiceConstants.FAILOVER.equalsIgnoreCase(endpointType)) {
                /* Due to the limitation in provided json from Publisher, both production and sandbox environments are
                identified as the same type*/
                prodEndpointConfig.setType(EndpointType.failover);
                sandEndpointConfig.setType(EndpointType.failover);

                //Adding additional production/sandbox failover endpoints
                JsonNode prodFailoverEndpointNode = rootNode.withArray(RESTServiceConstants.PRODUCTION_FAILOVERS);
                if (prodFailoverEndpointNode != null) {
                    for (JsonNode node : prodFailoverEndpointNode) {
                        prodEndpointConfig.addEndpoint(node.get(RESTServiceConstants.URL).asText());
                    }
                }

                JsonNode sandFailoverEndpointNode = rootNode.withArray(RESTServiceConstants.SANDBOX_FAILOVERS);
                if (sandFailoverEndpointNode != null) {
                    for (JsonNode node : sandFailoverEndpointNode) {
                        sandEndpointConfig.addEndpoint(node.get(RESTServiceConstants.URL).asText());
                    }
                }
            }
            else{
                prodEndpointConfig.setType(EndpointType.http);
                sandEndpointConfig.setType(EndpointType.http);
            }
        } else if (RESTServiceConstants.LOAD_BALANCE.equalsIgnoreCase(endpointType)) {

            prodEndpointConfig.setType(EndpointType.load_balance);
            sandEndpointConfig.setType(EndpointType.load_balance);

            JsonNode prodEndpoints = rootNode.withArray(RESTServiceConstants.PRODUCTION_ENDPOINTS);
            if (prodEndpoints != null) {
                for (JsonNode node : prodEndpoints) {
                    prodEndpointConfig.addEndpoint(node.get(RESTServiceConstants.URL).asText());
                }
            }

            JsonNode sandboxEndpoints = rootNode.withArray(RESTServiceConstants.SANDBOX_ENDPOINTS);
            if (sandboxEndpoints != null) {
                for (JsonNode node : sandboxEndpoints) {
                    sandEndpointConfig.addEndpoint(node.get(RESTServiceConstants.URL).asText());
                }
            }
        }

        if(prodEndpointConfig.getEndpoints() != null && prodEndpointConfig.getEndpoints().size()>0){
            endpointconfig.setProdEndpointList(prodEndpointConfig);
        }

        if(sandEndpointConfig.getEndpoints() != null && sandEndpointConfig.getEndpoints().size()>0){
            endpointconfig.setSandboxEndpointList(sandEndpointConfig);
        }

        return endpointconfig;
    }

    public static void addFunction(String function, String type, String apiID, String routeConfigPath, String projectName) {

        APIRouteEndpointConfig api = RouteUtils.getGlobalEpConfig(apiID, GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));

        if (type.equals("in")) {
            api.setFunctionIn(function);
        } else if (type.equals("out")) {
            api.setFunctionOut(function);
        }
        JsonNode jn = getRoutesConfig(routeConfigPath);
        addAPIRouteEndpointConfigAsGlobalEp(jn, apiID, api);
        writeRoutesConfig(jn, routeConfigPath);
    }

    public static void AddGlobalFunction(String routeConfigPath, String function, String type) {

        JsonNode rootNode = getRoutesConfig(routeConfigPath);
        JsonNode jsonNode = rootNode.get("global_function");

        if (type.equals(IN)) {
            ((ObjectNode) jsonNode).put("functionIn", function);
        }
        if (type.equals(OUT)) {
            ((ObjectNode) jsonNode).put("functionOut", function);
        }

        writeRoutesConfig(rootNode, routeConfigPath);

    }
}
