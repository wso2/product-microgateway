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

    public static void setMgwDefinition(String path) {
        try {
            if (rootDefinition == null) {
                validateYamlSyntax(path);
                rootDefinition = OBJECT_MAPPER_YAML.readValue(new File(path), MgwRootDefinition.class);
                LOGGER.info(GatewayCliConstants.PROJECT_DEFINITION_FILE + " is parsed successfully");
            }
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

    public static MgwEndpointConfigDTO getResourceEpConfigForCodegen(String basePath, String path, String method) {
        if(!isResourceAvailable(basePath, path, method)){
            return null;
        }
        EndpointListRouteDTO prodList = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(path).getEndpointListDefinition(method).getProdEndpointList();
        EndpointListRouteDTO sandList = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(path).getEndpointListDefinition(method).getSandEndpointList();
        return RouteUtils.convertToMgwServiceMap(prodList,sandList);
    }

    public static String getRequestInterceptor(String basePath, String path, String method){
        if(!isResourceAvailable(basePath, path, method)){
            return null;
        }
        return rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(method).getRequestInterceptor();
    }

    public static String getResponseInterceptor(String basePath, String path, String method){
        if(!isResourceAvailable(basePath, path, method)){
            return null;
        }
        return rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(method).getResponseInterceptor();
    }

    public static String getThrottlePolicy(String basePath, String path, String method){
        if(!isResourceAvailable(basePath, path, method)){
            return null;
        }
        return rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(method).getThrottlePolicy();
    }

    private static boolean isResourceAvailable(String basePath, String path, String method){
        if(rootDefinition.getApis().getApiFromBasepath(basePath) == null){
            return false;
        }
        if(rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path) == null){
            return false;
        }
        if(rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path)
                .getEndpointListDefinition(method) == null){
            return false;
        }
        return true;
    }

    /**
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
     *
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
