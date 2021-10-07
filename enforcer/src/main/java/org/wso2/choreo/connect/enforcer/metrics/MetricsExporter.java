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
package org.wso2.choreo.connect.enforcer.metrics;

import java.util.HashMap;

/**
 * This interface contains the definition for a Metrics Exporting implementation.
 */
public interface MetricsExporter {

    /**
     * Accepts a map of metrics information that needs to be published
     *
     * @param metrics a hashmap with the metrics key value pairs
     */
    void trackMetrics(HashMap<String, Double> metrics);

    /**
     * Accepts a key key value pair pertaining to the metrics information that needs to be published
     *
     * @param key the key pertaining to the metric
     * @param value the value of the metric
     */
    void trackMetric(String key, double value);
}
