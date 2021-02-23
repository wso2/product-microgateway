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
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.API;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Application;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Event;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.server.AccessLoggingService;

import java.util.HashMap;
import java.util.Map;


/**
 * This is the filter is for Analytics.
 */
public class AnalyticsFilter implements Filter {
    private static final Logger logger = LogManager.getLogger(AnalyticsFilter.class);
    private static Map<String, ResponseEvent> responseEventMap;

    public AnalyticsFilter() {
        AccessLoggingService accessLoggingService = new AccessLoggingService();
        if (accessLoggingService.init(this)) {
            responseEventMap = new HashMap<>();
            logger.info("Analytics filter initiated");
            //start analytics publishing server
        } else {
            responseEventMap = null;
            logger.warn("Analytics filter initiation failed due to access logger service failure");
        }
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
        APIConfig apiConfig = requestContext.getMathedAPI().getAPIConfig();


        Event event = new Event();

        API api = new API();
        api.setApiId("not-implemented.");
        api.setApiCreator("not-implemented.");
        api.setApiName(apiConfig.getName());
        api.setApiVersion(apiConfig.getVersion());
        api.setApiType("HTTP");
        api.setApiCreatorTenantDomain("carbon.super");

        Application app = new Application();
        app.setKeyType("Production");
        app.setApplicationId("not-implemented.");
        app.setApplicationName("not-implemented.");
        app.setApplicationOwner("not-implemented.");

        requestContext.addMetadataToMap("ApiId", "Not-Implemented");
        requestContext.addMetadataToMap("ApiCreator", "Not Implemented");
        requestContext.addMetadataToMap("ApiName", apiConfig.getName());
        requestContext.addMetadataToMap("ApiVersion", apiConfig.getVersion());
        requestContext.addMetadataToMap("ApiType", "HTTP");
        requestContext.addMetadataToMap("ApiCreatorDomain", "Not Implemented");

        requestContext.addMetadataToMap("ApplicationKeyType", "Production");
        requestContext.addMetadataToMap("setApplicationId", "not-implemented");
        requestContext.addMetadataToMap("setApplicationName", "not-implemented");
        requestContext.addMetadataToMap("setApplicationOwner", "not-implemented");

        requestContext.addMetadataToMap("CorrelationId", "xxxxxx");

        return true;
    }
}
