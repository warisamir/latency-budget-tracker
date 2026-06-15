package com.coinbase.latencytracker.dto;

import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionResponse {

    private String transactionId;
    private String traceId;
    private boolean success;
    private String status;

    // Result
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;

    // Latency report
    private LatencyReport latencyReport;

    private Instant timestamp;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LatencyReport {
        private long totalLatencyMs;
        private long budgetMs;
        private boolean withinBudget;
        private double overallDeviationPercent;
        private Severity worstSeverity;
        private List<StageLatency> stages;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StageLatency {
        private String stage;
        private long actualMs;
        private long budgetMs;
        private boolean exceeded;
        private double deviationPercent;
        private Severity severity;
    }
}
