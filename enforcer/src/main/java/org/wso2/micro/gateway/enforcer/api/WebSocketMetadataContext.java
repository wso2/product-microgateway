package org.wso2.micro.gateway.enforcer.api;


import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

/**
 * WebSocketMetadataContext object is sent along with external auth response for WebSocket APIs as dynamic metadata.
 * Dynamic metadata will be available for the duration of a websocket connection and will be sent to enforcer with
 * other websocket frame related metadata like frame size, upstream host etc through the websocket metadata grpc service.
 * Those metadata will be used for web socket throttling and analytics.
 */

public class WebSocketMetadataContext{
    // TODO - (LahriuUdayanga) Finalize the instance variables
    private final String streamId;
    private final int frameLength;
    private final String upstreamHost;
    private final String basepath;
    private final AuthenticationContext authenticationContext;

    private WebSocketMetadataContext(Builder builder){
        this.streamId = builder.streamId;
        this.frameLength = builder.frameLength;
        this.upstreamHost = builder.upstreamHost;
        this.basepath = builder.basepath;
        this.authenticationContext = builder.authenticationContext;


    }

    public static class Builder{
        private final String streamId;
        private int frameLength;
        private String upstreamHost;
        private String basepath;
        private AuthenticationContext authenticationContext;

        public Builder(String streamId){
            this.streamId = streamId;
        }

        public Builder setFrameLength(int frameLength){
            this.frameLength = frameLength;
            return this;
        }

        public Builder setUpstreamHost(String upstreamHost){
            this.upstreamHost = upstreamHost;
            return this;
        }

        public Builder setBasepath(String basepath){
            this.basepath = basepath;
            return this;
        }

        public Builder setAuthenticationContext(AuthenticationContext authenticationContext){
            this.authenticationContext = authenticationContext;
            return this;
        }

        public WebSocketMetadataContext build() {
            return new WebSocketMetadataContext(this);
        }

    }

    public String getStreamId() {
        return streamId;
    }

    public int getFrameLength() {
        return frameLength;
    }

    public String getUpstreamHost() {
        return upstreamHost;
    }

    public String getBasepath() {
        return basepath;
    }

    public AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }
}
