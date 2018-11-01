package org.wso2.apimgt.gateway.cli.model.rest;

public class ServiceDiscovery {
    private String endpointType;
    private Endpoint endpoint;

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }
}
