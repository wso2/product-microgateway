package org.wso2.choreo.connect.tests.apim.dto;

public class Subscription {
    private final String apiName;
    private final String appName;
    private final String tier;

    public Subscription(String apiName, String appName, String tier) {
        this.apiName = apiName;
        this.appName = appName;
        this.tier = tier;
    }

    public String getApiName() {
        return apiName;
    }

    public String getAppName() {
        return appName;
    }

    public String getTier() {
        return tier;
    }
}
