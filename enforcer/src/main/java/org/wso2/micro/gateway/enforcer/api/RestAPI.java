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

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.api.Api;
import org.wso2.gateway.discovery.api.Resource;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.security.AuthFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specific implementation for a Rest API type APIs.
 */
public class RestAPI implements API {
    private static final Logger logger = LogManager.getLogger(RestAPI.class);

    private APIConfig apiConfig;
    private List<Filter> filters = new ArrayList<>();

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public String init(CheckRequest request) {
        String basePath = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_BASE_PATH_PARAM);
        String name = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_API_NAME_PARAM);
        String version = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_VERSION_PARAM);
        List<ResourceConfig> resources = extractResourceConfig(request.getAttributes().getContextExtensionsMap());
        this.apiConfig = new APIConfig.Builder(name).basePath(basePath).version(version).resources(resources).build();

        initFilters();
        return basePath;
    }

    @Override
    public String init(Api api) {
        String basePath = api.getBasePath();
        String name = api.getTitle();
        String version = api.getVersion();
        List<ResourceConfig> resources = new ArrayList<>();
        for (Resource res: api.getResourcesList()) {
            // TODO: (Praminda) handle multiple methods for a resource
            // TODO: (Praminda) handle all fields of resource
            String method = res.getMethods(0);
            ResourceConfig resConfig = buildResource(res.getPath(), method);

            resources.add(resConfig);
        }
        this.apiConfig = new APIConfig.Builder(name).basePath(basePath).version(version).resources(resources).build();

        initFilters();
        return basePath;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject();
        if (executeFilterChain(requestContext)) {
            responseObject.setStatusCode(200);
            if (requestContext.getResponseHeaders() != null) {
                responseObject.setHeaderMap(requestContext.getResponseHeaders());
            }
        } else {
            responseObject.setStatusCode(Integer.parseInt(requestContext.getProperties().get("code").toString()));
            responseObject.setErrorCode(requestContext.getProperties().get("error_code").toString());
            responseObject.setErrorDescription(requestContext.getProperties().get("error_description").toString());
        }

        return responseObject;
    }

    @Override
    public APIConfig getAPIConfig() {
        return this.apiConfig;
    }

    /**
     * Extract elected resource details from the request attributes.
     *
     * @param attributes request attributes
     * @return resource configuration identified by the request
     */
    private List<ResourceConfig> extractResourceConfig(Map<String, String> attributes) {
        // TODO: (Praminda) cover error cases
        String resPath = attributes.get(APIConstants.GW_RES_PATH_PARAM);
        String[] methods = attributes.get(APIConstants.GW_RES_METHOD_PARAM).split(" ");
        List<ResourceConfig> resources = new ArrayList<>(1);

        for (String m : methods) {
            resources.add(buildResource(resPath, m));
        }

        return resources;
    }

    private ResourceConfig buildResource(String resPath, String resMethod) {
        ResourceConfig resource = new ResourceConfig();
        resource.setPath(resPath);
        resource.setMethod(ResourceConfig.HttpMethods.valueOf(resMethod.toUpperCase()));

        return resource;
    }

    private void initFilters() {
        AuthFilter authFilter = new AuthFilter();
        authFilter.init(apiConfig);
        this.filters.add(authFilter);
    }
}
