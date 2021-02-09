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
        // TODO (amalimatharaarachchi) process message and set analytics data
        return true;
    }

    @Override
    public void init(APIConfig apiConfig) {
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        APIConfig apiConfig = requestContext.getMathedAPI().getAPIConfig();
        ResponseEvent responseEvent = new ResponseEvent();
        responseEvent.setCorrelationId(requestContext.getCorrelationID());
        responseEvent.setKeyType(requestContext.);
        responseEvent.setApiId(apiUuid);
        responseEvent.setApiName(apiName);
        responseEvent.setApiVersion(apiVersion);
        responseEvent.setApiCreator(apiCreator);
        responseEvent.setApiMethod(httpMethod);
        responseEvent.setApiCreatorTenantDomain(MultitenantUtils.getTenantDomain(apiCreator));
        responseEvent.setApiResourceTemplate(apiResourceTemplate);
        responseEvent.setDestination(endpointAddress);
        responseEvent.setApplicationId(applicationId);
        responseEvent.setApplicationName(applicationName);
        responseEvent.setApplicationOwner(applicationOwner);

        responseEvent.setRegionId(REGION_ID);
        responseEvent.setGatewayType(APIMgtGatewayConstants.GATEWAY_TYPE);
        responseEvent.setUserAgent(userAgent);
        responseEvent.setProxyResponseCode(String.valueOf(proxyResponseCode));
        responseEvent.setTargetResponseCode(String.valueOf(targetResponseCode));
        responseEvent.setResponseCacheHit(String.valueOf(isCacheHit));
        responseEvent.setResponseLatency(String.valueOf(responseTime));
        responseEvent.setBackendLatency(String.valueOf(backendLatency));
        responseEvent.setRequestMediationLatency(String.valueOf(reqMediationLatency));
        responseEvent.setResponseMediationLatency(String.valueOf(resMediationLatency));
        responseEvent.setDeploymentId(DEPLOYMENT_ID);
        responseEvent.setEventType(SUCCESS_EVENT_TYPE);
        return true;
    }
}
