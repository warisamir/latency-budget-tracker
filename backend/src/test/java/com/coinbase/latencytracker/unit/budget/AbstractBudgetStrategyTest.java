package com.coinbase.latencytracker.unit.budget;

import com.coinbase.latencytracker.budget.AbstractBudgetStrategy;
import com.coinbase.latencytracker.budget.BudgetResult;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AbstractBudgetStrategy severity classification logic.
 *
 * Budget = 40ms (DATABASE_QUERY). Deviation thresholds:
 *   < 0       → OK
 *   0–24.9%   → LOW
 *   25–74.9%  → MEDIUM
 *   75–149.9% → HIGH
 *   ≥ 150%    → CRITICAL
 */
class AbstractBudgetStrategyTest {

    // Anonymous concrete strategy for testing
    private AbstractBudgetStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AbstractBudgetStrategy() {
            @Override public Stage getStage()    { return Stage.DATABASE_QUERY; }
            @Override public long  getBudgetMs() { return 40L; }
        };
    }

    // ─── Happy paths ────────────────────────────────────────────────────────

    @Test
    void evaluate_withinBudget_returnsOk() {
        BudgetResult result = strategy.evaluate(30L);
        assertThat(result.isExceeded()).isFalse();
        assertThat(result.getSeverity()).isEqualTo(Severity.OK);
        assertThat(result.getDeviationPercent()).isEqualTo(0.0);
    }

    @Test
    void evaluate_exactlyOnBudget_returnsOk() {
        BudgetResult result = strategy.evaluate(40L);
        assertThat(result.isExceeded()).isFalse();
    }

    @Test
    void evaluate_oneMillisecondOver_returnsLow() {
        BudgetResult result = strategy.evaluate(41L);
        assertThat(result.isExceeded()).isTrue();
        assertThat(result.getSeverity()).isEqualTo(Severity.LOW);
    }

    // ─── Severity boundary tests ─────────────────────────────────────────────

    @ParameterizedTest(name = "actual={0}ms → severity={1}")
    @CsvSource({
        "40,  OK",       // exact budget → OK
        "49,  LOW",      // 22.5% over
        "50,  LOW",      // 24.9% over (just under MEDIUM threshold)
        "51,  MEDIUM",   // 27.5% over
        "70,  MEDIUM",   // 75% - just under HIGH
        "71,  HIGH",     // 77.5% over
        "100, HIGH",     // 150% - just under CRITICAL
        "101, CRITICAL"  // 152.5% over
    })
    void evaluate_severityBoundaries(long actualMs, Severity expectedSeverity) {
        BudgetResult result = strategy.evaluate(actualMs);
        if (expectedSeverity == Severity.OK) {
            assertThat(result.getSeverity()).isEqualTo(Severity.OK);
        } else {
            assertThat(result.getSeverity()).isEqualTo(expectedSeverity);
        }
    }

    // ─── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void evaluate_zeroMs_returnsOk() {
        BudgetResult result = strategy.evaluate(0L);
        assertThat(result.isExceeded()).isFalse();
        assertThat(result.getActualMs()).isEqualTo(0L);
    }

    @Test
    void evaluate_massiveLatency_returnsCritical() {
        BudgetResult result = strategy.evaluate(10_000L); // 10 seconds
        assertThat(result.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(result.getDeviationPercent()).isGreaterThan(150.0);
    }

    // ─── Worst case ──────────────────────────────────────────────────────────

    @Test
    void evaluate_maxLongValue_doesNotOverflow() {
        // Worst case: Long.MAX_VALUE latency. Should not throw ArithmeticException.
        BudgetResult result = strategy.evaluate(Long.MAX_VALUE / 2);
        assertThat(result.isExceeded()).isTrue();
        assertThat(result.getSeverity()).isEqualTo(Severity.CRITICAL);
    }
}
