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
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigDefaults;

import java.util.Map;

/**
 * This class is responsible for managing and exporting tracing spans
 */
public class AzureTraceExporter implements TracerBuilder {

    private static final Logger LOGGER = LogManager.getLogger(AzureTraceExporter.class);
    private static AzureTraceExporter azureTraceExporter;

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
     * Initialize the tracer with AzureMonitorTraceExporter
     */
    @Override
    public Tracer initTracer(Map<String, String> properties) throws TracingException {

        String connectionString = properties.get(TracingConstants.CONNECTION_STRING);
        if (StringUtils.isEmpty(connectionString)) {
            throw new TracingException("Error initializing Azure Trace Exporter. ConnectionString is null or empty.");
        } else {
            String maxTracesPerSecondString = properties.get(TracingConstants.MAXIMUM_TRACES_PER_SECOND);
            int maxTracesPerSecond = StringUtils.isEmpty(maxTracesPerSecondString) ?
                    ConfigDefaults.MAXIMUM_TRACES_PER_SECOND : Integer.valueOf(maxTracesPerSecondString);
            String instrumentationName = StringUtils.isEmpty(properties.get(TracingConstants.INSTRUMENTATION_NAME)) ?
                    ConfigDefaults.INSTRUMENTATION_NAME : properties.get(TracingConstants.INSTRUMENTATION_NAME);

            AzureMonitorTraceExporter exporter = new AzureMonitorExporterBuilder()
                    .connectionString(connectionString).buildTraceExporter();

            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .setSampler(new RateLimitingSampler(maxTracesPerSecond))
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();

            OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider).buildAndRegisterGlobal();
            LOGGER.debug("Tracer successfully initialized with Azure Trace Exporter.");

            return openTelemetrySdk.getTracer(instrumentationName);
        }
    }
}
