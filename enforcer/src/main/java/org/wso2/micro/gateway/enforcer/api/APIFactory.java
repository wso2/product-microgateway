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
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.discovery.ApiDiscoveryClient;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the meta data of all the APIS deployed in the gateway node. Adding/Updating API requires to register the
 * API with this factory class. When requests are received this factory instance will be queried to find the matching
 * API, and then request will be dispatched to that API.
 */
public class APIFactory {
    private static final Logger logger = LogManager.getLogger(APIFactory.class);

    private static APIFactory apiFactory;
    private ConcurrentHashMap<String, API> apis = new ConcurrentHashMap<>();

    private APIFactory() {}

    public static APIFactory getInstance() {
        if (apiFactory == null) {
            apiFactory = new APIFactory();
        }
        return apiFactory;
    }

    public void init() {
        ApiDiscoveryClient ads =  ApiDiscoveryClient.getInstance();
        ads.watchApis();
    }

    public void addApi(API api) {
        String apiKey = getApiKey(api);
        apis.put(apiKey, api);
    }

    public void addApis(List<Api> apis) {
        //TODO: (Praminda) Use apiId as the map key. Need to add the apiId to envoy context meta
        ConcurrentHashMap<String, API> newApis = new ConcurrentHashMap<>();

        //TODO: (Lahiru) Check apiType field of API to differentiate WebSocket APIs
        for (Api api : apis) {
            if (api.getResourcesCount() != 0) {
                RestAPI enforcerApi = new RestAPI();
                enforcerApi.init(api);
                String apiKey = getApiKey(enforcerApi);
                newApis.put(apiKey, enforcerApi);
            } else {
                WebSocketAPI webSocketAPI = new WebSocketAPI();
                webSocketAPI.init(api);
                String apiKey = getApiKey(webSocketAPI);
                newApis.put(apiKey, webSocketAPI);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Total APIs in new cache: {}", newApis.size());
        }
        this.apis = newApis;
    }

    public void removeApi(API api) {
        String apiKey = getApiKey(api);
        apis.remove(apiKey);
    }

    public API getMatchedAPI(CheckRequest request) {
        // TODO: (Praminda) Change the init type depending on the api type param from gw
        String basePath = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_BASE_PATH_PARAM);
        String version = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_VERSION_PARAM);
        String apiKey = basePath + '/' + version;
        if (logger.isDebugEnabled()) {
            logger.debug("Looking for matching API with basepath: {} and version: {}", basePath, version);
        }

        return apis.get(apiKey);
    }

    public WebSocketAPI getMatchedAPI(RateLimitRequest rateLimitRequest) {
        String basePath = rateLimitRequest.getMetadataContext().getFilterMetadataMap()
                .get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.GW_BASE_PATH_PARAM)
                .getStringValue();
        String version = rateLimitRequest.getMetadataContext().getFilterMetadataMap().
                get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.GW_VERSION_PARAM).getStringValue();
        String apiKey = basePath + '/' + version;
        if (logger.isDebugEnabled()) {
            logger.debug("Looking for matching API with basepath: {} and version: {}", basePath, version);
        }
        return (WebSocketAPI) apis.get(apiKey);
    }

    public ResourceConfig getMatchedResource(API api, String matchedResourcePath, String method) {
        List<ResourceConfig> resourceConfigList = api.getAPIConfig().getResources();
        return resourceConfigList.stream()
                .filter(resourceConfig -> resourceConfig.getPath().equals(matchedResourcePath)).
                        filter(resourceConfig -> (method == null) || resourceConfig.getMethod()
                                .equals(ResourceConfig.HttpMethods.valueOf(method))).findFirst().orElse(null);
    }

    public ResourceConfig getMatchedBasePath(API api, String basePath) {
        ResourceConfig resourceConfig = new ResourceConfig();
        if (api.getAPIConfig().getBasePath().equals(basePath)) {
            resourceConfig.setPath(basePath);
            resourceConfig.setMethod(ResourceConfig.HttpMethods.GET);
        }
        return resourceConfig;
    }

    private String getApiKey(API api) {
        return api.getAPIConfig().getBasePath() + '/' + api.getAPIConfig().getVersion();
    }
}
