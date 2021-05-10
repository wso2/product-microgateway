package org.wso2.choreo.connect.tests.apim.dto;

public class AppWithConsumerKey {
    private String applicationId;
    private String consumerKey;
    private String consumerSecret;

    public AppWithConsumerKey(String applicationId, String consumerKey, String consumerSecret) {
        this.applicationId = applicationId;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }
}
