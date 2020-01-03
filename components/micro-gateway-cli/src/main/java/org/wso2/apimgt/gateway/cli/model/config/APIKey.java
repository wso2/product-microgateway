package org.wso2.apimgt.gateway.cli.model.config;

import io.swagger.v3.oas.models.security.SecurityScheme.In;

/**
 * Definition of APIKey passed in to mustache
 * templates.
 */
public class APIKey {
    private In in;
    private String name;

    public In getIn() {
        return in;
    }

    public void setIn(In in) {
        this.in = in;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
