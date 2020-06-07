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
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.CLICompileTimeException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointDTO;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.EndpointUrlTypeEnum;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;
import org.wso2.apimgt.gateway.cli.model.route.EndpointType;
import org.wso2.apimgt.gateway.cli.model.route.RouteEndpointConfig;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 * Utility functions for handling endpoints.
 */
public final class RouteUtils {
    private static final ObjectMapper OBJECT_MAPPER_JSON = new ObjectMapper();

    /**
     * To parse the Endpoint Configuration received from API Manager to RouteEndpointConfig Object.
     *
     * @param epConfigJson Endpoint Configuration Json received from Publisher API
     * @param epSecurity   Endpoint Security details received from Publisher API
     * @return RouteEndpointConfig object
     */
    public static RouteEndpointConfig parseEndpointConfig(String epConfigJson, APIEndpointSecurityDTO epSecurity)
            throws MalformedURLException, CLICompileTimeException {
        RouteEndpointConfig endpointConfig = new RouteEndpointConfig();
        EndpointListRouteDTO prodEndpointConfig = new EndpointListRouteDTO();
        EndpointListRouteDTO sandEndpointConfig = new EndpointListRouteDTO();

        //set securityConfig to the both environments
        prodEndpointConfig.setSecurityConfig(epSecurity);
        sandEndpointConfig.setSecurityConfig(epSecurity);

        JsonNode rootNode;
        try {
            rootNode = OBJECT_MAPPER_JSON.readTree(epConfigJson);
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
            } else {
                prodEndpointConfig.setType(EndpointType.http);
                sandEndpointConfig.setType(EndpointType.http);
            }
        } else if (RESTServiceConstants.LOAD_BALANCE.equalsIgnoreCase(endpointType)) {
            JsonNode prodEndpoints = rootNode.withArray(RESTServiceConstants.PRODUCTION_ENDPOINTS);
            JsonNode sandboxEndpoints = rootNode.withArray(RESTServiceConstants.SANDBOX_ENDPOINTS);
            prodEndpointConfig.setType(EndpointType.load_balance);
            sandEndpointConfig.setType(EndpointType.load_balance);

            if (prodEndpoints != null) {
                for (JsonNode node : prodEndpoints) {
                    prodEndpointConfig.addEndpoint(node.get(RESTServiceConstants.URL).asText());
                }
            }

            if (sandboxEndpoints != null) {
                for (JsonNode node : sandboxEndpoints) {
                    sandEndpointConfig.addEndpoint(node.get(RESTServiceConstants.URL).asText());
                }
            }
        } else if (RESTServiceConstants.ADDRESS.equalsIgnoreCase(endpointType)) {
            JsonNode prodEndpointNode = rootNode.get(RESTServiceConstants.PRODUCTION_ENDPOINTS);
            JsonNode sandEndpointNode = rootNode.get(RESTServiceConstants.SANDBOX_ENDPOINTS);
            prodEndpointConfig.setType(EndpointType.address);
            sandEndpointConfig.setType(EndpointType.address);

            if (prodEndpointNode != null) {
                prodEndpointConfig.addEndpoint(prodEndpointNode.get(RESTServiceConstants.URL).asText());
            }

            if (sandEndpointNode != null) {
                sandEndpointConfig.addEndpoint(sandEndpointNode.get(RESTServiceConstants.URL).asText());
            }
        }
        try {
            prodEndpointConfig.validateEndpoints();
        } catch (CLICompileTimeException e) {
            throw new CLICompileTimeException("The provided production endpoint is invalid.\n\t-" +
                    e.getTerminalMsg(), e);
        }
        try {
            sandEndpointConfig.validateEndpoints();
        } catch (CLICompileTimeException e) {
            throw new CLICompileTimeException("The provided production endpoint is invalid.\n\t-" +
                    e.getTerminalMsg(), e);
        }

        if (prodEndpointConfig.getEndpoints() != null && prodEndpointConfig.getEndpoints().size() > 0) {
            endpointConfig.setProdEndpointList(prodEndpointConfig);
        }

        if (sandEndpointConfig.getEndpoints() != null && sandEndpointConfig.getEndpoints().size() > 0) {
            endpointConfig.setSandboxEndpointList(sandEndpointConfig);
        }

        return endpointConfig;
    }

    /**
     * Convert the RouteEndpointConfig object to MgwEndpointConfigDTO for the ease of source code generation.
     *
     * @param prodEpListDTO list of production endpoints
     * @param sandEpListDTO list of sandbox endpoints
     * @return MgeEndpointConfig Object corresponding to provided routeEndpointConfig
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
            prod.setAdvanceEndpointConfig(prodEpListDTO.getAdvanceEndpointConfig());
        }

        if (sandEpListDTO != null) {
            sandbox = new MgwEndpointListDTO();
            sandbox.setEndpointUrlType(EndpointUrlTypeEnum.SAND);
            setEndpointType(sandEpListDTO, sandbox);
            setEndpointUrls(sandEpListDTO, sandbox);
            sandbox.setSecurityConfig(sandEpListDTO.getSecurityConfig());
            sandbox.setName(sandEpListDTO.getName());
            sandbox.setAdvanceEndpointConfig(sandEpListDTO.getAdvanceEndpointConfig());
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
                    CmdUtils.printVerbose("'" + EndpointType.http + "' is not effective with many urls. " +
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

            // if endpointList size is one, we ignore the user input for 'type'
            destObject.setType(EndpointType.http);
            if (!endpointType.equals(EndpointType.http)) {
                CmdUtils.printVerbose(endpointType + " is changed to " + EndpointType.http +
                        " as only one endpoint is available.");
            }
        }
    }

    /**
     * Set endpoint Urls from {@link EndpointListRouteDTO} object to {@link MgwEndpointConfigDTO} object.
     *
     * @param sourceObject {@link EndpointListRouteDTO} object
     * @param destObject {@link MgwEndpointListDTO} object
     */
    private static void setEndpointUrls(EndpointListRouteDTO sourceObject, MgwEndpointListDTO destObject) {
        ArrayList<MgwEndpointDTO> mgwEpList = new ArrayList<>();

        for (String ep : sourceObject.getEndpoints()) {
            mgwEpList.add(new MgwEndpointDTO(ep));
        }

        //if any etcd enabled key is available, update the destObject for the usage of ballerina code generation
        for (MgwEndpointDTO mgwEndpointDTO : mgwEpList) {
            if (mgwEndpointDTO.isEtcdEnabled()) {
                destObject.setEndpointListEtcdEnabled(true);
                break;
            }
        }
        destObject.setEndpoints(mgwEpList);
    }
}
