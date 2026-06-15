package com.coinbase.latencytracker.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiError {
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
    private Instant timestamp;
    private List<FieldError> fieldErrors;

    @Getter @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
