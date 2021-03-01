package org.wso2.micro.gateway.enforcer.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.api.Api;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.cors.CorsFilter;
import org.wso2.micro.gateway.enforcer.security.AuthFilter;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

import java.util.ArrayList;
import java.util.List;

public class WebSocketAPI implements API {

    private static final Logger logger = LogManager.getLogger(WebSocketAPI.class);
    private APIConfig apiConfig;
    private final List<Filter> filters = new ArrayList<>();
    private final List<Filter> upgradeFilters = new ArrayList<>();

    @Override
    public List<Filter> getFilters() {
        return null;
    }

    @Override
    public String init(Api api) {
        String basePath = api.getBasePath();
        String name = api.getTitle();
        String version = api.getVersion();
        this.apiConfig = new APIConfig.Builder(name).basePath(basePath).version(version).
                apiType(APIConstants.ApiType.WEB_SOCKET).build();
        initFilters();
        return basePath;
    }

    @Override
    public ResponseObject process(RequestContext requestContext) {
        ResponseObject responseObject = new ResponseObject();
        if (executeFilterChain(requestContext)) {
            responseObject.setStatusCode(APIConstants.StatusCodes.OK.getCode());
            responseObject.setAuthenticationContext(requestContext.getAuthenticationContext());
            responseObject.setApiConfig(apiConfig);
            if (requestContext.getResponseHeaders() != null) {
                responseObject.setHeaderMap(requestContext.getResponseHeaders());
            }
        } else {
            // If a enforcer stops with a false, it will be passed directly to the client.
            responseObject.setDirectResponse(true);
            responseObject.setStatusCode(Integer.parseInt(
                    requestContext.getProperties().get(APIConstants.MessageFormat.STATUS_CODE).toString()));
            if (requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_CODE) != null) {
                responseObject.setErrorCode(
                        requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_CODE).toString());
            }
            if (requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_MESSAGE) != null) {
                responseObject.setErrorMessage(requestContext.getProperties()
                        .get(APIConstants.MessageFormat.ERROR_MESSAGE).toString());
            }
            if (requestContext.getProperties().get(APIConstants.MessageFormat.ERROR_DESCRIPTION) != null) {
                responseObject.setErrorDescription(requestContext.getProperties()
                        .get(APIConstants.MessageFormat.ERROR_DESCRIPTION).toString());
            }
            if (requestContext.getResponseHeaders() != null && requestContext.getResponseHeaders().size() > 0) {
                responseObject.setHeaderMap(requestContext.getResponseHeaders());
            }
        }
        return responseObject;
    }

    @Override
    public APIConfig getAPIConfig() {
        return this.apiConfig;
    }

    @Override
    public boolean executeFilterChain(RequestContext requestContext) {
        logger.info("normal filter chain");
        boolean proceed;
        for (Filter filter : getHttpFilters()) {
            proceed = filter.handleRequest(requestContext);
            logger.info("proceed:"+ proceed);
            if (!proceed) {
                return false;
            }
        }
        return true;
    }


    public List<Filter> getUpgradeFilters(){
        return upgradeFilters;
    }

    public List<Filter> getHttpFilters(){
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
        // TODO (LahiruUdayanga) - Initiate upgrade filter chain.
        // WebSocket throttle filter
        // WebSocket analytics filter
    }

    public AuthenticationContext processMetadata(RequestContext requestContext){
        return requestContext.getAuthenticationContext();
    }
}
