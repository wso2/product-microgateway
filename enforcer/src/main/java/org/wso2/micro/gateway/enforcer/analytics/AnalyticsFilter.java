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

    private AnalyticsFilter () {
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
        // TODO: (VirajSalaka) Decide on whether to include/exclude the options requests, Cors requests
        AuthenticationContext authContext = requestContext.getAuthenticationContext();

        requestContext.addMetadataToMap(MetadataConstants.API_ID_KEY, authContext.getApiUUID());
        requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_KEY, authContext.getApiPublisher());
        requestContext.addMetadataToMap(MetadataConstants.API_NAME_KEY, authContext.getApiName());
        requestContext.addMetadataToMap(MetadataConstants.API_VERSION_KEY, authContext.getApiVersion());
        requestContext.addMetadataToMap(MetadataConstants.API_TYPE_KEY, "HTTP");
        requestContext.addMetadataToMap(MetadataConstants.API_CREATOR_TENANT_DOMAIN_KEY, "carbon.super");

        requestContext.addMetadataToMap(MetadataConstants.APP_KEY_TYPE_KEY, authContext.getKeyType());
        requestContext.addMetadataToMap(MetadataConstants.APP_ID_KEY, authContext.getApplicationId());
        requestContext.addMetadataToMap(MetadataConstants.APP_NAME_KEY, authContext.getApplicationName());
        requestContext.addMetadataToMap(MetadataConstants.APP_OWNER_KEY, authContext.getSubscriber());

        requestContext.addMetadataToMap(MetadataConstants.CORRELATION_ID_KEY, requestContext.getCorrelationID());
        // TODO: (VirajSalaka) Move this out of this method as these remain static
        requestContext.addMetadataToMap(MetadataConstants.REGION_KEY, "not implemented");
        requestContext.addMetadataToMap("GatewayType", "SYNAPSE");

        // As in the matched API, only the resources under the matched resource template are selected.
        requestContext.addMetadataToMap(MetadataConstants.API_RESOURCE_TEMPLATE_KEY,
                requestContext.getMatchedResourcePath().getPath());

        if (requestContext.getProperties().containsKey(APIConstants.MessageFormat.STATUS_CODE)) {
            requestContext.addMetadataToMap(MetadataConstants.ERROR_CODE_KEY,
                    requestContext.getProperties().get(APIConstants.MessageFormat.STATUS_CODE).toString());
        }
        return true;
    }
}
