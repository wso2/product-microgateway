package org.wso2.micro.gateway.enforcer.api;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.api.Api;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.cors.CorsFilter;
import org.wso2.micro.gateway.enforcer.security.AuthFilter;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

import java.util.ArrayList;
import java.util.List;

public class WebSocketAPI implements API <Context, Context>{

    private static final Logger logger = LogManager.getLogger(WebSocketAPI.class);
    private APIConfig apiConfig;
    private List<Filter<RequestContext>> filters = new ArrayList<>();
    private List<Filter<WebSocketMetadataContext>> upgradeFilters = new ArrayList<>();

    @Override
    public List<Filter<Context>> getFilters() {
        return null;
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
        initFilters();
        return basePath;
    }

    @Override
    public Context process(Context context) {
        if(context instanceof WebSocketMetadataContext){
            return context;
        }else {
            logger.info("websocket api process"+ context.toString());
            WebSocketAuthResponse webSocketAuthResponse = new WebSocketAuthResponse();
            if(executeFilterChain(context)){
                AuthenticationContext authenticationContext = ((RequestContext)context).getAuthenticationContext();
                webSocketAuthResponse.setWebSocketMetadataContext(authenticationContext, apiConfig);
                webSocketAuthResponse.setStatusCode(200);
                if (((RequestContext)context).getResponseHeaders() != null) {
                    webSocketAuthResponse.setHeaderMap(((RequestContext)context).getResponseHeaders());
                }
            } else {
                webSocketAuthResponse.setDirectResponse(true);
                webSocketAuthResponse.setStatusCode(Integer.parseInt(((RequestContext)context).getProperties().get("code").toString()));
                if (((RequestContext)context).getProperties().get("error_code") != null) {
                    webSocketAuthResponse.setErrorCode(((RequestContext)context).getProperties().get("error_code").toString());
                }
                if (((RequestContext)context).getProperties().get("error_code") != null) {
                    webSocketAuthResponse.setErrorDescription(((RequestContext)context).getProperties()
                            .get("error_description").toString());
                }
                if (((RequestContext)context).getResponseHeaders() != null && ((RequestContext)context).getResponseHeaders().size() > 0) {
                    webSocketAuthResponse.setHeaderMap(((RequestContext)context).getResponseHeaders());
                }
            }
            return webSocketAuthResponse;
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
            logger.info("normal filter chain");
            boolean proceed;
            for (Filter filter : getHttpFilters()) {
                proceed = filter.handleRequest(context);
                logger.info("proceed:"+ proceed);
                if (!proceed) {
                    return false;
                }
            }
            return true;
        }
    }

    public List<Filter<WebSocketMetadataContext>> getUpgradeFilters(){
        return upgradeFilters;
    }

    public List<Filter<RequestContext>> getHttpFilters(){
        return filters;
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
