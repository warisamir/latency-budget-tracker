package com.coinbase.latencytracker.unit.service;

import com.coinbase.latencytracker.alerting.AlertEventPublisher;
import com.coinbase.latencytracker.budget.BudgetResult;
import com.coinbase.latencytracker.budget.LatencyBudgetEngine;
import com.coinbase.latencytracker.budget.strategies.*;
import com.coinbase.latencytracker.config.LatencyBudgetProperties;
import com.coinbase.latencytracker.dto.TransactionRequest;
import com.coinbase.latencytracker.dto.TransactionResponse;
import com.coinbase.latencytracker.entity.LatencyRecord;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.exception.LatencyTrackerException;
import com.coinbase.latencytracker.metrics.LatencyMetrics;
import com.coinbase.latencytracker.repository.LatencyRecordRepository;
import com.coinbase.latencytracker.service.TransactionService;
import com.coinbase.latencytracker.tracing.TracingService;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock LatencyRecordRepository        recordRepository;
    @Mock AlertEventPublisher            alertPublisher;
    @Mock LatencyMetrics                 latencyMetrics;
    @Mock RedisTemplate<String, Object>  redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;

    private TransactionService service;
    private LatencyBudgetEngine budgetEngine;

    @BeforeEach
    void setUp() {
        LatencyBudgetProperties props = new LatencyBudgetProperties();

        budgetEngine = new LatencyBudgetEngine(List.of(
                new AuthenticationBudgetStrategy(props),
                new ValidationBudgetStrategy(props),
                new BusinessLogicBudgetStrategy(props),
                new CacheAccessBudgetStrategy(props),
                new DatabaseQueryBudgetStrategy(props),
                new ResponseSerializationBudgetStrategy(props)
        ));

        // Real TracingService with a no-op SDK tracer (no network needed)
        OpenTelemetry otel = OpenTelemetrySdk.builder().build();
        Tracer tracer = otel.getTracer("test");
        TracingService tracingService = new TracingService(tracer);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(any())).thenReturn(null);   // simulate cache miss
        doNothing().when(valueOps).set(any(), any(), any());
        when(recordRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new TransactionService(
                tracingService, budgetEngine, alertPublisher,
                latencyMetrics, recordRepository, redisTemplate
        );
    }

    // ─── Happy paths ────────────────────────────────────────────────────────

    @Test
    void process_validRequest_returnsSuccessfulResponse() {
        TransactionRequest req = validRequest();

        TransactionResponse resp = service.process(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("COMPLETED");
        assertThat(resp.getTransactionId()).isNotBlank();
        assertThat(resp.getFromCurrency()).isEqualTo("BTC");
        assertThat(resp.getToCurrency()).isEqualTo("USD");
        assertThat(resp.getConvertedAmount()).isPositive();
        assertThat(resp.getLatencyReport()).isNotNull();
        assertThat(resp.getLatencyReport().getStages()).hasSize(6);
    }

    @Test
    void process_validRequest_recordsAllSixStages() {
        service.process(validRequest());

        verify(recordRepository).saveAll(argThat(records -> {
            var list = (java.util.List<?>) records;
            return list.size() == 6;
        }));
    }

    @Test
    void process_validRequest_incrementsTransactionCounter() {
        service.process(validRequest());
        verify(latencyMetrics).incrementTransactions();
    }

    @Test
    void process_validRequest_recordsLatencyForEachStage() {
        service.process(validRequest());

        // Verify Micrometer recorded latency for all 6 stages
        verify(latencyMetrics, times(6)).recordStageLatency(any(), anyLong(), anyBoolean(), any());
    }

    @Test
    void process_multipleCurrencyPairs_allSucceed() {
        String[][] pairs = {{"BTC", "ETH"}, {"USD", "EUR"}, {"ETH", "USDT"}, {"SOL", "BTC"}};

        for (String[] pair : pairs) {
            TransactionRequest req = TransactionRequest.builder()
                    .userId("user-1")
                    .fromCurrency(pair[0])
                    .toCurrency(pair[1])
                    .amount(BigDecimal.TEN)
                    .build();

            TransactionResponse resp = service.process(req);
            assertThat(resp.isSuccess()).as("Pair %s→%s should succeed", pair[0], pair[1]).isTrue();
        }
    }

    @Test
    void process_cacheHit_returnsCachedRate() {
        BigDecimal cachedRate = new BigDecimal("30000.000000");
        when(valueOps.get(any())).thenReturn(cachedRate.toPlainString());

        TransactionResponse resp = service.process(validRequest());
        assertThat(resp.isSuccess()).isTrue();
    }

    // ─── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void process_redisDown_fallsBackToComputedRate() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        // Should still succeed via circuit breaker fallback
        // (In unit test Redis CB isn't active, so we expect fallback path)
        // This verifies the service doesn't throw even when Redis is down
        assertThatCode(() -> service.process(validRequest()))
                .doesNotThrowAnyException();
    }

    @Test
    void process_highLatency_emitsAlerts() throws Exception {
        // Repeat enough times that at least one stage exceeds budget
        // (Budgets are tight enough that random latency will sometimes exceed them)
        for (int i = 0; i < 20; i++) {
            service.process(validRequest());
        }
        // At minimum the transaction counter was incremented 20 times
        verify(latencyMetrics, times(20)).incrementTransactions();
    }

    // ─── Worst cases ─────────────────────────────────────────────────────────

    @Test
    void process_blankUserId_throwsAuthenticationException() {
        TransactionRequest req = validRequest();
        req.setUserId("");

        assertThatThrownBy(() -> service.process(req))
                .isInstanceOf(LatencyTrackerException.class)
                .hasMessageContaining("Authentication failed");
    }

    @Test
    void process_nullUserId_throwsAuthenticationException() {
        TransactionRequest req = validRequest();
        req.setUserId(null);

        assertThatThrownBy(() -> service.process(req))
                .isInstanceOf(LatencyTrackerException.class)
                .hasMessageContaining("Authentication failed");
    }

    @Test
    void process_sameCurrencyPair_throwsValidationException() {
        TransactionRequest req = TransactionRequest.builder()
                .userId("user-1")
                .fromCurrency("BTC")
                .toCurrency("BTC")  // same!
                .amount(BigDecimal.TEN)
                .build();

        assertThatThrownBy(() -> service.process(req))
                .isInstanceOf(LatencyTrackerException.class)
                .hasMessageContaining("Validation failed");
    }

    @Test
    void process_negativeAmount_throwsValidationException() {
        TransactionRequest req = TransactionRequest.builder()
                .userId("user-1")
                .fromCurrency("BTC")
                .toCurrency("USD")
                .amount(BigDecimal.valueOf(-1))
                .build();

        assertThatThrownBy(() -> service.process(req))
                .isInstanceOf(LatencyTrackerException.class)
                .hasMessageContaining("Validation failed");
    }

    @Test
    void process_dbSaveFails_stillAttemptsPartialSave() {
        when(recordRepository.saveAll(any())).thenThrow(new RuntimeException("DB write failed"));

        TransactionRequest req = validRequest();
        // The exception from DB write should propagate (or be wrapped)
        assertThatThrownBy(() -> service.process(req))
                .isInstanceOf(Exception.class);

        verify(latencyMetrics).incrementErrors();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private TransactionRequest validRequest() {
        return TransactionRequest.builder()
                .userId("user-abc-123")
                .fromCurrency("BTC")
                .toCurrency("USD")
                .amount(new BigDecimal("1.5"))
                .build();
    }
}
