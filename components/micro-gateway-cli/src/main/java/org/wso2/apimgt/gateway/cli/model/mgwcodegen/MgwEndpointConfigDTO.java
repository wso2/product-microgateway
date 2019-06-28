package org.wso2.apimgt.gateway.cli.model.mgwcodegen;

/**
 * Production and Sandbox endpoints holder.
 * Instance of this class consists of {@link MgwEndpointListDTO}
 * type production and sandbox endpoint definitions.
 */
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
