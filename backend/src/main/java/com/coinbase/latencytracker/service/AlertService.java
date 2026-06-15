package com.coinbase.latencytracker.service;

import com.coinbase.latencytracker.dto.AlertDto;
import com.coinbase.latencytracker.entity.Alert;
import com.coinbase.latencytracker.exception.LatencyTrackerException;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public Page<AlertDto> getActiveAlerts(Pageable pageable) {
        return alertRepository.findByResolvedFalse(pageable).map(this::toDto);
    }

    public List<AlertDto> getCriticalAlerts() {
        return alertRepository.findCriticalActiveAlerts().stream().map(this::toDto).toList();
    }

    @Transactional
    public AlertDto resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new LatencyTrackerException("Alert not found: " + id, 404));
        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());
        return toDto(alertRepository.save(alert));
    }

    public long countActiveAlerts() {
        return alertRepository.countActiveAlertsSince(Instant.now().minusSeconds(3600));
    }

    private AlertDto toDto(Alert a) {
        return AlertDto.builder()
                .id(a.getId())
                .transactionId(a.getTransactionId())
                .traceId(a.getTraceId())
                .stage(a.getStage())
                .severity(a.getSeverity())
                .actualLatencyMs(a.getActualLatencyMs())
                .budgetMs(a.getBudgetMs())
                .deviationPercent(a.getDeviationPercent())
                .message(a.getMessage())
                .resolved(a.isResolved())
                .resolvedAt(a.getResolvedAt())
                .notificationSent(a.isNotificationSent())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
