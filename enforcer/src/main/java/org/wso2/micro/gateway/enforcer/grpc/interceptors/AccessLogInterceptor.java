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

package org.wso2.micro.gateway.enforcer.grpc.interceptors;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.websocket.WebSocketFrameRequest;

/**
 * Intercepts the gRPC request comes to the enforcer and logs the request access data.
 */
public class AccessLogInterceptor implements ServerInterceptor {
    private static final Logger logger = LogManager.getLogger(AccessLogInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        if (logger.isDebugEnabled()) {
            EnforcerServerCall enforcerServerCall = new EnforcerServerCall(serverCall);
            ServerCall.Listener listener = serverCallHandler.startCall(enforcerServerCall, metadata);
            return new EnforcerForwardingServerCallListener<ReqT>(serverCall.getMethodDescriptor(), listener) {
                @Override
                public void onMessage(ReqT message) {
                    if (message instanceof CheckRequest) {
                        CheckRequest checkRequest = (CheckRequest) message;
                        enforcerServerCall.setStartTime(System.currentTimeMillis());
                        enforcerServerCall.setTraceId(checkRequest.getAttributes().getRequest().getHttp().getId());
                        super.onMessage(message);
                    }else if(message instanceof WebSocketFrameRequest){
                        WebSocketFrameRequest webSocketFrameRequest = (WebSocketFrameRequest) message;
                        enforcerServerCall.setStartTime(System.currentTimeMillis());
                        enforcerServerCall.setTraceId(webSocketFrameRequest.getNodeId());
                    }
                }
            };
        }
        return serverCallHandler.startCall(serverCall, metadata);
    }

    private class EnforcerServerCall<ReqT, RespT> extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

        ServerCall<ReqT, RespT> serverCall;
        long startTime;
        String traceId;

        protected EnforcerServerCall(ServerCall<ReqT, RespT> serverCall) {
            super(serverCall);
            this.serverCall = serverCall;
        }

        @Override
        public void sendMessage(RespT message) {
            serverCall.sendMessage(message);
            if (message instanceof CheckResponse) {
                long responseTimeMillis = System.currentTimeMillis() - startTime;
                CheckResponse checkResponse = (CheckResponse) message;
                // log pattern -> trace ID, gRPC method name, response status code, response time
                logger.info(String.format("%s %s %d %d", traceId, serverCall.getMethodDescriptor().getFullMethodName(),
                        checkResponse.getStatus().getCode(), responseTimeMillis));
            }
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }
    }

    private class EnforcerForwardingServerCallListener<M>
            extends io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener<M> {
        String methodName;

        protected EnforcerForwardingServerCallListener(MethodDescriptor method, ServerCall.Listener<M> listener) {
            super(listener);
            methodName = method.getFullMethodName();
        }
    }
}

