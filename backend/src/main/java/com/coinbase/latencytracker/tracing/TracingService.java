package com.coinbase.latencytracker.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Central tracing facade.
 *
 * Wraps any callable in an OTel span, records latency,
 * propagates errors, and captures structured attributes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TracingService {

    private static final AttributeKey<String>  ATTR_TRANSACTION_ID = AttributeKey.stringKey("transaction.id");
    private static final AttributeKey<String>  ATTR_USER_ID        = AttributeKey.stringKey("user.id");
    private static final AttributeKey<Long>    ATTR_LATENCY_MS     = AttributeKey.longKey("latency.ms");
    private static final AttributeKey<Long>    ATTR_BUDGET_MS      = AttributeKey.longKey("budget.ms");
    private static final AttributeKey<Boolean> ATTR_EXCEEDED       = AttributeKey.booleanKey("budget.exceeded");
    private static final AttributeKey<Double>  ATTR_DEVIATION      = AttributeKey.doubleKey("budget.deviation_percent");
    private static final AttributeKey<String>  ATTR_SEVERITY       = AttributeKey.stringKey("budget.severity");
    private static final AttributeKey<String>  ATTR_ERROR          = AttributeKey.stringKey("error.message");

    private final Tracer tracer;

    /**
     * Record a traced stage that returns a result.
     *
     * @param spanName      OTel span name (e.g. "latency.authentication")
     * @param transactionId the parent transaction ID
     * @param userId        the authenticated user ID (may be null during auth stage)
     * @param callable      the work to execute inside the span
     * @return TimedResult containing the result and measured latency
     */
    public <T> TimedResult<T> traceStage(String spanName,
                                         String transactionId,
                                         String userId,
                                         Callable<T> callable) {
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(Context.current())
                .startSpan();

        span.setAttribute(ATTR_TRANSACTION_ID, transactionId);
        if (userId != null) {
            span.setAttribute(ATTR_USER_ID, userId);
        }

        long start = System.currentTimeMillis();
        try (Scope ignored = span.makeCurrent()) {
            T result = callable.call();
            long latencyMs = System.currentTimeMillis() - start;

            span.setAttribute(ATTR_LATENCY_MS, latencyMs);
            span.setStatus(StatusCode.OK);

            return TimedResult.success(result, latencyMs, extractTraceId(span), extractSpanId(span));
        } catch (Exception ex) {
            long latencyMs = System.currentTimeMillis() - start;
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.setAttribute(ATTR_ERROR, ex.getMessage());
            span.setAttribute(ATTR_LATENCY_MS, latencyMs);

            log.error("Span [{}] failed after {}ms: {}", spanName, latencyMs, ex.getMessage());
            return TimedResult.failure(ex, latencyMs, extractTraceId(span), extractSpanId(span));
        } finally {
            span.end();
        }
    }

    /**
     * Enrich a span with budget evaluation result.
     * Call AFTER traceStage to annotate the span with budget metadata.
     */
    public void annotateBudget(String spanName,
                               long actualMs,
                               long budgetMs,
                               double deviationPercent,
                               String severity,
                               boolean exceeded) {
        Span current = Span.current();
        if (current.isRecording()) {
            current.setAttribute(ATTR_BUDGET_MS, budgetMs);
            current.setAttribute(ATTR_EXCEEDED, exceeded);
            current.setAttribute(ATTR_DEVIATION, deviationPercent);
            current.setAttribute(ATTR_SEVERITY, severity);

            if (exceeded) {
                current.addEvent("budget_exceeded", Attributes.of(
                        ATTR_LATENCY_MS, actualMs,
                        ATTR_BUDGET_MS, budgetMs
                ));
            }
        }
    }

    /** Returns the current trace ID from the active span (hex string or empty). */
    public String currentTraceId() {
        Span span = Span.current();
        return span.getSpanContext().isValid()
                ? span.getSpanContext().getTraceId()
                : "";
    }

    private String extractTraceId(Span span) {
        return span.getSpanContext().isValid() ? span.getSpanContext().getTraceId() : "";
    }

    private String extractSpanId(Span span) {
        return span.getSpanContext().isValid() ? span.getSpanContext().getSpanId() : "";
    }

    // -------------------------------------------------------------------------
    // Result container
    // -------------------------------------------------------------------------

    public record TimedResult<T>(
            T value,
            long latencyMs,
            String traceId,
            String spanId,
            boolean success,
            Exception error
    ) {
        public static <T> TimedResult<T> success(T value, long latencyMs, String traceId, String spanId) {
            return new TimedResult<>(value, latencyMs, traceId, spanId, true, null);
        }

        public static <T> TimedResult<T> failure(Exception ex, long latencyMs, String traceId, String spanId) {
            return new TimedResult<>(null, latencyMs, traceId, spanId, false, ex);
        }

        public boolean failed() { return !success; }
    }
}
