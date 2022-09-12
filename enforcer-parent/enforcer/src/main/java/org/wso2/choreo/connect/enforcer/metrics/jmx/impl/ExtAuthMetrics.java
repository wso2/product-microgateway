/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.choreo.connect.enforcer.metrics.jmx.impl;

import org.wso2.choreo.connect.enforcer.jmx.MBeanRegistrator;
import org.wso2.choreo.connect.enforcer.metrics.jmx.api.ExtAuthMetricsMXBean;

/**
 * Singleton MBean for ExtAuth Service metrics.
 */
public class ExtAuthMetrics implements ExtAuthMetricsMXBean {

    private static ExtAuthMetrics extAuthMetricsMBean = null;

    private long totalRequestCount = 0;
    private long averageResponseTimeMillis = 0;
    private long maxResponseTimeMillis = Long.MIN_VALUE;
    private long minResponseTimeMillis = Long.MAX_VALUE;

    private ExtAuthMetrics() {
        MBeanRegistrator.registerMBean(this);
    }

    /**
     * Getter for the Singleton ExtAuthMetrics instance.
     * 
     * @return ExtAuthMetrics
     */
    public static ExtAuthMetrics getInstance() {
        if (extAuthMetricsMBean == null) {
            extAuthMetricsMBean = new ExtAuthMetrics();
        }
        return extAuthMetricsMBean;
    }

    public long getTotalRequestCount() {
        return totalRequestCount;
    };

    public long getAverageResponseTimeMillis() {
        return averageResponseTimeMillis;
    };

    public long getMaxResponseTimeMillis() {
        return maxResponseTimeMillis;
    };

    public long getMinResponseTimeMillis() {
        return minResponseTimeMillis;
    };

    public synchronized void recordMetric(long responseTimeMillis) {
        this.totalRequestCount += 1;
        this.averageResponseTimeMillis = (this.averageResponseTimeMillis + responseTimeMillis) / totalRequestCount;
        this.minResponseTimeMillis = Math.min(this.minResponseTimeMillis, responseTimeMillis);
        this.maxResponseTimeMillis = Math.max(this.maxResponseTimeMillis, responseTimeMillis);
    }

    public synchronized void resetExtAuthMetrics() {
        this.totalRequestCount = 0;
        this.averageResponseTimeMillis = 0;
        this.maxResponseTimeMillis = Long.MIN_VALUE;
        this.minResponseTimeMillis = Long.MAX_VALUE;
    }
}
