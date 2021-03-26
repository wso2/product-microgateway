/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.enforcer.api;

import org.wso2.gateway.discovery.api.Api;
import org.wso2.gateway.discovery.api.Operation;
import org.wso2.gateway.discovery.api.Resource;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.cors.CorsFilter;
import org.wso2.micro.gateway.enforcer.security.AuthFilter;
import org.wso2.micro.gateway.enforcer.throttle.ThrottleFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specific implementation for a Rest API type APIs.
 */
public class RestAPI implements API {
    private final List<Filter> filters = new ArrayList<>();
    private APIConfig apiConfig;
    private String apiLifeCycleState;

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public String init(Api api) {
        String basePath = api.getBasePath();
        String name = api.getTitle();
        String version = api.getVersion();
        List<String> securitySchemes = api.getSecuritySchemeList();
        List<ResourceConfig> resources = new ArrayList<>();

        for (Resource res: api.getResourcesList()) {
            for (Operation operation : res.getMethodsList()) {
                ResourceConfig resConfig = buildResource(operation, res.getPath());
                resources.add(resConfig);
            }
        }

        this.apiLifeCycleState = api.getApiLifeCycleState();
        this.apiConfig = new APIConfig.Builder(name).basePath(basePath).version(version).resources(resources)
                .apiLifeCycleState(apiLifeCycleState).securitySchema(securitySchemes).tier(api.getTier())
                .disableSecurity(api.getDisableSecurity()).build();
        initFilters();
        return basePath;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject();
        if (executeFilterChain(requestContext)) {
            responseObject.setStatusCode(APIConstants.StatusCodes.OK.getCode());
            if (requestContext.getResponseHeaders() != null && requestContext.getResponseHeaders().size() > 0) {
                responseObject.setHeaderMap(requestContext.getResponseHeaders());
            }
        } else {
            // If a enforcer stops with a false, it will be passed directly to the client.
            responseObject.setDirectResponse(true);
            responseObject.setStatusCode(Integer.parseInt(
                    requestContext.getProperties().get(APIConstants.MessageFormat.STATUS_CODE).toString()));
            if (requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_CODE) != null) {
                responseObject.setErrorCode(
                        requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_CODE).toString());
            }
            if (requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_MESSAGE) != null) {
                responseObject.setErrorMessage(requestContext.getProperties()
                        .get(APIConstants.MessageFormat.ERROR_MESSAGE).toString());
            }
            if (requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_DESCRIPTION) != null) {
                responseObject.setErrorDescription(requestContext.getProperties()
                        .get(APIConstants.MessageFormat.ERROR_DESCRIPTION).toString());
            }
            if (requestContext.getResponseHeaders() != null && requestContext.getResponseHeaders().size() > 0) {
                responseObject.setHeaderMap(requestContext.getResponseHeaders());
            }
        }
        return responseObject;
    }

    @Override
    public APIConfig getAPIConfig() {
        return this.apiConfig;
    }

    private ResourceConfig buildResource(Operation operation, String resPath) {
        ResourceConfig resource = new ResourceConfig();
        resource.setPath(resPath);
        resource.setMethod(ResourceConfig.HttpMethods.valueOf(operation.getMethod().toUpperCase()));
        resource.setTier(operation.getTier());
        resource.setDisableSecurity(operation.getDisableSecurity());
        Map<String, List<String>> securityMap = new HashMap<>();
        operation.getSecurityList().forEach(securityList -> securityList.getScopeListMap().forEach((key, security) -> {
            if (security != null && security.getScopesList().size() > 0) {
                List<String> scopeList = new ArrayList<>(security.getScopesList());
                securityMap.put(key, scopeList);
            }
        }));
        resource.setSecuritySchemas(securityMap);
        return resource;
    }

    private void initFilters() {
        CorsFilter corsFilter = new CorsFilter();
        this.filters.add(corsFilter);
        // TODO : re-vist the logic with apim prototype implemetation
        if (!APIConstants.PROTOTYPED_LIFE_CYCLE_STATUS.equals(apiLifeCycleState)) {
            AuthFilter authFilter = new AuthFilter();
            authFilter.init(apiConfig);
            this.filters.add(authFilter);

        }

        // enable throttle filter
        if (ConfigHolder.getInstance().getConfig().getThrottleConfig().isGlobalPublishingEnabled()) {
            ThrottleFilter throttleFilter = new ThrottleFilter();
            throttleFilter.init(apiConfig);
            this.filters.add(throttleFilter);
        }
    }
}
