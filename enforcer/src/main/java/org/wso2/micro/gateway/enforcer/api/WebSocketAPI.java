package org.wso2.micro.gateway.enforcer.api;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import org.wso2.gateway.discovery.api.Api;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;

import java.util.ArrayList;
import java.util.List;

public class WebSocketAPI implements API{

    private APIConfig apiConfig;
    private List<Filter> filters = new ArrayList<>();

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public String init(CheckRequest request) {
        return null;
    }

    @Override
    public String init(Api api) {
        return null;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        return null;
    }

    @Override
    public APIConfig getAPIConfig() {
        return null;
    }

    @Override
    public boolean executeFilterChain(RequestContext requestContext) {
        return false;
    }
}
