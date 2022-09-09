package org.wso2.choreo.connect.enforcer.config.dto;

/**
 * MBean API for Thread Pool Configuration.
 */
public interface ThreadPoolConfigMBean {
    /**
     * Getter for core size.
     * 
     * @return int
     */
    public int getCoreSize();

    /**
     * Getter for max size.
     * 
     * @return int
     */
    public int getMaxSize();

    /**
     * Getter for keep alive size.
     * 
     * @return int
     */
    public int getKeepAliveTime();

    /**
     * Getter for queue size.
     * 
     * @return int
     */
    public int getQueueSize();
}
