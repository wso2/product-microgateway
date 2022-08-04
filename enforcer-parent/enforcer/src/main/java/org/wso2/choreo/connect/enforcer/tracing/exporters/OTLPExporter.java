package org.wso2.choreo.connect.enforcer.tracing.exporters;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
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
        // String path = properties.get(TracingConstants.CONF_ENDPOINT);
        String endpoint;
        ConfigHolder conf = ConfigHolder.getInstance();
//        try {
//            int port = Integer.parseInt(properties.get(TracingConstants.CONF_PORT));
//            endpoint = new URL("http", host, port).toString();
//        } catch (MalformedURLException e) {
//            throw new TracingException("Couldn't initialize the OTLP exporter. Invalid endpoint definition", e);
//        }

        OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder().setEndpoint("http://jaeger:4317")
                .build();
        String serviceName = TracingConstants.SERVICE_NAME_PREFIX + '-' + conf.getEnvVarConfig().getEnforcerLabel();
        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
        String maxTracesPerSecondString = properties.get(TracingConstants.CONF_MAX_TRACES_PER_SEC);
        int maxTracesPerSecond = StringUtils.isEmpty(maxTracesPerSecondString) ?
                TracingConstants.DEFAULT_MAX_TRACES_PER_SEC : Integer.parseInt(maxTracesPerSecondString);
        String instrumentName = properties.get(TracingConstants.CONF_INSTRUMENTATION_NAME);

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
