package com.coinbase.latencytracker.alerting;

import com.coinbase.latencytracker.budget.BudgetResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around ApplicationEventPublisher for type-safe alert publishing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishBudgetViolation(String transactionId, String traceId, BudgetResult result) {
        log.debug("Publishing AlertEvent for stage={} severity={}", result.getStage(), result.getSeverity());
        publisher.publishEvent(new AlertEvent(this, transactionId, traceId, result));
    }
}
