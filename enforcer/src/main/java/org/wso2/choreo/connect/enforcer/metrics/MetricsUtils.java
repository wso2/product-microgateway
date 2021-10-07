/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.choreo.connect.enforcer.metrics;

import com.google.protobuf.UInt32Value;
import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Latencies;
import org.wso2.choreo.connect.enforcer.analytics.ChoreoAnalyticsProvider;
import org.wso2.choreo.connect.enforcer.constants.AnalyticsConstants;

import java.util.HashMap;

/**
 * Common utility functions to publish metrics.
 */
public class MetricsUtils {

    private static final Logger LOGGER = LogManager.getLogger(MetricsUtils.class);

    /**
     * Method to process and publish metrics obtained from access logs.
     *
     * @param message the StreamAccessLogsMessage object.
     */
    public static void handlePublishingMetrics(StreamAccessLogsMessage message) {

        for (int i = 0; i < message.getHttpLogs().getLogEntryCount(); i++) {
            HTTPAccessLogEntry logEntry = message.getHttpLogs().getLogEntry(i);
            MetricsExporter metricsExporter = MetricsManager.getInstance();

            UInt32Value httpResponseProperties = logEntry.getResponse().getResponseCode();
            metricsExporter.trackMetric(MetricsConstants.RESPONSE_CODE, httpResponseProperties.getValue());
            AnalyticsDataProvider provider = new ChoreoAnalyticsProvider(logEntry);
            Latencies latencies = provider.getLatencies();

            // handle do not publish event
            if ((!StringUtils.isEmpty(logEntry.getResponse().getResponseCodeDetails()))
                    && logEntry.getResponse().getResponseCodeDetails()
                    .equals(AnalyticsConstants.EXT_AUTH_DENIED_RESPONSE_DETAIL)
                    // Token endpoint calls needs to be removed as well
                    || (AnalyticsConstants.TOKEN_ENDPOINT_PATH.equals(logEntry.getRequest().getOriginalPath()))
                    // Health endpoint calls are not published
                    || (AnalyticsConstants.HEALTH_ENDPOINT_PATH.equals(logEntry.getRequest().getOriginalPath()))) {
                LOGGER.debug("Metric is ignored as it is already published by the enforcer.");
                publishMetrics(metricsExporter, latencies);
                continue;
            }
            publishMetrics(metricsExporter, latencies);
        }
    }

    /**
     * Method to export/publish metrics.
     *
     * @param metricsExporter the exporter instance.
     * @param latencies the latencies to be exported.
     */
    private static void publishMetrics(MetricsExporter metricsExporter, Latencies latencies) {

        HashMap<String, Double> valueMap = new HashMap<>();

        valueMap.put(MetricsConstants.RESPONSE_LATENCY, (double) latencies.getResponseLatency());
        valueMap.put(MetricsConstants.RESPONSE_MEDIATION_LATENCY,
                (double) latencies.getResponseMediationLatency());
        valueMap.put(MetricsConstants.REQUEST_MEDIATION_LATENCY,
                (double) latencies.getRequestMediationLatency());
        valueMap.put(MetricsConstants.BACKEND_LATENCY, (double) latencies.getBackendLatency());

        metricsExporter.trackMetrics(valueMap);
    }
}
