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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.util.Json;
import org.wso2.apimgt.gateway.cli.cmd.GatewayLauncherCmd;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.mgwServiceMap.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.mgwServiceMap.MgwEndpointDTO;
import org.wso2.apimgt.gateway.cli.model.mgwServiceMap.MgwEndpointListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.EndpointUrlTypeEnum;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.route.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RouteUtils {
    //todo: rename variable name
    private static final ObjectMapper OBJECT_MAPPER_YAML = new ObjectMapper(new YAMLFactory());
    //todo: set routesConfigPath as class variable
    private static final ObjectMapper OBJECT_MAPPER_JSON = new ObjectMapper();
    private static final String BASE_PATHS = "basePaths";
    private static final String GLOBAL_ENDPOINTS = "globalEndpoints";
    private static final String RESOURCES = "resources";
    private static final String GLOBAL_FUNCTION = "resources";
    private static JsonNode routesConfig;
    //todo: change accordingly
    private static String routesConfigPath;

    public static final String IN = "in";
    public static final String OUT = "out";
    public static final String FUNCTION_IN ="functionIn";
    public static final String FUNCTION_OUT ="functionOut";


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

    private static void addBasePath(JsonNode rootNode, String apiId, String basePath){

        JsonNode basePathsNode = rootNode.get(BASE_PATHS);
        String modifiedBasePath = basePath.startsWith("/") ? basePath : ( "/" + basePath);
        //todo: validate whether the basePath is already available
        //todo: validate basepath syntax
        ArrayNode arrayNode = ((ObjectNode) basePathsNode).putArray(apiId);
        arrayNode.add(modifiedBasePath);
    }

    private static void addBasePath(JsonNode rootNode, ExtendedAPI api){
        JsonNode basePathsNode = rootNode.get(BASE_PATHS);
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        ArrayNode arrayNode = ((ObjectNode) basePathsNode).putArray(apiId);
        arrayNode.add(api.getContext() + "/" + api.getVersion());
        if(api.getIsDefaultVersion()){
            arrayNode.add(api.getContext());
        }
    }

    private static void addGlobalEndpoint(JsonNode rootNode, String apiName, String apiVersion, String apiId,
                                   String endpointConfigJson){

        EndpointConfig endpointConfig = parseEndpointConfig(endpointConfigJson);
        APIRouteEndpointConfig apiEpConfig = new APIRouteEndpointConfig();
        apiEpConfig.setApiName(apiName);
        apiEpConfig.setApiVersion(apiVersion);
        apiEpConfig.setProdEndpointList(endpointConfig.getProdEndpointList());
        apiEpConfig.setSandboxEndpointList(endpointConfig.getSandboxEndpointList());
        addAPIRouteEndpointConfigAsGlobalEp(rootNode, apiId, apiEpConfig);
    }

    private static void addAPIRouteEndpointConfigAsGlobalEp(JsonNode rootNode, String apiId,
                                                            APIRouteEndpointConfig apiEpConfig){
        JsonNode globalEpsNode = rootNode.get(GLOBAL_ENDPOINTS);
        //todo: check if the apiId is unique
        ((ObjectNode) globalEpsNode).set(apiId, OBJECT_MAPPER_YAML.valueToTree(apiEpConfig));
    }

    private static void writeRoutesConfig(JsonNode routesConfig, String routesConfigPath){
        try{
            OBJECT_MAPPER_YAML.writeValue(new File(routesConfigPath), routesConfig);
            RouteUtils.routesConfig = routesConfig;
        }catch(IOException e){
            throw new CLIInternalException("Error while writing to the routes.yaml");
        }
    }


    private static JsonNode getRoutesConfig(String routesConfigPath){
        if(routesConfig != null){
            return routesConfig;
        }
        if(routesConfigPath == null){
            throw new CLIInternalException("routes.yaml is not provided");
        }
        try {
            routesConfig = OBJECT_MAPPER_YAML.readTree(new File(routesConfigPath));
        } catch (IOException e) {
            throw new CLIInternalException("Error while reading the routesConfiguration in path : " + routesConfigPath);
        }
        if(routesConfig == null){
            routesConfig = OBJECT_MAPPER_YAML.createObjectNode();
        }
        JsonNode basePathsNode = null;
        JsonNode globalEpsNode = null;
        JsonNode resourcesNode = null;
        JsonNode globalFunctionNode = null;

        if(!routesConfig.isNull()){
            basePathsNode = routesConfig.get(BASE_PATHS);
            globalEpsNode = routesConfig.get(GLOBAL_ENDPOINTS);
            resourcesNode = routesConfig.get(RESOURCES);
            globalFunctionNode = routesConfig.get(GLOBAL_FUNCTION);
        }

        if(basePathsNode == null){
            basePathsNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) routesConfig).set(BASE_PATHS, basePathsNode);
        }

        if(globalEpsNode == null){
            globalEpsNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) routesConfig).set(GLOBAL_ENDPOINTS, globalEpsNode);
        }

        if(resourcesNode == null){
            resourcesNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) routesConfig).set(RESOURCES, resourcesNode);
        }

        if(globalFunctionNode == null){
            globalFunctionNode = OBJECT_MAPPER_YAML.createObjectNode();
            ((ObjectNode) routesConfig).set(GLOBAL_FUNCTION, globalFunctionNode);
        }
        return routesConfig;
    }

    public static String[] getBasePath(String apiId, String routesConfigPath){

        JsonNode rootNode = getRoutesConfig(routesConfigPath);
        ArrayNode arrayNode = (ArrayNode) rootNode.get(BASE_PATHS).get(apiId);

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
        JsonNode globalEpConfig = rootNode.get(GLOBAL_ENDPOINTS).get(apiId);
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

    public static List<String[]> listApis(String projectName){
        JsonNode rootNode = getRoutesConfig(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));
        JsonNode basePathsNode = rootNode.get(BASE_PATHS);
        Iterator<Map.Entry<String, JsonNode>> fields = basePathsNode.fields();
        List<String[]> apis = new ArrayList<>();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            for(JsonNode value: field.getValue()){
                String[] row = new String[4];
                row[0] = key;
                row[1] = rootNode.get(GLOBAL_ENDPOINTS).get(key).get("name").asText();
                row[2] = rootNode.get(GLOBAL_ENDPOINTS).get(key).get("version").asText();
                row[3] = value.asText();
                apis.add(row);
            }
        }
        return apis;
    }

    public static void saveResourceRoute(String resourceId, String endpointConfigJson, String routesConfigPath){
        //todo: resolve adding method name and resource name if necessary
        EndpointConfig endpointConfig =
                parseEndpointConfig(endpointConfigJson);
        JsonNode rootNode = getRoutesConfig(routesConfigPath);
        JsonNode resourcesNode = rootNode.get(RESOURCES);
        //todo: validate if the resource_id already exists
        ((ObjectNode) resourcesNode).set(resourceId, OBJECT_MAPPER_YAML.valueToTree(endpointConfig));
        writeRoutesConfig(rootNode, routesConfigPath);
    }

    private static EndpointConfig parseEndpointConfig(String endpointConfigJson){
        //todo: validate the endpointConfig
        EndpointConfig endpointConfig;
        try {
            //todo: bring yaml file
            endpointConfig = OBJECT_MAPPER_JSON.readValue(endpointConfigJson, EndpointConfig.class);
            return  endpointConfig;
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while parsing the provided endpointConfig Json");
        }
    }

    public static MgwEndpointConfigDTO convertRouteToMgwServiceMap(EndpointConfig routeEndpointConfig){
        MgwEndpointConfigDTO endpointConfigDTO = new MgwEndpointConfigDTO();

        MgwEndpointListDTO prod = null;
        MgwEndpointListDTO sandbox = null;

        if(routeEndpointConfig.getProdEndpointList() != null &&
                routeEndpointConfig.getProdEndpointList().getEndpoints() != null){
            prod = new MgwEndpointListDTO();
            prod.setEndpointUrlType(EndpointUrlTypeEnum.PROD);
            prod.setType(routeEndpointConfig.getProdEndpointList().getType());
            ArrayList<MgwEndpointDTO> prodEpList = new ArrayList<>();
            for (String ep : routeEndpointConfig.getProdEndpointList().getEndpoints()){
                prodEpList.add(new MgwEndpointDTO(ep));
            }
            prod.setEndpoints(prodEpList);
        }

        if(routeEndpointConfig.getSandboxEndpointList() != null &&
                routeEndpointConfig.getSandboxEndpointList().getEndpoints() != null){
            sandbox = new MgwEndpointListDTO();
            sandbox.setEndpointUrlType(EndpointUrlTypeEnum.SAND);
            sandbox.setType(routeEndpointConfig.getSandboxEndpointList().getType());
            ArrayList<MgwEndpointDTO> sandEpList = new ArrayList<>();
            for (String ep : routeEndpointConfig.getSandboxEndpointList().getEndpoints()){
                sandEpList.add(new MgwEndpointDTO(ep));
            }
            sandbox.setEndpoints(sandEpList);
        }

        endpointConfigDTO.setProdEndpointList(prod);
        endpointConfigDTO.setSandboxEndpointList(sandbox);

        return endpointConfigDTO;
    }

    public static MgwEndpointConfigDTO getResourceEpConfig(String apiName, String apiVersion, String resource,
                                                           String method){
        JsonNode rootNode = getRoutesConfig(routesConfigPath);
        String resourceId = HashUtils.generateResourceId(apiName, apiVersion, resource, method);
        try {
            JsonNode resourceNode = rootNode.get(RESOURCES).get(resourceId);
            if(resourceNode != null){
                EndpointConfig endpointConfig = OBJECT_MAPPER_YAML.readValue(rootNode.get(RESOURCES).get(resourceId)
                        .toString(), EndpointConfig.class);
                return convertRouteToMgwServiceMap(endpointConfig);
            }
            else{
                return null;
            }
        } catch (IOException e) {
            throw new CLIInternalException("EndpointConfig for the given resource cannot be parsed");
        }
    }

    public static void setRoutesConfigPath(String routesConfigPath) {
        RouteUtils.routesConfigPath = routesConfigPath;
    }

    public static void addFunction(String function, String type, String apiID, String routeConfigPath,
                                   String projectName) {

        APIRouteEndpointConfig api = RouteUtils.getGlobalEpConfig(apiID,
                GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));

        if (type.equals(IN)) {
            api.setFunctionIn(function);
        } else if (type.equals(OUT)) {
            api.setFunctionOut(function);
        }
        JsonNode jn = getRoutesConfig(routeConfigPath);
        addAPIRouteEndpointConfigAsGlobalEp(jn, apiID, api);
        writeRoutesConfig(jn, routeConfigPath);
    }

    public static void AddGlobalFunction(String routeConfigPath, String function, String type) {

        JsonNode rootNode = getRoutesConfig(routeConfigPath);
        JsonNode jsonNode = rootNode.get(GLOBAL_FUNCTION);

        if (type.equals(IN)) {
            ((ObjectNode) jsonNode).put(FUNCTION_IN, function);
        }
        if (type.equals(OUT)) {
            ((ObjectNode) jsonNode).put(FUNCTION_OUT, function);
        }

        writeRoutesConfig(rootNode, routeConfigPath);

    }
}
