package com.coinbase.latencytracker.unit.budget;

import com.coinbase.latencytracker.budget.BudgetResult;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetResultTest {

    // ─── Happy paths ────────────────────────────────────────────────────────

    @Test
    void ok_whenActualWithinBudget() {
        BudgetResult result = BudgetResult.ok(Stage.AUTHENTICATION, 15L, 20L);

        assertThat(result.isExceeded()).isFalse();
        assertThat(result.isWithinBudget()).isTrue();
        assertThat(result.getSeverity()).isEqualTo(Severity.OK);
        assertThat(result.getDeviationPercent()).isEqualTo(0.0);
        assertThat(result.getActualMs()).isEqualTo(15L);
        assertThat(result.getBudgetMs()).isEqualTo(20L);
    }

    @Test
    void ok_whenActualEqualsExactBudget() {
        BudgetResult result = BudgetResult.ok(Stage.VALIDATION, 10L, 10L);

        assertThat(result.isExceeded()).isFalse();
        assertThat(result.getSeverity()).isEqualTo(Severity.OK);
    }

    @Test
    void exceeded_withCorrectFieldsSet() {
        BudgetResult result = BudgetResult.exceeded(Stage.DATABASE_QUERY, 60L, 40L, 50.0, Severity.MEDIUM);

        assertThat(result.isExceeded()).isTrue();
        assertThat(result.isWithinBudget()).isFalse();
        assertThat(result.getStage()).isEqualTo(Stage.DATABASE_QUERY);
        assertThat(result.getActualMs()).isEqualTo(60L);
        assertThat(result.getBudgetMs()).isEqualTo(40L);
        assertThat(result.getDeviationPercent()).isEqualTo(50.0);
        assertThat(result.getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    // ─── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void ok_whenActualIsZero() {
        BudgetResult result = BudgetResult.ok(Stage.VALIDATION, 0L, 10L);
        assertThat(result.isExceeded()).isFalse();
        assertThat(result.getActualMs()).isEqualTo(0L);
    }

    @ParameterizedTest(name = "deviation={1}% → severity={2}")
    @CsvSource({
        "21, 5.0,   LOW",
        "25, 25.0,  MEDIUM",
        "35, 75.0,  HIGH",
        "50, 150.0, CRITICAL"
    })
    void exceeded_severityMapping(long actual, double deviation, Severity expectedSeverity) {
        BudgetResult result = BudgetResult.exceeded(Stage.CACHE_ACCESS, actual, 20L, deviation, expectedSeverity);
        assertThat(result.getSeverity()).isEqualTo(expectedSeverity);
    }

    // ─── Equality / hash ────────────────────────────────────────────────────

    @Test
    void equalsAndHashCode_areValueBased() {
        BudgetResult r1 = BudgetResult.ok(Stage.AUTHENTICATION, 10L, 20L);
        BudgetResult r2 = BudgetResult.ok(Stage.AUTHENTICATION, 10L, 20L);
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }
}
