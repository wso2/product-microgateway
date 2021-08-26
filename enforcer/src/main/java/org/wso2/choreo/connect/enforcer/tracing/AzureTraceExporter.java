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

import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;

public class AzureTraceExporter {

    private static final Logger LOGGER = LogManager.getLogger(AzureTraceExporter.class);

    private static Tracer tracer;

    private static Boolean isTracerInitialized = false;

    private static boolean isTracingEnabled;

    private static final int MAX_TRACES_PER_SECOND = 2;

    private static final String INSTRUMENTATION_NAME = "Choreo";

    private static String connectionString = "InstrumentationKey=e52808e3-d7e7-44fe-a102-eab3660bbeff;IngestionEndpoint=https://westus2-2.in.applicationinsights.azure.com/";

    public AzureTraceExporter() {
        isTracingEnabled = ConfigHolder.getInstance().getConfig().getTracingConfig().isTracingEnabled();
        if (isTracingEnabled && !isTracerInitialized) {
            initializeTracer();
        }
    }

    public void initializeTracer() {
        AzureMonitorTraceExporter exporter = new AzureMonitorExporterBuilder()
                .connectionString(connectionString)
                .buildTraceExporter();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().setSampler(new RateLimitingSampler(MAX_TRACES_PER_SECOND)).addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
        LOGGER.debug("AzureTraceExporter is successfully initialized.");
        isTracerInitialized = true;
        this.tracer = openTelemetrySdk.getTracer(INSTRUMENTATION_NAME);
    }

    private static Sampler selectSampler(String samplerType, int maxTracesPerSecond) {
        switch (samplerType) {
            default:
                return new RateLimitingSampler(2);
            case "alwaysOn":
                return Sampler.alwaysOn();
            case "alwaysOff":
                return Sampler.alwaysOff();
            case "rateLimiting":
                return new RateLimitingSampler(maxTracesPerSecond);
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
    public static TracingSpan startSpan(String spanName, TracingSpan parentSpan, TracingTracer tracer) {

        if (parentSpan == null) {
            Span span = tracer.getTracingTracer().spanBuilder(spanName).startSpan();
            return new TracingSpan(span);
        } else {
            Object sp = parentSpan.getSpan();
            Span childSpan = null;
            if (sp != null) {
                if (sp instanceof io.opentelemetry.api.trace.Span) {
                    childSpan = tracer.getTracingTracer().spanBuilder(spanName).setParent(Context.current().with((Span) sp)).startSpan();
                }
                return new TracingSpan(childSpan);
            }
        }
        return null;
    }

    /**
     * Set tag to the span
     *
     * @param span  the span tag is to be set
     * @param key   key
     * @param value value
     */
    public static void setTag(TracingSpan span, String key, String value) {

        Object sp = span.getSpan();
        if (sp instanceof io.opentelemetry.api.trace.Span) {
            ((Span) sp).setAttribute(key, value);
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
    public static void finishSpan(TracingSpan span) {

        Object sp = span.getSpan();
        if (sp instanceof io.opentelemetry.api.trace.Span) {
            ((io.opentelemetry.api.trace.Span) sp).end();
        }
    }

    public static TracingTracer getGlobalTracer() {

        return new TracingTracer(tracer);
    }

    public static boolean tracingEnabled() {

        return isTracingEnabled;
    }

}