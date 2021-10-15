package org.wso2.choreo.connect.commons.model;

public class RetryConfig {
    int count;
    String[] statusCodes;

    public RetryConfig(int count, String[] statusCodes) {
        this.count = count;
        this.statusCodes = statusCodes;
    }

    public int getCount() {
        return count;
    }

    public String[] getStatusCodes() {
        return statusCodes;
    }
}
