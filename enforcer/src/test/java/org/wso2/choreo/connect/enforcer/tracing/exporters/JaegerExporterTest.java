package org.wso2.choreo.connect.enforcer.tracing.exporters;

import io.opentelemetry.api.trace.Tracer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingException;

import java.util.HashMap;
import java.util.Map;

public class JaegerExporterTest {
    private static Map<String, String> okProps;
    private static Map<String, String> badProps;

    @BeforeClass
    public static void setup() {
        okProps = new HashMap<>();
        badProps = new HashMap<>();
        okProps.put(TracingConstants.CONNECTION_STRING, "http://localhost:9411/api/v2/span");
        okProps.put(TracingConstants.MAXIMUM_TRACES_PER_SECOND, "3");
        okProps.put(TracingConstants.INSTRUMENTATION_NAME, "CC");
        badProps.put(TracingConstants.CONNECTION_STRING, "localhost:9411");
    }

    @Test
    public void testSuccessExporterInit() throws TracingException {
        Tracer t = JaegerExporter.getInstance().initTracer(okProps);
        Assert.assertNotNull("Tracer can't be null", t);
    }

    @Test
    public void testInitWithInvalidEP() {
        Assert.assertThrows("Incorrect exception was thrown", IllegalArgumentException.class, () ->
                JaegerExporter.getInstance().initTracer(badProps));
    }

    @Test
    public void testInitWithoutEP() {
        badProps.put(TracingConstants.CONNECTION_STRING, "");
        Assert.assertThrows("Incorrect exception was thrown", TracingException.class, () ->
                JaegerExporter.getInstance().initTracer(badProps));
    }
}
