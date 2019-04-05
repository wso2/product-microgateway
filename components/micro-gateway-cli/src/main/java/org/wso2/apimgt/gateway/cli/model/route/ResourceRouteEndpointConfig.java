package org.wso2.apimgt.gateway.cli.model.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceRouteEndpointConfig extends EndpointConfig{

    private String name = null;
    //todo: Use Enum
    private String method = null;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("method")
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
