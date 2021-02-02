package org.wso2.micro.gateway.enforcer.api;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import org.checkerframework.checker.units.qual.A;
import org.wso2.gateway.discovery.api.Api;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.cors.CorsFilter;
import org.wso2.micro.gateway.enforcer.security.AuthFilter;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;

public class WebSocketAPI implements API <Context, Context>{

    private APIConfig apiConfig;
    private List<Filter> filters = new ArrayList<>();
    private List<Filter> upgradeFilters = new ArrayList<>();

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
        String basePath = api.getBasePath();
        String name = api.getTitle();
        String version = api.getVersion();
        this.apiConfig = new APIConfig.Builder(name).basePath(basePath).version(version).build();
        return basePath;
    }

    @Override
    public Context process(Context context) {
        if(context instanceof WebSocketMetadataContext){
            return WebSocketResponseObject.OK;
        }else {
            ResponseObject responseObject = new ResponseObject();
            if(executeFilterChain(context)){
                responseObject.setStatusCode(200);
                if (((RequestContext)context).getResponseHeaders() != null) {
                    responseObject.setHeaderMap(((RequestContext)context).getResponseHeaders());
                }
            } else {
                responseObject.setDirectResponse(true);
                responseObject.setStatusCode(Integer.parseInt(((RequestContext)context).getProperties().get("code").toString()));
                if (((RequestContext)context).getProperties().get("error_code") != null) {
                    responseObject.setErrorCode(((RequestContext)context).getProperties().get("error_code").toString());
                }
                if (((RequestContext)context).getProperties().get("error_code") != null) {
                    responseObject.setErrorDescription(((RequestContext)context).getProperties()
                            .get("error_description").toString());
                }
                if (((RequestContext)context).getResponseHeaders() != null && ((RequestContext)context).getResponseHeaders().size() > 0) {
                    responseObject.setHeaderMap(((RequestContext)context).getResponseHeaders());
                }
            }
            return responseObject;
        }
    }

    @Override
    public APIConfig getAPIConfig() {
        return this.apiConfig;
    }

    @Override
    public boolean executeFilterChain(Context context) {
        if(context instanceof WebSocketMetadataContext){
            return true;
        }else {
            boolean proceed;
            for (Filter filter : getFilters()) {
                proceed = filter.handleRequest(context);
                if (!proceed) {
                    return false;
                }
            }
            return true;
        }
    }

    public List<Filter> getUpgradeFilters(){
        return upgradeFilters;
    }

    public void initFilters(){
        AuthFilter authFilter = new AuthFilter();
        authFilter.init(apiConfig);
        CorsFilter corsFilter = new CorsFilter();
        this.filters.add(corsFilter);
        this.filters.add(authFilter);
    }

    public void initUpgradeFilters(){

    }
}
