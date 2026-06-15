package com.coinbase.latencytracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "latency_regressions", indexes = {
    @Index(name = "idx_regression_stage_created", columnList = "stage,created_at"),
    @Index(name = "idx_regression_detected_at", columnList = "detected_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatencyRegression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private LatencyRecord.Stage stage;

    @Column(name = "current_p95_ms")
    private double currentP95Ms;

    @Column(name = "current_p99_ms")
    private double currentP99Ms;

    @Column(name = "baseline_p95_ms")
    private double baselineP95Ms;

    @Column(name = "baseline_p99_ms")
    private double baselineP99Ms;

    @Column(name = "degradation_percent")
    private double degradationPercent;

    @Column(name = "comparison_window", length = 20)
    private String comparisonWindow;  // "1h", "24h", "7d"

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 10)
    private LatencyRecord.Severity severity;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
