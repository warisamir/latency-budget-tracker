package com.coinbase.latencytracker.dto;

import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertDto {
    private Long id;
    private String transactionId;
    private String traceId;
    private Stage stage;
    private Severity severity;
    private long actualLatencyMs;
    private long budgetMs;
    private double deviationPercent;
    private String message;
    private boolean resolved;
    private Instant resolvedAt;
    private boolean notificationSent;
    private Instant createdAt;
}
