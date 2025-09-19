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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.api.Api;
import org.wso2.choreo.connect.discovery.api.ChoreoComponentInfo;
import org.wso2.choreo.connect.discovery.api.Operation;
import org.wso2.choreo.connect.discovery.api.Resource;
import org.wso2.choreo.connect.discovery.api.Scopes;
import org.wso2.choreo.connect.discovery.api.SecurityList;
import org.wso2.choreo.connect.discovery.api.SecurityScheme;
import org.wso2.choreo.connect.enforcer.analytics.AnalyticsFilter;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.BackendJWTConfiguration;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointSecurity;
import org.wso2.choreo.connect.enforcer.commons.model.ExtendedOperation;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.FilterDTO;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.cors.CorsFilter;
import org.wso2.choreo.connect.enforcer.security.AuthFilter;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleFilter;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Specific implementation for a Rest API type APIs.
 */
public class RestAPI implements API {
    private static final Logger logger = LogManager.getLogger(RestAPI.class);
    private final List<Filter> filters = new ArrayList<>();
    private APIConfig apiConfig;
    private String apiLifeCycleState;

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public String init(Api api) {
        String vhost = api.getVhost();
        String basePath = api.getBasePath();
        String name = api.getTitle();
        String version = api.getVersion();
        String apiType = api.getApiType();
        Map<String, EndpointCluster> endpoints = new HashMap<>();
        Map<String, SecuritySchemaConfig> securitySchemeDefinitions = new HashMap<>();
        Map<String, List<String>> securityScopesMap = new HashMap<>();
        List<ResourceConfig> resources = new ArrayList<>();
        EndpointSecurity endpointSecurity = new EndpointSecurity();
        BackendJWTConfiguration backendJWTConfiguration = new BackendJWTConfiguration();
        List<ExtendedOperation> extendedOperations = new ArrayList<>();

        EndpointCluster productionEndpoints = Utils.processEndpoints(api.getProductionEndpoints());
        EndpointCluster sandboxEndpoints = Utils.processEndpoints(api.getSandboxEndpoints());
        if (productionEndpoints != null) {
            endpoints.put(APIConstants.API_KEY_TYPE_PRODUCTION, productionEndpoints);
        }
        if (sandboxEndpoints != null) {
            endpoints.put(APIConstants.API_KEY_TYPE_SANDBOX, sandboxEndpoints);
        }
        if (api.getEnableBackendJWT() && api.getBackendJWTConfiguration() != null) {
            backendJWTConfiguration.setAudiences(api.getBackendJWTConfiguration().getAudiencesList());
        }

        for (SecurityScheme securityScheme : api.getSecuritySchemeList()) {
            if (securityScheme.getType() != null) {
                String definitionName = securityScheme.getDefinitionName();
                SecuritySchemaConfig securitySchemaConfig = new SecuritySchemaConfig();
                securitySchemaConfig.setDefinitionName(definitionName);
                securitySchemaConfig.setType(securityScheme.getType());
                securitySchemaConfig.setName(securityScheme.getName());
                securitySchemaConfig.setIn(securityScheme.getIn());
                securitySchemeDefinitions.put(definitionName, securitySchemaConfig);
            }
        }

        for (SecurityList securityList : api.getSecurityList()) {
            for (Map.Entry<String, Scopes> entry : securityList.getScopeListMap().entrySet()) {
                securityScopesMap.put(entry.getKey(), new ArrayList<>());
                if (entry.getValue() != null && entry.getValue().getScopesList().size() > 0) {
                    List<String> scopeList = new ArrayList<>(entry.getValue().getScopesList());
                    securityScopesMap.replace(entry.getKey(), scopeList);
                }
                // only supports security scheme OR combinations. Example -
                // Security:
                // - api_key: []
                //   oauth: [] <-- AND operation is not supported hence ignoring oauth here.
                break;
            }
        }

        for (Resource res : api.getResourcesList()) {
            Map<String, EndpointCluster> endpointClusterMap = new HashMap();
            EndpointCluster prodEndpointCluster = Utils.processEndpoints(res.getProductionEndpoints());
            EndpointCluster sandEndpointCluster = Utils.processEndpoints(res.getSandboxEndpoints());
            if (prodEndpointCluster != null) {
                endpointClusterMap.put(APIConstants.API_KEY_TYPE_PRODUCTION, prodEndpointCluster);
            }
            if (sandEndpointCluster != null) {
                endpointClusterMap.put(APIConstants.API_KEY_TYPE_SANDBOX, sandEndpointCluster);
            }

            for (Operation operation : res.getMethodsList()) {
                ResourceConfig resConfig = Utils.buildResource(operation, res.getPath(), securityScopesMap);
                resConfig.setEndpoints(endpointClusterMap);
                resources.add(resConfig);
            }
        }

        for (org.wso2.choreo.connect.discovery.api.ExtendedOperation extendedOperation
                : api.getExtendedOperationsList()) {
            ExtendedOperation extendedOperationConfig = new ExtendedOperation();
            extendedOperationConfig.setName(extendedOperation.getName());
            extendedOperationConfig.setVerb(extendedOperation.getVerb());
            extendedOperationConfig.setDescription(extendedOperation.getDescription());
            extendedOperationConfig.setSchema(extendedOperation.getSchema());
            extendedOperationConfig.setMode(extendedOperation.getMode());
            if (extendedOperation.getProxyMapping() != null) {
                extendedOperationConfig.setApiName(extendedOperation.getProxyMapping().getName());
                extendedOperationConfig.setApiVersion(extendedOperation.getProxyMapping().getVersion());
                extendedOperationConfig.setApiContext(extendedOperation.getProxyMapping().getContext());
                extendedOperationConfig.setApiTarget(extendedOperation.getProxyMapping().getTarget());
                extendedOperationConfig.setApiVerb(extendedOperation.getProxyMapping().getVerb());
            }
            if (extendedOperation.getBackendMapping() != null) {
                extendedOperationConfig.setBackendEndpoint(extendedOperation.getBackendMapping().getEndpoint());
                extendedOperationConfig.setBackendTarget(extendedOperation.getBackendMapping().getTarget());
                extendedOperationConfig.setBackendVerb(extendedOperation.getBackendMapping().getVerb());
            }
            extendedOperations.add(extendedOperationConfig);
        }

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

        org.wso2.choreo.connect.enforcer.commons.model.ChoreoComponentInfo choreoComponentInfo = getChoreoComponentInfo(
                api);

        this.apiLifeCycleState = api.getApiLifeCycleState();
        this.apiConfig = new APIConfig.Builder(name).uuid(api.getId()).vhost(vhost).basePath(basePath).version(version)
                .resources(resources).apiType(apiType).apiLifeCycleState(apiLifeCycleState).tier(api.getTier())
                .apiSecurity(securityScopesMap).securitySchemeDefinitions(securitySchemeDefinitions)
                .disableSecurity(api.getDisableSecurity()).authHeader(api.getAuthorizationHeader())
                .apiKeyHeader(api.getApiKeyHeader()).endpoints(endpoints).endpointSecurity(endpointSecurity)
                .organizationId(api.getOrganizationId()).apiProvider(api.getApiProvider())
                .enableBackendJWT(api.getEnableBackendJWT()).backendJWTConfiguration(backendJWTConfiguration)
                .deploymentType(api.getDeploymentType())
                .environmentId(api.getEnvironmentId())
                .choreoEnvironmentId(api.getChoreoEnvironmentId())
                .environmentName(api.getEnvironmentName())
                .choreoComponentInfo(choreoComponentInfo)
                .extendedOperations(extendedOperations)
                .build();

        initFilters();
        return basePath;
    }

    private static org.wso2.choreo.connect.enforcer.commons.model.ChoreoComponentInfo getChoreoComponentInfo(
            Api api) {
        ChoreoComponentInfo infoProto = api.getChoreoComponentInfo();
        org.wso2.choreo.connect.enforcer.commons.model.ChoreoComponentInfo choreoComponentInfo =
                new org.wso2.choreo.connect.enforcer.commons.model.ChoreoComponentInfo();
        if (infoProto == null) {
            return choreoComponentInfo;
        }
        choreoComponentInfo.setOrganizationID(infoProto.getOrganizationID());
        choreoComponentInfo.setProjectID(infoProto.getProjectID());
        choreoComponentInfo.setComponentID(infoProto.getComponentID());
        choreoComponentInfo.setVersionID(infoProto.getVersionID());
        return choreoComponentInfo;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject(requestContext.getRequestID());
        responseObject.setRequestPath(requestContext.getRequestPath());
        responseObject.setApiUuid(apiConfig.getUuid());
        boolean analyticsEnabled = ConfigHolder.getInstance().getConfig().getAnalyticsConfig().isEnabled();

        Utils.populateRemoveAndProtectedHeaders(requestContext);

        if (executeFilterChain(requestContext)) {
            responseObject.setRemoveHeaderMap(requestContext.getRemoveHeaders());
            responseObject.setQueryParamsToRemove(requestContext.getQueryParamsToRemove());
            responseObject.setQueryParamMap(requestContext.getQueryParameters());
            responseObject.setStatusCode(APIConstants.StatusCodes.OK.getCode());
            if (requestContext.getAddHeaders() != null && requestContext.getAddHeaders().size() > 0) {
                responseObject.setHeaderMap(requestContext.getAddHeaders());
            }
            if (analyticsEnabled) {
                AnalyticsFilter.getInstance().handleSuccessRequest(requestContext);
            }
            // set metadata for interceptors
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
            if (analyticsEnabled && !APIConstants.CORS_FAILURE.equals(requestContext.getExtAuthDetails())
                    && !FilterUtils.isSkippedAnalyticsFaultEvent(responseObject.getErrorCode())) {
                AnalyticsFilter.getInstance().handleFailureRequest(requestContext);
            }
            responseObject.setMetaDataMap(requestContext.getMetadataMap());
        }

        return responseObject;
    }

    @Override
    public APIConfig getAPIConfig() {
        return this.apiConfig;
    }

    private void initFilters() {
        // TODO : re-vist the logic with apim prototype implemetation

        AuthFilter authFilter = new AuthFilter();
        authFilter.init(apiConfig, null);
        this.filters.add(authFilter);

        // enable throttle filter
        ThrottleFilter throttleFilter = new ThrottleFilter();
        throttleFilter.init(apiConfig, null);
        this.filters.add(throttleFilter);

        loadCustomFilters(apiConfig);

        // CORS filter is added as the first filter, and it is not customizable.
        CorsFilter corsFilter = new CorsFilter();
        this.filters.add(0, corsFilter);
    }

    private void loadCustomFilters(APIConfig apiConfig) {
        FilterDTO[] customFilters = ConfigHolder.getInstance().getConfig().getCustomFilters();
        // Needs to sort the filter in ascending order to position the filter in the given position.
        Arrays.sort(customFilters, Comparator.comparing(FilterDTO::getPosition));
        Map<String, Filter> filterImplMap = new HashMap<>(customFilters.length);
        ServiceLoader<Filter> loader = ServiceLoader.load(Filter.class);
        for (Filter filter : loader) {
            filterImplMap.put(filter.getClass().getName(), filter);
        }

        for (FilterDTO filterDTO : customFilters) {
            if (filterImplMap.containsKey(filterDTO.getClassName())) {
                if (filterDTO.getPosition() <= 0 || filterDTO.getPosition() - 1 > filters.size()) {
                    logger.error("Position provided for the filter is invalid. "
                            + filterDTO.getClassName() + " : " + filterDTO.getPosition() + "(Filters list size is "
                            + filters.size() + ")");
                    continue;
                }
                Filter filter = filterImplMap.get(filterDTO.getClassName());
                filter.init(apiConfig, filterDTO.getConfigProperties());
                // Since the position starts from 1
                this.filters.add(filterDTO.getPosition() - 1, filter);
            } else {
                logger.error("No Filter Implementation is found in the classPath under the provided name : "
                        + filterDTO.getClassName());
            }
        }
    }
}
