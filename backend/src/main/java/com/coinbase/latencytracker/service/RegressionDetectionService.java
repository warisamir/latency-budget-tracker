package com.coinbase.latencytracker.service;

import com.coinbase.latencytracker.config.LatencyBudgetProperties;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.entity.LatencyRegression;
import com.coinbase.latencytracker.repository.LatencyRecordRepository;
import com.coinbase.latencytracker.repository.LatencyRegressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Latency Regression Detection Service.
 *
 * Runs on a configurable schedule. For each stage, compares the current P95/P99
 * against baselines from 1h, 24h, and 7d ago. If degradation exceeds configured
 * thresholds, a LatencyRegression record is persisted and logged as a warning.
 *
 * Degradation thresholds → severity:
 *   < 5%   → no regression
 *   5–9%   → LOW
 *   10–19% → MEDIUM
 *   20–49% → HIGH
 *   ≥ 50%  → CRITICAL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegressionDetectionService {

    private static final String[] WINDOWS = {"1h", "24h", "7d"};

    private final LatencyRecordRepository    recordRepository;
    private final LatencyRegressionRepository regressionRepository;
    private final LatencyBudgetProperties    props;

    @Scheduled(fixedDelayString = "${latency.regression.check-interval-ms:300000}",
               initialDelay = 60_000)
    @Transactional
    public void detect() {
        log.info("Running latency regression detection...");
        Instant now = Instant.now();
        List<LatencyRegression> detected = new ArrayList<>();

        for (Stage stage : Stage.values()) {
            // Current window: last 30 minutes
            double currentP95 = recordRepository
                    .findP95ByStageAndCreatedAtAfter(stage.name(), now.minus(30, ChronoUnit.MINUTES))
                    .orElse(0.0);
            double currentP99 = recordRepository
                    .findP99ByStageAndCreatedAtAfter(stage.name(), now.minus(30, ChronoUnit.MINUTES))
                    .orElse(0.0);

            if (currentP95 == 0.0) continue; // no recent data

            for (String window : WINDOWS) {
                Instant since    = windowStart(window, now);
                Instant until    = windowEnd(window, now);
                double baseP95   = recordRepository
                        .findP95ByStageAndCreatedAtAfter(stage.name(), since)
                        .orElse(0.0);
                double baseP99   = recordRepository
                        .findP99ByStageAndCreatedAtAfter(stage.name(), since)
                        .orElse(0.0);

                if (baseP95 == 0.0) continue;

                double degradation = ((currentP95 - baseP95) / baseP95) * 100.0;
                Severity severity  = classifySeverity(degradation);

                if (severity != null) {
                    log.warn("Regression detected — stage={} window={} currentP95={}ms baseP95={}ms degradation={}% severity={}",
                            stage, window,
                            String.format("%.1f", currentP95),
                            String.format("%.1f", baseP95),
                            String.format("%.1f", degradation),
                            severity);

                    detected.add(LatencyRegression.builder()
                            .stage(stage)
                            .currentP95Ms(currentP95)
                            .currentP99Ms(currentP99)
                            .baselineP95Ms(baseP95)
                            .baselineP99Ms(baseP99)
                            .degradationPercent(degradation)
                            .comparisonWindow(window)
                            .severity(severity)
                            .detectedAt(now)
                            .build());
                }
            }
        }

        if (!detected.isEmpty()) {
            regressionRepository.saveAll(detected);
            log.warn("Saved {} regression record(s)", detected.size());
        } else {
            log.info("No regressions detected");
        }
    }

    private Severity classifySeverity(double degradation) {
        var t = props.getRegression().getThresholds();
        if (degradation < t.getLow())      return null;   // no regression
        if (degradation < t.getMedium())   return Severity.LOW;
        if (degradation < t.getHigh())     return Severity.MEDIUM;
        if (degradation < t.getCritical()) return Severity.HIGH;
        return Severity.CRITICAL;
    }

    private Instant windowStart(String window, Instant now) {
        return switch (window) {
            case "1h"  -> now.minus(2,  ChronoUnit.HOURS);
            case "24h" -> now.minus(25, ChronoUnit.HOURS);
            case "7d"  -> now.minus(8,  ChronoUnit.DAYS);
            default    -> now.minus(2,  ChronoUnit.HOURS);
        };
    }

    private Instant windowEnd(String window, Instant now) {
        return switch (window) {
            case "1h"  -> now.minus(1,  ChronoUnit.HOURS);
            case "24h" -> now.minus(24, ChronoUnit.HOURS);
            case "7d"  -> now.minus(7,  ChronoUnit.DAYS);
            default    -> now.minus(1,  ChronoUnit.HOURS);
        };
    }
}
