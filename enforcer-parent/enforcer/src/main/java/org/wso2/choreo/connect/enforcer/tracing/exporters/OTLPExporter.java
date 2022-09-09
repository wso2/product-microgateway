/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.tracing.exporters;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.tracing.RateLimitingSampler;
import org.wso2.choreo.connect.enforcer.tracing.TracerBuilder;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingException;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Trace Exporter for Open Telemetry Protocol
 */
public class OTLPExporter implements TracerBuilder {

    private static final Logger LOGGER = LogManager.getLogger(OTLPExporter.class);

    private static OTLPExporter exporter;

    public static OTLPExporter getInstance() {
        if (exporter == null) {
            synchronized (new Object()) {
                if (exporter == null) {
                    exporter = new OTLPExporter();
                }
            }
        }
        return exporter;
    }

    @Override
    public OpenTelemetrySdk initSdk(Map<String, String> properties) throws TracingException {
        String host = properties.get(TracingConstants.CONF_HOST);
        String portString = properties.get(TracingConstants.CONF_PORT);
        String authHeaderName = properties.get(TracingConstants.CONF_AUTH_HEADER_NAME);
        String authHeaderValue = properties.get(TracingConstants.CONF_AUTH_HEADER_VALUE);
        String connectionString = properties.get(TracingConstants.CONF_CONNECTION_STRING);
        ConfigHolder conf = ConfigHolder.getInstance();
        OtlpGrpcSpanExporterBuilder otlpGrpcSpanExporterBuilder = OtlpGrpcSpanExporter.builder();

        if (connectionString != null) {
            otlpGrpcSpanExporterBuilder.setEndpoint(connectionString);
        } else if (host != null && portString != null) {
            try {
                int port = Integer.parseInt(portString);
                String endpoint = new URIBuilder().setHost(host).setPort(port)
                        .setScheme(host.contains("https") ? "https" : "http")
                        .build().toString();
                otlpGrpcSpanExporterBuilder.setEndpoint(endpoint);
            } catch (URISyntaxException | NumberFormatException e) {
                throw new TracingException("Couldn't initialize the OTLP exporter. Invalid endpoint definition", e);
            }
        } else {
            throw new TracingException("Invalid endpoint configuration for OTLP gRPC collector endpoint. " +
                    "Please provide host, port or connectionString");
        }

        // Optional auth header for Saas providers and other telemetry backends that supports token/key based
        // authentication.
        if (!StringUtils.isBlank(authHeaderName) && !StringUtils.isBlank(authHeaderValue)) {
            otlpGrpcSpanExporterBuilder.addHeader(authHeaderName, authHeaderValue);
        }

        OtlpGrpcSpanExporter otlpGrpcSpanExporter = otlpGrpcSpanExporterBuilder.build();
        String serviceName = TracingConstants.SERVICE_NAME_PREFIX + '-' + conf.getEnvVarConfig().getEnforcerLabel();
        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
        String maxTracesPerSecondString = properties.get(TracingConstants.CONF_MAX_TRACES_PER_SEC);
        int maxTracesPerSecond = StringUtils.isEmpty(maxTracesPerSecondString) ?
                TracingConstants.DEFAULT_MAX_TRACES_PER_SEC : Integer.parseInt(maxTracesPerSecondString);

        SdkTracerProvider provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor
                        .create(otlpGrpcSpanExporter)).setSampler(new RateLimitingSampler(maxTracesPerSecond))
                        .setResource(Resource.getDefault().merge(serviceNameResource)).build();
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
        LOGGER.info("Trace SDK successfully initialized with OTLP Trace Exporter");
        return openTelemetrySdk;
    }
}
