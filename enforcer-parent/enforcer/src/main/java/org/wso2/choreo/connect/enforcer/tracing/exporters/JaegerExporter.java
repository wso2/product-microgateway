/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.tracing.exporters;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.tracing.RateLimitingSampler;
import org.wso2.choreo.connect.enforcer.tracing.TracerBuilder;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * This class is responsible for exporting tracing spans for jaeger.
 */
public class JaegerExporter implements TracerBuilder {

    private static final Logger LOGGER = LogManager.getLogger(JaegerExporter.class);
    private static JaegerExporter exporter;

    public static JaegerExporter getInstance() {
        if (exporter == null) {
            synchronized (new Object()) {
                if (exporter == null) {
                    exporter = new JaegerExporter();
                }
            }
        }
        return exporter;
    }

    /**
     * Initialize the tracer SDK with {@link JaegerExporter}.
     */
    @Override
    public OpenTelemetrySdk initSdk(Map<String, String> properties) throws TracingException {
        String ep;
        String host = properties.get(TracingConstants.CONF_HOST);
        String path = properties.get(TracingConstants.CONF_ENDPOINT);
        ConfigHolder conf = ConfigHolder.getInstance();
        try {
            int port = Integer.parseInt(properties.get(TracingConstants.CONF_PORT));
            ep = new URL("http", host, port, path).toString();
        } catch (MalformedURLException | NumberFormatException e) {
            throw new TracingException("Couldn't initialize the zipkin exporter. Invalid endpoint definition", e);
        }

        JaegerThriftSpanExporter jaegerExporter = JaegerThriftSpanExporter.builder().setEndpoint(ep).build();
        String serviceName = TracingConstants.SERVICE_NAME_PREFIX + '-' + conf.getEnvVarConfig().getEnforcerLabel();
        Resource serviceNameResource =
                Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
        String maxTracesPerSecondString = properties.get(TracingConstants.CONF_MAX_TRACES_PER_SEC);
        int maxTracesPerSecond = StringUtils.isEmpty(maxTracesPerSecondString) ?
                TracingConstants.DEFAULT_MAX_TRACES_PER_SEC : Integer.parseInt(maxTracesPerSecondString);

        // Set to process the spans by the Jaeger Exporter
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
                .setSampler(new RateLimitingSampler(maxTracesPerSecond))
                .setResource(Resource.getDefault().merge(serviceNameResource))
                .build();
        OpenTelemetrySdk ot = OpenTelemetrySdk.builder().setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        LOGGER.info("Trace SDK successfully initialized with Jaeger Trace Exporter.");
        return ot;
    }
}
