package org.wso2.micro.gateway.enforcer.api;


/**
 * WebSocketMetadataContext object is sent along with external auth response for WebSocket APIs as dynamic metadata.
 * Dynamic metadata will be available for the duration of a websocket connection and will be sent to enforcer with
 * other websocket frame related metadata like frame size, upstream host etc through the websocket metadata grpc service.
 * Those metadata will be used for web socket throttling and analytics.
 */
public class WebSocketMetadataContext implements Context{
    // TODO - (LahriuUdayanga) Finalize the instance variables
    private final String streamId;
    private final String applicationId;
    private final String applicationTier;
    private final String apiTier;
    private final String subscriptionTier;
    private final String username;
    private final String basepath;
    private final String apiVersion;
    private final String subscriberTenantDomain;
    private final String apiName;

    private WebSocketMetadataContext(Builder builder){
        this.streamId = builder.streamId;
        this.applicationId = builder.applicationId;
        this.applicationTier = builder.applicationTier;
        this.apiTier = builder.apiTier;
        this.subscriptionTier = builder.subscriptionTier;
        this.username = builder.username;
        this.basepath = builder.basepath;
        this.apiVersion = builder.apiVersion;
        this.subscriberTenantDomain = builder.subscriberTenantDomain;
        this.apiName = builder.apiName;
    }

    public static class Builder{
        private final String streamId;
        private String applicationId;
        private String applicationTier;
        private String apiTier;
        private String subscriptionTier;
        private String username;
        private String basepath;
        private String apiVersion;
        private String subscriberTenantDomain;
        private String apiName;

        public Builder(String streamId){
            this.streamId = streamId;
        }

        public Builder setApplicationId(String applicationId){
            this.applicationId = applicationId;
            return this;
        }

        public Builder setApplicationTier(String applicationTier){
            this.applicationTier = applicationTier;
            return this;
        }

        public Builder setApiTier(String apiTier){
            this.apiTier = apiTier;
            return this;
        }

        public Builder setSubscriptionTier(String subscriptionTier){
            this.subscriptionTier = subscriptionTier;
            return this;
        }

        public Builder setUsername(String username){
            this.username = username;
            return this;
        }

        public Builder setBasepath(String basepath){
            this.basepath = basepath;
            return this;
        }

        public Builder setApiVersion(String apiVersion){
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder setSubscriberTenantDomain(String subscriberTenantDomain){
            this.subscriberTenantDomain = subscriberTenantDomain;
            return this;
        }

        public Builder setApiName(String apiName){
            this.apiName = apiName;
            return this;
        }

        public WebSocketMetadataContext build() {
            return new WebSocketMetadataContext(this);
        }

    }

    public String getStreamId() {
        return streamId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getApplicationTier() {
        return applicationTier;
    }

    public String getApiTier() {
        return apiTier;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public String getUsername() {
        return username;
    }

    public String getBasepath() {
        return basepath;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getSubscriberTenantDomain() {
        return subscriberTenantDomain;
    }

    public String getApiName() {
        return apiName;
    }
}
