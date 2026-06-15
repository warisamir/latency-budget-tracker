package com.coinbase.latencytracker.budget;

import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import lombok.*;

/**
 * Immutable result of evaluating a single stage against its latency budget.
 */
@Getter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public final class BudgetResult {

    private final Stage    stage;
    private final long     actualMs;
    private final long     budgetMs;
    private final double   deviationPercent;
    private final Severity severity;
    private final boolean  exceeded;

    public static BudgetResult ok(Stage stage, long actualMs, long budgetMs) {
        return new BudgetResult(stage, actualMs, budgetMs, 0.0, Severity.OK, false);
    }

    public static BudgetResult exceeded(Stage stage, long actualMs, long budgetMs,
                                        double deviationPercent, Severity severity) {
        return new BudgetResult(stage, actualMs, budgetMs, deviationPercent, severity, true);
    }

    public boolean isWithinBudget() { return !exceeded; }
}
