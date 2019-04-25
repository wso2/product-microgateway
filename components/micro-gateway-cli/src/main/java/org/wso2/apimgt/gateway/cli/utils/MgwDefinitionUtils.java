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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.mgwdefinition.MgwRootDefinition;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;

import java.io.File;
import java.io.IOException;


/**
 * This class includes the Util functions related to operations on MgwDefinition.
 */
public class MgwDefinitionUtils {

    private static final ObjectMapper OBJECT_MAPPER_YAML = new ObjectMapper(new YAMLFactory());
    private static final Logger LOGGER = LoggerFactory.getLogger(MgwDefinitionUtils.class);
    //private static String routesConfigPath;
    private static MgwRootDefinition rootDefinition;
    private static String projectName;

    public static void configureMgwDefinition(String project) {
        projectName = project;
        try {
            String definitionFilePath = GatewayCmdUtils.getProjectMgwDefinitionFilePath(project);
            rootDefinition = OBJECT_MAPPER_YAML.readValue(new File(definitionFilePath), MgwRootDefinition.class);
        } catch (IOException e) {
            throw GatewayCmdUtils.createValidationException("Error while reading the " +
                    GatewayCliConstants.PROJECT_DEFINITION_FILE + ".", e, LOGGER);
        }
    }

    /**
     * Get basePath from the definition.yaml
     *
     * @param apiName    API name
     * @param apiVersion API version
     * @return basePath
     */
    public static String getBasePath(String apiName, String apiVersion) {
        String basePath = rootDefinition.getApis().getBasepathFromAPI(apiName, apiVersion);
        if (basePath == null) {
            throw GatewayCmdUtils.createValidationException("Error: The API '" + apiName + "' and version '" +
                    apiVersion + "' is not " + "found in the " +
                    GatewayCliConstants.PROJECT_DEFINITION_FILE + ".", LOGGER);
        }
        return basePath;
    }

    public static EndpointListRouteDTO getProdEndpointList(String basePath) {
        return rootDefinition.getApis().getApiFromBasepath(basePath).getProdEpList();
    }

    public static EndpointListRouteDTO getSandEndpointList(String basePath) {
        return rootDefinition.getApis().getApiFromBasepath(basePath).getProdEpList();
    }

    public static String getSecurity(String basePath) {
        return rootDefinition.getApis().getApiFromBasepath(basePath).getSecurity();
    }

    public static APICorsConfigurationDTO getCorsConfiguration(String basePath) {
        return rootDefinition.getApis().getApiFromBasepath(basePath).getCorsConfiguration();
    }

    public static MgwEndpointConfigDTO getResourceEpConfigForCodegen(String basePath, String path, String operation) {
        if(!isResourceAvailable(basePath, path, operation)){
            return null;
        }
        EndpointListRouteDTO prodList = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(path).getEndpointListDefinition(operation).getProdEndpointList();
        EndpointListRouteDTO sandList = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(path).getEndpointListDefinition(operation).getSandEndpointList();
        return RouteUtils.convertToMgwServiceMap(prodList,sandList);
    }

    /**
     * Get the request interceptor for a given resource in an API.
     *
     * @param basePath basePath of the API
     * @param path path variable of the resource
     * @param operation operation of the resource
     * @return  request interceptor of the resource if available, otherwise null.
     */
    public static String getRequestInterceptor(String basePath, String path, String operation) {
        if (!isResourceAvailable(basePath, path, operation)) {
            return null;
        }
        String interceptor = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(operation).getRequestInterceptor();
        validateInterceptorAvailability(interceptor, basePath, path, operation);
        return interceptor;
    }

    /**
     * Get the response interceptor for a given resource in an API.
     *
     * @param basePath basePath of the API
     * @param path path variable of the resource
     * @param operation operation of the resource
     * @return  response interceptor of the resource if available, otherwise null.
     */
    public static String getResponseInterceptor(String basePath, String path, String operation) {
        if (!isResourceAvailable(basePath, path, operation)) {
            return null;
        }
        String interceptor = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(operation).getResponseInterceptor();
        validateInterceptorAvailability(interceptor, basePath, path, operation);
        return interceptor;
    }

    public static String getThrottlePolicy(String basePath, String path, String operation){
        if(!isResourceAvailable(basePath, path, operation)){
            return null;
        }
        return rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(operation).getThrottlePolicy();
    }

    /**
     * Check if the given resource is available in the definition.yaml.
     *
     * @param basePath basePath of the API
     * @param path path of the resource
     * @param operation operation of the resource
     * @return true if the resource is available
     */
    private static boolean isResourceAvailable(String basePath, String path, String operation){
        if(rootDefinition.getApis().getApiFromBasepath(basePath) == null){
            return false;
        }
        if(rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path) == null){
            return false;
        }
        if(rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path)
                .getEndpointListDefinition(operation) == null){
            return false;
        }
        return true;
    }

    /**
     * Get the API-level request interceptor of an API.
     *
     * @param basePath basePath of the API
     * @return API response request function name
     */
    public static String getApiRequestInterceptor(String basePath){
        String interceptor = rootDefinition.getApis().getApiFromBasepath(basePath).getRequestInterceptor();
        validateInterceptorAvailability(interceptor, basePath, null, null);
        return interceptor;
    }

    /**
     * Get the API-level response interceptor of an API.
     *
     * @param basePath basePath of the API
     * @return  API response interceptor function name
     */
    public static String getApiResponseInterceptor(String basePath){
        String interceptor = rootDefinition.getApis().getApiFromBasepath(basePath).getResponseInterceptor();
        validateInterceptorAvailability(interceptor, basePath, null, null);
        return interceptor;
    }

    /**
     * Validate the existence of the interceptor inside interceptors directory.
     * Throws an runtime error if the interceptor is not found.
     * if the provided interceptor name is null, 'null' will be returned.
     *
     * @param interceptorName name of the interceptor
     * @param basePath basePath
     * @param path path of the resource (if interceptor is Api level keep null)
     * @param operation operation of the resource (if interceptor is Api level keep null)
     */
    private static void validateInterceptorAvailability(String interceptorName, String basePath, String path,
                                                        String operation) {
        if (interceptorName == null) {
            return;
        }
        File file = new File(GatewayCmdUtils.getProjectInterceptorsDirectoryPath(projectName) +
                File.separator + interceptorName + GatewayCliConstants.EXTENSION_BAL);
        if (!file.exists()) {
            String errorMsg = "The interceptor '" + interceptorName + "' mentioned under basePath:'" + basePath + "' ";
            //if the interceptor is resource level
            if (path != null && operation != null) {
                errorMsg += "path:'" + path + "' operation:'" + operation + "' ";
            }
            errorMsg += "is not available in the " + GatewayCliConstants.PROJECT_DEFINITION_FILE + ".";
            LOGGER.error(errorMsg);
            throw new CLIRuntimeException(errorMsg);
        }
     * To validate the provided definition.yaml follows the yaml syntax
     *
     * @param filePath file path to definition.yaml
     */
    private static void validateYamlSyntax(String filePath) {
        File file = new File(filePath);
        //to check the existence of definitions.yaml file
        if (!file.exists()) {
            throw GatewayCmdUtils.createValidationException("'definition.yaml' file does not exists.", LOGGER);
        }
        try {
            OBJECT_MAPPER_YAML.readTree(file);
            //if the provided definitions.yaml file does not follow the yaml syntax
        } catch (IOException e) {
            throw GatewayCmdUtils.createValidationException("'definitions.yaml file cannot be parsed as yaml document.",
                    LOGGER);
        }
    }

    /**
     * To find out the api information which is not used for code generation but included in the definitions.yaml
     */
    public static void FindNotUsedAPIInformation() {
        rootDefinition.getApis().getApisMap().forEach((k, v) -> {
            if (!v.getIsUsed()) {
                String msg = "API '" + v.getTitle() + "' version: '" + v.getVersion() + "' is not used but " +
                        "added to the " + GatewayCliConstants.PROJECT_DEFINITION_FILE + ".";
                LOGGER.warn(msg);
                GatewayCmdUtils.printVerbose(msg);
            }
        });
    }
}
