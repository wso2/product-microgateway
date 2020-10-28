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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.Filter;
import org.wso2.micro.gateway.filter.core.api.config.APIConfig;
import org.wso2.micro.gateway.filter.core.api.config.ResourceConfig;
import org.wso2.micro.gateway.filter.core.security.AuthFilter;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specific implementation for a Rest API type APIs.
 */
public class RestAPI implements API {
    private static final Logger logger = LogManager.getLogger(RestAPI.class);

    private APIConfig apiConfig;
    private OpenAPI openAPI;
    private List<Filter> filters = new ArrayList<>();

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public String init(Object apiDefinition) {
        try {

            this.openAPI = (OpenAPI) apiDefinition;
            String basePath = resolveBasePath();
            this.apiConfig = new APIConfig.Builder(openAPI.getInfo().getTitle()).version(openAPI.getInfo().getVersion())
                    .basePath(basePath).resources(getResourcesFromOpenAPI(openAPI.getPaths())).build();
            initFilters();
            return basePath;
        } catch (ClassCastException e) {
            //API initialization should continue after a class cast exception
            logger.error("Error while reading the open API definition", e);
        }
        return null;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject();
        if (executeFilterChain(requestContext)) {
            responseObject.setStatusCode(200);
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

    private List<ResourceConfig> getResourcesFromOpenAPI(Paths paths) {
        List<ResourceConfig> resourceConfigs = new ArrayList<>();
        paths.forEach((name, path) -> {
            path.readOperationsMap().forEach((httpMethod, operation) -> {
                ResourceConfig resourceConfig = new ResourceConfig();
                resourceConfig.setPath(name);
                resourceConfig.setMethod(ResourceConfig.HttpMethods.valueOf(httpMethod.name()));
                resourceConfigs.add(resourceConfig);
            });
        });
        return resourceConfigs;
    }

    private String resolveBasePath() {
        String basePath = "/";
        if (openAPI.getExtensions() != null && openAPI.getExtensions().containsKey(APIConstants.X_WSO2_BASE_PATH)) {
            return openAPI.getExtensions().get(APIConstants.X_WSO2_BASE_PATH).toString();
        }
        List<Server> servers = openAPI.getServers();
        if (servers != null) {
            Server server = servers.get(0);
            String url = replaceOpenAPIServerTemplate(server);
            try {
                basePath = new URI(url).getPath();
            } catch (URISyntaxException e) {
                logger.error(e);
            }
        }
        return basePath;
    }

    /**
     * Open API server object can have server templating. Ex: https://{customerId}.saas-app.com:{port}/v2.
     * When adding the back end url this method will replace the template values with the default value.
     *
     * @param server {@link Server} object of the open API definition
     * @return templated server url replaced with default values
     */
    private static String replaceOpenAPIServerTemplate(Server server) {
        //server url templating can have urls similar to 'https://{customerId}.saas-app.com:{port}/v2'
        String url = server.getUrl();
        Pattern serverTemplate = Pattern.compile("\\{.*?}");
        Matcher matcher = serverTemplate.matcher(url);
        while (matcher.find()) {
            if (server.getVariables() != null && server.getVariables()
                    .containsKey(matcher.group(0).substring(1, matcher.group(0).length() - 1))) {
                String defaultValue = server.getVariables()
                        .get(matcher.group(0).substring(1, matcher.group(0).length() - 1)).getDefault();
                url = url.replaceAll("/" + matcher.group(0), defaultValue);
            } else {
                logger.error("Open API server url templating is used for the url : " + url
                        + ". But default values is not provided for the variable '" + matcher.group(0)
                        + "'. Hence correct url will not be resolved during the runtime "
                        + "unless url is overridden during the runtime");
            }
        }
        return url;
    }

    private void initFilters() {
        AuthFilter authFilter = new AuthFilter();
        authFilter.init(apiConfig);
        this.filters.add(authFilter);
    }
}
