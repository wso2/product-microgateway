package org.wso2.choreo.connect.enforcer.metrics.jmx.api;

/**
 * MBean API for ExtAuth Service metrics.
 */
public interface ExtAuthMetricsMXBean {

    /**
     * Getter for total request count.
     * 
     * @return long
     */
    public long getTotalRequestCount();

    /**
     * Getter for average response time in milli seconds.
     * 
     * @return long
     */
    public long getAverageResponseTimeMillis();

    /**
     * Getter for maximum response time in milliseconds.
     * 
     * @return long
     */
    public long getMaxResponseTimeMillis();

    /**
     * Getter for mimnimum response time in milliseconds.
     * 
     * @return long
     */
    public long getMinResponseTimeMillis();

    /**
     * Calculates the metrics for response time and records them.
     * 
     * @param responseTimeMillis
     */
    public void recordMetric(long responseTimeMillis);

    /**
     * Resets all the metrics to thier initial values.
     */
    public void resetExtAuthMetrics();

}
