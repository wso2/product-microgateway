package org.wso2.micro.gateway.enforcer.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.api.*;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;

public class WebSocketHandler implements RequestHandler<RateLimitRequest, WebSocketMetadataContext>{
    private static final Logger logger = LogManager.getLogger(WebSocketHandler.class);
    @Override
    public WebSocketMetadataContext process(RateLimitRequest request) {
        API<WebSocketMetadataContext, WebSocketMetadataContext> matchedAPI = APIFactory.getInstance().getMatchedAPI(request);
        if(matchedAPI == null){
            return null;
        } else if (logger.isDebugEnabled()) {
            APIConfig api = matchedAPI.getAPIConfig();
            logger.debug("API {}/{} found in the cache", api.getBasePath(), api.getVersion());
        }
        WebSocketMetadataContext webSocketMetadata = buildMetadataContext(matchedAPI, request);

        return matchedAPI.process(webSocketMetadata);
    }

    private WebSocketMetadataContext buildMetadataContext(API<WebSocketMetadataContext, WebSocketMetadataContext> api, RateLimitRequest rateLimitRequest){
        String streamId = rateLimitRequest.getMetadataContext().getFilterMetadataMap().
                get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.WEBSOCKET_STREAM_ID).getStringValue();
        String apiName = rateLimitRequest.getMetadataContext().getFilterMetadataMap().get(APIConstants.EXT_AUTHZ_METADATA)
                .getFieldsMap().get(APIConstants.GW_API_NAME_PARAM).getStringValue();
        String apiVersion = rateLimitRequest.getMetadataContext().getFilterMetadataMap().get(APIConstants.EXT_AUTHZ_METADATA)
                .getFieldsMap().get(APIConstants.GW_VERSION_PARAM).getStringValue();
        String apiBasepath = rateLimitRequest.getMetadataContext().getFilterMetadataMap().get(APIConstants.EXT_AUTHZ_METADATA)
                .getFieldsMap().get(APIConstants.GW_BASE_PATH_PARAM).getStringValue();
        return new WebSocketMetadataContext.Builder(streamId).setApiName(apiName).setApiVersion(apiVersion)
                .setBasepath(apiBasepath).build();
    }
}
