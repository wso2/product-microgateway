package org.wso2.apimgt.gateway.cli.model.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIRouteEndpointConfig extends EndpointConfig{

    private String apiName = null;
    private String apiVersion = null;

    @JsonProperty("name")
    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    @JsonProperty("version")
    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}
