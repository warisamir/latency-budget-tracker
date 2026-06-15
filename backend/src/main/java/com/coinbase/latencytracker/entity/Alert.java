package com.coinbase.latencytracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_stage_severity", columnList = "stage,severity"),
    @Index(name = "idx_alert_created_at", columnList = "created_at"),
    @Index(name = "idx_alert_resolved", columnList = "resolved"),
    @Index(name = "idx_alert_transaction_id", columnList = "transaction_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private LatencyRecord.Stage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private LatencyRecord.Severity severity;

    @Column(name = "actual_latency_ms", nullable = false)
    private long actualLatencyMs;

    @Column(name = "budget_ms", nullable = false)
    private long budgetMs;

    @Column(name = "deviation_percent", nullable = false)
    private double deviationPercent;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "notification_sent", nullable = false)
    private boolean notificationSent;

    @Column(name = "notification_channel", length = 50)
    private String notificationChannel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
