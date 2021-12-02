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

package org.wso2.choreo.connect.enforcer.grpc.interceptors;

import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.tracing.TracerFactory;
import org.wso2.choreo.connect.enforcer.tracing.TracingContextHolder;
import org.wso2.choreo.connect.enforcer.tracing.Utils;

/**
 * gRPC interceptor to initialize tracing using context propagated from router.
 */
public class OpenTelemetryInterceptor implements ServerInterceptor {
    private static final Logger logger = LogManager.getLogger(OpenTelemetryInterceptor.class);

    // Extract the Distributed Context from the gRPC metadata
    private static final TextMapGetter<Metadata> getter =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(Metadata carrier) {
                    return carrier.keys();
                }

                @Override
                public String get(Metadata carrier, String key) {
                    Metadata.Key<String> k = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                    if (carrier.containsKey(k)) {
                        return carrier.get(k);
                    }
                    return "";
                }
            };

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                      ServerCallHandler<ReqT, RespT> next) {
        logger.debug("Intercepting the request");

        if (Utils.tracingEnabled()) {
            TextMapPropagator propagator = TracerFactory.getInstance().getTextPropagator();
            Context parentContext = propagator.extract(Context.current(), headers, getter);
            TracingContextHolder.getInstance().setContext(parentContext);
            logger.debug("Attached to propagated parent tracing context.");
        }
        return Contexts.interceptCall(io.grpc.Context.current(), call, headers, next);
    }
}
