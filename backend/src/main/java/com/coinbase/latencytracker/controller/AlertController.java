package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.dto.AlertDto;
import com.coinbase.latencytracker.service.AlertService;
import com.coinbase.latencytracker.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Page<AlertDto>> getActiveAlerts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        long start = System.currentTimeMillis();
        LoggerUtil.debug(log, "Fetching active alerts - page={}, size={}", page, size);

        Page<AlertDto> alerts = alertService.getActiveAlerts(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        LoggerUtil.logPerformance(log, "getActiveAlerts", System.currentTimeMillis() - start);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/critical")
    public ResponseEntity<List<AlertDto>> getCriticalAlerts() {
        long start = System.currentTimeMillis();
        LoggerUtil.debug(log, "Fetching critical alerts");

        List<AlertDto> critical = alertService.getCriticalAlerts();

        LoggerUtil.logPerformance(log, "getCriticalAlerts", System.currentTimeMillis() - start);
        LoggerUtil.info(log, "Retrieved {} critical alerts", critical.size());
        return ResponseEntity.ok(critical);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countActiveAlerts() {
        LoggerUtil.debug(log, "Counting active alerts");
        long count = alertService.countActiveAlerts();
        LoggerUtil.info(log, "Active alerts count: {}", count);
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<AlertDto> resolveAlert(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        LoggerUtil.info(log, "Resolving alert - id={}", id);

        try {
            AlertDto resolved = alertService.resolveAlert(id);
            LoggerUtil.logPerformance(log, "resolveAlert", System.currentTimeMillis() - start);
            return ResponseEntity.ok(resolved);
        } catch (Exception e) {
            LoggerUtil.error(log, "Failed to resolve alert {}", e, id);
            throw e;
        }
    }
}
