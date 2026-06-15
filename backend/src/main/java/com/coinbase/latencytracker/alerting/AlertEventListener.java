package com.coinbase.latencytracker.alerting;

import com.coinbase.latencytracker.budget.BudgetResult;
import com.coinbase.latencytracker.config.LatencyBudgetProperties;
import com.coinbase.latencytracker.entity.Alert;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Listens for AlertEvents and persists + dispatches notifications.
 *
 * Key behaviours:
 *  - Deduplication: skip if an active alert for the same stage was created
 *    within the cooldown window.
 *  - Async: does not block the request thread.
 *  - Notifications: currently logs (Slack/email hooks are stubs ready for wiring).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEventListener {

    private final AlertRepository            alertRepository;
    private final LatencyBudgetProperties    props;
    private final NotificationService        notificationService;

    @EventListener
    @Async
    @Transactional
    public void onAlertEvent(AlertEvent event) {
        Stage  stage    = event.getBudgetResult().getStage();
        long   cooldown = props.getAlerting().getCooldownMinutes();
        Instant since   = Instant.now().minusSeconds(cooldown * 60);

        // Deduplication check
        List<Alert> existing = alertRepository.findActiveAlertsForStageSince(stage, since);
        if (!existing.isEmpty()) {
            log.debug("Alert suppressed (cooldown) — stage={} cooldown={}min", stage, cooldown);
            return;
        }

        // Create alert record
        var result = event.getBudgetResult();
        Alert alert = Alert.builder()
                .transactionId(event.getTransactionId())
                .traceId(event.getTraceId())
                .stage(stage)
                .severity(result.getSeverity())
                .actualLatencyMs(result.getActualMs())
                .budgetMs(result.getBudgetMs())
                .deviationPercent(result.getDeviationPercent())
                .message(buildMessage(result))
                .resolved(false)
                .notificationSent(false)
                .build();

        alert = alertRepository.save(alert);

        // Fire notifications
        try {
            notificationService.sendAlert(alert);
            alert.setNotificationSent(true);
            alert.setNotificationChannel("slack");
            alertRepository.save(alert);
        } catch (Exception ex) {
            log.error("Notification failed for alert #{}: {}", alert.getId(), ex.getMessage());
        }
    }

    private String buildMessage(BudgetResult r) {
        return String.format(
            "[%s] Latency budget exceeded — actual=%dms budget=%dms deviation=%.1f%% severity=%s",
            r.getStage(), r.getActualMs(), r.getBudgetMs(), r.getDeviationPercent(), r.getSeverity()
        );
    }
}
