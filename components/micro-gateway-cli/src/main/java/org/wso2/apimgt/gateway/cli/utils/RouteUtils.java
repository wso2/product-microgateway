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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointDTO;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointListDTO;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class RouteUtils {
    private static final ObjectMapper OBJECT_MAPPER_YAML = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper OBJECT_MAPPER_JSON = new ObjectMapper();
    private static final String BASE_PATHS = "basePaths";
    private static final String GLOBAL_ENDPOINTS = "globalEndpoints";
    private static final String RESOURCES = "resources";
    private static final String GLOBAL_FUNCTION = "resources";
    private static JsonNode routesConfig;
    private static String routesConfigPath;
    public static final String IN = "in";
    public static final String OUT = "out";
    public static final String FUNCTION_IN ="functionIn";
    public static final String FUNCTION_OUT ="functionOut";

    /**
     * save endpoint configuration and basePath of API for developer first approach
     * @param apiDefPath   Path to API Definition
     * @param basePath    BasePath
     * @param endpointConfigJson  Endpoint configuration Json as a String
     */
    public static void saveGlobalEpAndBasepath(String apiDefPath, String basePath, String endpointConfigJson){
        String apiId = OpenAPICodegenUtils.generateAPIdForSwagger(apiDefPath);
        String[] apiNameAndVersion = OpenAPICodegenUtils.getAPINameVersionFromSwagger(apiDefPath);


        String apiName = apiNameAndVersion[0];
        String apiVersion = apiNameAndVersion[1];

        JsonNode routesConfig = getRoutesConfig();
        addBasePath(routesConfig, apiId, basePath);
        addGlobalEndpoint(routesConfig, apiName, apiVersion, apiId, endpointConfigJson);
        writeRoutesConfig(routesConfig);
    }

    /**
     * save endpoint configuration and basePath of API
     * @param apiList List of APIs
     */
    public static void saveGlobalEpAndBasepath(List<ExtendedAPI> apiList, boolean isForceFully) {
        JsonNode routesConfig = getRoutesConfig();
        for(ExtendedAPI api : apiList){
            String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());

            if(hasApiInRoutesConfig(apiId) && !isForceFully){
                throw GatewayCmdUtils.createUsageException("The provided API id '" + apiId + "' already " +
                        "has an endpointConfiguration. use -f or --force to forcefully update the " +
                        "endpointConfiguration");
            }
            APIRouteEndpointConfig apiEpConfig = new APIRouteEndpointConfig();
            apiEpConfig.setApiName(api.getName());
            apiEpConfig.setApiVersion(api.getVersion());

            RouteEndpointConfig endpointConfig = parseEndpointConfig(api.getEndpointConfig(), api.getEndpointSecurity());

            apiEpConfig.setProdEndpointList(endpointConfig.getProdEndpointList());
            apiEpConfig.setSandboxEndpointList(endpointConfig.getSandboxEndpointList());

            addBasePath(routesConfig, api);

            addAPIRouteEndpointConfigAsGlobalEp(routesConfig, apiId, apiEpConfig);
        }
        writeRoutesConfig(routesConfig);
    }

    /**
     * Add basePath to the JsonNode representation of routes configuration (developer first approach)
     * @param rootNode   Routes configuration JsonNode
     * @param apiId      API Id
     * @param basePath      basePath
     */
    private static void addBasePath(JsonNode rootNode, String apiId, String basePath){

        JsonNode basePathsNode = rootNode.get(BASE_PATHS);
        String modifiedBasePath = basePath.startsWith("/") ? basePath : ( "/" + basePath);
        validateBasePath(basePathsNode, modifiedBasePath);
        ArrayNode arrayNode = ((ObjectNode) basePathsNode).putArray(apiId);
        arrayNode.add(modifiedBasePath);
    }

    /**
     * Add basePath to the JsonNode representation of routes configuration
     * @param rootNode   Routes configuration JsonNode
     * @param api    ExtendedAPI object
     */
    private static void addBasePath(JsonNode rootNode, ExtendedAPI api){
        JsonNode basePathsNode = rootNode.get(BASE_PATHS);
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        String basePath = api.getContext() + "/" + api.getVersion();
        validateBasePath(basePathsNode, basePath);
        ArrayNode arrayNode = ((ObjectNode) basePathsNode).putArray(apiId);
        arrayNode.add(basePath);
        if(api.getIsDefaultVersion()){
            validateBasePath(basePathsNode, api.getContext());
            arrayNode.add(api.getContext());
        }
    }

    /**
     * check if the basePath is available (basePath needs to be unique) if it is not available Runtime exception throws
     * @param basePathsNode     Basepaths node of routesConfig JsonNode
     * @param basePath          BasePath
     */
    private static void validateBasePath(JsonNode basePathsNode, String basePath){
        AtomicBoolean isAvailable = new AtomicBoolean(true);
        basePathsNode.fields().forEachRemaining( api -> {
            for(JsonNode basePathEntry : api.getValue()){
                if(basePathEntry.asText().equals(basePath)){
                    isAvailable.set(false);
                }
            }
        });
        if(!isAvailable.get()){
            throw new CLIRuntimeException("Error: The provided basePath: \"" + basePath +
                    "\" is already used. Try another basePath");
        }
    }

    /**
     * Add API level endpoint Configuration
     * @param rootNode      Routes configuration JsonNode
     * @param apiName       API name
     * @param apiVersion    API version
     * @param apiId         API Id
     * @param endpointConfigJson    Endpoint configuration Json as String
     */
    private static void addGlobalEndpoint(JsonNode rootNode, String apiName, String apiVersion, String apiId,
                                   String endpointConfigJson){

        RouteEndpointConfig endpointConfig = parseEndpointConfig(endpointConfigJson);
        APIRouteEndpointConfig apiEpConfig = new APIRouteEndpointConfig();
        apiEpConfig.setApiName(apiName);
        apiEpConfig.setApiVersion(apiVersion);
        apiEpConfig.setProdEndpointList(endpointConfig.getProdEndpointList());
        apiEpConfig.setSandboxEndpointList(endpointConfig.getSandboxEndpointList());
        addAPIRouteEndpointConfigAsGlobalEp(rootNode, apiId, apiEpConfig);
    }

    /**
     * Add API level endpoint configuration to the Routes Configuration JsonNode
     * @param rootNode      Routes configuration JsonNode
     * @param apiId         API Id
     * @param apiEpConfig   RouteEndpointConfig object (Endpoint configuration)
     */
    private static void addAPIRouteEndpointConfigAsGlobalEp(JsonNode rootNode, String apiId,
                                                            APIRouteEndpointConfig apiEpConfig){
        JsonNode globalEpsNode = rootNode.get(GLOBAL_ENDPOINTS);
        ((ObjectNode) globalEpsNode).set(apiId, OBJECT_MAPPER_YAML.valueToTree(apiEpConfig));
    }

    /**
     * Write the RoutesConfiguration JsonNode Object to routes.yaml file
     * @param routesConfig  Routes configuration JsonNode
     */
    private static void writeRoutesConfig(JsonNode routesConfig){
        try{
            OBJECT_MAPPER_YAML.writeValue(new File(routesConfigPath), routesConfig);
            RouteUtils.routesConfig = routesConfig;
        }catch(IOException e){
            throw new CLIInternalException("Error while writing to the routes.yaml");
        }
    }

    /**
     * Get routes configuration as a JsonNode Object
     * if the routes configuration file exists, we take that
     * @return Routes Configuration as a JsonNode
     */
    private static JsonNode getRoutesConfig(){
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

    /**
     * get basePath of an API
     * @param apiId     API Id
     * @return          BasePath as a String
     */
    public static String[] getBasePath(String apiId){

        JsonNode rootNode = getRoutesConfig();
        ArrayNode arrayNode = (ArrayNode) rootNode.get(BASE_PATHS).get(apiId);

        if(arrayNode.size() == 2){
            return new String[] {arrayNode.get(0).asText(), arrayNode.get(1).asText()};
        }
        return new String[] {arrayNode.get(0).asText()};
    }

    /**
     * get Global EndpointConfig of an API
     * @param apiId     API Id
     * @return          API Level Endpoint Configuration as a APIRouteEndpointConfig
     */
    static APIRouteEndpointConfig getGlobalEpConfig(String apiId){
        JsonNode rootNode = getRoutesConfig();
        JsonNode globalEpConfig = rootNode.get(GLOBAL_ENDPOINTS).get(apiId);
        APIRouteEndpointConfig apiRouteEndpointConfig;

        if(globalEpConfig == null){
            return null;
        }

        try {
            apiRouteEndpointConfig = OBJECT_MAPPER_YAML.readValue(globalEpConfig.toString(),
                    APIRouteEndpointConfig.class);
        } catch (IOException e) {
            throw new CLIInternalException("Error while mapping Global Endpoint JsonNode object to " +
                    "APIRouteEndpointConfig object");
        }
        return apiRouteEndpointConfig;
    }

    /**
     * To parse the Endpoint Configuration received from API Manager to RouteEndpointConfig Object
     * @param endpointConfigJson    Endpoint Configuration Json received from Publisher API
     * @param endpointSecurity      Endpoint Security details received from Publisher API
     * @return                      RouteEndpointConfig object
     */
    public static RouteEndpointConfig parseEndpointConfig(String endpointConfigJson, APIEndpointSecurityDTO endpointSecurity){

        RouteEndpointConfig endpointConfig = new RouteEndpointConfig();
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
            endpointConfig.setProdEndpointList(prodEndpointConfig);
        }

        if(sandEndpointConfig.getEndpoints() != null && sandEndpointConfig.getEndpoints().size()>0){
            endpointConfig.setSandboxEndpointList(sandEndpointConfig);
        }

        return endpointConfig;
    }

    /**
     * List all the available APIs
     * @return All the available APIs {api-Id, name, version, basePath}
     */
    public static List<String[]> listApis(){
        JsonNode rootNode = getRoutesConfig();
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

    /**
     * save the endpoint configuration for a given resource
     * @param resourceId            resource Id
     * @param endpointConfigJson    Endpoint Configuration Json
     */
    public static void saveResourceRoute(String resourceId, String endpointConfigJson){
        //todo: resolve adding method name and resource name if necessary
        RouteEndpointConfig endpointConfig =
                parseEndpointConfig(endpointConfigJson);
        JsonNode rootNode = getRoutesConfig();
        JsonNode resourcesNode = rootNode.get(RESOURCES);
        ((ObjectNode) resourcesNode).set(resourceId, OBJECT_MAPPER_YAML.valueToTree(endpointConfig));
        writeRoutesConfig(rootNode);
    }

    private static RouteEndpointConfig parseEndpointConfig(String endpointConfigJson){
        RouteEndpointConfig endpointConfig;
        try {
            if(endpointConfigJson.startsWith("{")){
                endpointConfig = OBJECT_MAPPER_JSON.readValue(endpointConfigJson, RouteEndpointConfig.class);
            }else{
                endpointConfig = OBJECT_MAPPER_YAML.readValue(endpointConfigJson, RouteEndpointConfig.class);
            }
            return  endpointConfig;
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while parsing the provided endpointConfig Json");
        }
    }

    /**
     * Convert the RouteEndpointConfig object to MgwEndpointConfigDTO for the ease of source code generation
     * @param
     * @return                      MgeEndpointConfig Object corresponding to provided routeEndpointConfig
     */
    static MgwEndpointConfigDTO convertToMgwServiceMap(EndpointListRouteDTO prodEpListDTO, EndpointListRouteDTO
            sandEpListDTO) {
        MgwEndpointConfigDTO endpointConfigDTO = new MgwEndpointConfigDTO();

        MgwEndpointListDTO prod = null;
        MgwEndpointListDTO sandbox = null;

        if (prodEpListDTO != null) {
            prod = new MgwEndpointListDTO();
            prod.setEndpointUrlType(EndpointUrlTypeEnum.PROD);
            setEndpointType(prodEpListDTO, prod);
            setEndpointUrls(prodEpListDTO, prod);
            prod.setSecurityConfig(prodEpListDTO.getSecurityConfig());
            prod.setName(prodEpListDTO.getName());
        }

        if (sandEpListDTO != null) {
            sandbox = new MgwEndpointListDTO();
            sandbox.setEndpointUrlType(EndpointUrlTypeEnum.SAND);
            setEndpointType(sandEpListDTO, sandbox);
            setEndpointUrls(sandEpListDTO, sandbox);
            sandbox.setSecurityConfig(sandEpListDTO.getSecurityConfig());
            sandbox.setName(sandEpListDTO.getName());
        }

        endpointConfigDTO.setProdEndpointList(prod);
        endpointConfigDTO.setSandboxEndpointList(sandbox);

        return endpointConfigDTO;
    }

    /**
     * Set endpoint type from {@link EndpointListRouteDTO} object to {@link MgwEndpointConfigDTO} object.
     *
     * @param sourceObject {@link EndpointListRouteDTO} object
     * @param destObject   {@link MgwEndpointListDTO} object
     */
    private static void setEndpointType(EndpointListRouteDTO sourceObject, MgwEndpointListDTO destObject) {
        int endpointListSize = sourceObject.getEndpoints().size();
        EndpointType endpointType = sourceObject.getType();
        if (endpointListSize > 1) {
            if (endpointType == null) {
                //default value is load_balance
                destObject.setType(EndpointType.load_balance);
                return;
            }
            switch (endpointType) {
                case http:
                    //if endpointList size is greater than one but the given type is http, verbose log will be printed.
                    GatewayCmdUtils.printVerbose("'" + EndpointType.http + "' is not effective with many urls. " +
                            "Only the first url will be used.");
                    destObject.setType(endpointType);
                    break;
                case failover:
                    //if endpointList type is explicitly provided as failover
                    destObject.setType(endpointType);
                    break;
                default:
                    destObject.setType(EndpointType.load_balance);
            }
        } else {
            if (endpointType == null) {
                destObject.setType(EndpointType.http);
                return;
            }
            //if endpointList size is one, we ignore the user input for 'type'
            destObject.setType(EndpointType.http);
            if (endpointType.equals(EndpointType.failover) || endpointType.equals(EndpointType.load_balance)) {
                GatewayCmdUtils.printVerbose(endpointType + " is changed to " + EndpointType.http +
                        " as one endpoint is available.");
            }
        }
    }

    /**
     * Set endpoint Urls from {@link EndpointListRouteDTO} object to {@link MgwEndpointConfigDTO} object.
     *
     * @param sourceObject {@link EndpointListRouteDTO} object
     *                     * @param destObject {@link MgwEndpointListDTO} object
     */
    private static void setEndpointUrls(EndpointListRouteDTO sourceObject, MgwEndpointListDTO destObject) {
        ArrayList<MgwEndpointDTO> mgwEpList = new ArrayList<>();
        for (String ep : sourceObject.getEndpoints()) {
            mgwEpList.add(new MgwEndpointDTO(ep));
        }
        //if any etcd enabled key is available, update the destObject for the usage of ballerina code generation
        for (MgwEndpointDTO mgwEndpointDTO : mgwEpList) {
            if(mgwEndpointDTO.isEtcdEnabled()){
                destObject.setEndpointListEtcdEnabled(true);
                break;
            }
        }
        destObject.setEndpoints(mgwEpList);
    }

//    /**
//     * get resource Endpoint configuration as MgwEndpointConfigDTO (for ballerina code generation)
//     * @param apiName       API name
//     * @param apiVersion    API version
//     * @param resource      Resource Name
//     * @param method        Method
//     * @return              MgwEndpointConfigDTO object
//     */
//    public static MgwEndpointConfigDTO getResourceEpConfigForCodegen(String apiName, String apiVersion, String resource,
//                                                                     String method) {
//        RouteEndpointConfig endpointConfig = getResourceEpConfig(apiName, apiVersion, resource, method);
//        if (endpointConfig != null) {
//            return convertToMgwServiceMap(endpointConfig);
//        } else {
//            return null;
//        }
//    }

    private static RouteEndpointConfig getResourceEpConfig(String apiName, String apiVersion, String resourceName, String method){
        try {
            String resourceId = HashUtils.generateResourceId(apiName, apiVersion, resourceName, method);
            JsonNode resourceNode = getResourceJsonNode(resourceId);
            if(resourceNode != null){
                return OBJECT_MAPPER_YAML.readValue(resourceNode.toString(), RouteEndpointConfig.class);
            }
            else{
                return null;
            }
        } catch (IOException e) {
            throw new CLIInternalException("RouteEndpointConfig for the given resource cannot be parsed");
        }
    }

    private static JsonNode getResourceJsonNode(String resourceId){
        JsonNode rootNode = getRoutesConfig();
        return rootNode.get(RESOURCES).get(resourceId);
    }

    public static boolean hasResourceInRoutesConfig(String resourceId){
        if(getResourceJsonNode(resourceId) == null){
            return false;
        }
        return true;
    }

    /**
     * get Resource Endpoint Configuration as a yaml String
     * @param resourceId    Resource Id
     * @return              Endpoint Configuration as a yaml String
     */
    public static String getResourceAsYaml(String resourceId){
        JsonNode resourceNode = getResourceJsonNode(resourceId);
        if(resourceNode != null){
            try {
                return OBJECT_MAPPER_YAML.writeValueAsString(resourceNode);
            } catch (JsonProcessingException e) {
                throw new CLIInternalException("Resource node cannot be parsed to a yaml");
            }
        }
        return "No endpoints available";
    }

    public static void addFunction(String function, String type, String apiID) {

        APIRouteEndpointConfig api = RouteUtils.getGlobalEpConfig(apiID);

        if (type.equals(IN)) {
            api.setFunctionIn(function);
        } else if (type.equals(OUT)) {
            api.setFunctionOut(function);
        }
        JsonNode jn = getRoutesConfig();
        addAPIRouteEndpointConfigAsGlobalEp(jn, apiID, api);
        writeRoutesConfig(jn);
    }

    public static void AddGlobalFunction(String function, String type) {

        JsonNode rootNode = getRoutesConfig();
        JsonNode jsonNode = rootNode.get(GLOBAL_FUNCTION);

        if (type.equals(IN)) {
            ((ObjectNode) jsonNode).put(FUNCTION_IN, function);
        }
        if (type.equals(OUT)) {
            ((ObjectNode) jsonNode).put(FUNCTION_OUT, function);
        }

        writeRoutesConfig(rootNode);
    }

    public static void updateResourceRoute(String id, String endpointConfigJson){
        if(getResourceJsonNode(id) == null){
            throw new CLIRuntimeException("Provided Resource ID is not available : " + id);
        }
        saveResourceRoute(id, endpointConfigJson);
    }

    public static void updateAPIRoute(String apiId, String endpointConfigJson){
        JsonNode rootNode = getRoutesConfig();
        APIRouteEndpointConfig apiEpConfig = getGlobalEpConfig(apiId);
        if(apiEpConfig == null){
            throw new CLIRuntimeException("Provided API ID is not available : " + apiId);
        }
        addGlobalEndpoint(rootNode, apiEpConfig.getApiName(), apiEpConfig.getApiVersion(), apiId, endpointConfigJson);
        writeRoutesConfig(rootNode);
    }

    public static boolean hasApiInRoutesConfig(String apiId){
        return null != getGlobalEpConfig(apiId);
    }

    public static void setRoutesConfigPath(String routesConfigPath) {
        RouteUtils.routesConfigPath = routesConfigPath;
    }
}
