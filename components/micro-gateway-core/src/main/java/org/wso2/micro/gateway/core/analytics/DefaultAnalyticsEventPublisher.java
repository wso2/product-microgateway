/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.core.analytics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ballerinalang.jvm.values.api.BMap;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.common.analytics.AnalyticsCommonConfiguration;
import org.wso2.carbon.apimgt.common.analytics.AnalyticsServiceReferenceHolder;
import org.wso2.carbon.apimgt.common.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.analytics.collectors.impl.GenericRequestDataCollector;

import java.util.HashMap;

/**
 * Microgateway Analytics Data publisher.
 */
public class DefaultAnalyticsEventPublisher {

    private static final Logger log = LogManager.getLogger(DefaultAnalyticsEventPublisher.class);
    private static final org.slf4j.Logger log1 = LoggerFactory.getLogger(DefaultAnalyticsEventPublisher.class);
    private static final org.slf4j.Logger log2 = LoggerFactory.getLogger("ballerina");

    public static void initELKDataPublisher() {
        HashMap<String, String> reporterProperties = new HashMap<>();
        reporterProperties.put("type", "elk");
        AnalyticsCommonConfiguration commonConfiguration = new AnalyticsCommonConfiguration(reporterProperties);
        AnalyticsServiceReferenceHolder.getInstance().setConfigurations(commonConfiguration);
    }

    public static void initChoreoDataPublisher(String configEndpoint, String authToken) {
        HashMap<String, String> reporterProperties = new HashMap<>();
        reporterProperties.put("auth.api.token", authToken);
        reporterProperties.put("auth.api.url", configEndpoint);
        reporterProperties.put("proxy_config_enable", "false");
        AnalyticsCommonConfiguration commonConfiguration = new AnalyticsCommonConfiguration(reporterProperties);
        AnalyticsServiceReferenceHolder.getInstance().setConfigurations(commonConfiguration);
    }

    public static void publishEventData(BMap<String, Object> eventData) {
        AnalyticsDataProvider provider = new MGWAnalyticsDataProvider(eventData);
        GenericRequestDataCollector dataCollector = new GenericRequestDataCollector(provider);
        try {
            dataCollector.collectData();
        } catch (Exception e) {
            log.error("Error Occurred when collecting data", e);
        }
    }
}
