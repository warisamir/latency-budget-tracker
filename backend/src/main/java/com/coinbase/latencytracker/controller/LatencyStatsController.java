package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.dto.LatencyStatsDto;
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
}
