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

import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.analytics.AnalyticsCommonConfiguration;
import org.wso2.carbon.apimgt.common.analytics.AnalyticsServiceReferenceHolder;
import org.wso2.carbon.apimgt.common.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.analytics.collectors.impl.GenericRequestDataCollector;
import org.wso2.carbon.apimgt.common.analytics.exceptions.AnalyticsException;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.EventCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.choreo.connect.enforcer.constants.AnalyticsConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Default Analytics Event publisher to the analytics cloud.
 */
public class DefaultAnalyticsEventPublisher implements AnalyticsEventPublisher {
    private static final String AUTH_TOKEN_KEY = "auth.api.token";
    private static final String AUTH_URL = "auth.api.url";
    private static boolean isChoreoDeployment = false;

    private static final Logger logger = LogManager.getLogger(DefaultAnalyticsEventPublisher.class);

    @Override
    public void handleGRPCLogMsg(StreamAccessLogsMessage message) {
        for (int i = 0; i < message.getHttpLogs().getLogEntryCount(); i++) {
            HTTPAccessLogEntry logEntry = message.getHttpLogs().getLogEntry(i);
            logger.trace("Received logEntry from Router " + message.getIdentifier().getNode() +
                    " : " + message.toString());
            if (doNotPublishEvent(logEntry)) {
                logger.debug("LogEntry is ignored as it is already published by the enforcer.");
                continue;
            }
            AnalyticsDataProvider provider = new MgwAnalyticsProvider(logEntry, isChoreoDeployment);
            // If the APIName is not available, the event should not be published.
            // 404 errors are not logged due to this.
            if (provider.getEventCategory() == EventCategory.FAULT
                    && provider.getFaultType() == FaultCategory.OTHER) {
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

    @Override
    public void init(Map<String, String> configuration) {
        if (StringUtils.isEmpty(configuration.get(AnalyticsConstants.AUTH_URL_CONFIG_KEY))
                || StringUtils.isEmpty(AnalyticsConstants.AUTH_TOKEN_CONFIG_KEY)) {
            logger.error(AnalyticsConstants.AUTH_URL_CONFIG_KEY + " and / or " +
                    AnalyticsConstants.AUTH_TOKEN_CONFIG_KEY +
                    "properties are not provided under analytics configurations.");
            return;
        }
        Map<String, String> publisherConfig = new HashMap<>(2);
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            if (AnalyticsConstants.AUTH_TOKEN_CONFIG_KEY.equals(entry.getKey())) {
                publisherConfig.put(AUTH_TOKEN_KEY, configuration.get(AnalyticsConstants.AUTH_TOKEN_CONFIG_KEY));
                continue;
            } else if (AnalyticsConstants.AUTH_URL_CONFIG_KEY.equals(entry.getKey())) {
                publisherConfig.put(AUTH_URL, configuration.get(AnalyticsConstants.AUTH_URL_CONFIG_KEY));
                continue;
            }
            publisherConfig.put(entry.getKey(), entry.getValue());
        }

        AnalyticsCommonConfiguration commonConfiguration = new AnalyticsCommonConfiguration(publisherConfig);
        if (configuration.containsKey("isChoreoDeployment")
                && configuration.get("isChoreoDeployment").toLowerCase().equals("true")) {
            isChoreoDeployment = true;
            commonConfiguration.setResponseSchema("CHOREO_RESPONSE");
            commonConfiguration.setFaultSchema("CHOREO_ERROR");
        }
        AnalyticsServiceReferenceHolder.getInstance().setConfigurations(commonConfiguration);
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
                // TODO: (VirajSalaka) healthcheck calls also needs to filtered out.
                && (!AnalyticsConstants.TOKEN_ENDPOINT_PATH.equals(logEntry.getRequest().getOriginalPath()));
    }
}
