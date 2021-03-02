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
        AuthenticationContext authContext = requestContext.getAuthenticationContext();

        requestContext.addMetadataToMap("ApiId", "Not-Implemented");
        requestContext.addMetadataToMap("ApiCreator", authContext.getApiPublisher());
        requestContext.addMetadataToMap("ApiName", authContext.getApiName());
        requestContext.addMetadataToMap("ApiVersion", authContext.getApiVersion());
        requestContext.addMetadataToMap("ApiType", "HTTP");
        requestContext.addMetadataToMap("ApiCreatorTenantDomain", "carbon.super");

        requestContext.addMetadataToMap("ApplicationKeyType", authContext.getKeyType());
        requestContext.addMetadataToMap("ApplicationId", authContext.getApplicationId());
        requestContext.addMetadataToMap("ApplicationName", authContext.getApplicationName());
        requestContext.addMetadataToMap("ApplicationOwner", authContext.getSubscriber());

        requestContext.addMetadataToMap("CorrelationId", requestContext.getCorrelationID());
        requestContext.addMetadataToMap("DeploymentId", "not implemented");
        // TODO: (VirajSalaka) Move this out of this method as these remain static
        requestContext.addMetadataToMap("RegionId", "not implemented");
        requestContext.addMetadataToMap("GatewayType", "Envoy");

        // As in the matched API, only the resources under the matched resource template are selected.
        requestContext.addMetadataToMap("ApiResourceTemplate",
                requestContext.getMathedAPI().getAPIConfig().getResources().get(0).getPath());
        return true;
    }
}
