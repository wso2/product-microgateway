package org.wso2.micro.gateway.filter.core.listener.events;

import java.util.Set;

public class DeployAPIInGatewayEvent extends Event {

    private String apiId;
    private Set<String> gatewayLabels;

    public DeployAPIInGatewayEvent(String eventId, long timestamp, String type, String tenanrDomain, String apiId,
                                   Set<String> gatewayLabels) {
        this.eventId = eventId;
        this.timeStamp = timestamp;
        this.type = type;
        this.tenantDomain = tenanrDomain;

        this.apiId = apiId;
        this.gatewayLabels = gatewayLabels;

    }

    public Set<String> getGatewayLabels() {

        return gatewayLabels;
    }

    public void setGatewayLabels(Set<String> gatewayLabels) {

        this.gatewayLabels = gatewayLabels;
    }

    public String getApiId() {

        return apiId;
    }

    public void setApiId(String apiId) {

        this.apiId = apiId;
    }

}
