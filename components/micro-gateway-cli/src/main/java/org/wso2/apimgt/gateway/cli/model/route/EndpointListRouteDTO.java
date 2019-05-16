package org.wso2.apimgt.gateway.cli.model.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;

import java.util.ArrayList;
import java.util.List;

//todo: add constants
/**
 * This class hold the available endpoints and securityConfig details (in the routes.yaml)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndpointListRouteDTO {

    private APIEndpointSecurityDTO securityConfig = null;
    private EndpointType type = null;
    private List<String> endpoints = null;
    private String name;

    @JsonProperty("securityConfig")
    public APIEndpointSecurityDTO getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(APIEndpointSecurityDTO securityConfig) {
        this.securityConfig = securityConfig;
    }

    @JsonProperty("type")
    public EndpointType getType() {
        return type;
    }

    public void setType(EndpointType type) {
        this.type = type;
    }

    @JsonProperty(value = "urls", required = true)
    public List<String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    //todo: add "add endpoint" method
    public void addEndpoint(String endpoint){
        if(endpoints == null){
            endpoints = new ArrayList<>();
        }
        //todo: indicate if a duplicate has occurred ?
        if(!endpoints.contains(endpoint)){
            endpoints.add(endpoint);
        }
    }
}
