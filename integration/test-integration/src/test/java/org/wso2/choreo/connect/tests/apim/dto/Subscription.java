package org.wso2.choreo.connect.tests.apim.dto;

public class Subscription {
    private final String apiName;
    private final String apiVersion;
    private final String appName;
    private final String tier;

    public Subscription(String apiName, String apiVersion, String appName, String tier) {
        this.apiName = apiName;
        this.apiVersion = apiVersion;
        this.appName = appName;
        this.tier = tier;
    }

    public String getApiName() {
        return apiName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getAppName() {
        return appName;
    }

    public String getTier() {
        return tier;
    }
}
