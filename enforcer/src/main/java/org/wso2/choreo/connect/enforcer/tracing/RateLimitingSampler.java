/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wso2.choreo.connect.enforcer.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;

/**
 * This class is copied from https://github.com/open-telemetry/opentelemetry-java/blob/v1.0.0/sdk-extensions/
 * jaeger-remote-sampler/src/main/java/io/opentelemetry/sdk/extension/trace/jaeger/sampler/RateLimitingSampler.java.
 * This sampler uses a leaky bucket rate limiter to ensure that traces are sampled with a certain constant rate.
 */
public class RateLimitingSampler implements Sampler {
    public static final String TYPE = "ratelimiting";

    private final double maxTracesPerSecond;
    private final RateLimiter rateLimiter;
    private final SamplingResult onSamplingResult;
    private final SamplingResult offSamplingResult;

    /**
     * Creates rate limiting sampler.
     *
     * @param maxTracesPerSecond the maximum number of sampled traces per second.
     */
    public RateLimitingSampler(int maxTracesPerSecond) {
        this.maxTracesPerSecond = maxTracesPerSecond;
        double maxBalance = maxTracesPerSecond < 1.0 ? 1.0 : maxTracesPerSecond;
        this.rateLimiter = new RateLimiter(maxTracesPerSecond, maxBalance, Clock.getDefault());
        Attributes attributes = Attributes.empty();
        this.onSamplingResult = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE, attributes);
        this.offSamplingResult = SamplingResult.create(SamplingDecision.DROP, attributes);
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        final Span parentSpan = Span.fromContext(parentContext);
        final SpanContext parentSpanContext = parentSpan.getSpanContext();
        if (parentSpanContext.isValid()) {
            // child span
            return parentSpanContext.isSampled() ? onSamplingResult : offSamplingResult;
        }
        // root span
        return this.rateLimiter.checkCredit(1.0) ? onSamplingResult : offSamplingResult;
    }

    @Override
    public String getDescription() {
        return String.format("RateLimitingSampler{%.2f}", maxTracesPerSecond);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    // Visible for testing
    double getMaxTracesPerSecond() {
        return maxTracesPerSecond;
    }
}
