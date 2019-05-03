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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.definition.DefinitionConfig;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Represents the microgateway definitions file.
 * <p>
 * Implementation contains the methods required to build a valid definition object model
 * from a definition file.
 * </p>
 */
public class MgwDefinitionBuilder {
    private static DefinitionConfig definitionConfig;
    private static String projectName;
    private static final Logger LOGGER = LoggerFactory.getLogger(MgwDefinitionBuilder.class);

//    /**
//     * Builds the {@link DefinitionConfig} object model for {@link GatewayCliConstants#PROJECT_DEFINITION_FILE}.
//     * Before parsing the yaml file to {@link DefinitionConfig}, validation will be performed on
//     * the the input project definition file.
//     *
//     * @param project microgateway project name
//     */
//    public static void build(String project) {
//        projectName = project;
//        String definitionPath = GatewayCmdUtils.getProjectMgwDefinitionFilePath(project);
//        File definitionFile = new File(definitionPath);
//
//        try {
//            InputStream isSchema = MgwDefinitionBuilder.class.getClassLoader()
//                    .getResourceAsStream(GatewayCliConstants.DEFINITION_SCHEMA_FILE);
//            definitionConfig = YamlValidator.parse(definitionFile, isSchema, DefinitionConfig.class);
//        } catch (IOException e) {
//            throw new CLIRuntimeException("Error while reading the " + GatewayCliConstants.PROJECT_DEFINITION_FILE +
//                    ".", e);
//        }
//
//        try {
//            //update the interceptor map
//            setInterceptors();
//        } catch (IOException e) {
//            throw new CLIRuntimeException("Error while reading the '" + GatewayCliConstants.PROJECT_INTERCEPTORS_DIR +
//                    "' directory");
//        }
//    }

    /**
     * Get basePath from the definition.yaml.
     *
     * @param apiName    API name
     * @param apiVersion API version
     * @return basePath
     */
    public static String getBasePath(String apiName, String apiVersion) {
        String basePath = definitionConfig.getApis().getBasepathFromAPI(apiName, apiVersion);
        if (basePath == null) {
            throw new CLIRuntimeException("Error: The API '" + apiName + "' and version '" + apiVersion + "' is not " +
                    "found in the " + GatewayCliConstants.PROJECT_DEFINITION_FILE + ".");
        }

        return basePath;
    }

    public static EndpointListRouteDTO getProdEndpointList(String basePath) {
        return definitionConfig.getApis().getApiFromBasepath(basePath).getProdEpList();
    }

    public static EndpointListRouteDTO getSandEndpointList(String basePath) {
        return definitionConfig.getApis().getApiFromBasepath(basePath).getProdEpList();
    }

    public static String getSecurity(String basePath) {
        return definitionConfig.getApis().getApiFromBasepath(basePath).getSecurity();
    }

    public static APICorsConfigurationDTO getCorsConfiguration(String basePath) {
        return definitionConfig.getApis().getApiFromBasepath(basePath).getCorsConfiguration();
    }

    public static MgwEndpointConfigDTO getResourceEpConfigForCodegen(String basePath, String path, String operation) {
        if (!isResourceAvailable(basePath, path, operation)) {
            return null;
        }
        EndpointListRouteDTO prodList = definitionConfig.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(path).getEndpointListDefinition(operation).getProdEndpointList();
        EndpointListRouteDTO sandList = definitionConfig.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(path).getEndpointListDefinition(operation).getSandEndpointList();
        return RouteUtils.convertToMgwServiceMap(prodList, sandList);
    }

    /**
     * Get the request interceptor for a given resource in an API.
     *
     * @param basePath  basePath of the API
     * @param path      path variable of the resource
     * @param operation operation of the resource
     * @return request interceptor of the resource if available, otherwise null.
     */
    public static String getRequestInterceptor(String basePath, String path, String operation) {
        if (!isResourceAvailable(basePath, path, operation)) {
            return null;
        }
        String interceptor = definitionConfig.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(operation).getRequestInterceptor();
        //validateInterceptorAvailability(requestInterceptorMap, interceptor, basePath, path, operation);

        return interceptor;
    }

    /**
     * Get the response interceptor for a given resource in an API.
     *
     * @param basePath  basePath of the API
     * @param path      path variable of the resource
     * @param operation operation of the resource
     * @return response interceptor of the resource if available, otherwise null.
     */
    public static String getResponseInterceptor(String basePath, String path, String operation) {
        if (!isResourceAvailable(basePath, path, operation)) {
            return null;
        }
        String interceptor = definitionConfig.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(operation).getResponseInterceptor();
        //validateInterceptorAvailability(responseInterceptorMap, interceptor, basePath, path, operation);

        return interceptor;
    }

    public static String getThrottlePolicy(String basePath, String path, String operation) {
        if (!isResourceAvailable(basePath, path, operation)) {
            return null;
        }
        return definitionConfig.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path).
                getEndpointListDefinition(operation).getThrottlePolicy();
    }

    /**
     * Check if the given resource is available in the definition.yaml.
     *
     * @param basePath  basePath of the API
     * @param path      path of the resource
     * @param operation operation of the resource
     * @return true if the resource is available
     */
    private static boolean isResourceAvailable(String basePath, String path, String operation) {
        if (definitionConfig.getApis().getApiFromBasepath(basePath) == null) {
            return false;
        }
        if (definitionConfig.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path) == null) {
            return false;
        }
        return definitionConfig.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(path)
                .getEndpointListDefinition(operation) != null;
    }

    /**
     * Get the API-level request interceptor of an API.
     *
     * @param basePath basePath of the API
     * @return API response request function name
     */
    public static String getApiRequestInterceptor(String basePath) {
        String interceptor = definitionConfig.getApis().getApiFromBasepath(basePath).getRequestInterceptor();
        //validateInterceptorAvailability(requestInterceptorMap, interceptor, basePath, null, null);
        return interceptor;
    }

    /**
     * Get the API-level response interceptor of an API.
     *
     * @param basePath basePath of the API
     * @return API response interceptor function name
     */
    public static String getApiResponseInterceptor(String basePath) {
        String interceptor = definitionConfig.getApis().getApiFromBasepath(basePath).getResponseInterceptor();
        //validateInterceptorAvailability(responseInterceptorMap, interceptor, basePath, null, null);
        return interceptor;
    }

    /**
     * To find out the api information which is not used for code generation but included in the definitions.yaml
     */
    public static void FindUnusedAPIInformation() {
        definitionConfig.getApis().getApisMap().forEach((k, v) -> {
            if (!v.getIsDefinitionUsed()) {
                String msg = "API '" + v.getTitle() + "' version: '" + v.getVersion() + "' is not used but " +
                        "added to the " + GatewayCliConstants.PROJECT_DEFINITION_FILE + ".";
                LOGGER.warn(msg);
                GatewayCmdUtils.printVerbose(msg);
            }
        });
    }

}

