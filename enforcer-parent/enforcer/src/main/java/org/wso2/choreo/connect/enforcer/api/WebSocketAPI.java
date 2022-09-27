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
import org.wso2.choreo.connect.discovery.api.Operation;
import org.wso2.choreo.connect.discovery.api.Resource;
import org.wso2.choreo.connect.discovery.api.Scopes;
import org.wso2.choreo.connect.discovery.api.SecurityList;
import org.wso2.choreo.connect.discovery.api.SecurityScheme;
import org.wso2.choreo.connect.enforcer.analytics.AnalyticsFilter;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointSecurity;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.cors.CorsFilter;
import org.wso2.choreo.connect.enforcer.interceptor.MediationPolicyFilter;
import org.wso2.choreo.connect.enforcer.security.AuthFilter;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleConstants;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleFilter;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketMetaDataFilter;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketThrottleFilter;
import org.wso2.choreo.connect.enforcer.websocket.WebSocketThrottleResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specific implementation for a WebSocket API type APIs. Contains 2 filter chains to process initial HTTP request and
 * websocket frame data.
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
        Map<String, SecuritySchemaConfig> securitySchemes = new HashMap<>();
        Map<String, List<String>> apiSecurity = new HashMap<>();
        Map<String, EndpointCluster> endpoints = new HashMap<>();
        List<ResourceConfig> resources = new ArrayList<>();

        EndpointCluster productionEndpoints = Utils.processEndpoints(api.getProductionEndpoints());
        EndpointCluster sandboxEndpoints = Utils.processEndpoints(api.getSandboxEndpoints());
        if (productionEndpoints != null) {
            endpoints.put(APIConstants.API_KEY_TYPE_PRODUCTION, productionEndpoints);
        }
        if (sandboxEndpoints != null) {
            endpoints.put(APIConstants.API_KEY_TYPE_SANDBOX, sandboxEndpoints);
        }

        for (SecurityScheme securityScheme : api.getSecuritySchemeList()) {
            if (securityScheme.getType() != null) {
                String schemaType = securityScheme.getType();
                SecuritySchemaConfig securitySchemaConfig = new SecuritySchemaConfig();
                securitySchemaConfig.setDefinitionName(securityScheme.getDefinitionName());
                securitySchemaConfig.setType(schemaType);
                securitySchemaConfig.setName(securityScheme.getName());
                securitySchemaConfig.setIn(securityScheme.getIn());
                securitySchemes.put(schemaType, securitySchemaConfig);
            }
        }

        for (SecurityList securityList : api.getSecurityList()) {
            for (Map.Entry<String, Scopes> entry : securityList.getScopeListMap().entrySet()) {
                apiSecurity.put(entry.getKey(), new ArrayList<>());
                if (entry.getValue() != null && entry.getValue().getScopesList().size() > 0) {
                    List<String> scopeList = new ArrayList<>(entry.getValue().getScopesList());
                    apiSecurity.replace(entry.getKey(), scopeList);
                }
                // only supports security scheme OR combinations. Example -
                // Security:
                // - api_key: []
                //   oauth: [] <-- AND operation is not supported hence ignoring oauth here.
                break;
            }
        }

        EndpointSecurity endpointSecurity = new EndpointSecurity();
        if (api.getEndpointSecurity().hasProductionSecurityInfo()) {
            endpointSecurity.setProductionSecurityInfo(
                    APIProcessUtils.convertProtoEndpointSecurity(
                            api.getEndpointSecurity().getProductionSecurityInfo()));
        }
        if (api.getEndpointSecurity().hasSandBoxSecurityInfo()) {
            endpointSecurity.setSandBoxSecurityInfo(
                    APIProcessUtils.convertProtoEndpointSecurity(
                            api.getEndpointSecurity().getSandBoxSecurityInfo()));
        }
        for (Resource res : api.getResourcesList()) {
            for (Operation operation : res.getMethodsList()) {
                ResourceConfig resConfig = Utils.buildResource(operation, res.getPath(), apiSecurity);
                resConfig.setPolicyConfig(Utils.genPolicyConfig(operation.getPolicies()));
                resConfig.setTier(api.getTier());
                resources.add(resConfig);
            }
        }

        this.apiLifeCycleState = api.getApiLifeCycleState();
        this.apiConfig = new APIConfig.Builder(name).uuid(api.getId()).vhost(vhost).basePath(basePath).version(version)
                .apiType(apiType).apiLifeCycleState(apiLifeCycleState)
                .apiSecurity(apiSecurity).tier(api.getTier()).endpointSecurity(endpointSecurity)
                .authHeader(api.getAuthorizationHeader()).disableSecurity(api.getDisableSecurity())
                .organizationId(api.getOrganizationId()).endpoints(endpoints).resources(resources)
                .securitySchemeDefinitions(securitySchemes).build();
        initFilters();
        initUpgradeFilters();
        return basePath;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject();
        responseObject.setRequestPath(requestContext.getRequestPath());
        boolean analyticsEnabled = ConfigHolder.getInstance().getConfig().getAnalyticsConfig().isEnabled();
        if (executeFilterChain(requestContext)) {
            if (analyticsEnabled) {
                AnalyticsFilter.getInstance().handleSuccessRequest(requestContext);
            }
            responseObject.setStatusCode(APIConstants.StatusCodes.OK.getCode());
            responseObject.setQueryParamsToAdd(requestContext.getQueryParamsToAdd());
            if (requestContext.getAddHeaders() != null && requestContext.getAddHeaders().size() > 0) {
                responseObject.setHeaderMap(requestContext.getAddHeaders());
            }
            logger.debug("ext_authz metadata: {}", requestContext.getMetadataMap());
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
            if (requestContext.getAddHeaders() != null && requestContext.getAddHeaders().size() > 0) {
                responseObject.setHeaderMap(requestContext.getAddHeaders());
            }
            if (analyticsEnabled && !FilterUtils.isSkippedAnalyticsFaultEvent(responseObject.getErrorCode())) {
                AnalyticsFilter.getInstance().handleFailureRequest(requestContext);
                responseObject.setMetaDataMap(new HashMap<>(0));
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
        boolean proceed;
        for (Filter filter : getHttpFilters()) {
            proceed = filter.handleRequest(requestContext);
            if (!proceed) {
                return false;
            }
        }
        return true;
    }

    public boolean executeUpgradeFilterChain(RequestContext requestContext) {
        boolean proceed;
        for (Filter filter : getUpgradeFilters()) {
            proceed = filter.handleRequest(requestContext);
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
        // Cors Filter
        CorsFilter corsFilter = new CorsFilter();
        this.filters.add(corsFilter);
        // PathRewriteFilter
        MediationPolicyFilter mediationPolicyFilter = new MediationPolicyFilter();
        this.filters.add(mediationPolicyFilter);
        // Auth Filter
        AuthFilter authFilter = new AuthFilter();
        authFilter.init(apiConfig, null);
        this.filters.add(authFilter);
        // Throttle Filter if throttling is enabled
        if (ConfigHolder.getInstance().getConfig().getThrottleConfig().isGlobalPublishingEnabled()) {
            ThrottleFilter throttleFilter = new ThrottleFilter();
            throttleFilter.init(apiConfig, null);
            this.filters.add(throttleFilter);
        }
        // WebSocketMetadata filter
        WebSocketMetaDataFilter metaDataFilter = new WebSocketMetaDataFilter();
        metaDataFilter.init(apiConfig, null);
        this.filters.add(metaDataFilter);
    }

    public void initUpgradeFilters() {
        // TODO (LahiruUdayanga) - Initiate upgrade filter chain.
        // WebSocket throttle filter
        // WebSocket analytics filter
        if (ConfigHolder.getInstance().getConfig().getThrottleConfig().isGlobalPublishingEnabled()) {
            WebSocketThrottleFilter webSocketThrottleFilter = new WebSocketThrottleFilter();
            webSocketThrottleFilter.init(apiConfig, null);
            this.upgradeFilters.add(webSocketThrottleFilter);
        }
    }

    public WebSocketThrottleResponse processFramedata(RequestContext requestContext) {
        logger.trace("processFramedata called for websocket frame with basepath : {}", requestContext
                .getMatchedAPI().getBasePath());
        if (executeUpgradeFilterChain(requestContext)) {
            WebSocketThrottleResponse webSocketThrottleResponse = new WebSocketThrottleResponse();
            webSocketThrottleResponse.setOkState();
            return webSocketThrottleResponse;
        }
        WebSocketThrottleResponse webSocketThrottleResponse = new WebSocketThrottleResponse();
        webSocketThrottleResponse.setOverLimitState();
        if (requestContext.getProperties().containsKey(APIConstants.MessageFormat.ERROR_CODE)) {
            webSocketThrottleResponse.setApimErrorCode(Integer.parseInt(requestContext.getProperties()
                    .get(APIConstants.MessageFormat.ERROR_CODE).toString()));
        } else {
            webSocketThrottleResponse.setApimErrorCode(ThrottleConstants.THROTTLE_CONDITION_UNKNOWN);
            logger.error("Error code is not assigned for the websocket throttle response. : " + requestContext
                    .getMatchedAPI().getBasePath(),
                    ErrorDetails.errorLog(LoggingConstants.Severity.MAJOR, 5200));
        }
        webSocketThrottleResponse.setThrottlePeriod(
                (Long) requestContext.getProperties().get(ThrottleConstants.HEADER_RETRY_AFTER));
        return webSocketThrottleResponse;
    }
}
