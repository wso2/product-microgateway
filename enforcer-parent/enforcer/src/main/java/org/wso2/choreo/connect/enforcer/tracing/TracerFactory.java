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

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.TracingDTO;
import org.wso2.choreo.connect.enforcer.server.AuthServer;
import org.wso2.choreo.connect.enforcer.tracing.exporters.AzureExporter;
import org.wso2.choreo.connect.enforcer.tracing.exporters.JaegerExporter;
import org.wso2.choreo.connect.enforcer.tracing.exporters.OTLPExporter;
import org.wso2.choreo.connect.enforcer.tracing.exporters.ZipkinExporter;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a factory class used to implement tracers with different exporters.
 */
public class TracerFactory {

    private static final Logger logger = LogManager.getLogger(AuthServer.class);
    private Tracer tracer;
    private TextMapPropagator textPropagator;
    private static TracerFactory tracerFactory;

    public static TracerFactory getInstance() {
        if (tracerFactory == null) {
            synchronized (new Object()) {
                if (tracerFactory == null) {
                    tracerFactory = new TracerFactory();
                }
            }
        }
        return tracerFactory;
    }

    public void init() throws TracingException {
        OpenTelemetrySdk sdk = null;
        TracingDTO tracingConfig = ConfigHolder.getInstance().getConfig().getTracingConfig();
        String exporterType = tracingConfig.getExporterType();
        Map<String, String> properties = new HashMap<>(tracingConfig.getConfigProperties());
        String instrumentName = properties.get(TracingConstants.CONF_INSTRUMENTATION_NAME);
        String instrumentationName = StringUtils.isEmpty(instrumentName) ?
                TracingConstants.DEFAULT_INSTRUMENTATION_NAME : instrumentName;

        if (!properties.isEmpty()) {
            properties.replaceAll((k, v) -> Utils.replaceEnvRegex(v));
        } else {
            throw new TracingException("Error initializing Tracer. Missing required configuration parameters.");
        }

        // Future tracer implementations can be initialized from here
        if (StringUtils.isEmpty(exporterType)) {
            logger.warn("Tracer exporter type not defined, defaulting to Jaeger Trace Exporter");
            exporterType = TracingConstants.JAEGER_TRACE_EXPORTER;
        }
        if (exporterType.equalsIgnoreCase(TracingConstants.AZURE_TRACE_EXPORTER)) {
            sdk = AzureExporter.getInstance().initSdk(properties);
        } else if (TracingConstants.JAEGER_TRACE_EXPORTER.equalsIgnoreCase(exporterType)) {
            sdk = JaegerExporter.getInstance().initSdk(properties);
        } else if (TracingConstants.ZIPKIN_TRACE_EXPORTER.equalsIgnoreCase(exporterType)) {
            sdk = ZipkinExporter.getInstance().initSdk(properties);
        } else if (TracingConstants.OTLP_TRACE_EXPORTER.equalsIgnoreCase(exporterType)) {
            sdk = OTLPExporter.getInstance().initSdk(properties);
        } else {
            throw new TracingException("Tracer exporter type: " + exporterType + "not found!");
        }

        if (sdk == null) {
            return;
        }
        this.tracer = sdk.getTracer(instrumentationName);
        this.textPropagator = sdk.getPropagators().getTextMapPropagator();
    }

    public TextMapPropagator getTextPropagator() {
        return textPropagator;
    }

    public Tracer getTracer() {
        return tracer;
    }
}
