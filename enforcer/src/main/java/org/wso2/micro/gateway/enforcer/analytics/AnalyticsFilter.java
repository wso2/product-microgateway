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

package org.wso2.micro.gateway.enforcer.analytics;

import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.analytics.AnalyticsServiceReferenceHolder;
import org.wso2.carbon.apimgt.common.gateway.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.gateway.analytics.collectors.impl.GenericRequestDataCollector;
import org.wso2.carbon.apimgt.common.gateway.analytics.exceptions.AnalyticsException;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.AnalyticsConstants;
import org.wso2.micro.gateway.enforcer.constants.MetadataConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;
import org.wso2.micro.gateway.enforcer.util.FilterUtils;

import java.util.HashMap;
import java.util.Map;

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
    private static final String AUTH_TOKEN_KEY = "auth.api.token";
    private static final String AUTH_URL = "auth.api.url";

    private AnalyticsFilter() {
        Map<String, String> configuration = new HashMap<>(2);
        configuration.put(AUTH_TOKEN_KEY, ConfigHolder.getInstance().getConfig().getAnalyticsConfig().getAuthToken());
        configuration.put(AUTH_URL, ConfigHolder.getInstance().getConfig().getAnalyticsConfig().getAuthURL());
        AnalyticsServiceReferenceHolder.getInstance().setConfigurations(configuration);
        // TODO: (VirajSalaka) Load Class
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
        for (int i = 0; i < message.getHttpLogs().getLogEntryCount(); i++) {
            HTTPAccessLogEntry logEntry = message.getHttpLogs().getLogEntry(i);
            logger.trace("Received logEntry from Router " + message.getIdentifier().getNode() +
                    " : " + message.toString());
            if (doNotPublishEvent(logEntry)) {
                logger.debug("LogEntry is ignored as it is already published by the enforcer.");
                continue;
            }
            AnalyticsDataProvider provider = new MgwAnalyticsProvider(logEntry);
            // If the APIName is not available, the event should not be published.
            // 404 errors are not logged due to this.
            if (provider.getFaultType() == FaultCategory.OTHER) {
                continue;
            }
            GenericRequestDataCollector dataCollector = new GenericRequestDataCollector(provider);
            try {
                dataCollector.collectData();
                logger.debug("Event is published.");
            } catch (AnalyticsException e) {
                logger.error("Error while publishing the event to the analytics portal.", e);
            }
        }
    }

    public void handleSuccessRequest(RequestContext requestContext) {
        String apiName = requestContext.getMathedAPI().getAPIConfig().getName();
        String apiVersion = requestContext.getMathedAPI().getAPIConfig().getVersion();
        String apiType = requestContext.getMathedAPI().getAPIConfig().getApiType();
        AuthenticationContext authContext = AnalyticsUtils.getAuthenticationContext(requestContext);

        requestContext.addMetadataToMap(MetadataConstants.API_ID_KEY, AnalyticsUtils.getAPIId(requestContext));
        requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getApiPublisher()));
        requestContext.addMetadataToMap(MetadataConstants.API_NAME_KEY, apiName);
        requestContext.addMetadataToMap(MetadataConstants.API_VERSION_KEY, apiVersion);
        requestContext.addMetadataToMap(MetadataConstants.API_TYPE_KEY, apiType);
        requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_TENANT_DOMAIN_KEY,
                FilterUtils.getTenantDomainFromRequestURL(
                        requestContext.getMathedAPI().getAPIConfig().getBasePath()) == null
                        ? APIConstants.SUPER_TENANT_DOMAIN_NAME
                        : requestContext.getMathedAPI().getAPIConfig().getBasePath());

        // Default Value would be PRODUCTION
        requestContext.addMetadataToMap(MetadataConstants.APP_KEY_TYPE_KEY,
                authContext.getKeyType() == null ? APIConstants.API_KEY_TYPE_PRODUCTION : authContext.getKeyType());
        requestContext.addMetadataToMap(MetadataConstants.APP_ID_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getApplicationId()));
        requestContext.addMetadataToMap(MetadataConstants.APP_NAME_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getApplicationName()));
        requestContext.addMetadataToMap(MetadataConstants.APP_OWNER_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getSubscriber()));

        requestContext.addMetadataToMap(MetadataConstants.CORRELATION_ID_KEY, requestContext.getRequestID());
        // TODO: (VirajSalaka) Move this out of this method as these remain static
        requestContext.addMetadataToMap(MetadataConstants.REGION_KEY, AnalyticsUtils.setDefaultIfNull(null));

        // As in the matched API, only the resources under the matched resource template are selected.
        requestContext.addMetadataToMap(MetadataConstants.API_RESOURCE_TEMPLATE_KEY,
                requestContext.getMatchedResourcePath().getPath());

        requestContext.addMetadataToMap(MetadataConstants.DESTINATION, resolveEndpoint(requestContext));
    }

    private String resolveEndpoint(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        // KeyType could be sandbox only if the keytype is set fetched from the Eventhub
        if (authContext != null && authContext.getKeyType() != null
                && authContext.getKeyType().equals(APIConstants.API_KEY_TYPE_SANDBOX)) {
            // keyType is sandbox but the sandbox endpoints are null this will result in authentication failure.
            // Hence null scenario is impossible to occur.
            return requestContext.getMathedAPI().getAPIConfig().getSandboxUrls() != null ?
                    requestContext.getMathedAPI().getAPIConfig().getSandboxUrls().get(0) : "";
        }
        // This does not cause problems at the moment Since the current microgateway supports only one URL
        return requestContext.getMathedAPI().getAPIConfig().getProductionUrls().get(0);
    }

    public void handleFailureRequest(RequestContext requestContext) {
        MgwFaultAnalyticsProvider provider = new MgwFaultAnalyticsProvider(requestContext);
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
    }

    private boolean doNotPublishEvent(HTTPAccessLogEntry logEntry) {
        // If ext_auth_denied request comes, the event is already published from the enforcer.
        // There is a chance that the analytics event is published from enforcer and then result in ext_authz_error
        // responseCodeDetail due to some error/exception within enforcer implementation. This scenario is not
        // handled as it should be fixed from enforcer.
        return (!StringUtils.isEmpty(logEntry.getResponse().getResponseCodeDetails()))
                && logEntry.getResponse().getResponseCodeDetails()
                .equals(AnalyticsConstants.EXT_AUTH_DENIED_RESPONSE_DETAIL)
                // Token endpoint calls needs to be removed as well
                && (!AnalyticsConstants.TOKEN_ENDPOINT_PATH.equals(logEntry.getRequest().getOriginalPath()));
    }
}
