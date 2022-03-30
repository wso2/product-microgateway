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

package org.wso2.choreo.connect.enforcer.analytics;

import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsMessage;
import io.opentelemetry.context.Scope;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.carbon.apimgt.common.analytics.collectors.impl.GenericRequestDataCollector;
import org.wso2.carbon.apimgt.common.analytics.exceptions.AnalyticsException;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.constants.MetadataConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the filter is for Analytics.
 * If the request is failed at enforcer (due to throttling, authentication failures) the analytics event is
 * published by the filter itself.
 * If the request is allowed to proceed, the dynamic metadata will be populated so that the analytics event can be
 * populated from grpc access logs within AccessLoggingService.
 */
public class AnalyticsFilter {
    private static final Logger logger = LogManager.getLogger(AnalyticsFilter.class);
    private static AnalyticsFilter analyticsFilter;
    private static AnalyticsEventPublisher publisher;

    private AnalyticsFilter() {
        Map<String, String> configuration =
                ConfigHolder.getInstance().getConfig().getAnalyticsConfig().getConfigProperties();
        boolean isChoreoDeployment = configuration.containsKey(AnalyticsConstants.IS_CHOREO_DEPLOYMENT_CONFIG_KEY)
                && configuration.get(AnalyticsConstants.IS_CHOREO_DEPLOYMENT_CONFIG_KEY)
                .toLowerCase().equals("true");
        String customAnalyticsPublisher =
                ConfigHolder.getInstance().getConfig().getAnalyticsConfig().getConfigProperties()
                        .get(AnalyticsConstants.PUBLISHER_IMPL_CONFIG_KEY);
        Map<String, String> publisherConfig = new HashMap<>(2);
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            // We are always expecting <String, String> Map as configuration.
            publisherConfig.put(entry.getKey(), getEnvValue(entry.getValue()).toString());
        }
        publisher = loadAnalyticsPublisher(customAnalyticsPublisher, isChoreoDeployment);
        if (publisher != null) {
            publisher.init(publisherConfig);
        }
    }

    public static AnalyticsFilter getInstance() {
        if (analyticsFilter == null) {
            synchronized (new Object()) {
                if (analyticsFilter == null) {
                    analyticsFilter = new AnalyticsFilter();
                }
            }
        }
        return analyticsFilter;
    }

    public void handleGRPCLogMsg(StreamAccessLogsMessage message) {
        if (publisher != null) {
            publisher.handleGRPCLogMsg(message);
        } else {
            logger.error("Cannot publish the analytics event as analytics publisher is null.",
                    ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 5102));
        }
    }

    public void handleWebsocketFrameRequest(WebSocketFrameRequest frameRequest) {
        if (publisher != null) {
            publisher.handleWebsocketFrameRequest(frameRequest);
        } else {
            logger.error("Cannot publish the analytics event as analytics publisher is null.",
                    ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 5102));
        }
    }

    public void handleSuccessRequest(RequestContext requestContext) {
        TracingSpan analyticsSpan = null;
        Scope analyticsSpanScope = null;
        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                analyticsSpan = Utils.startSpan(TracingConstants.ANALYTICS_SPAN, tracer);
                analyticsSpanScope = analyticsSpan.getSpan().makeCurrent();
                Utils.setTag(analyticsSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));
            }
            String apiName = requestContext.getMatchedAPI().getName();
            String apiVersion = requestContext.getMatchedAPI().getVersion();
            String apiType = requestContext.getMatchedAPI().getApiType();
            boolean isMockAPI = requestContext.getMatchedAPI().isMockedApi();
            AuthenticationContext authContext = AnalyticsUtils.getAuthenticationContext(requestContext);

            requestContext.addMetadataToMap(MetadataConstants.API_ID_KEY, AnalyticsUtils.getAPIId(requestContext));
            requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_KEY,
                    AnalyticsUtils.setDefaultIfNull(authContext.getApiPublisher()));
            requestContext.addMetadataToMap(MetadataConstants.API_NAME_KEY, apiName);
            requestContext.addMetadataToMap(MetadataConstants.API_VERSION_KEY, apiVersion);
            requestContext.addMetadataToMap(MetadataConstants.API_TYPE_KEY, apiType);
            requestContext.addMetadataToMap(MetadataConstants.IS_MOCK_API, String.valueOf(isMockAPI));

            String tenantDomain = FilterUtils.getTenantDomainFromRequestURL(
                    requestContext.getMatchedAPI().getBasePath());
            requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_TENANT_DOMAIN_KEY,
                    tenantDomain == null ? APIConstants.SUPER_TENANT_DOMAIN_NAME : tenantDomain);

            // Default Value would be PRODUCTION
            requestContext.addMetadataToMap(MetadataConstants.APP_KEY_TYPE_KEY,
                    authContext.getKeyType() == null ? APIConstants.API_KEY_TYPE_PRODUCTION : authContext.getKeyType());
            requestContext.addMetadataToMap(MetadataConstants.APP_UUID_KEY,
                    AnalyticsUtils.setDefaultIfNull(authContext.getApplicationUUID()));
            requestContext.addMetadataToMap(MetadataConstants.APP_NAME_KEY,
                    AnalyticsUtils.setDefaultIfNull(authContext.getApplicationName()));
            requestContext.addMetadataToMap(MetadataConstants.APP_OWNER_KEY,
                    AnalyticsUtils.setDefaultIfNull(authContext.getSubscriber()));

            requestContext.addMetadataToMap(MetadataConstants.CORRELATION_ID_KEY, requestContext.getRequestID());
            requestContext.addMetadataToMap(MetadataConstants.REGION_KEY,
                    ConfigHolder.getInstance().getEnvVarConfig().getEnforcerRegionId());

            // As in the matched API, only the resources under the matched resource template are selected.
            requestContext.addMetadataToMap(MetadataConstants.API_RESOURCE_TEMPLATE_KEY,
                    requestContext.getMatchedResourcePath().getPath());

            requestContext.addMetadataToMap(MetadataConstants.DESTINATION, resolveEndpoint(requestContext));

            requestContext.addMetadataToMap(MetadataConstants.API_ORGANIZATION_ID,
                    requestContext.getMatchedAPI().getOrganizationId());
            requestContext.addMetadataToMap(MetadataConstants.CLIENT_IP_KEY, requestContext.getClientIp());
            requestContext.addMetadataToMap(MetadataConstants.USER_AGENT_KEY,
                    AnalyticsUtils.setDefaultIfNull(requestContext.getHeaders().get("user-agent")));
        } finally {
            if (Utils.tracingEnabled()) {
                analyticsSpanScope.close();
                Utils.finishSpan(analyticsSpan);
            }
        }
    }

    public String resolveEndpoint(RequestContext requestContext) {

        // For MockAPIs there is no backend, Hence "MockImplementation" is added as the destination.
        if (requestContext.getMatchedAPI().isMockedApi()) {
            return "MockImplementation";
        }

        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        // KeyType could be sandbox only if the keytype is set fetched from the Eventhub
        if (authContext != null && authContext.getKeyType() != null
                && authContext.getKeyType().equals(APIConstants.API_KEY_TYPE_SANDBOX)) {
            // keyType is sandbox but the sandbox endpoints are null this will result in authentication failure.
            // Hence null scenario is impossible to occur.
            return requestContext.getMatchedAPI().getEndpoints().containsKey(APIConstants.API_KEY_TYPE_SANDBOX) ?
                    requestContext.getMatchedAPI().getEndpoints().get(APIConstants.API_KEY_TYPE_SANDBOX)
                            .getUrls().get(0) : "";
        }
        // This does not cause problems at the moment Since the current microgateway supports only one URL
        return requestContext.getMatchedAPI().getEndpoints().get(APIConstants.API_KEY_TYPE_PRODUCTION)
                .getUrls().get(0);
    }

    public void handleFailureRequest(RequestContext requestContext) {
        TracingSpan analyticsSpan = null;
        Scope analyticsSpanScope = null;

        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                analyticsSpan = Utils.startSpan(TracingConstants.ANALYTICS_FAILURE_SPAN, tracer);
                analyticsSpanScope = analyticsSpan.getSpan().makeCurrent();
                Utils.setTag(analyticsSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));

            }
            if (publisher == null) {
                logger.error("Cannot publish the failure event as analytics publisher is null.",
                        ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 5103));
                return;
            }
            ChoreoFaultAnalyticsProvider provider = new ChoreoFaultAnalyticsProvider(requestContext);
            // To avoid incrementing counter for options call
            if (provider.getProxyResponseCode() == 200 || provider.getProxyResponseCode() == 204) {
                return;
            }
            GenericRequestDataCollector dataCollector = new GenericRequestDataCollector(provider);
            try {
                dataCollector.collectData();
                logger.debug("Analytics event for failure event is published.");
            } catch (AnalyticsException e) {
                logger.error("Error while publishing the analytics event. ", e);
            }
        } finally {
            if (Utils.tracingEnabled()) {
                analyticsSpanScope.close();
                Utils.finishSpan(analyticsSpan);
            }
        }
    }

    private static AnalyticsEventPublisher loadAnalyticsPublisher(String className, boolean isChoreoDeployment) {

        // For the choreo deployment, class name need not to be provided.
        if (StringUtils.isEmpty(className)) {
            logger.debug("Proceeding with default analytics publisher.");
            if (isChoreoDeployment) {
                return new DefaultAnalyticsEventPublisher(AnalyticsConstants.CHOREO_RESPONSE_SCHEMA,
                        AnalyticsConstants.CHOREO_FAULT_SCHEMA);
            }
            return new DefaultAnalyticsEventPublisher();
        }

        try {
            Class<AnalyticsEventPublisher> clazz = (Class<AnalyticsEventPublisher>) Class.forName(className);
            Constructor<AnalyticsEventPublisher> constructor = clazz.getConstructor();
            AnalyticsEventPublisher publisher = constructor.newInstance();
            logger.info("Proceeding with the custom analytics publisher implementation: " + className);
            return publisher;
        } catch (ClassNotFoundException e) {
            logger.error("Error while loading the custom analytics publisher class.",
                    ErrorDetails.errorLog(LoggingConstants.Severity.MAJOR, 5105), e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            logger.error("Error while generating AnalyticsEventPublisherInstance from the class",
                    ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 5106), e);
        }
        return null;
    }

    // TODO: (RajithRoshan) Avoid Code duplication and process the map entries while initial env variable based
    // resolution.
    private Object getEnvValue(Object configValue) {
        if (configValue instanceof String) {
            String value = (String) configValue;
            return replaceEnvRegex(value);
        } else if (configValue instanceof char[]) {
            String value = String.valueOf((char[]) configValue);
            return replaceEnvRegex(value).toCharArray();
        }
        return configValue;
    }

    private String replaceEnvRegex(String value) {
        Matcher m = Pattern.compile("\\$env\\{(.*?)\\}").matcher(value);
        if (value.contains(Constants.ENV_PREFIX)) {
            while (m.find()) {
                String envName = value.substring(m.start() + 5, m.end() - 1);
                if (System.getenv(envName) != null) {
                    value = value.replace(value.substring(m.start(), m.end()), System.getenv(envName));
                }
            }
        }
        return value;
    }
}
