package org.wso2.choreo.connect.enforcer.config.dto;

/**
 * This contains authorization header properties.
 */
public class AuthHeaderDto {
    private boolean enableOutboundAuthHeader = false;
    private String authorizationHeader = "";

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public boolean isEnableOutboundAuthHeader() {
        return enableOutboundAuthHeader;
    }

    public void setEnableOutboundAuthHeader(boolean enableOutboundAuthHeader) {
        this.enableOutboundAuthHeader = enableOutboundAuthHeader;
    }
}
