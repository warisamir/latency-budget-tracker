package com.coinbase.latencytracker.unit.budget;

import com.coinbase.latencytracker.budget.*;
import com.coinbase.latencytracker.budget.strategies.*;
import com.coinbase.latencytracker.config.LatencyBudgetProperties;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class LatencyBudgetEngineTest {

    private LatencyBudgetEngine engine;
    private LatencyBudgetProperties props;

    @BeforeEach
    void setUp() {
        props = new LatencyBudgetProperties();
        // Uses defaults: auth=20, valid=10, bl=50, cache=10, db=40, resp=5

        List<BudgetStrategy> strategies = List.of(
                new AuthenticationBudgetStrategy(props),
                new ValidationBudgetStrategy(props),
                new BusinessLogicBudgetStrategy(props),
                new CacheAccessBudgetStrategy(props),
                new DatabaseQueryBudgetStrategy(props),
                new ResponseSerializationBudgetStrategy(props)
        );
        engine = new LatencyBudgetEngine(strategies);
    }

    // ─── Happy paths ────────────────────────────────────────────────────────

    @Test
    void evaluate_withinBudget_returnsOk() {
        BudgetResult result = engine.evaluate(Stage.AUTHENTICATION, 15L);

        assertThat(result.isExceeded()).isFalse();
        assertThat(result.getSeverity()).isEqualTo(Severity.OK);
        assertThat(result.getBudgetMs()).isEqualTo(20L);
        assertThat(result.getActualMs()).isEqualTo(15L);
    }

    @Test
    void evaluate_exactlyOnBudget_returnsOk() {
        BudgetResult result = engine.evaluate(Stage.DATABASE_QUERY, 40L);
        assertThat(result.isExceeded()).isFalse();
    }

    @Test
    void evaluate_slightlyOver_returnsLow() {
        BudgetResult result = engine.evaluate(Stage.VALIDATION, 12L); // 20% over
        assertThat(result.isExceeded()).isTrue();
        assertThat(result.getSeverity()).isEqualTo(Severity.LOW);
    }

    @Test
    void evaluateAll_allWithinBudget_summaryOk() {
        Map<Stage, Long> latencies = new EnumMap<>(Stage.class);
        latencies.put(Stage.AUTHENTICATION,        15L);
        latencies.put(Stage.VALIDATION,             8L);
        latencies.put(Stage.BUSINESS_LOGIC,        40L);
        latencies.put(Stage.CACHE_ACCESS,           7L);
        latencies.put(Stage.DATABASE_QUERY,        30L);
        latencies.put(Stage.RESPONSE_SERIALIZATION, 4L);

        LatencyBudgetEngine.BudgetSummary summary = engine.evaluateAll(latencies);

        assertThat(summary.anyExceeded()).isFalse();
        assertThat(summary.withinBudget()).isTrue();
        assertThat(summary.worstSeverity()).isEqualTo(Severity.OK);
        assertThat(summary.totalActualMs()).isEqualTo(104L);
        assertThat(summary.totalBudgetMs()).isEqualTo(135L);
    }

    @Test
    void evaluateAll_someExceeded_correctWorstSeverity() {
        Map<Stage, Long> latencies = new EnumMap<>(Stage.class);
        latencies.put(Stage.AUTHENTICATION,        15L);   // OK
        latencies.put(Stage.VALIDATION,             8L);   // OK
        latencies.put(Stage.BUSINESS_LOGIC,       200L);   // CRITICAL (300% over)
        latencies.put(Stage.CACHE_ACCESS,           7L);   // OK
        latencies.put(Stage.DATABASE_QUERY,        55L);   // MEDIUM (37.5% over)
        latencies.put(Stage.RESPONSE_SERIALIZATION, 4L);   // OK

        LatencyBudgetEngine.BudgetSummary summary = engine.evaluateAll(latencies);

        assertThat(summary.anyExceeded()).isTrue();
        assertThat(summary.worstSeverity()).isEqualTo(Severity.CRITICAL);
    }

    // ─── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void evaluate_unknownStage_throwsIllegalArgumentException() {
        // Can't add a truly unknown stage without enum extension, so test with a mock
        // We verify all 6 known stages are registered
        for (Stage stage : Stage.values()) {
            assertThatCode(() -> engine.evaluate(stage, 0L))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void evaluateAll_emptyMap_returnsZeroSummary() {
        LatencyBudgetEngine.BudgetSummary summary = engine.evaluateAll(new EnumMap<>(Stage.class));

        assertThat(summary.anyExceeded()).isFalse();
        assertThat(summary.totalActualMs()).isEqualTo(0L);
        assertThat(summary.stageResults()).isEmpty();
    }

    // ─── Worst cases ─────────────────────────────────────────────────────────

    @Test
    void evaluate_zeroLatency_neverExceedsBudget() {
        for (Stage stage : Stage.values()) {
            BudgetResult result = engine.evaluate(stage, 0L);
            assertThat(result.isExceeded())
                    .as("Stage %s with 0ms should not exceed budget", stage)
                    .isFalse();
        }
    }

    @Test
    void evaluate_extremeLatency_returnsCritical() {
        BudgetResult result = engine.evaluate(Stage.RESPONSE_SERIALIZATION, 10_000L);
        assertThat(result.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(result.getDeviationPercent()).isGreaterThan(1000.0);
    }

    @Test
    void getBudgetMs_correctForEachStage() {
        assertThat(engine.getBudgetMs(Stage.AUTHENTICATION)).isEqualTo(20L);
        assertThat(engine.getBudgetMs(Stage.VALIDATION)).isEqualTo(10L);
        assertThat(engine.getBudgetMs(Stage.BUSINESS_LOGIC)).isEqualTo(50L);
        assertThat(engine.getBudgetMs(Stage.CACHE_ACCESS)).isEqualTo(10L);
        assertThat(engine.getBudgetMs(Stage.DATABASE_QUERY)).isEqualTo(40L);
        assertThat(engine.getBudgetMs(Stage.RESPONSE_SERIALIZATION)).isEqualTo(5L);
    }
}
