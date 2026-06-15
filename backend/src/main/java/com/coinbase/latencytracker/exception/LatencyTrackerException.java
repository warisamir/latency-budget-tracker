package com.coinbase.latencytracker.exception;

public class LatencyTrackerException extends RuntimeException {
    private final int statusCode;

    public LatencyTrackerException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public LatencyTrackerException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() { return statusCode; }
}
