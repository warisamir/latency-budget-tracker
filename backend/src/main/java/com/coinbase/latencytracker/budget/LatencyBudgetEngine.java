package com.coinbase.latencytracker.budget;

import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates budget evaluation across all stages using the Strategy Pattern.
 *
 * Each stage has a registered BudgetStrategy. The engine:
 *   1. Looks up the strategy for the stage.
 *   2. Calls evaluate(actualMs) to produce a BudgetResult.
 *   3. Returns the result for persistence and alerting.
 */
@Service
@Slf4j
public class LatencyBudgetEngine {

    private final Map<Stage, BudgetStrategy> strategies;

    /**
     * Spring auto-collects all BudgetStrategy beans; we index them by stage.
     */
    public LatencyBudgetEngine(List<BudgetStrategy> strategyList) {
        strategies = new EnumMap<>(Stage.class);
        for (BudgetStrategy s : strategyList) {
            strategies.put(s.getStage(), s);
            log.info("Registered budget strategy: {} → {}ms", s.getStage(), s.getBudgetMs());
        }
    }

    /**
     * Evaluate a single stage against its budget.
     *
     * @param stage    the pipeline stage
     * @param actualMs measured latency in milliseconds
     * @return BudgetResult with deviation and severity
     * @throws IllegalArgumentException if stage has no registered strategy
     */
    public BudgetResult evaluate(Stage stage, long actualMs) {
        BudgetStrategy strategy = strategies.get(stage);
        if (strategy == null) {
            throw new IllegalArgumentException("No budget strategy registered for stage: " + stage);
        }
        BudgetResult result = strategy.evaluate(actualMs);
        if (result.isExceeded()) {
            log.warn("Budget exceeded — stage={} actual={}ms budget={}ms deviation={}% severity={}",
                    stage, actualMs, result.getBudgetMs(),
                    String.format("%.1f", result.getDeviationPercent()), result.getSeverity());
        }
        return result;
    }

    /**
     * Evaluate all stages at once and return aggregate summary.
     */
    public BudgetSummary evaluateAll(Map<Stage, Long> actualLatencies) {
        Map<Stage, BudgetResult> results = new EnumMap<>(Stage.class);
        boolean anyExceeded = false;
        Severity worst = Severity.OK;

        for (Map.Entry<Stage, Long> entry : actualLatencies.entrySet()) {
            BudgetResult r = evaluate(entry.getKey(), entry.getValue());
            results.put(entry.getKey(), r);
            if (r.isExceeded()) {
                anyExceeded = true;
                if (r.getSeverity().ordinal() > worst.ordinal()) {
                    worst = r.getSeverity();
                }
            }
        }

        long totalActual = actualLatencies.values().stream().mapToLong(Long::longValue).sum();
        long totalBudget = strategies.values().stream().mapToLong(BudgetStrategy::getBudgetMs).sum();
        double overallDeviation = totalActual > totalBudget
                ? ((double)(totalActual - totalBudget) / totalBudget) * 100.0
                : 0.0;

        return new BudgetSummary(results, totalActual, totalBudget, overallDeviation, anyExceeded, worst);
    }

    public long getBudgetMs(Stage stage) {
        BudgetStrategy s = strategies.get(stage);
        return s != null ? s.getBudgetMs() : 0L;
    }

    // -------------------------------------------------------------------------

    public record BudgetSummary(
            Map<Stage, BudgetResult> stageResults,
            long totalActualMs,
            long totalBudgetMs,
            double overallDeviationPercent,
            boolean anyExceeded,
            Severity worstSeverity
    ) {
        public boolean withinBudget() { return !anyExceeded; }
    }
}
