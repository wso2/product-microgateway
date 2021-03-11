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

package org.wso2.micro.gateway.enforcer.analytics;

import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.analytics.collectors.impl.GenericRequestDataCollector;
import org.wso2.carbon.apimgt.common.gateway.analytics.exceptions.AnalyticsException;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.MetadataConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

/**
 * This is the filter is for Analytics.
 */
public class AnalyticsFilter implements Filter {
    private static final Logger logger = LogManager.getLogger(AnalyticsFilter.class);
    private static AnalyticsFilter analyticsFilter;

    private AnalyticsFilter() {
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

    public boolean handleMsg(StreamAccessLogsMessage message) {
        // TODO (amalimatharaarachchi) process message and set analytics data'
        return true;
    }

    @Override
    public void init(APIConfig apiConfig) {
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        String apiName = requestContext.getMathedAPI().getAPIConfig().getName();
        String apiVersion = requestContext.getMathedAPI().getAPIConfig().getVersion();
        // TODO: (VirajSalaka) Decide on whether to include/exclude the options requests, Cors requests
        AuthenticationContext authContext = AnalyticsUtils.getAuthenticationContext(requestContext);

        requestContext.addMetadataToMap(MetadataConstants.API_ID_KEY, AnalyticsUtils.getAPIId(requestContext));
        requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getApiPublisher()));
        requestContext.addMetadataToMap(MetadataConstants.API_NAME_KEY, apiName);
        requestContext.addMetadataToMap(MetadataConstants.API_VERSION_KEY, apiVersion);
        // TODO: (VirajSalaka) Retrieve from APIConfig
        requestContext.addMetadataToMap(MetadataConstants.API_TYPE_KEY, "HTTP");
        // TODO: (VirajSalaka) Retrieve From Configuration
        requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_TENANT_DOMAIN_KEY, "carbon.super");

        // Default Value would be PRODUCTION
        requestContext.addMetadataToMap(MetadataConstants.APP_KEY_TYPE_KEY,
                authContext.getKeyType() == null ? APIConstants.API_KEY_TYPE_PRODUCTION : authContext.getKeyType());
        // TODO: (VirajSalaka) Come up with creative scheme
        requestContext.addMetadataToMap(MetadataConstants.APP_ID_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getApplicationId()));
        requestContext.addMetadataToMap(MetadataConstants.APP_NAME_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getApplicationName()));
        requestContext.addMetadataToMap(MetadataConstants.APP_OWNER_KEY,
                AnalyticsUtils.setDefaultIfNull(authContext.getSubscriber()));

        requestContext.addMetadataToMap(MetadataConstants.CORRELATION_ID_KEY, requestContext.getCorrelationID());
        // TODO: (VirajSalaka) Move this out of this method as these remain static
        requestContext.addMetadataToMap(MetadataConstants.REGION_KEY, AnalyticsUtils.setDefaultIfNull(null));
        requestContext.addMetadataToMap("GatewayType", "ENVOY");

        // As in the matched API, only the resources under the matched resource template are selected.
        requestContext.addMetadataToMap(MetadataConstants.API_RESOURCE_TEMPLATE_KEY,
                requestContext.getMatchedResourcePath().getPath());

        return true;
    }

    public void handleFailureRequest(RequestContext requestContext) {
        MgwFaultAnalyticsProvider provider = new MgwFaultAnalyticsProvider(requestContext);
        // To avoid incrementing counter for options call
        if (provider.getProxyResponseCode() >= 200 && provider.getProxyResponseCode() < 300) {
            return;
        }
        GenericRequestDataCollector dataCollector = new GenericRequestDataCollector(provider);
        try {
            dataCollector.collectData();
        } catch (AnalyticsException e) {
            logger.error("Analtytics Error. ", e);
        }
    }
}
