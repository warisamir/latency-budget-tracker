package com.coinbase.latencytracker.service;

import com.coinbase.latencytracker.dto.AlertDto;
import com.coinbase.latencytracker.entity.Alert;
import com.coinbase.latencytracker.exception.LatencyTrackerException;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.repository.AlertRepository;
import com.coinbase.latencytracker.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;

    public Page<AlertDto> getActiveAlerts(Pageable pageable) {
        LoggerUtil.debug(log, "Fetching active alerts with pagination: {}", pageable);
        Page<AlertDto> result = alertRepository.findByResolvedFalse(pageable).map(this::toDto);
        LoggerUtil.debug(log, "Retrieved {} active alerts (page {} of {})",
                result.getNumberOfElements(), result.getNumber(), result.getTotalPages());
        return result;
    }

    public List<AlertDto> getCriticalAlerts() {
        LoggerUtil.debug(log, "Fetching critical alerts");
        List<AlertDto> critical = alertRepository.findCriticalActiveAlerts()
                .stream()
                .map(this::toDto)
                .toList();
        LoggerUtil.info(log, "Retrieved {} critical alerts", critical.size());
        return critical;
    }

    @Transactional
    public AlertDto resolveAlert(Long id) {
        LoggerUtil.debug(log, "Attempting to resolve alert: {}", id);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.warn(log, "Alert not found for resolution: {}", id);
                    return new LatencyTrackerException("Alert not found: " + id, 404);
                });

        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());
        AlertDto resolved = toDto(alertRepository.save(alert));

        LoggerUtil.info(log, "Successfully resolved alert {} (stage={})", id, alert.getStage());
        return resolved;
    }

    public long countActiveAlerts() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        long count = alertRepository.countActiveAlertsSince(oneHourAgo);
        LoggerUtil.debug(log, "Active alerts in last hour: {}", count);
        return count;
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
