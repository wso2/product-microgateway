package org.wso2.apimgt.gateway.cli.model.config;

public class APIConfig {

    private String swaggerPath;
    private String endpoint;

    public String getSwaggerPath() {
        return swaggerPath;
    }

    public void setSwaggerPath(String swaggerPath) {
        this.swaggerPath = swaggerPath;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
