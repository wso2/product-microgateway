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

import java.util.Timer;
import java.util.TimerTask;

/**
 * Singleton MBean for ExtAuth Service metrics.
 */
public class ExtAuthMetrics extends TimerTask implements ExtAuthMetricsMXBean {

    private static final long REQUEST_COUNT_INTERVAL_MILLIS = 5 * 60 * 1000;
    private static ExtAuthMetrics extAuthMetricsMBean = null;

    private long requestCountInLastFiveMinuteWindow = 0;
    private long requestCountWindowStartTimeMillis = System.currentTimeMillis();
    private long totalRequestCount = 0;
    private double averageResponseTimeMillis = 0;
    private double maxResponseTimeMillis = Double.MIN_VALUE;
    private double minResponseTimeMillis = Double.MAX_VALUE;

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
            synchronized (ExtAuthMetrics.class) {
                if (extAuthMetricsMBean == null) {
                    Timer timer = new Timer();
                    extAuthMetricsMBean = new ExtAuthMetrics();
                    extAuthMetricsMBean.requestCountWindowStartTimeMillis = System.currentTimeMillis();
                    timer.schedule(extAuthMetricsMBean, REQUEST_COUNT_INTERVAL_MILLIS, REQUEST_COUNT_INTERVAL_MILLIS);
                }
            }
        }
        return extAuthMetricsMBean;
    }

    @Override
    public long getTotalRequestCount() {
        return totalRequestCount;
    };

    @Override
    public double getAverageResponseTimeMillis() {
        return averageResponseTimeMillis;
    };

    @Override
    public double getMaxResponseTimeMillis() {
        return maxResponseTimeMillis;
    };

    @Override
    public double getMinResponseTimeMillis() {
        return minResponseTimeMillis;
    };

    public synchronized void recordMetric(long responseTimeMillis) {
        this.requestCountInLastFiveMinuteWindow += 1;
        this.totalRequestCount += 1;
        this.averageResponseTimeMillis = this.averageResponseTimeMillis +
                (responseTimeMillis - this.averageResponseTimeMillis) / totalRequestCount;
        this.minResponseTimeMillis = Math.min(this.minResponseTimeMillis, responseTimeMillis);
        this.maxResponseTimeMillis = Math.max(this.maxResponseTimeMillis, responseTimeMillis);
    }

    @Override
    public synchronized void resetExtAuthMetrics() {
        this.totalRequestCount = 0;
        this.averageResponseTimeMillis = 0;
        this.maxResponseTimeMillis = Double.MIN_VALUE;
        this.minResponseTimeMillis = Double.MAX_VALUE;
    }

    @Override
    public synchronized void run() {
        requestCountWindowStartTimeMillis = System.currentTimeMillis();
        requestCountInLastFiveMinuteWindow = 0;
    }

    @Override
    public long getRequestCountInLastFiveMinuteWindow() {
        return requestCountInLastFiveMinuteWindow;
    }

    @Override
    public long getRequestCountWindowStartTimeMillis() {
        return requestCountWindowStartTimeMillis;
    }
}
