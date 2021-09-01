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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.choreo.connect.enforcer.tracing;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;
import com.azure.monitor.opentelemetry.exporter.AzureMonitorTraceExporter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.api.trace.Span;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.TracingDTO;

public class AzureTraceExporter {

    private static final Logger LOGGER = LogManager.getLogger(AzureTraceExporter.class);
    private Tracer tracer;
    private boolean isTracerInitialized = false;
    private boolean isTracingEnabled = false;
    private static AzureTraceExporter azureTraceExporter;

    public AzureTraceExporter() {
        initializeTracer();
    }

    public static AzureTraceExporter getInstance() {
        if (azureTraceExporter == null) {
            synchronized (new Object()) {
                if (azureTraceExporter == null) {
                    azureTraceExporter = new AzureTraceExporter();
                }
            }
        }
        return azureTraceExporter;
    }

    /**
     * Method to initialize the tracer
     */
    private void initializeTracer() {

        TracingDTO tracingConfig = ConfigHolder.getInstance().getConfig().getTracingConfig();
        isTracingEnabled = tracingConfig.isTracingEnabled();

        if (isTracingEnabled && !isTracerInitialized) {
            String connectionString = tracingConfig.getConnectionString();
            if (StringUtils.isEmpty(connectionString)) {
                throw new RuntimeException("ConnectionString is mandatory when tracing is enabled");
            }
            AzureMonitorTraceExporter exporter = new AzureMonitorExporterBuilder()
                    .connectionString(connectionString)
                    .buildTraceExporter();
            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .setSampler(new RateLimitingSampler(tracingConfig.getMaximumTracesPerSecond()))
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();

            OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider).buildAndRegisterGlobal();
            isTracerInitialized = true;
            LOGGER.debug("AzureTraceExporter is successfully initialized.");
            this.tracer = openTelemetrySdk.getTracer(tracingConfig.getInstrumentationName());
        }
    }

    /**
     * Start the tracing span
     *
     * @param spanName   the name of the span
     * @param parentSpan the parent span
     * @param tracer     io.opentelemetry.api.trace.Span
     * @return a TracingSpan object
     */
    public TracingSpan startSpan(String spanName, TracingSpan parentSpan, TracingTracer tracer) {

        if (parentSpan == null) {
            Span span = tracer.getTracingTracer().spanBuilder(spanName).startSpan();
            return new TracingSpan(span);
        } else {
            Span childSpan = null;
            Span sp = parentSpan.getSpan();
            if (sp != null) {
                childSpan = tracer.getTracingTracer().spanBuilder(spanName).setParent(Context.current().with(sp)).startSpan();

            }
            return new TracingSpan(childSpan);
        }
    }

    /**
     * Set tag to the span
     *
     * @param span  the span tag is to be set
     * @param key   key
     * @param value value
     */
    public void setTag(TracingSpan span, String key, String value) {

        Span sp = span.getSpan();
        if (sp != null) {
            sp.setAttribute(key, value);
        }
    }

    public Tracer getTracer() {
        return tracer;
    }

    /**
     * Finish the span
     *
     * @param span span that is to be finished
     */
    public void finishSpan(TracingSpan span) {

        Span sp = span.getSpan();
        if (sp != null) {
            sp.end();
        }
    }

    public TracingTracer getGlobalTracer() {

        return new TracingTracer(tracer);
    }

    public boolean tracingEnabled() {

        return isTracingEnabled;
    }
}
