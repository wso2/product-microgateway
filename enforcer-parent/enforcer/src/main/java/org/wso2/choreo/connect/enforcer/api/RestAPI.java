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
import org.wso2.choreo.connect.discovery.api.Certificate;
import org.wso2.choreo.connect.discovery.api.Operation;
import org.wso2.choreo.connect.discovery.api.Resource;
import org.wso2.choreo.connect.discovery.api.Scopes;
import org.wso2.choreo.connect.discovery.api.SecurityList;
import org.wso2.choreo.connect.discovery.api.SecurityScheme;
import org.wso2.choreo.connect.enforcer.analytics.AnalyticsFilter;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointSecurity;
import org.wso2.choreo.connect.enforcer.commons.model.MockedApiConfig;
import org.wso2.choreo.connect.enforcer.commons.model.MockedContentExamples;
import org.wso2.choreo.connect.enforcer.commons.model.MockedHeaderConfig;
import org.wso2.choreo.connect.enforcer.commons.model.MockedResponseConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.AuthHeaderDto;
import org.wso2.choreo.connect.enforcer.config.dto.FilterDTO;
import org.wso2.choreo.connect.enforcer.config.dto.MutualSSLDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.cors.CorsFilter;
import org.wso2.choreo.connect.enforcer.interceptor.MediationPolicyFilter;
import org.wso2.choreo.connect.enforcer.security.AuthFilter;
import org.wso2.choreo.connect.enforcer.security.mtls.MtlsUtils;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleFilter;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;
import org.wso2.choreo.connect.enforcer.util.MockImplUtils;

import java.security.KeyStore;
import java.security.KeyStoreException;
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
        Map<String, String> mtlsCertificateTiers = new HashMap<>();
        String mutualSSL = api.getMutualSSL();
        boolean applicationSecurity = api.getApplicationSecurity();

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
                resConfig.setPolicyConfig(Utils.genPolicyConfig(operation.getPolicies()));
                resConfig.setEndpoints(endpointClusterMap);
                resConfig.setMockApiConfig(getMockedApiOperationConfig(operation.getMockedApiConfig(),
                        operation.getMethod()));
                resources.add(resConfig);
            }
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

        KeyStore trustStore;
        try {
            trustStore = MtlsUtils.createTrustStore(api.getClientCertificatesList());
        } catch (KeyStoreException e) {
            throw new SecurityException(e);
        }

        for (Certificate certificate : api.getClientCertificatesList()) {
            mtlsCertificateTiers.put(certificate.getAlias(), certificate.getTier());
        }

        this.apiLifeCycleState = api.getApiLifeCycleState();
        this.apiConfig = new APIConfig.Builder(name).uuid(api.getId()).vhost(vhost).basePath(basePath).version(version)
                .resources(resources).apiType(apiType).apiLifeCycleState(apiLifeCycleState).tier(api.getTier())
                .apiSecurity(securityScopesMap).securitySchemeDefinitions(securitySchemeDefinitions)
                .disableSecurity(api.getDisableSecurity()).authHeader(api.getAuthorizationHeader())
                .endpoints(endpoints).endpointSecurity(endpointSecurity).mockedApi(api.getIsMockedApi())
                .trustStore(trustStore).organizationId(api.getOrganizationId())
                .mtlsCertificateTiers(mtlsCertificateTiers).mutualSSL(mutualSSL)
                .applicationSecurity(applicationSecurity).build();

        initFilters();
        return basePath;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject(requestContext.getRequestID());
        responseObject.setRequestPath(requestContext.getRequestPath());
        boolean analyticsEnabled = ConfigHolder.getInstance().getConfig().getAnalyticsConfig().isEnabled();

        populateRemoveAndProtectedHeaders(requestContext);
        boolean isExistsMatchedResourcePath = requestContext.getMatchedResourcePaths() != null &&
                requestContext.getMatchedResourcePaths().size() > 0;
        // This flag is used to apply cors filter
        boolean isOptionCall = requestContext.getRequestMethod().contains(HttpConstants.OPTIONS);
        if (!isExistsMatchedResourcePath && !isOptionCall) {
            // handle other not allowed non option calls
            requestContext.getProperties()
                    .put(APIConstants.MessageFormat.STATUS_CODE, APIConstants.StatusCodes.NOTFOUND.getCode());
            requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_CODE,
                    APIConstants.StatusCodes.NOTFOUND.getValue());
            requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_MESSAGE,
                    APIConstants.NOT_FOUND_MESSAGE);
            requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_DESCRIPTION,
                    APIConstants.NOT_FOUND_DESCRIPTION);
        }
        if ((isExistsMatchedResourcePath || isOptionCall) && executeFilterChain(requestContext)) {
            responseObject.setRemoveHeaderMap(requestContext.getRemoveHeaders());
            responseObject.setQueryParamsToRemove(requestContext.getQueryParamsToRemove());
            responseObject.setRemoveAllQueryParams(requestContext.isRemoveAllQueryParams());
            responseObject.setQueryParamsToAdd(requestContext.getQueryParamsToAdd());
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
            if (requestContext.getMatchedAPI().isMockedApi()) {
                MockImplUtils.processMockedApiCall(requestContext, responseObject);
                return responseObject;
            }
        } else {
            // If enforcer stops with a false, it will be passed directly to the client.
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

    private MockedApiConfig getMockedApiOperationConfig(
            org.wso2.choreo.connect.discovery.api.MockedApiConfig mockedApiConfig, String operationName) {
        MockedApiConfig configData = new MockedApiConfig();
        Map<String, MockedResponseConfig> responses = new HashMap<>();
        for (org.wso2.choreo.connect.discovery.api.MockedResponseConfig response : mockedApiConfig.getResponsesList()) {
            MockedResponseConfig responseData = new MockedResponseConfig();
            List<MockedHeaderConfig> headers = new ArrayList<>();
            for (org.wso2.choreo.connect.discovery.api.MockedHeaderConfig header : response.getHeadersList()) {
                MockedHeaderConfig headerConfig = new MockedHeaderConfig();
                headerConfig.setName(header.getName());
                headerConfig.setValue(header.getValue());
                headers.add(headerConfig);
            }
            responseData.setHeaders(headers);
            HashMap<String, MockedContentExamples> contentMap = new HashMap<>();

            for (org.wso2.choreo.connect.discovery.api.MockedContentConfig contentConfig : response.getContentList()) {
                MockedContentExamples mockedContentExamples = new MockedContentExamples();
                HashMap<String, String> exampleMap = new HashMap<>();
                for (org.wso2.choreo.connect.discovery.api.MockedContentExample exampleConfig :
                        contentConfig.getExamplesList()) {
                    exampleMap.put(exampleConfig.getRef(), exampleConfig.getBody());
                }
                mockedContentExamples.setExampleMap(exampleMap);
                contentMap.put(contentConfig.getContentType(), mockedContentExamples);
            }
            responseData.setContentMap(contentMap);
            responses.put(response.getCode(), responseData);
        }
        configData.setResponses(responses);
        logger.debug("Mock API config processed successfully for the " + operationName + " operation.");
        return configData;
    }

    private void initFilters() {
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

        MediationPolicyFilter mediationPolicyFilter = new MediationPolicyFilter();
        this.filters.add(mediationPolicyFilter);
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

    private void populateRemoveAndProtectedHeaders(RequestContext requestContext) {
        Map<String, SecuritySchemaConfig> securitySchemeDefinitions =
                requestContext.getMatchedAPI().getSecuritySchemeDefinitions();
        // API key headers are considered to be protected headers, such that the header would not be sent
        // to backend and traffic manager.
        // This would prevent leaking credentials, even if user is invoking unsecured resource with some
        // credentials.
        for (Map.Entry<String, SecuritySchemaConfig> entry : securitySchemeDefinitions.entrySet()) {
            SecuritySchemaConfig schema = entry.getValue();
            if (APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME.equalsIgnoreCase(schema.getType())) {
                if (APIConstants.SWAGGER_API_KEY_IN_HEADER.equals(schema.getIn())) {
                    requestContext.getProtectedHeaders().add(schema.getName());
                    requestContext.getRemoveHeaders().add(schema.getName());
                    continue;
                }
                if (APIConstants.SWAGGER_API_KEY_IN_QUERY.equals(schema.getIn())) {
                    requestContext.getQueryParamsToRemove().add(schema.getName());
                }
            }
        }

        // Internal-Key credential is considered to be protected headers, such that the header would not be sent
        // to backend and traffic manager.
        String internalKeyHeader = ConfigHolder.getInstance().getConfig().getAuthHeader()
                .getTestConsoleHeaderName().toLowerCase();
        requestContext.getRemoveHeaders().add(internalKeyHeader);
        // Avoid internal key being published to the Traffic Manager
        requestContext.getProtectedHeaders().add(internalKeyHeader);

        // Remove Authorization Header
        AuthHeaderDto authHeader = ConfigHolder.getInstance().getConfig().getAuthHeader();
        String authHeaderName = FilterUtils.getAuthHeaderName(requestContext);
        if (!authHeader.isEnableOutboundAuthHeader()) {
            requestContext.getRemoveHeaders().add(authHeaderName);
        }
        // Authorization Header should not be included in the throttle publishing event.
        requestContext.getProtectedHeaders().add(authHeaderName);

        // not allow clients to set cluster header manually
        requestContext.getRemoveHeaders().add(AdapterConstants.CLUSTER_HEADER);

        // Remove mTLS certificate header
        MutualSSLDto mtlsInfo = ConfigHolder.getInstance().getConfig().getMtlsInfo();
        String certificateHeaderName = FilterUtils.getCertificateHeaderName();
        if (!mtlsInfo.isEnableOutboundCertificateHeader()) {
            requestContext.getRemoveHeaders().add(certificateHeaderName);
        }
        // mTLS Certificate Header should not be included in the throttle publishing event.
        requestContext.getProtectedHeaders().add(certificateHeaderName);
    }
}
