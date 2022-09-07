package org.wso2.choreo.connect.enforcer.metrics.jmx.impl;

import org.wso2.choreo.connect.enforcer.jmx.MBeanRegistrator;
import org.wso2.choreo.connect.enforcer.metrics.jmx.api.ExtAuthMetricsMXBean;

/**
 * Singleton MBean for ExtAuth Service metrics.
 */
public class ExtAuthMetrics implements ExtAuthMetricsMXBean {

    private static ExtAuthMetrics extAuthMetricsMBean = null;

    private long totalRequestCount = 0;

    private long totalResponseCount = 0;

    private long averageResponseTimeMillis = 0;

    private long maxResponseTimeMillis = Long.MIN_VALUE;

    private long minResponseTimeMillis = Long.MAX_VALUE;

    private ExtAuthMetrics() {
        MBeanRegistrator.registerMBean(this);
    }

    public static ExtAuthMetrics getInstance() {
        if (extAuthMetricsMBean == null) {
            extAuthMetricsMBean = new ExtAuthMetrics();
        }
        return extAuthMetricsMBean;
    }

    public long getTotalRequestCount() {
        return totalRequestCount;
    };

    public long getTotalResponseCount() {
        return totalResponseCount;
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

    public synchronized void recordMetric(long timeMillis) {
        this.totalRequestCount += 1;
        this.averageResponseTimeMillis = (this.averageResponseTimeMillis + timeMillis) / totalRequestCount;
        this.minResponseTimeMillis = Math.min(this.minResponseTimeMillis, timeMillis);
        this.maxResponseTimeMillis = Math.max(this.maxResponseTimeMillis, timeMillis);
    }
}
