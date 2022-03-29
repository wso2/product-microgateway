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
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.AnalyticsConstants;
import org.wso2.choreo.connect.enforcer.websocket.MetadataConstants;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.choreo.connect.enforcer.analytics.AnalyticsConstants.ERROR_SCHEMA;
import static org.wso2.choreo.connect.enforcer.analytics.AnalyticsConstants.RESPONSE_SCHEMA;
import static org.wso2.choreo.connect.enforcer.constants.MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY;

/**
 * Default Analytics Event publisher to the analytics cloud.
 */
public class DefaultAnalyticsEventPublisher implements AnalyticsEventPublisher {
    private static final String AUTH_TOKEN_KEY = "auth.api.token";
    private static final String AUTH_URL = "auth.api.url";
    public final String responseSchema;
    public final String faultSchema;

    private static final Logger logger = LogManager.getLogger(DefaultAnalyticsEventPublisher.class);

    public DefaultAnalyticsEventPublisher() {
        this.responseSchema = RESPONSE_SCHEMA;
        this.faultSchema = ERROR_SCHEMA;
    }

    public DefaultAnalyticsEventPublisher(String responseSchema, String faultSchema) {
        this.responseSchema = responseSchema;
        this.faultSchema = faultSchema;
    }

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
            AnalyticsDataProvider provider;
            if (AnalyticsUtils.isMockAPISuccessRequest(logEntry)) {
                provider = new ChoreoAnalyticsProviderForMockAPISuccess(logEntry);
            } else {
                provider = new ChoreoAnalyticsProvider(logEntry);
            }
            // If the APIName is not available, the event should not be published.
            // 404 errors are not logged due to this.
            if (provider.getEventCategory() == EventCategory.FAULT
                    && provider.getFaultType() == FaultCategory.OTHER) {
                continue;
            }
            collectDataToPublish(provider);
        }
    }

    @Override
    public void handleWebsocketFrameRequest(WebSocketFrameRequest webSocketFrameRequest) {
        AnalyticsDataProvider  provider = new ChoreoAnalyticsForWSProvider(webSocketFrameRequest);
        collectDataToPublish(provider);
    }

    @Override
    public void init(Map<String, String> configuration) {
        if (StringUtils.isEmpty(configuration.get(AnalyticsConstants.AUTH_URL_CONFIG_KEY))
                || StringUtils.isEmpty(AnalyticsConstants.AUTH_TOKEN_CONFIG_KEY)) {
            logger.error(AnalyticsConstants.AUTH_URL_CONFIG_KEY + " and / or " +
                    AnalyticsConstants.AUTH_TOKEN_CONFIG_KEY +
                    "  are not provided under analytics configurations.");
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
        if (!StringUtils.isEmpty(responseSchema)) {
            commonConfiguration.setResponseSchema(responseSchema);
        }
        if (!StringUtils.isEmpty(faultSchema)) {
            commonConfiguration.setFaultSchema(faultSchema);
        }
        AnalyticsServiceReferenceHolder.getInstance().setConfigurations(commonConfiguration);
    }

    private boolean doNotPublishEvent(HTTPAccessLogEntry logEntry) {

        // If the logEntry corresponds to success mock api request, it should be published using logEntry.
        // IsMockAPI flag is only set when it corresponds to a success request.
        if (AnalyticsUtils.isMockAPISuccessRequest(logEntry)) {
            return false;
        }

        // If ext_auth_denied request comes, the event is already published from the enforcer.
        // There is a chance that the analytics event is published from enforcer and then result in ext_authz_error
        // responseCodeDetail due to some error/exception within enforcer implementation. This scenario is not
        // handled as it should be fixed from enforcer.
        return (!StringUtils.isEmpty(logEntry.getResponse().getResponseCodeDetails()))
                && logEntry.getResponse().getResponseCodeDetails()
                .equals(AnalyticsConstants.EXT_AUTH_DENIED_RESPONSE_DETAIL)
                // Token endpoint calls needs to be removed as well
                || (AnalyticsConstants.TOKEN_ENDPOINT_PATH.equals(logEntry.getRequest().getOriginalPath()))
                // Health endpoint calls are not published
                || (AnalyticsConstants.HEALTH_ENDPOINT_PATH.equals(logEntry.getRequest().getOriginalPath()))
                // already published websocket log entries should not be published to the analytics again.
                || alreadyPublishedWebsocketHttpLogEntry(logEntry);
    }

    // If the access log entry has the status code of 101 and it is a websocket related log entry,
    // it corresponds to the successful websocket upgrade. And that event is handled via the
    // WebsocketResponseObserver.
    private boolean alreadyPublishedWebsocketHttpLogEntry(HTTPAccessLogEntry logEntry) {
        if (logEntry.hasCommonProperties() && logEntry.getCommonProperties().hasMetadata()
                && logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                .get(EXT_AUTH_METADATA_CONTEXT_KEY) != null &&
                logEntry.getCommonProperties().getMetadata()
                .getFilterMetadataMap().get(EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap()
                .get(MetadataConstants.API_TYPE_KEY) != null) {
            return APIConstants.ApiType.WEB_SOCKET.equals(logEntry.getCommonProperties().getMetadata()
                    .getFilterMetadataMap().get(EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap()
                    .get(MetadataConstants.API_TYPE_KEY).getStringValue()) &&
                    logEntry.getResponse().getResponseCode().getValue() == 101;
        }
        return false;
    }

    private void collectDataToPublish(AnalyticsDataProvider provider) {
        GenericRequestDataCollector dataCollector = new GenericRequestDataCollector(provider);
        String correlationID = "";
        if (provider.getMetaInfo() != null) {
            correlationID = provider.getMetaInfo().getCorrelationId();
        }
        try {
            dataCollector.collectData();
            logger.debug("Event is published. : " + correlationID);
        } catch (AnalyticsException e) {
            logger.error("Error while publishing the event to the analytics portal. : "
                    + correlationID,
                    ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 5100), e);
        }
    }

}
