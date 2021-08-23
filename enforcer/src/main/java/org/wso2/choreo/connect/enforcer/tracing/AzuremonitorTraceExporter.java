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
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.api.trace.Span;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AzuremonitorTraceExporter {

    private static final Logger LOGGER = LogManager.getLogger(AzuremonitorTraceExporter.class);

    private static Tracer tracer;

    private static Boolean isMonitorInitialized = false;

    private static final String NAME = "Choreo";

    private static String connectionString = "InstrumentationKey=e52808e3-d7e7-44fe-a102-eab3660bbeff;IngestionEndpoint=https://westus2-2.in.applicationinsights.azure.com/";

    public AzuremonitorTraceExporter() {
        if (!isMonitorInitialized) {
            initializeTracer();
        }
    }

    public void initializeTracer() {
            AzureMonitorTraceExporter exporter = new AzureMonitorExporterBuilder()
                    .connectionString(connectionString)
                    .buildTraceExporter();
            SdkTracerProvider tracerProvider = SdkTracerProvider.builder().setSampler(Sampler.alwaysOn())
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                    .build();

            OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .buildAndRegisterGlobal();
            isMonitorInitialized = true;
            this.tracer = openTelemetrySdk.getTracer(NAME);
    }

    /**
     * Start the tracing span
     *
     * @param spanName
     * @param parentSpan
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
                } /*else {
                    childSpan = tracer.getTracingTracer().buildSpan(spanName).asChildOf((SpanContext) sp).start();
                }
            } else {
                childSpan = tracer.getTracingTracer().buildSpan(spanName).start();
            }*/
                return new TracingSpan(childSpan);
            }
        }
        return null;
    }

    /**
     * Set tag to the span
     *
     * @param span
     * @param key
     * @param value
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
     * @param span
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

}
