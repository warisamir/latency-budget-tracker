package com.coinbase.latencytracker.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.MDC;
import java.util.UUID;

/**
 * Centralized logging utility for consistent contextual logging across the application.
 * Manages trace/span IDs and provides structured logging methods.
 */
@Slf4j
public class LoggerUtil {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";

    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    public static String generateSpanId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static void setTraceContext(String traceId, String spanId) {
        MDC.put(TRACE_ID, traceId);
        MDC.put(SPAN_ID, spanId);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    public static void clearTraceContext() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }

    // ──── Debug Logging ────
    public static void debug(Logger logger, String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] " + message, prepareArgs(args));
        }
    }

    // ──── Info Logging ────
    public static void info(Logger logger, String message, Object... args) {
        logger.info("[{}] " + message, prepareArgs(args));
    }

    // ──── Warning Logging ────
    public static void warn(Logger logger, String message, Object... args) {
        logger.warn("[{}] " + message, prepareArgs(args));
    }

    // ──── Error Logging ────
    public static void error(Logger logger, String message, Throwable e, Object... args) {
        logger.error("[{}] " + message, prepareArgs(args), e);
    }

    public static void error(Logger logger, String message, Object... args) {
        logger.error("[{}] " + message, prepareArgs(args));
    }

    // ──── Performance Logging ────
    public static void logPerformance(Logger logger, String operation, long durationMs) {
        if (durationMs > 100) {
            warn(logger, "{} took {}ms (slow operation)", operation, durationMs);
        } else {
            debug(logger, "{} completed in {}ms", operation, durationMs);
        }
    }

    // ──── Request/Response Logging ────
    public static void logRequest(Logger logger, String method, String path, String queryString) {
        String query = queryString != null && !queryString.isEmpty() ? "?" + queryString : "";
        info(logger, "Incoming {} {}{}", method, path, query);
    }

    public static void logResponse(Logger logger, String method, String path, int status, long durationMs) {
        String statusClass = status < 400 ? "success" : status < 500 ? "client error" : "server error";
        info(logger, "Response {} {} - {} ({}ms) [{}]", method, path, status, durationMs, statusClass);
    }

    // ──── Private Utility ────
    private static Object[] prepareArgs(Object... args) {
        Object[] result = new Object[args.length + 1];
        result[0] = getTraceId() != null ? getTraceId() : "N/A";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
