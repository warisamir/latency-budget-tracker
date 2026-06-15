package com.coinbase.latencytracker.config;

import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultTracerConfig {

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer defaultTracer() {
        return new NoOpTracer();
    }

    public static class NoOpTracer implements Tracer {
        @Override
        public io.opentelemetry.api.trace.SpanBuilder spanBuilder(String spanName) {
            return new NoOpSpanBuilder();
        }
    }

    public static class NoOpSpanBuilder implements io.opentelemetry.api.trace.SpanBuilder {
        @Override
        public io.opentelemetry.api.trace.SpanBuilder setAttribute(String key, String value) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder setAttribute(String key, long value) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder setAttribute(String key, double value) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder setAttribute(String key, boolean value) {
            return this;
        }

        @Override
        public <T> io.opentelemetry.api.trace.SpanBuilder setAttribute(io.opentelemetry.api.common.AttributeKey<T> key, T value) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder setSpanKind(io.opentelemetry.api.trace.SpanKind spanKind) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder setParent(io.opentelemetry.context.Context context) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder setNoParent() {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder addLink(io.opentelemetry.api.trace.SpanContext spanContext) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder addLink(io.opentelemetry.api.trace.SpanContext spanContext, io.opentelemetry.api.common.Attributes attributes) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.SpanBuilder setStartTimestamp(long startTimestamp, java.util.concurrent.TimeUnit unit) {
            return this;
        }

        @Override
        public io.opentelemetry.api.trace.Span startSpan() {
            return io.opentelemetry.api.trace.Span.getInvalid();
        }
    }
}
