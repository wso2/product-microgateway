/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.websocket;


import org.wso2.choreo.connect.enforcer.security.AuthenticationContext;

/**
 * WebSocketMetadataContext object is the corresponding class implementation for dynamic metadata received from
 * WebSocket Metadata Service. Dynamic metadata will be available for the duration of a websocket connection and
 * will be sent to enforcer through the websocket metadata grpc service. Those metadata will be used for web socket
 * throttling and analytics.
 */

public class WebSocketMetadataContext {
    // TODO - (LahriuUdayanga) Finalize the instance variables
    private final String streamId;
    private final int frameLength;
    private final String upstreamHost;
    private final String basepath;
    private final String remoteIp;
    private final AuthenticationContext authenticationContext;

    private WebSocketMetadataContext(Builder builder) {
        this.streamId = builder.streamId;
        this.frameLength = builder.frameLength;
        this.upstreamHost = builder.upstreamHost;
        this.basepath = builder.basepath;
        this.authenticationContext = builder.authenticationContext;
        this.remoteIp = builder.remoteIp;

    }

    /**
     * Builder class fpr WebSocketMetadataContext
     */
    public static class Builder {
        private final String streamId;
        private int frameLength;
        private String upstreamHost;
        private String basepath;
        private String remoteIp;
        private AuthenticationContext authenticationContext;

        public Builder(String streamId) {
            this.streamId = streamId;
        }

        public Builder setFrameLength(int frameLength) {
            this.frameLength = frameLength;
            return this;
        }

        public Builder setUpstreamHost(String upstreamHost) {
            this.upstreamHost = upstreamHost;
            return this;
        }

        public Builder setBasepath(String basepath) {
            this.basepath = basepath;
            return this;
        }

        public Builder setAuthenticationContext(AuthenticationContext authenticationContext) {
            this.authenticationContext = authenticationContext;
            return this;
        }

        public Builder setRemoteIp(String remoteIp) {
            this.remoteIp = remoteIp;
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

    public String getRemoteIp() {
        return remoteIp;
    }
}
