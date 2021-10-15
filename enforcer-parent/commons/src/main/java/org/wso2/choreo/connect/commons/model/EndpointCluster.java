package org.wso2.choreo.connect.commons.model;

import java.util.List;

public class EndpointCluster {
    private List<String> urls;
    private RetryConfig retryConfig;

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }
}
