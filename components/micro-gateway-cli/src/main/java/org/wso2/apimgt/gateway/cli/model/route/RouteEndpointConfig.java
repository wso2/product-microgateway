package org.wso2.apimgt.gateway.cli.model.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the endpoint configurations for the given environment. (in the routes.yaml)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteEndpointConfig {

    private EndpointListRouteDTO prodEndpointList = null;
    private EndpointListRouteDTO sandboxEndpointList = null;

    @JsonProperty("prod")
    public EndpointListRouteDTO getProdEndpointList() {
        return prodEndpointList;
    }

    public void setProdEndpointList(EndpointListRouteDTO prodEndpointList) {
        this.prodEndpointList = prodEndpointList;
    }

    @JsonProperty("sandbox")
    public EndpointListRouteDTO getSandboxEndpointList() {
        return sandboxEndpointList;
    }

    public void setSandboxEndpointList(EndpointListRouteDTO sandboxEndpointList) {
        this.sandboxEndpointList = sandboxEndpointList;
    }
}
