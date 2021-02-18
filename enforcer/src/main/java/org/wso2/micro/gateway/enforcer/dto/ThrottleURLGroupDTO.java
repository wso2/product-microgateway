package org.wso2.micro.gateway.enforcer.dto;

/**
 * Throttle URL groups configurations.
 */
public class ThrottleURLGroupDTO {
    String[] receiverURLs;
    String[] authURLs;
    String type;

    public String[] getReceiverURLs() {
        return receiverURLs;
    }

    public void setReceiverURLs(String[] receiverURLs) {
        this.receiverURLs = receiverURLs;
    }

    public String[] getAuthURLs() {
        return authURLs;
    }

    public void setAuthURLs(String[] authURLs) {
        this.authURLs = authURLs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
