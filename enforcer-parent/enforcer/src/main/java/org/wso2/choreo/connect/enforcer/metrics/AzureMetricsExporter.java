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

import com.microsoft.applicationinsights.TelemetryClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * This class is responsible for managing and exporting metrics for azure application insights.
 */
public class AzureMetricsExporter implements MetricsExporter {

    private static final Logger LOGGER = LogManager.getLogger(AzureMetricsExporter.class);
    private TelemetryClient telemetry;

    public AzureMetricsExporter() {
        telemetry = new TelemetryClient();
        LOGGER.debug("AzureTraceExporter is successfully initialized.");
    }

    public void trackMetrics(HashMap<String, Double> metrics) {
        for (String key:metrics.keySet()) {
            telemetry.trackMetric(key, metrics.get(key));
        }
    }

    public void trackMetric(String key, double value) {
        telemetry.trackMetric(key, value);
    }

}
