package org.wso2.micro.gateway.enforcer.api;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

import java.util.UUID;

public class WebSocketAuthResponse extends ResponseObject{

    private static final Logger logger = LogManager.getLogger(WebSocketAuthResponse.class);

    public WebSocketAuthResponse() {
        super();
    }

    private WebSocketMetadataContext webSocketMetadataContext;

    public WebSocketMetadataContext getWebSocketMetadataContext() {
        return webSocketMetadataContext;
    }

    public void setWebSocketMetadataContext(AuthenticationContext authenticationContext, APIConfig apiConfig) {
        // TODO - (LahiruUdayanga) set all the related metadata
        webSocketMetadataContext = new WebSocketMetadataContext.Builder(UUID.randomUUID().toString())
                .setBasepath(apiConfig.getBasePath())
                .setApiVersion(apiConfig.getVersion())
                .setApiName(apiConfig.getName()).build();
    }

    public Struct getMetadataStruct(){
        // TODO - (LahiruUdayanga) set all the related metadata to protobuf struct
        Struct.Builder structBuilder = Struct.newBuilder();
        return structBuilder.putFields(APIConstants.WEBSOCKET_STREAM_ID, Value.newBuilder().setStringValue(webSocketMetadataContext.getStreamId()).build())
                .putFields(APIConstants.GW_BASE_PATH_PARAM, Value.newBuilder().setStringValue(webSocketMetadataContext.getBasepath()).build())
                .putFields(APIConstants.GW_VERSION_PARAM, Value.newBuilder().setStringValue(webSocketMetadataContext.getApiVersion()).build())
                .putFields(APIConstants.GW_API_NAME_PARAM, Value.newBuilder().setStringValue(webSocketMetadataContext.getApiName()).build()).build();
    }
}
