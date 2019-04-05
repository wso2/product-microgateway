package org.wso2.apimgt.gateway.cli.model.mgwServiceMap;

public class MgwEndpointConfigDTO {

    private MgwEndpointListDTO prodEndpoints = null;
    private MgwEndpointListDTO sandEndpoints = null;

    public MgwEndpointListDTO getProdEndpointList() {
        return prodEndpoints;
    }

    public void setProdEndpointList(MgwEndpointListDTO prodEndpointList) {
        this.prodEndpoints = prodEndpointList;
    }

    public MgwEndpointListDTO getSandboxEndpointList() {
        return sandEndpoints;
    }

    public void setSandboxEndpointList(MgwEndpointListDTO sandboxEndpointList) {
        this.sandEndpoints = sandboxEndpointList;
    }
}
