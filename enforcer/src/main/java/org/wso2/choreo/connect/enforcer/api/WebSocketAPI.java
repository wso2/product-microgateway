/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.api.Api;
import org.wso2.choreo.connect.enforcer.Filter;
import org.wso2.choreo.connect.enforcer.api.config.APIConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.cors.CorsFilter;
import org.wso2.choreo.connect.enforcer.security.AuthFilter;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketMetaDataFilter;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketResponseObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Specific implementation for a WebSocket API type APIs. Contains 2 filter chains to process initial HTTP request and
 * websocket frame metadata.
 */
public class WebSocketAPI implements API {

    private static final Logger logger = LogManager.getLogger(WebSocketAPI.class);
    private APIConfig apiConfig;
    private final List<Filter> filters = new ArrayList<>();
    private final List<Filter> upgradeFilters = new ArrayList<>();
    private String apiLifeCycleState;

    @Override
    public List<Filter> getFilters() {
        return null;
    }

    @Override
    public String init(Api api) {
        String vhost = api.getVhost();
        String basePath = api.getBasePath();
        String name = api.getTitle();
        String version = api.getVersion();
        String apiType = api.getApiType();
        List<String> securitySchemes = api.getSecuritySchemeList();

        this.apiLifeCycleState = api.getApiLifeCycleState();
        this.apiConfig = new APIConfig.Builder(name).vhost(vhost).basePath(basePath).version(version)
                .apiType(apiType).apiLifeCycleState(apiLifeCycleState)
                .securitySchema(securitySchemes).tier(api.getTier()).endpointSecurity(api.getEndpointSecurity())
                .authHeader(api.getAuthorizationHeader()).disableSecurity(api.getDisableSecurity()).build();
        initFilters();
        return basePath;
    }

    @Override
    public ResponseObject process(
            RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject();
        if (executeFilterChain(requestContext)) {
            responseObject.setStatusCode(APIConstants.StatusCodes.OK.getCode());
            if (requestContext.getResponseHeaders() != null && requestContext.getResponseHeaders().size() > 0) {
                responseObject.setHeaderMap(requestContext.getResponseHeaders());
            }
            logger.info(requestContext.getMetadataMap());
            responseObject.setMetaDataMap(requestContext.getMetadataMap());
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

    @Override
    public boolean executeFilterChain(RequestContext requestContext) {
        logger.info("normal filter chain");
        boolean proceed;
        for (Filter filter : getHttpFilters()) {
            proceed = filter.handleRequest(requestContext);
            logger.info("proceed:" + proceed);
            if (!proceed) {
                return false;
            }
        }
        return true;
    }


    public List<Filter> getUpgradeFilters() {
        return upgradeFilters;
    }

    public List<Filter> getHttpFilters() {
        return filters;
    }

    public void initFilters() {
        AuthFilter authFilter = new AuthFilter();
        authFilter.init(apiConfig);
        CorsFilter corsFilter = new CorsFilter();
        WebSocketMetaDataFilter metaDataFilter = new WebSocketMetaDataFilter();
        metaDataFilter.init(apiConfig);
        this.filters.add(corsFilter);
        this.filters.add(authFilter);
        this.filters.add(metaDataFilter);
    }

    public void initUpgradeFilters() {
        // TODO (LahiruUdayanga) - Initiate upgrade filter chain.
        // WebSocket throttle filter
        // WebSocket analytics filter
    }

    public WebSocketResponseObject processMetadata(RequestContext requestContext) {
        logger.info("XXXXXXXXX processMetadata"+requestContext.toString());
        return null;
    }


}
