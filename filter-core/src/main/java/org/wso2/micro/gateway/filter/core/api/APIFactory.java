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
package org.wso2.micro.gateway.filter.core.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.api.config.APIConfig;
import org.wso2.micro.gateway.filter.core.api.config.ResourceConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the meta data of all the APIS deployed in the gateway node. Adding/Updating API requires to register the
 * API with this factory class. When requests are received this factory instance will be queried to find the matching
 * API, and then request will be dispatched to that API.
 */
public class APIFactory {
    private static final Logger logger = LogManager.getLogger(APIFactory.class);

    private static APIFactory apiFactory;
    private ConcurrentHashMap<String, API> apiMap = new ConcurrentHashMap<>();

    private APIFactory() {

    }

    public static APIFactory getInstance() {
        if (apiFactory == null) {
            apiFactory = new APIFactory();
        }
        return apiFactory;
    }

    public void addAPI(Object apiDefinition, String type) {
        if ("http".equals(type)) {
            API api = new RestAPI();
            String uniqueAPIContext = api.init(apiDefinition);
            apiMap.putIfAbsent(uniqueAPIContext, api);
        }
    }

    public API getMatchedAPI(String requestPath, String method) {
        Optional<Map.Entry<String, API>> mapEntry = apiMap.entrySet().stream()
                .filter(map -> requestPath.startsWith(map.getKey())).filter(apiEntry -> {
                    ResourceConfig resourceConfigMatched = getMatchedResource(apiEntry.getValue(), requestPath, method);
                    if (resourceConfigMatched != null) {
                        return true;
                    }
                    return false;

                }).findFirst();
        if (mapEntry.isPresent()) {
            return mapEntry.get().getValue();
        }
        logger.error("No matching API found for the path : " + requestPath + " and method : " + method);
        return null;
    }

    public ResourceConfig getMatchedResource(API api, String requestPath, String method) {
        APIConfig apiConfig = api.getAPIConfig();
        String resourcePath = requestPath.substring(apiConfig.getBasePath().length());
        List<ResourceConfig> resourceConfigList = api.getAPIConfig().getResources();
        return resourceConfigList.stream().filter(resourceConfig -> resourceConfig.getPath().equals(resourcePath)).
                filter(resourceConfig -> (method == null) || resourceConfig.getMethod()
                        .equals(ResourceConfig.HttpMethods.valueOf(method))).findFirst().orElse(null);
    }
}
