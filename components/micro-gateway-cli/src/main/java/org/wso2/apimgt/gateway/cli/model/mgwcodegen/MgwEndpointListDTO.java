package org.wso2.apimgt.gateway.cli.model.mgwcodegen;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.EndpointUrlTypeEnum;
import org.wso2.apimgt.gateway.cli.model.route.EndpointType;

import java.util.List;


/**
 * List of {@link MgwEndpointDTO} definitions with other endpoint
 * related details such as endpoint security.
 * <p>
 *     Instance of this class always represent a single endpoint
 *     declaration wso2 OpenAPI extension.
 * </p>
 * <p>
 *     Ex:
 *        x-wso2-production-endpoints:
 *          urls:
 *            - http://www.mocky.io/v2/5cd28b9a310000bf293397f9
 * </p>
 */
public class MgwEndpointListDTO {

    private APIEndpointSecurityDTO securityConfig = null;
    private EndpointType type = null;
    private List<MgwEndpointDTO> endpoints = null;
    private EndpointUrlTypeEnum endpointUrlType = null;
    private String name = null;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean endpointListEtcdEnabled = false;

    public APIEndpointSecurityDTO getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(APIEndpointSecurityDTO securityConfig) {
        this.securityConfig = securityConfig;
    }

    public EndpointType getType() {
        return type;
    }

    public void setType(EndpointType type) {
        this.type = type;
    }

    public List<MgwEndpointDTO> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<MgwEndpointDTO> endpoints) {
        this.endpoints = endpoints;
    }

    public EndpointUrlTypeEnum getEndpointUrlType() {
        return endpointUrlType;
    }

    public void setEndpointUrlType(EndpointUrlTypeEnum endpointUrlType) {
        this.endpointUrlType = endpointUrlType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEndpointListEtcdEnabled(boolean endpointListEtcdEnabled) {
        this.endpointListEtcdEnabled = endpointListEtcdEnabled;
    }
}
