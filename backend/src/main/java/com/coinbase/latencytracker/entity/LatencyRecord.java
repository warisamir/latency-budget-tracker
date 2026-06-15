package com.coinbase.latencytracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "latency_records", indexes = {
    @Index(name = "idx_lr_created_at", columnList = "created_at"),
    @Index(name = "idx_lr_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_lr_stage", columnList = "stage"),
    @Index(name = "idx_lr_created_stage", columnList = "created_at,stage")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "span_id", length = 32)
    private String spanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private Stage stage;

    /** Actual latency in milliseconds */
    @Column(name = "actual_latency_ms", nullable = false)
    private long actualLatencyMs;

    /** Budget for this stage in milliseconds */
    @Column(name = "budget_ms", nullable = false)
    private long budgetMs;

    /** (actual - budget) / budget * 100 */
    @Column(name = "deviation_percent")
    private double deviationPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 10)
    private Severity severity;

    @Column(name = "budget_exceeded", nullable = false)
    private boolean budgetExceeded;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "success", nullable = false)
    private boolean success;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum Stage {
        AUTHENTICATION,
        VALIDATION,
        BUSINESS_LOGIC,
        CACHE_ACCESS,
        DATABASE_QUERY,
        RESPONSE_SERIALIZATION
    }

    public enum Severity {
        OK, LOW, MEDIUM, HIGH, CRITICAL
    }
}
