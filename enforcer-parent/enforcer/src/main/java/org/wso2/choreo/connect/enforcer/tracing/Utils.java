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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import org.wso2.choreo.connect.enforcer.constants.Constants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Span utility class
 */
public class Utils {

    private static boolean isTracingEnabled = false;

    /**
     * Initialize a span with parent context. Used to link the enforcer traces under
     * router's trace context.
     *
     * @param spanName the name of the span
     * @param context  parent trace context
     * @param tracer   a tracer instance
     * @return a TracingSpan object
     */
    public static TracingSpan startSpan(String spanName, Context context, TracingTracer tracer) {
        if (context == null) {
            return startSpan(spanName, tracer);
        }
        Span cs = tracer.getTracingTracer().spanBuilder(spanName)
                .setParent(context)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        return new TracingSpan(cs);
    }

    /**
     * Start the tracing span
     *
     * @param spanName the name of the span
     * @param tracer   io.opentelemetry.api.trace.Span
     * @return a TracingSpan object
     */
    public static TracingSpan startSpan(String spanName, TracingTracer tracer) {
        Span span = tracer.getTracingTracer().spanBuilder(spanName).startSpan();
        return new TracingSpan(span);
    }

    /**
     * Set tag to the span
     *
     * @param span  the span tag is to be set
     * @param key   key
     * @param value value
     */
    public static void setTag(TracingSpan span, String key, String value) {

        Span sp = span.getSpan();
        if (sp != null) {
            sp.setAttribute(key, value);
        }
    }

    /**
     * Finish the span
     *
     * @param span span that is to be finished
     */
    public static void finishSpan(TracingSpan span) {

        Span sp = span.getSpan();
        if (sp != null) {
            sp.end();
        }
    }

    public static void setTracingEnabled(boolean isTracingEnabled) {

        Utils.isTracingEnabled = isTracingEnabled;
    }

    public static boolean tracingEnabled() {

        return isTracingEnabled;
    }

    public static TracingTracer getGlobalTracer() {

        return new TracingTracer(TracerFactory.getInstance().getTracer());
    }

    public static String replaceEnvRegex(String value) {
        Matcher m = Pattern.compile("\\$env\\{(.*?)\\}").matcher(value);
        if (value.contains(Constants.ENV_PREFIX)) {
            while (m.find()) {
                String envName = value.substring(m.start() + 5, m.end() - 1);
                if (System.getenv(envName) != null) {
                    value = value.replace(value.substring(m.start(), m.end()), System.getenv(envName));
                }
            }
        }
        return value;
    }
}
