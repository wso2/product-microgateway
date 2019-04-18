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
    //private static String routesConfigPath;
    private static MgwRootDefinition rootDefinition;

    public static void setMgwDefinition(String path) {
        try {
            if (rootDefinition == null) {
                rootDefinition = OBJECT_MAPPER_YAML.readValue(new File(path), MgwRootDefinition.class);
            }
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while reading the routes.yaml", e);
        }
    }

    //todo: check the need of validate basepath
    public static String getBasePath(String apiName, String apiVersion) {
        return rootDefinition.getApis().getBasepathFromAPI(apiName, apiVersion);
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

    public static MgwEndpointConfigDTO getResourceEpConfigForCodegen(String apiName, String apiVersion, String resourceName, String method) {
        String basePath = getBasePath(apiName, apiVersion);
        if(rootDefinition.getApis().getApiFromBasepath(basePath) == null){
            return null;
        }
        if(rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(resourceName) == null){
            return null;
        }
        if(rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().getMgwResource(resourceName)
                .getEndpointListDefinition(method) == null){
            return null;
        }
        EndpointListRouteDTO prodList = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(resourceName).getEndpointListDefinition(method).getProdEndpointList();
        EndpointListRouteDTO sandList = rootDefinition.getApis().getApiFromBasepath(basePath).getPathsDefinition().
                getMgwResource(resourceName).getEndpointListDefinition(method).getSandEndpointList();
        return RouteUtils.convertToMgwServiceMap(prodList,sandList);
    }
}
