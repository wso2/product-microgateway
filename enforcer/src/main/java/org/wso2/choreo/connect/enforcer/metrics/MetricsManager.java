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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.dto.MetricsDTO;

/**
 * This class is responsible for managing configs and instances related to metrics
 */
public class MetricsManager {

    private static final Logger LOGGER = LogManager.getLogger(MetricsManager.class);
    private static String exporterType = "none";
    private static boolean isMetricsEnabled = false;
    private static MetricsExporter metricsExporter;

    public static MetricsExporter getInstance() {
        if (metricsExporter == null) {
            synchronized (new Object()) {
                if (metricsExporter == null) {
                    if (exporterType.equals(MetricsConstants.AZURE_METRICS_EXPORTER)) {
                        metricsExporter = new AzureMetricsExporter();
                    } else {
                        LOGGER.error("Metrics exporter type: " + exporterType + " not found!");
                    }
                }
            }
        }
        return metricsExporter;
    }

    /**
     * Method to initialize metrics
     */
    public static void initializeMetrics(MetricsDTO enforcerConfig) {

        String type = enforcerConfig.getMetricsType();
        if (StringUtils.isEmpty(type)) {
            LOGGER.warn("Metrics type not defined, defaulting to Azure metrics");
            exporterType = MetricsConstants.AZURE_METRICS_EXPORTER;
        } else {
            exporterType = type;
        }

        isMetricsEnabled = true;
        LOGGER.debug("Metrics Manager is successfully initialized with type: " + exporterType + ".");
    }

    public static boolean isMetricsEnabled() {

        return isMetricsEnabled;
    }
}
