package org.wso2.choreo.connect.commons.model;

/**
 * The Retry configuration of the cluster which will be referred when setting the route headers
 */
public class RetryConfig {
    int count;
    Integer[] statusCodes;

    /**
     * @param count Number of times to retry
     * @param statusCodes Http status codes on which retrying must be done
     */
    public RetryConfig(int count, Integer[] statusCodes) {
        this.count = count;
        this.statusCodes = statusCodes;
    }

    /**
     * @return Number of times to retry
     */
    public int getCount() {
        return count;
    }

    /**
     * @return Http status codes on which retrying must be done
     */
    public Integer[] getStatusCodes() {
        return statusCodes;
    }
}
