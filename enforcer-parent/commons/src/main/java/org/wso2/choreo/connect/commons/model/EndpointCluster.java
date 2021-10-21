package org.wso2.choreo.connect.commons.model;

import java.util.List;

/**
 * EndpointCluster contains the URLs and the config for a set of Endpoints such as prod, sandbox
 */
public class EndpointCluster {
    private List<String> urls;
    private RetryConfig retryConfig;

    /**
     * @return URLs of the cluster
     */
    public List<String> getUrls() {
        return urls;
    }

    /**
     * @param urls URLs of the cluster
     */
    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    /**
     * @return Retry configuration of the cluster
     */
    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    /**
     * @param retryConfig Retry configuration of the cluster
     */
    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }
}
