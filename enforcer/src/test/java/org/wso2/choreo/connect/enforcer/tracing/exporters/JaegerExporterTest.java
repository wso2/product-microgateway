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
        okProps.put(TracingConstants.CONF_ENDPOINT, "http://localhost:14268/api/traces");
        okProps.put(TracingConstants.CONF_MAX_TRACES_PER_SEC, "3");
        okProps.put(TracingConstants.CONF_INSTRUMENTATION_NAME, "CC");
        badProps.put(TracingConstants.CONF_ENDPOINT, "localhost:14268");
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
        badProps.put(TracingConstants.CONF_ENDPOINT, "");
        Assert.assertThrows("Incorrect exception was thrown", TracingException.class, () ->
                JaegerExporter.getInstance().initTracer(badProps));
    }
}
