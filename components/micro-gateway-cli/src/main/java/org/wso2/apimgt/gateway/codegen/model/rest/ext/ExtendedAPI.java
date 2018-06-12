package org.wso2.apimgt.gateway.codegen.service.bean.ext;

import org.wso2.apimgt.gateway.codegen.service.bean.APIDetailedDTO;
import org.wso2.apimgt.gateway.codegen.service.bean.EndpointConfig;

public class ExtendedAPI extends APIDetailedDTO {
    private EndpointConfig endpointConfigRepresentation = null;

    public EndpointConfig getEndpointConfigRepresentation() {
        return endpointConfigRepresentation;
    }

    public void setEndpointConfigRepresentation(EndpointConfig endpointConfigRepresentation) {
        this.endpointConfigRepresentation = endpointConfigRepresentation;
    }
}
