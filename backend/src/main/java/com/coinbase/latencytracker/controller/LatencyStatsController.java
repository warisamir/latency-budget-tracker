package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.dto.LatencyStatsDto;
import com.coinbase.latencytracker.service.LatencyStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/latency")
@RequiredArgsConstructor
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
        return ResponseEntity.ok(statsService.getStats(window));
    }
}
