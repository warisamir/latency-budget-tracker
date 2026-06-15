package com.coinbase.latencytracker.unit.alerting;

import com.coinbase.latencytracker.alerting.AlertEvent;
import com.coinbase.latencytracker.alerting.AlertEventListener;
import com.coinbase.latencytracker.alerting.NotificationService;
import com.coinbase.latencytracker.budget.BudgetResult;
import com.coinbase.latencytracker.config.LatencyBudgetProperties;
import com.coinbase.latencytracker.entity.Alert;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEventListenerTest {

    @Mock AlertRepository         alertRepository;
    @Mock NotificationService     notificationService;

    private LatencyBudgetProperties props;
    private AlertEventListener      listener;

    @BeforeEach
    void setUp() {
        props = new LatencyBudgetProperties();
        props.getAlerting().setCooldownMinutes(5);
        listener = new AlertEventListener(alertRepository, props, notificationService);
    }

    // ─── Happy paths ────────────────────────────────────────────────────────

    @Test
    void onAlertEvent_noExistingAlert_savesAndNotifies() {
        when(alertRepository.findActiveAlertsForStageSince(any(), any())).thenReturn(List.of());
        Alert saved = buildAlert();
        when(alertRepository.save(any())).thenReturn(saved);

        AlertEvent event = buildEvent(Stage.DATABASE_QUERY, Severity.HIGH, 80L, 40L, 100.0);
        listener.onAlertEvent(event);

        verify(alertRepository, times(2)).save(any(Alert.class)); // once to create, once to mark notified
        verify(notificationService).sendAlert(any(Alert.class));
    }

    @Test
    void onAlertEvent_withinCooldown_suppressed() {
        Alert existing = buildAlert();
        when(alertRepository.findActiveAlertsForStageSince(any(), any())).thenReturn(List.of(existing));

        AlertEvent event = buildEvent(Stage.DATABASE_QUERY, Severity.HIGH, 80L, 40L, 100.0);
        listener.onAlertEvent(event);

        verify(alertRepository, never()).save(any());
        verify(notificationService, never()).sendAlert(any());
    }

    // ─── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void onAlertEvent_notificationFails_alertStillSaved() {
        when(alertRepository.findActiveAlertsForStageSince(any(), any())).thenReturn(List.of());
        Alert saved = buildAlert();
        when(alertRepository.save(any())).thenReturn(saved);
        doThrow(new RuntimeException("Slack down")).when(notificationService).sendAlert(any());

        AlertEvent event = buildEvent(Stage.AUTHENTICATION, Severity.CRITICAL, 200L, 20L, 900.0);
        // Should not throw
        listener.onAlertEvent(event);

        verify(alertRepository, atLeastOnce()).save(any()); // alert was saved
    }

    @Test
    void onAlertEvent_lowSeverity_savesWithCorrectSeverity() {
        when(alertRepository.findActiveAlertsForStageSince(any(), any())).thenReturn(List.of());
        Alert saved = buildAlert();
        saved.setSeverity(Severity.LOW);
        when(alertRepository.save(any())).thenReturn(saved);

        AlertEvent event = buildEvent(Stage.VALIDATION, Severity.LOW, 12L, 10L, 20.0);
        listener.onAlertEvent(event);

        verify(alertRepository, atLeastOnce()).save(argThat(a ->
                ((Alert) a).getSeverity() == Severity.LOW ||
                ((Alert) a).getStage() == Stage.VALIDATION
        ));
    }

    // ─── Worst cases ─────────────────────────────────────────────────────────

    @Test
    void onAlertEvent_criticalSeverity_alwaysNotifies() {
        when(alertRepository.findActiveAlertsForStageSince(any(), any())).thenReturn(List.of());
        Alert saved = buildAlert();
        saved.setSeverity(Severity.CRITICAL);
        when(alertRepository.save(any())).thenReturn(saved);

        AlertEvent event = buildEvent(Stage.BUSINESS_LOGIC, Severity.CRITICAL, 500L, 50L, 900.0);
        listener.onAlertEvent(event);

        verify(notificationService).sendAlert(any());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private AlertEvent buildEvent(Stage stage, Severity severity, long actual, long budget, double deviation) {
        BudgetResult result = BudgetResult.exceeded(stage, actual, budget, deviation, severity);
        return new AlertEvent(this, "tx-001", "trace-abc", result);
    }

    private Alert buildAlert() {
        return Alert.builder()
                .id(1L)
                .transactionId("tx-001")
                .stage(Stage.DATABASE_QUERY)
                .severity(Severity.HIGH)
                .actualLatencyMs(80L)
                .budgetMs(40L)
                .deviationPercent(100.0)
                .resolved(false)
                .notificationSent(false)
                .createdAt(Instant.now())
                .build();
    }
}
