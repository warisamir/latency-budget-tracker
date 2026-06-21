package com.coinbase.latencytracker.budget;

import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Base implementation that applies the standard deviation → severity mapping.
 *
 * Thresholds (deviation %):
 *   < 0      → OK       (under budget)
 *   0–24     → LOW      (slightly over)
 *   25–74    → MEDIUM
 *   75–149   → HIGH
 *   ≥ 150    → CRITICAL
 */
@Slf4j
public abstract class AbstractBudgetStrategy implements BudgetStrategy {

    @Override
    public BudgetResult evaluate(long actualMs) {
        Stage stage   = getStage();
        long  budget  = getBudgetMs();

        if (actualMs <= budget) {
            LoggerUtil.debug(log, "Budget OK - stage={} actual={}ms budget={}ms", stage, actualMs, budget);
            return BudgetResult.ok(stage, actualMs, budget);
        }

        double deviation = ((double)(actualMs - budget) / budget) * 100.0;
        Severity severity = classifySeverity(deviation);

        LoggerUtil.warn(log, "Budget exceeded - stage={} actual={}ms budget={}ms deviation={:.1f}% severity={}",
                stage, actualMs, budget, deviation, severity);

        return BudgetResult.exceeded(stage, actualMs, budget, deviation, severity);
    }

    private Severity classifySeverity(double deviationPercent) {
        if (deviationPercent < 25.0)  return Severity.LOW;
        if (deviationPercent < 75.0)  return Severity.MEDIUM;
        if (deviationPercent < 150.0) return Severity.HIGH;
        return Severity.CRITICAL;
    }
}
