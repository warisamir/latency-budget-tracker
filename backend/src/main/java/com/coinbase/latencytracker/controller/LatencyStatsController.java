package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.dto.LatencyStatsDto;
import com.coinbase.latencytracker.dto.LatencyHistoryDto;
import com.coinbase.latencytracker.service.LatencyStatsService;
import com.coinbase.latencytracker.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/latency")
@RequiredArgsConstructor
@Slf4j
public class LatencyStatsController {

    private final LatencyStatsService statsService;

    /**
     * GET /api/v1/latency/stats?window=1h|24h|7d
     *
     * Returns P50/P95/P99 per stage, violation rates, and budget compliance.
     */
    @GetMapping("/stats")
    public ResponseEntity<LatencyStatsDto> getStats(
            @RequestParam(defaultValue = "1h") String window) {
        long start = System.currentTimeMillis();
        LoggerUtil.debug(log, "Fetching latency stats - window={}", window);

        try {
            LatencyStatsDto stats = statsService.getStats(window);
            LoggerUtil.logPerformance(log, "getStats", System.currentTimeMillis() - start);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            LoggerUtil.error(log, "Failed to fetch stats for window={}", e, window);
            throw e;
        }
    }

    /**
     * GET /api/v1/latency/history?window=1h|24h|7d&buckets=12
     *
     * Returns time-bucketed latency history for trend charts.
     */
    @GetMapping("/history")
    public ResponseEntity<LatencyHistoryDto> getHistory(
            @RequestParam(defaultValue = "24h") String window,
            @RequestParam(defaultValue = "24") int buckets) {
        long start = System.currentTimeMillis();
        LoggerUtil.debug(log, "Fetching latency history - window={}, buckets={}", window, buckets);

        try {
            LatencyHistoryDto history = statsService.getHistory(window, buckets);
            LoggerUtil.logPerformance(log, "getHistory", System.currentTimeMillis() - start);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            LoggerUtil.error(log, "Failed to fetch history for window={}", e, window);
            throw e;
        }
    }
}
