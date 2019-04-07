/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.mgwServiceMap.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.mgwServiceMap.MgwEndpointDTO;
import org.wso2.apimgt.gateway.cli.model.mgwServiceMap.MgwEndpointListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.EndpointUrlTypeEnum;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.route.EndpointConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Utilities used by ballerina code generator.
 */
public class OpenApiCodegenUtils {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiCodegenUtils.class);

    public static String readApi(String filePath) {
        String responseStr;
        try {
            responseStr = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error while reading api definition.", e);
            throw new CLIInternalException("Error while reading api definition.");
        }
        return responseStr;
    }

    //todo: move this to some other class
    public static void setAdditionalConfigs(String projectName, ExtendedAPI api) {
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        MgwEndpointConfigDTO mgwEndpointConfigDTO =
                convertRouteToMgwServiceMap(RouteUtils.getGlobalEpConfig( apiId,
                GatewayCmdUtils.getProjectRoutesConfFilePath(projectName)));
        api.setEndpointConfigRepresentation(mgwEndpointConfigDTO);
        // 0th element represents the specific basepath
        api.setSpecificBasepath(RouteUtils.getBasePath(apiId,
                GatewayCmdUtils.getProjectRoutesConfFilePath(projectName)) [0]);
        api.setApiSecurity(JsonProcessingUtils.getAPIMetadata(projectName, apiId).getSecurity());
        api.setCorsConfiguration(JsonProcessingUtils.getAPIMetadata(projectName, apiId).getCorsConfigurationDTO());
    }

    private static MgwEndpointConfigDTO convertRouteToMgwServiceMap(EndpointConfig routeEndpointConfig){
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

}
