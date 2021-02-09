package org.wso2.micro.gateway.enforcer.api;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
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
        logger.info("getApplicationId"+authenticationContext.getApplicationId());
        logger.info("getApplicationTier"+authenticationContext.getApplicationTier());
        logger.info("getApiTier"+authenticationContext.getApiTier());
        logger.info("getSubscriber"+authenticationContext.getSubscriber());
        logger.info("getUsername"+authenticationContext.getUsername());
        logger.info("getBasePath"+apiConfig.getBasePath());
        logger.info("getVersion"+apiConfig.getVersion());
        logger.info("getSubscriberTenantDomain"+authenticationContext.getSubscriberTenantDomain());
        logger.info("getName"+apiConfig.getName());

        webSocketMetadataContext = new WebSocketMetadataContext.Builder(UUID.randomUUID().toString())
//                .setApplicationId(authenticationContext.getApplicationId())
//                .setApplicationTier(authenticationContext.getApplicationTier())
//                .setApiTier(authenticationContext.getApiTier())
//                .setSubscriptionTier(authenticationContext.getSubscriber())
//                .setUsername(authenticationContext.getUsername())
                .setBasepath(apiConfig.getBasePath())
                .setApiVersion(apiConfig.getVersion())
//                .setSubscriberTenantDomain(authenticationContext.getSubscriberTenantDomain())
                .setApiName(apiConfig.getName()).build();
    }

    public Struct getMetadataStruct(){
        Struct.Builder structBuilder = Struct.newBuilder();
        return structBuilder.putFields("streamId", Value.newBuilder().setStringValue(webSocketMetadataContext.getStreamId()).build())
//                .putFields("applicationId", Value.newBuilder().setStringValue(webSocketMetadataContext.getApplicationId()).build())
//                .putFields("applicationTier", Value.newBuilder().setStringValue(webSocketMetadataContext.getApplicationTier()).build())
//                .putFields("apiTier", Value.newBuilder().setStringValue(webSocketMetadataContext.getApiTier()).build())
//                .putFields("subscriptionTier", Value.newBuilder().setStringValue(webSocketMetadataContext.getSubscriptionTier()).build())
//                .putFields("username", Value.newBuilder().setStringValue(webSocketMetadataContext.getUsername()).build())
                .putFields("basepath", Value.newBuilder().setStringValue(webSocketMetadataContext.getBasepath()).build())
                .putFields("apiVersion", Value.newBuilder().setStringValue(webSocketMetadataContext.getApiVersion()).build())
//                .putFields("subscriberTenantDomain", Value.newBuilder().setStringValue(webSocketMetadataContext.getSubscriberTenantDomain()).build())
                .putFields("apiName", Value.newBuilder().setStringValue(webSocketMetadataContext.getApiName()).build()).build();
    }
}
