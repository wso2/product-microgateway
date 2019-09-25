package org.wso2.apimgt.gateway.cli.model.mgwcodegen;

import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointProxyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.EndpointUrlTypeEnum;
import org.wso2.apimgt.gateway.cli.model.route.EndpointType;

import java.util.List;


public class MgwEndpointListDTO {

    private APIEndpointSecurityDTO securityConfig = null;
    private EndpointType type = null;
    private List<MgwEndpointDTO> endpoints = null;
    private EndpointUrlTypeEnum endpointUrlType = null;
    private String name = null;
    private boolean endpointListEtcdEnabled = false;
    private APIEndpointProxyDTO proxyConfig = null;

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

    public APIEndpointProxyDTO getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(APIEndpointProxyDTO proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
}
