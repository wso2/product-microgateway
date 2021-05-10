package org.wso2.choreo.connect.tests.apim.dto;

public class Application {
    private String appName;
    private String description;
    private String throttleTier;
    private org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO.TokenTypeEnum tokenType;

    public Application(String appName, String throttleTier,
                       org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO.TokenTypeEnum tokenType) {
        this.appName = appName;
        this.description = "An application";
        this.throttleTier = throttleTier;
        this.tokenType = tokenType;
    }

    public String getName() {
        return appName;
    }

    public String getDescription() {
        return description;
    }

    public String getThrottleTier() {
        return throttleTier;
    }

    public org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO.TokenTypeEnum getTokenType() {
        return tokenType;
    }
}
