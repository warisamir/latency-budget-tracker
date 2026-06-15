package com.coinbase.latencytracker.unit.service;

import com.coinbase.latencytracker.dto.AlertDto;
import com.coinbase.latencytracker.entity.Alert;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.exception.LatencyTrackerException;
import com.coinbase.latencytracker.repository.AlertRepository;
import com.coinbase.latencytracker.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock AlertRepository  alertRepository;

    AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(alertRepository);
    }

    // ─── Happy paths ────────────────────────────────────────────────────────

    @Test
    void getActiveAlerts_returnsMappedDtos() {
        Alert alert = buildAlert(1L, false);
        when(alertRepository.findByResolvedFalse(any())).thenReturn(new PageImpl<>(List.of(alert)));

        Page<AlertDto> result = service.getActiveAlerts(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        AlertDto dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStage()).isEqualTo(Stage.DATABASE_QUERY);
        assertThat(dto.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(dto.isResolved()).isFalse();
    }

    @Test
    void resolveAlert_setsResolvedTrueAndTimestamp() {
        Alert alert = buildAlert(42L, false);
        when(alertRepository.findById(42L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertDto result = service.resolveAlert(42L);

        assertThat(result.isResolved()).isTrue();
        assertThat(result.getResolvedAt()).isNotNull();
        verify(alertRepository).save(argThat(a -> a.isResolved() && a.getResolvedAt() != null));
    }

    @Test
    void getCriticalAlerts_returnsCorrectList() {
        Alert critical1 = buildAlert(1L, false);
        critical1.setSeverity(Severity.CRITICAL);
        when(alertRepository.findCriticalActiveAlerts()).thenReturn(List.of(critical1));

        List<AlertDto> result = service.getCriticalAlerts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
    }

    // ─── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void getActiveAlerts_emptyList_returnsEmptyPage() {
        when(alertRepository.findByResolvedFalse(any())).thenReturn(Page.empty());
        Page<AlertDto> result = service.getActiveAlerts(PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void resolveAlert_alreadyResolved_canStillResolveAgain() {
        Alert alert = buildAlert(99L, true); // already resolved
        alert.setResolvedAt(Instant.now().minusSeconds(60));
        when(alertRepository.findById(99L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertDto result = service.resolveAlert(99L); // idempotent
        assertThat(result.isResolved()).isTrue();
    }

    // ─── Worst cases ─────────────────────────────────────────────────────────

    @Test
    void resolveAlert_notFound_throws404() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAlert(999L))
                .isInstanceOf(LatencyTrackerException.class)
                .hasMessageContaining("Alert not found")
                .extracting(e -> ((LatencyTrackerException) e).getStatusCode())
                .isEqualTo(404);
    }

    @Test
    void getCriticalAlerts_repositoryThrows_propagatesException() {
        when(alertRepository.findCriticalActiveAlerts()).thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> service.getCriticalAlerts())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection lost");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Alert buildAlert(Long id, boolean resolved) {
        return Alert.builder()
                .id(id)
                .transactionId("tx-" + id)
                .traceId("trace-" + id)
                .stage(Stage.DATABASE_QUERY)
                .severity(Severity.HIGH)
                .actualLatencyMs(80L)
                .budgetMs(40L)
                .deviationPercent(100.0)
                .message("Test alert")
                .resolved(resolved)
                .notificationSent(true)
                .createdAt(Instant.now())
                .build();
    }
}
