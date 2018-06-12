package org.wso2.apimgt.gateway.codegen.service.bean;

import java.util.ArrayList;
import java.util.List;

public class EndpointConfig {
    private String endpointType;
    private List<Endpoint> prodEndpoints = null;
    private List<Endpoint> prodFailoverEndpoints = null;
    private List<Endpoint> sandEndpoints = null;
    private List<Endpoint> sandFailoverEndpoints = null;

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public List<Endpoint> getProdEndpoints() {
        return prodEndpoints;
    }

    public void setProdEndpoints(List<Endpoint> prodEndpoints) {
        this.prodEndpoints = prodEndpoints;
    }

    public List<Endpoint> getProdFailoverEndpoints() {
        return prodFailoverEndpoints;
    }

    public void setProdFailoverEndpoints(List<Endpoint> prodFailoverEndpoints) {
        this.prodFailoverEndpoints = prodFailoverEndpoints;
    }

    public List<Endpoint> getSandEndpoints() {
        return sandEndpoints;
    }

    public void setSandEndpoints(List<Endpoint> sandEndpoints) {
        this.sandEndpoints = sandEndpoints;
    }

    public List<Endpoint> getSandFailoverEndpoints() {
        return sandFailoverEndpoints;
    }

    public void setSandFailoverEndpoints(List<Endpoint> sandFailoverEndpoints) {
        this.sandFailoverEndpoints = sandFailoverEndpoints;
    }

    public void addSandEndpoint(Endpoint endpoint) {
        if (sandEndpoints == null) {
            sandEndpoints = new ArrayList<>();
        }
        sandEndpoints.add(endpoint);
    }

    public void addProdEndpoint(Endpoint endpoint) {
        if (prodEndpoints == null) {
            prodEndpoints = new ArrayList<>();
        }
        prodEndpoints.add(endpoint);
    }

    public void addProdFailoverEndpoint(Endpoint endpoint) {
        if (prodFailoverEndpoints == null) {
            prodFailoverEndpoints = new ArrayList<>();
        }
        prodFailoverEndpoints.add(endpoint);
    }

    public void addSandFailoverEndpoint(Endpoint endpoint) {
        if (sandFailoverEndpoints == null) {
            sandFailoverEndpoints = new ArrayList<>();
        }
        sandFailoverEndpoints.add(endpoint);
    }
}
