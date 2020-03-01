package org.wso2.apimgt.gateway.cli.model.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.wso2.apimgt.gateway.cli.exception.CLICompileTimeException;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;

import java.net.MalformedURLException;
import java.net.URL;
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

    /**
     * set endpoints to the {@link EndpointListRouteDTO} object.
     * endpoint could be either etcd(etcd_key, default_url) or default url.
     *
     * @param endpoints endpoint string
     */
    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add endpoint Urls.
     * @param endpoint endpoint URL
     */
    public void addEndpoint(String endpoint) {
        if (endpoints == null) {
            endpoints = new ArrayList<>();
        }
        //todo: indicate if a duplicate has occurred ?
        if (!endpoints.contains(endpoint)) {
            endpoints.add(endpoint);
        }
    }

    /**
     * Validate the provided set of endpoint URLs.
     * This
     * @throws CLICompileTimeException if Malformed Url is provided.
     */
    public void validateEndpoints() throws CLICompileTimeException {
        if (endpoints != null) {
            for (String endpoint : endpoints) {
                validateEndpointUrl(endpoint);
            }
        }
    }

    private void validateEndpointUrl(String endpointUrl) throws CLICompileTimeException {
        if (endpointUrl.trim().matches("etcd\\s*\\(.*,.*\\)")) {
            String temp = endpointUrl.substring(endpointUrl.indexOf("(") + 1, endpointUrl.indexOf(")"));
            String[] entries = temp.split(",");
            if (entries.length != 2) {
                throw new CLICompileTimeException("'etcd' key containing string should be provided as 'etcd " +
                        "(etcd_key, default_url)'.");
            }
        } else {
            try {
                new URL(endpointUrl);
            } catch (MalformedURLException e) {
                throw new CLICompileTimeException("Malformed Url is provided :" + endpointUrl, e);
            }
        }
    }
}
