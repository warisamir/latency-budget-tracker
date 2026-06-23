package com.coinbase.latencytracker.metrics;

import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Central Micrometer metrics registry for latency tracking.
 *
 * Metrics published:
 *   latency.stage.duration         — Timer per stage
 *   latency.budget.violations      — Counter per stage + severity
 *   latency.budget.deviation       — Gauge of last deviation %
 *   latency.transactions.total     — Counter
 *   latency.transactions.errors    — Counter
 */
@Component
@Slf4j
public class LatencyMetrics {

    private static final String METRIC_STAGE_DURATION  = "latency.stage.duration";
    private static final String METRIC_VIOLATIONS      = "latency.budget.violations";
    private static final String METRIC_TRANSACTIONS    = "latency.transactions.total";
    private static final String METRIC_ERRORS          = "latency.transactions.errors";

    private final MeterRegistry registry;
    private final Counter        transactionCounter;
    private final Counter        errorCounter;

    // Last known deviation per stage for gauge exposure
    private final Map<Stage, Double> lastDeviation = new ConcurrentHashMap<>();

    public LatencyMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.transactionCounter = Counter.builder(METRIC_TRANSACTIONS)
                .description("Total transactions processed")
                .register(registry);
        this.errorCounter = Counter.builder(METRIC_ERRORS)
                .description("Total transaction errors")
                .register(registry);

        // Register deviation gauges for each stage
        for (Stage stage : Stage.values()) {
            lastDeviation.put(stage, 0.0);
            Gauge.builder("latency.budget.deviation", lastDeviation,
                          m -> m.getOrDefault(stage, 0.0))
                 .description("Last budget deviation % for stage")
                 .tag("stage", stage.name())
                 .register(registry);
        }
    }

    public void recordStageLatency(Stage stage, long latencyMs, boolean exceeded, Severity severity) {
        // Timer with Prometheus histogram percentiles (P50, P95, P99)
        // These are exported to Prometheus and queryable via histogram_quantile()
        Timer.builder(METRIC_STAGE_DURATION)
             .description("Latency per pipeline stage (with P50/P95/P99 percentiles)")
             .tag("stage", stage.name())
             .tag("exceeded", String.valueOf(exceeded))
             .publishPercentiles(0.5, 0.95, 0.99)  // ← Prometheus histograms
             .publishPercentileHistogram()         // ← SLA boundaries
             .minimumExpectedValue(1, TimeUnit.MILLISECONDS)
             .maximumExpectedValue(1000, TimeUnit.MILLISECONDS)
             .register(registry)
             .record(latencyMs, TimeUnit.MILLISECONDS);

        // Violation counter
        if (exceeded) {
            Counter.builder(METRIC_VIOLATIONS)
                   .description("Budget violations per stage")
                   .tag("stage", stage.name())
                   .tag("severity", severity.name())
                   .register(registry)
                   .increment();
        }
    }

    public void updateDeviation(Stage stage, double deviationPercent) {
        lastDeviation.put(stage, deviationPercent);
    }

    public void incrementTransactions() {
        transactionCounter.increment();
    }

    public void incrementErrors() {
        errorCounter.increment();
    }
}
