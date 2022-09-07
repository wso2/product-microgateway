package org.wso2.choreo.connect.enforcer.metrics.jmx.api;

/**
 * MBean API for ExtAuth Service metrics.
 */
public interface ExtAuthMetricsMXBean {

    public long getTotalRequestCount();

    public long getTotalResponseCount();

    public long getAverageResponseTimeMillis();

    public long getMaxResponseTimeMillis();

    public long getMinResponseTimeMillis();

}
