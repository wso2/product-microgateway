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
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.tracing.RateLimitingSampler;
import org.wso2.choreo.connect.enforcer.tracing.TracerBuilder;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for exporting tracing spans for zipkin.
 */
public class ZipkinExporter implements TracerBuilder {
    private static final Logger LOGGER = LogManager.getLogger(ZipkinExporter.class);
    private static ZipkinExporter exporter;

    public static ZipkinExporter getInstance() {
        if (exporter == null) {
            synchronized (new Object()) {
                if (exporter == null) {
                    exporter = new ZipkinExporter();
                }
            }
        }
        return exporter;
    }

    /**
     * Initialize the tracer with {@link ZipkinExporter}.
     */
    @Override
    public Tracer initTracer(Map<String, String> properties) throws TracingException {
        String zipkinEp = properties.get(TracingConstants.CONF_ENDPOINT);

        if (StringUtils.isEmpty(zipkinEp)) {
            throw new TracingException("Error initializing zipkin Trace Exporter. Zipkin endpoint is missing.");
        }
        String readTimeoutString = properties.get(properties.get(TracingConstants.CONF_EXPORTER_TIMEOUT));
        long readTimeout = !StringUtils.isEmpty(readTimeoutString) ? Long.parseLong(readTimeoutString)
                : TracingConstants.DEFAULT_TRACING_READ_TIMEOUT;
        ZipkinSpanExporter zipkinExporter = ZipkinSpanExporter.builder()
                .setEndpoint(zipkinEp)
                .setReadTimeout(readTimeout, TimeUnit.SECONDS)
                .build();
        Resource serviceNameResource =
                Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, TracingConstants.SERVICE_NAME));
        String maxTracesPerSecondString = properties.get(TracingConstants.CONF_MAX_TRACES_PER_SEC);
        String instrumentName = properties.get(TracingConstants.CONF_INSTRUMENTATION_NAME);
        int maxTracesPerSecond = StringUtils.isEmpty(maxTracesPerSecondString) ?
                TracingConstants.DEFAULT_MAX_TRACES_PER_SEC : Integer.parseInt(maxTracesPerSecondString);
        String instrumentationName = StringUtils.isEmpty(instrumentName) ?
                TracingConstants.DEFAULT_INSTRUMENTATION_NAME : instrumentName;

        // Set to process the spans by the zipkin Exporter
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(zipkinExporter))
                .setSampler(new RateLimitingSampler(maxTracesPerSecond))
                .setResource(Resource.getDefault().merge(serviceNameResource))
                .build();
        OpenTelemetrySdk ot = OpenTelemetrySdk.builder().setTracerProvider(provider).buildAndRegisterGlobal();

        LOGGER.info("Tracer successfully initialized with Zipkin Trace Exporter.");
        return ot.getTracer(instrumentationName);
    }
}
