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
package org.wso2.choreo.connect.enforcer.api;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.api.Api;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.config.EnvVarConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.discovery.ApiDiscoveryClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the meta data of all the APIS deployed in the gateway node. Adding/Updating API requires to register the
 * API with this factory class. When requests are received this factory instance will be queried to find the matching
 * API, and then request will be dispatched to that API.
 */
public class APIFactory {

    private static final List<String> KNOWN_VHOST_PREFIXES =
            List.of("dev", "sandbox_dev", "prod", "sandbox", "dev-internal", "prod-internal");
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
        ApiDiscoveryClient ads = ApiDiscoveryClient.getInstance();
        ads.watchApis();
    }

    public void addApi(API api) {
        String apiKey = getApiKey(api);
        apis.put(apiKey, api);
    }

    public void addApis(List<Api> apis) {
        //TODO: (Praminda) Use apiId as the map key. Need to add the apiId to envoy context meta
        ConcurrentHashMap<String, API> newApis = new ConcurrentHashMap<>();

        for (Api api : apis) {
            if (APIConstants.ApiType.WEB_SOCKET.equals(api.getApiType())) {
                WebSocketAPI webSocketAPI = new WebSocketAPI();
                webSocketAPI.init(api);
                String apiKey = getApiKey(webSocketAPI);
                newApis.put(apiKey, webSocketAPI);
            } else {
                RestAPI enforcerApi = new RestAPI();
                enforcerApi.init(api);
                String apiKey = getApiKey(enforcerApi);
                newApis.put(apiKey, enforcerApi);
                if ((EnvVarConfig.getInstance().isDuplicateVhostEnabled() &&
                        KNOWN_VHOST_PREFIXES.contains(api.getVhost().split("\\.")[0])) ||
                        (EnvVarConfig.getInstance().isDuplicateVhostEnabledPdp())) {
                    newApis.put(getApiKeyWithOrgId(enforcerApi), enforcerApi);
                    if (api.getVhost().startsWith("sandbox.")) {
                        String duplicatedVhostProd =
                                api.getVhost().replaceFirst("sandbox\\.", "prod-sandbox.");
                        newApis.put(getApiKeyWithOrgId(enforcerApi, duplicatedVhostProd), enforcerApi);
                    } else if (api.getVhost().startsWith("sandbox_dev.")) {
                        String duplicatedVhostDev =
                                api.getVhost().replaceFirst("sandbox_dev\\.", "dev-sandbox.");
                        newApis.put(getApiKeyWithOrgId(enforcerApi, duplicatedVhostDev), enforcerApi);
                    }
                }
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
        String vHost = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_VHOST_PARAM);
        String basePath = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_BASE_PATH_PARAM);
        String version = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_VERSION_PARAM);
        String apiKey = getApiKey(vHost, basePath, version);
        if (logger.isDebugEnabled()) {
            logger.debug("Looking for matching API with basepath: {} and version: {}", basePath, version);
        }

        return apis.get(apiKey);
    }

    public API getMatchedAPIByKey(String apikey) {
        return apis.get(apikey);
    }

    public WebSocketAPI getMatchedAPI(WebSocketFrameRequest webSocketFrameRequest) {
        Map<String, String> extAuthMetadata = webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap();
        String vHost = extAuthMetadata.get(APIConstants.GW_VHOST_PARAM);
        String basePath = extAuthMetadata.get(APIConstants.GW_BASE_PATH_PARAM);
        String version = extAuthMetadata.get(APIConstants.GW_VERSION_PARAM);
        String apiKey = getApiKey(vHost, basePath, version);
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

    // For WebSocket APIs since there are no resources in WebSocket APIs.
    public ResourceConfig getMatchedBasePath(API api, String basePath) {
        ResourceConfig resourceConfig = new ResourceConfig();
        if (api.getAPIConfig().getBasePath().equals(basePath)) {
            resourceConfig.setPath(basePath);
            resourceConfig.setMethod(ResourceConfig.HttpMethods.GET);
            resourceConfig.setSecuritySchemas(api.getAPIConfig().getApiSecurity());
        }
        return resourceConfig;
    }

    private String getApiKey(API api) {
        APIConfig apiConfig = api.getAPIConfig();
        return getApiKey(apiConfig.getVhost(), apiConfig.getBasePath(), apiConfig.getVersion());
    }

    private String getApiKeyWithOrgId(API api) {
        APIConfig apiConfig = api.getAPIConfig();
        return String.format("%s-%s:%s:%s", apiConfig.getOrganizationId(),
                apiConfig.getVhost(), apiConfig.getBasePath(), apiConfig.getVersion());
    }

    private String getApiKeyWithOrgId(API api, String vhost) {
        APIConfig apiConfig = api.getAPIConfig();
        return String.format("%s-%s:%s:%s", apiConfig.getOrganizationId(),
                vhost, apiConfig.getBasePath(), apiConfig.getVersion());
    }

    public String getApiKey(String vhost, String basePath, String version) {
        return String.format("%s:%s:%s", vhost, basePath, version);
    }
}
