package com.coinbase.latencytracker.service;

import com.coinbase.latencytracker.alerting.AlertEventPublisher;
import com.coinbase.latencytracker.budget.BudgetResult;
import com.coinbase.latencytracker.budget.LatencyBudgetEngine;
import com.coinbase.latencytracker.budget.LatencyBudgetEngine.BudgetSummary;
import com.coinbase.latencytracker.dto.TransactionRequest;
import com.coinbase.latencytracker.dto.TransactionResponse;
import com.coinbase.latencytracker.dto.TransactionResponse.LatencyReport;
import com.coinbase.latencytracker.dto.TransactionResponse.StageLatency;
import com.coinbase.latencytracker.entity.LatencyRecord;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.exception.LatencyTrackerException;
import com.coinbase.latencytracker.metrics.LatencyMetrics;
import com.coinbase.latencytracker.repository.LatencyRecordRepository;
import com.coinbase.latencytracker.tracing.TracingService;
import com.coinbase.latencytracker.tracing.TracingService.TimedResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Core transaction processing pipeline.
 *
 * Each of the 6 stages is:
 *  1. Wrapped in a traced span
 *  2. Evaluated against its latency budget
 *  3. Persisted as a LatencyRecord
 *  4. Emits an AlertEvent if budget is exceeded
 *  5. Updates Micrometer metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private static final String EXCHANGE_RATE_KEY_PREFIX = "exchange:rate:";

    private final TracingService         tracingService;
    private final LatencyBudgetEngine    budgetEngine;
    private final AlertEventPublisher    alertPublisher;
    private final LatencyMetrics         latencyMetrics;
    private final LatencyRecordRepository recordRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public TransactionResponse process(TransactionRequest request) {
        String transactionId = UUID.randomUUID().toString();
        latencyMetrics.incrementTransactions();

        Map<Stage, Long>         latencies  = new EnumMap<>(Stage.class);
        Map<Stage, BudgetResult> budgets    = new EnumMap<>(Stage.class);
        List<LatencyRecord>      records    = new ArrayList<>();

        try {
            // ── STAGE 1: AUTHENTICATION ───────────────────────────────────
            TimedResult<String> authResult = tracingService.traceStage(
                    "latency.authentication", transactionId, null,
                    () -> authenticateUser(request.getUserId())
            );
            String traceId = authResult.traceId();
            BudgetResult authBudget = evaluateAndRecord(
                    Stage.AUTHENTICATION, authResult, transactionId, traceId,
                    records, latencies, budgets
            );
            if (authResult.failed()) {
                throw new LatencyTrackerException("Authentication failed", HttpStatus.UNAUTHORIZED.value());
            }

            String userId = authResult.value();

            // ── STAGE 2: VALIDATION ───────────────────────────────────────
            TimedResult<Void> validResult = tracingService.traceStage(
                    "latency.validation", transactionId, userId,
                    () -> { validateRequest(request); return null; }
            );
            evaluateAndRecord(Stage.VALIDATION, validResult, transactionId, traceId,
                    records, latencies, budgets);
            if (validResult.failed()) {
                throw new LatencyTrackerException("Validation failed: " + validResult.error().getMessage(),
                        HttpStatus.BAD_REQUEST.value());
            }

            // ── STAGE 3: BUSINESS LOGIC ───────────────────────────────────
            TimedResult<BigDecimal> rateResult = tracingService.traceStage(
                    "latency.business_logic", transactionId, userId,
                    () -> computeExchangeRate(request.getFromCurrency(), request.getToCurrency())
            );
            evaluateAndRecord(Stage.BUSINESS_LOGIC, rateResult, transactionId, traceId,
                    records, latencies, budgets);
            if (rateResult.failed()) {
                throw new LatencyTrackerException("Rate computation failed", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }

            // ── STAGE 4: CACHE ACCESS (Redis) ─────────────────────────────
            TimedResult<BigDecimal> cacheResult = tracingService.traceStage(
                    "latency.cache_access", transactionId, userId,
                    () -> fetchOrCacheRate(request.getFromCurrency(), request.getToCurrency(), rateResult.value())
            );
            evaluateAndRecord(Stage.CACHE_ACCESS, cacheResult, transactionId, traceId,
                    records, latencies, budgets);
            BigDecimal finalRate = cacheResult.success() ? cacheResult.value() : rateResult.value();

            // ── STAGE 5: DATABASE QUERY ───────────────────────────────────
            TimedResult<Void> dbResult = tracingService.traceStage(
                    "latency.database_query", transactionId, userId,
                    () -> { persistTransaction(transactionId, request, finalRate); return null; }
            );
            evaluateAndRecord(Stage.DATABASE_QUERY, dbResult, transactionId, traceId,
                    records, latencies, budgets);

            // ── STAGE 6: RESPONSE SERIALIZATION ──────────────────────────
            TimedResult<TransactionResponse> respResult = tracingService.traceStage(
                    "latency.response_serialization", transactionId, userId,
                    () -> buildResponse(transactionId, traceId, request, finalRate, latencies, budgets)
            );
            evaluateAndRecord(Stage.RESPONSE_SERIALIZATION, respResult, transactionId, traceId,
                    records, latencies, budgets);

            // Persist all latency records in batch
            recordRepository.saveAll(records);

            return respResult.value();

        } catch (LatencyTrackerException ex) {
            latencyMetrics.incrementErrors();
            recordRepository.saveAll(records); // persist what we have
            throw ex;
        } catch (Exception ex) {
            latencyMetrics.incrementErrors();
            recordRepository.saveAll(records);
            log.error("Unexpected error processing transaction {}: {}", transactionId, ex.getMessage(), ex);
            throw new LatencyTrackerException("Transaction processing failed", HttpStatus.INTERNAL_SERVER_ERROR.value(), ex);
        }
    }

    // ─── Stage implementations ──────────────────────────────────────────────

    private String authenticateUser(String userId) throws InterruptedException {
        // Simulate JWT validation / token introspection
        Thread.sleep(simulateLatency(5, 18));
        if (userId == null || userId.isBlank()) {
            throw new SecurityException("Invalid user ID");
        }
        return userId;
    }

    private void validateRequest(TransactionRequest req) throws InterruptedException {
        Thread.sleep(simulateLatency(2, 8));
        if (req.getFromCurrency().equals(req.getToCurrency())) {
            throw new IllegalArgumentException("fromCurrency and toCurrency must differ");
        }
        if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private BigDecimal computeExchangeRate(String from, String to) throws InterruptedException {
        Thread.sleep(simulateLatency(10, 45));
        // Deterministic pseudo-rate for demonstration
        int fromOrd = from.chars().sum();
        int toOrd   = to.chars().sum();
        double rate = (fromOrd == 0 ? 1 : (double) toOrd / fromOrd) * 1.0012;
        return BigDecimal.valueOf(rate).setScale(6, RoundingMode.HALF_UP);
    }

    @CircuitBreaker(name = "redis", fallbackMethod = "fetchRateFallback")
    private BigDecimal fetchOrCacheRate(String from, String to, BigDecimal rate)
            throws InterruptedException {
        Thread.sleep(simulateLatency(1, 9));
        String key = EXCHANGE_RATE_KEY_PREFIX + from + ":" + to;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return new BigDecimal(cached.toString());
        }
        redisTemplate.opsForValue().set(key, rate.toPlainString(), Duration.ofMinutes(5));
        return rate;
    }

    private BigDecimal fetchRateFallback(String from, String to, BigDecimal rate, Exception ex) {
        log.warn("Redis circuit open, using computed rate: {}", ex.getMessage());
        return rate;
    }

    @CircuitBreaker(name = "database", fallbackMethod = "persistFallback")
    private void persistTransaction(String txId, TransactionRequest req, BigDecimal rate)
            throws InterruptedException {
        Thread.sleep(simulateLatency(5, 35));
        // In a real system: save to transactions table. We persist latency records separately.
        log.debug("Transaction {} persisted to DB (rate={})", txId, rate);
    }

    private void persistFallback(String txId, TransactionRequest req, BigDecimal rate, Exception ex) {
        log.error("DB circuit open — transaction {} not persisted: {}", txId, ex.getMessage());
        throw new LatencyTrackerException("Database unavailable", HttpStatus.SERVICE_UNAVAILABLE.value(), ex);
    }

    private TransactionResponse buildResponse(String txId, String traceId, TransactionRequest req,
                                              BigDecimal rate, Map<Stage, Long> latencies,
                                              Map<Stage, BudgetResult> budgets) throws InterruptedException {
        Thread.sleep(simulateLatency(1, 4));

        BudgetSummary summary = budgetEngine.evaluateAll(latencies);

        List<StageLatency> stageLatencies = budgets.entrySet().stream()
                .map(e -> {
                    BudgetResult r = e.getValue();
                    return StageLatency.builder()
                            .stage(r.getStage().name())
                            .actualMs(r.getActualMs())
                            .budgetMs(r.getBudgetMs())
                            .exceeded(r.isExceeded())
                            .deviationPercent(r.getDeviationPercent())
                            .severity(r.getSeverity())
                            .build();
                })
                .toList();

        LatencyReport report = LatencyReport.builder()
                .totalLatencyMs(summary.totalActualMs())
                .budgetMs(summary.totalBudgetMs())
                .withinBudget(summary.withinBudget())
                .overallDeviationPercent(summary.overallDeviationPercent())
                .worstSeverity(summary.worstSeverity())
                .stages(stageLatencies)
                .build();

        BigDecimal converted = req.getAmount().multiply(rate).setScale(8, RoundingMode.HALF_UP);

        return TransactionResponse.builder()
                .transactionId(txId)
                .traceId(traceId)
                .success(true)
                .status("COMPLETED")
                .fromCurrency(req.getFromCurrency())
                .toCurrency(req.getToCurrency())
                .amount(req.getAmount())
                .convertedAmount(converted)
                .exchangeRate(rate)
                .latencyReport(report)
                .timestamp(Instant.now())
                .build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private BudgetResult evaluateAndRecord(Stage stage,
                                           TimedResult<?> timed,
                                           String transactionId,
                                           String traceId,
                                           List<LatencyRecord> records,
                                           Map<Stage, Long> latencies,
                                           Map<Stage, BudgetResult> budgets) {
        long latencyMs = timed.latencyMs();
        latencies.put(stage, latencyMs);

        BudgetResult result = budgetEngine.evaluate(stage, latencyMs);
        budgets.put(stage, result);

        // Update Micrometer
        latencyMetrics.recordStageLatency(stage, latencyMs, result.isExceeded(), result.getSeverity());
        latencyMetrics.updateDeviation(stage, result.getDeviationPercent());

        // Build persistence record
        LatencyRecord record = LatencyRecord.builder()
                .transactionId(transactionId)
                .traceId(traceId)
                .spanId(timed.spanId())
                .stage(stage)
                .actualLatencyMs(latencyMs)
                .budgetMs(result.getBudgetMs())
                .deviationPercent(result.getDeviationPercent())
                .severity(result.getSeverity())
                .budgetExceeded(result.isExceeded())
                .success(!timed.failed())
                .errorMessage(timed.failed() ? timed.error().getMessage() : null)
                .build();
        records.add(record);

        // Publish alert event if budget exceeded
        if (result.isExceeded()) {
            alertPublisher.publishBudgetViolation(transactionId, traceId, result);
        }

        return result;
    }

    /** Simulate realistic latency within [min, max] ms using a normal-ish distribution. */
    private long simulateLatency(long minMs, long maxMs) {
        return minMs + (long)(Math.random() * (maxMs - minMs));
    }
}
